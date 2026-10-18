package top.likoslupus.cellulosesz.application.backup

import org.slf4j.LoggerFactory
import top.likoslupus.cellulosesz.application.config.BackupSettings
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.*

/** The single outcome of a backup attempt. Expected failures are values, not exceptions. */
internal sealed interface BackupOutcome {

    data object Disabled : BackupOutcome

    data object NotConfigured : BackupOutcome

    data object AlreadyRunning : BackupOutcome

    data class Completed(
        val exitCode: Int,
        val output: String
    ) : BackupOutcome

    data object TimedOut : BackupOutcome

    data class Failed(val message: String) : BackupOutcome

}

/**
 * Runs the configured external backup command. The process is started directly from an argument
 * vector (no shell), off the server thread, with a hard timeout; at most one run is active at a
 * time. The periodic schedule lives on the runtime's coroutine scope and is cancelled with it.
 */
internal class BackupService(
    private val kernel: RuntimeKernel,
    private val settings: () -> BackupSettings,
    private val onlineCount: () -> Int,
) {

    private val logger = LoggerFactory.getLogger(BackupService::class.java)
    private val running = AtomicBoolean(false)
    private var schedule: Job? = null

    fun onServerStarted() {
        schedule()
    }

    fun onServerStopping() {
        schedule?.cancel()
        schedule = null
    }

    /** Re-arms the schedule after a config reload. A run already in flight is left alone. */
    fun reload() {
        onServerStopping()
        onServerStarted()
    }

    private fun schedule() {
        val config = settings()
        if (!config.enabled
            || config.command.isEmpty()
            || config.intervalMinutes <= 0
        ) {
            return
        }
        schedule = kernel.launch {
            while (isActive) {
                delay((config.intervalMinutes * 60_000L).milliseconds)
                if (config.alwaysRun || onlineCount() > 0) {
                    runBackup("scheduler")
                }
            }
        }
    }

    suspend fun runBackup(trigger: String): BackupOutcome =
        settings().let { config ->
            when {
                !config.enabled ->
                    BackupOutcome.Disabled

                config.command.isEmpty() ->
                    BackupOutcome.NotConfigured

                !running.compareAndSet(false, true) ->
                    BackupOutcome.AlreadyRunning

                else -> return try {
                    withContext(Dispatchers.IO) {
                        logger.info("backup_started trigger={} command={}", trigger, config.command)
                        val process = ProcessBuilder(config.command)
                                .redirectErrorStream(true)
                                .start()
                        val finished = process.waitFor(
                            config.timeoutSeconds.toLong(),
                            TimeUnit.SECONDS,
                        )
                        when {
                            !finished -> {
                                process.destroyForcibly()
                                logger.warn("backup_timeout trigger={}", trigger)
                                BackupOutcome.TimedOut
                            }

                            else -> {
                                val output = process.inputStream.bufferedReader()
                                        .use { it.readText() }
                                val exitCode = process.exitValue()
                                logger.info("backup_finished trigger={} exit={}", trigger, exitCode)
                                BackupOutcome.Completed(
                                    exitCode = exitCode,
                                    output = output.takeLast(config.maxOutputChars.coerceAtLeast(0)),
                                )
                            }
                        }
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (exception: Exception) {
                    logger.error("backup_failed trigger={}", trigger, exception)
                    BackupOutcome.Failed(exception.message ?: exception.javaClass.simpleName)
                } finally {
                    running.set(false)
                }
            }
        }

}
