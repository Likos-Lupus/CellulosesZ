package top.likoslupus.cellulosesz.administration.jail

import com.mojang.logging.LogUtils
import top.likoslupus.cellulosesz.administration.config.JailSettings
import top.likoslupus.cellulosesz.core.player.WorldPosition
import java.time.Clock
import java.time.Instant
import java.util.*
import kotlinx.coroutines.CancellationException

/**
 * Drives every jail movement: the confinement transfer of a fresh sentence, the re-anchor after an
 * escape, and the return on release. At most one transfer per player is in flight at a time.
 *
 * The sentence callbacks ([confined], [markReleasePending], [forget]) read and mutate the jail
 * service's server-thread-confined maps; the coordinator invokes them inside `onServerThread`
 * blocks. Storage calls happen off the server thread.
 */
internal class JailTransferCoordinator(
    private val runtime: JailRuntime,
    private val transfer: () -> JailTransferPort,
    private val sentences: JailSentenceRepository,
    private val settings: () -> JailSettings,
    private val clock: Clock,
    private val confined: (UUID) -> JailSentence?,
    private val markReleasePending: (JailSentence, Instant) -> Unit,
    private val forget: (UUID) -> Unit,
) {

    private val inFlight = HashSet<UUID>()

    /** Runs a transfer on the caller's stack and reports whether it was applied. */
    suspend fun runTransfer(
        playerId: UUID,
        location: JailLocation,
        kind: JailTransferKind,
    ): Boolean {
        val acquired = runtime.onServerThread { inFlight.add(playerId) }
        return acquired && try {
            val result = transfer().transfer(playerId, location.toDestination(), kind)
            result == JailTransferResult.Transferred
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error("failed to transfer player {} into jail", playerId, exception)
            false
        } finally {
            runtime.onServerThread { inFlight.remove(playerId) }
        }
    }

    fun scheduleTransfer(
        playerId: UUID,
        location: JailLocation,
        kind: JailTransferKind,
    ) {
        if (!inFlight.add(playerId)) {
            return
        }

        runtime.launch {
            try {
                transfer().transfer(playerId, location.toDestination(), kind)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                LOGGER.error("failed to re-anchor jailed player {}", playerId, exception)
            } finally {
                runtime.onServerThread { inFlight.remove(playerId) }
            }
        }
    }

    fun scheduleRelease(playerId: UUID) {
        if (!inFlight.add(playerId)) {
            return
        }

        runtime.launch {
            try {
                val sentence = runtime.onServerThread { confined(playerId) }
                    ?: return@launch
                releasePlayer(sentence)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                LOGGER.error("failed to release jailed player {}", playerId, exception)
            } finally {
                runtime.onServerThread { inFlight.remove(playerId) }
            }
        }
    }

    /**
     * Marks an expired active sentence release-pending, then drives the return transfer. Permanent
     * sentences (no [JailSentence.expiresAt]) never take this path.
     */
    fun scheduleExpiryRelease(playerId: UUID) {
        if (!inFlight.add(playerId)) {
            return
        }

        runtime.launch {
            try {
                val current = runtime.onServerThread { confined(playerId) }
                    ?: return@launch
                if (current.status != JailStatus.ACTIVE) {
                    return@launch
                }
                val now = clock.instant()
                val marked = sentences.updateStatus(
                    playerId,
                    JailStatus.RELEASE_PENDING,
                    now,
                )
                if (!marked) {
                    return@launch
                }
                runtime.onServerThread { markReleasePending(current, now) }
                releasePlayer(current)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                LOGGER.error("failed to expire jail sentence for {}", playerId, exception)
            } finally {
                runtime.onServerThread { inFlight.remove(playerId) }
            }
        }
    }

    /** Returns a release-pending player, then deletes the sentence. Must run off the server thread. */
    private suspend fun releasePlayer(sentence: JailSentence) {
        val playerId = sentence.playerId
        if (settings().returnOnRelease) {
            val result = releaseDestination(sentence)
            if (result != JailTransferResult.Transferred) {
                return
            }
        }
        val removed = sentences.remove(playerId)
        if (removed) {
            runtime.onServerThread { forget(playerId) }
        }
    }

    private suspend fun releaseDestination(sentence: JailSentence): JailTransferResult {
        val returnPosition = sentence.returnPosition
        return if (returnPosition != null) {
            transfer().transfer(
                sentence.playerId,
                returnPosition.toDestination(),
                JailTransferKind.RELEASE,
            )
        } else {
            transfer().release(sentence.playerId)
        }
    }

    private fun JailLocation.toDestination(): JailDestination =
        JailDestination(
            dimension = dimension,
            x = x,
            y = y,
            z = z,
            yaw = yaw,
            pitch = pitch,
        )

    private fun WorldPosition.toDestination(): JailDestination =
        JailDestination(
            dimension = dimension,
            x = x,
            y = y,
            z = z,
            yaw = yaw,
            pitch = pitch,
        )

    private companion object {

        private val LOGGER = LogUtils.getLogger()

    }

}
