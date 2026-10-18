package top.likoslupus.cellulosesz.utility.powertool

import com.mojang.logging.LogUtils
import top.likoslupus.cellulosesz.core.player.PlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.runtime.ServerThreadRunner
import top.likoslupus.cellulosesz.utility.config.PowertoolSettings
import java.time.Instant
import java.util.*
import kotlinx.coroutines.CancellationException

internal sealed interface PowertoolBindResult {

    data class Bound(
        val itemId: String,
        val trigger: PowertoolTrigger,
        val command: String,
    ) : PowertoolBindResult

    data class Cleared(val itemId: String) : PowertoolBindResult

    data object NoItemId : PowertoolBindResult

    data object Recursive : PowertoolBindResult

    data object InvalidCommand : PowertoolBindResult

    data object StorageUnavailable : PowertoolBindResult

}

/**
 * Powertool bindings and the interaction dispatcher. The binding set is cached in memory (hydrated at
 * startup and updated on every mutation) so the interaction path never touches the database or
 * suspends. [dispatch] runs on the server thread, called by the packet mixin.
 */
internal class PowertoolService(
    private val runner: ServerThreadRunner,
    private val repository: PowertoolRepository,
    private val kernel: RuntimeKernel,
    private val settings: () -> PowertoolSettings,
    private val clock: () -> Instant = Instant::now,
) {

    private data class Key(
        val itemId: String,
        val trigger: PowertoolTrigger,
    )

    private val bindings = HashMap<UUID, MutableMap<Key, String>>()
    private val disabled = HashSet<UUID>()
    private val lastFire = HashMap<UUID, Instant>()

    suspend fun load() {
        val loaded = try {
            repository.loadAll()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error("failed to load powertools", exception)
            return
        }
        runner.run {
            bindings.clear()
            loaded.forEach { (playerId, entries) ->
                val map = HashMap<Key, String>()
                entries.forEach { map[Key(it.itemId, it.trigger)] = it.command }
                bindings[playerId] = map
            }
        }
    }

    fun shutdown() {
        bindings.clear()
        disabled.clear()
        lastFire.clear()
    }

    suspend fun bind(
        playerId: UUID,
        itemId: String?,
        spec: PowertoolSpec,
    ): PowertoolBindResult {
        val id = itemId?.takeIf { it.isNotBlank() }
            ?: return PowertoolBindResult.NoItemId
        return when (spec) {
            PowertoolSpec.Clear -> clear(playerId, id)
            PowertoolSpec.Recursive -> PowertoolBindResult.Recursive
            PowertoolSpec.Invalid -> PowertoolBindResult.InvalidCommand
            is PowertoolSpec.Bound -> store(playerId, id, spec)
        }
    }

    private suspend fun store(
        playerId: UUID,
        itemId: String,
        spec: PowertoolSpec.Bound,
    ): PowertoolBindResult {
        val binding = PowertoolBinding(itemId, spec.trigger, spec.command)
        return try {
            repository.bind(playerId, binding, clock())
            runner.run {
                bindings.getOrPut(playerId) { HashMap() }[Key(itemId, spec.trigger)] = spec.command
            }
            PowertoolBindResult.Bound(itemId, spec.trigger, spec.command)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error("failed to store powertool for {}", playerId, exception)
            PowertoolBindResult.StorageUnavailable
        }
    }

    private suspend fun clear(playerId: UUID, itemId: String): PowertoolBindResult =
        try {
            repository.removeAll(playerId, itemId)
            runner.run {
                bindings[playerId]?.entries?.removeIf { it.key.itemId == itemId }
            }
            PowertoolBindResult.Cleared(itemId)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error("failed to clear powertool for {}", playerId, exception)
            PowertoolBindResult.StorageUnavailable
        }

    suspend fun list(playerId: UUID): List<PowertoolBinding> =
        runner.run {
            bindings[playerId]
                    ?.map { (key, command) -> PowertoolBinding(key.itemId, key.trigger, command) }
                    ?.sortedWith(compareBy({ it.itemId }, { it.trigger.name }))
                ?: emptyList()
        }

    /** Flips the player's powertool switch; returns the new enabled state. */
    suspend fun toggle(playerId: UUID): Boolean =
        runner.run {
            when (disabled.remove(playerId)) {
                true -> true
                false -> {
                    disabled += playerId
                    false
                }
            }
        }

    fun isEnabled(playerId: UUID): Boolean =
        playerId !in disabled

    fun enabled(): Boolean =
        settings().enabled

    fun forget(playerId: UUID) {
        disabled.remove(playerId)
        lastFire.remove(playerId)
    }

    /**
     * Runs the bound command for one interaction, returning true when a powertool fired (so the
     * caller cancels the interaction). Recursion into a powertool command is rejected, and each
     * player is throttled to one firing per `throttleSeconds`.
     */
    fun dispatch(
        playerId: UUID,
        itemId: String,
        trigger: PowertoolTrigger,
    ): Boolean {
        val config = settings()
        if (!config.enabled) {
            return false
        }
        if (playerId in disabled) {
            return false
        }
        val command = lookup(playerId, itemId, trigger)
            ?: return false
        if (PowertoolSpec.isRecursive(command)) {
            return false
        }
        val now = clock()
        val previous = lastFire[playerId]
        if (previous != null
            && now.isBefore(previous.plusSeconds(config.throttleSeconds.toLong()))
        ) {
            return false
        }
        val ran = runAs(playerId, command)
        if (ran) {
            lastFire[playerId] = now
        }
        return ran
    }

    private fun lookup(
        playerId: UUID,
        itemId: String,
        trigger: PowertoolTrigger,
    ): String? {
        val map = bindings[playerId] ?: return null
        return map[Key(itemId, trigger)]
            ?: map[Key(itemId, PowertoolTrigger.BOTH)]
    }

    private fun runAs(playerId: UUID, command: String): Boolean {
        val server = kernel.requireServer()
        val player = PlayerResolver.onlineById(server, playerId)
            ?: return false
        val source = player.createCommandSourceStack()
        return runCatching {
            val parsed = server.commands.dispatcher.parse(command, source)
            server.commands.performCommand(parsed, command)
        }.fold(
            onSuccess = { true },
            onFailure = { exception ->
                LOGGER.warn("powertool command failed for {}", playerId, exception)
                false
            },
        )
    }

    private companion object {

        private val LOGGER = LogUtils.getLogger()

    }

}
