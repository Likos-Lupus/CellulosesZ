package top.likoslupus.cellulosesz.communication.preferences

import com.mojang.logging.LogUtils
import kotlinx.coroutines.CancellationException
import top.likoslupus.cellulosesz.communication.config.PrivateMessageSettings
import top.likoslupus.cellulosesz.communication.config.ReplyMode
import top.likoslupus.cellulosesz.communication.messaging.ReceiveDecision
import top.likoslupus.cellulosesz.communication.messaging.RecipientPolicy
import top.likoslupus.cellulosesz.core.runtime.ServerThreadRunner
import java.util.*

internal sealed interface PreferenceUpdateResult {

    data class Updated(val preferences: MessagingPreferences) : PreferenceUpdateResult
    data object AlreadySet : PreferenceUpdateResult
    data object StorageUnavailable : PreferenceUpdateResult
    data object RuntimeStopping : PreferenceUpdateResult

}

internal sealed interface IgnoreUpdateResult {

    data class Updated(val preferences: MessagingPreferences) : IgnoreUpdateResult
    data object AlreadyIgnored : IgnoreUpdateResult
    data object NotIgnored : IgnoreUpdateResult
    data object IgnoreListFull : IgnoreUpdateResult
    data object StorageUnavailable : IgnoreUpdateResult
    data object RuntimeStopping : IgnoreUpdateResult

}

/**
 * Owns durable per-player preferences and the server-thread online cache. Durable changes are
 * published only after the atomic file write succeeds (write-before-publish), so a failed save can
 * never leave the session pretending the change persisted.
 */
internal class MessagingPreferencesService(
    private val runner: ServerThreadRunner,
    private val repository: MessagingPreferencesRepository,
    private val settings: () -> PrivateMessageSettings,
) : RecipientPolicy {

    private val online = HashMap<UUID, PreferenceState>()

    @Volatile private var stopping = false

    /** Server-thread confined; marks the player as loading before the async read begins. */
    fun beginLoad(playerId: UUID) {
        online[playerId] = PreferenceState.Loading
    }

    suspend fun load(playerId: UUID) {
        val state = try {
            val loaded = repository.load(playerId)
            PreferenceState.Ready(loaded ?: MessagingPreferences())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error(
                "failed to load communication preferences for {}",
                playerId,
                exception,
            )
            PreferenceState.Failed(exception.message ?: "unreadable preferences")
        }

        if (!stopping) {
            runner.run { online[playerId] = state }
        }
    }

    override fun canReceiveFrom(
        recipientId: UUID,
        senderId: UUID,
    ): ReceiveDecision =
        when (val state = online[recipientId]) {
            null, PreferenceState.Loading -> ReceiveDecision.LOADING
            is PreferenceState.Failed -> ReceiveDecision.DENIED
            is PreferenceState.Ready -> {
                val value = state.value
                when {
                    !value.receivePrivateMessages -> ReceiveDecision.DENIED
                    senderId in value.ignoredPlayerIds -> ReceiveDecision.DENIED
                    else -> ReceiveDecision.ALLOWED
                }
            }
        }

    override fun replyMode(playerId: UUID): ReplyMode =
        when (val state = online[playerId]) {
            is PreferenceState.Ready ->
                state.value.replyMode
                    ?: settings().defaultReplyMode

            else -> settings().defaultReplyMode
        }

    /** Server-thread confined snapshot used by command feedback. */
    fun current(playerId: UUID): MessagingPreferences? =
        (online[playerId] as? PreferenceState.Ready)?.value

    suspend fun setReceivePrivateMessages(
        playerId: UUID,
        playerName: String,
        enabled: Boolean,
    ): PreferenceUpdateResult {
        if (stopping) {
            return PreferenceUpdateResult.RuntimeStopping
        }

        val current = runner.run { current(playerId) }
            ?: return PreferenceUpdateResult.StorageUnavailable
        if (current.receivePrivateMessages == enabled) {
            return PreferenceUpdateResult.AlreadySet
        }

        val updated = current.copy(receivePrivateMessages = enabled)
        if (!persist(playerId, playerName, updated)) {
            return PreferenceUpdateResult.StorageUnavailable
        }

        runner.run { online[playerId] = PreferenceState.Ready(updated) }
        return PreferenceUpdateResult.Updated(updated)
    }

    suspend fun ignorePlayer(
        playerId: UUID,
        playerName: String,
        targetId: UUID,
    ): IgnoreUpdateResult {
        if (stopping) {
            return IgnoreUpdateResult.RuntimeStopping
        }

        val current = runner.run { current(playerId) }
            ?: return IgnoreUpdateResult.StorageUnavailable
        if (targetId in current.ignoredPlayerIds) {
            return IgnoreUpdateResult.AlreadyIgnored
        }
        if (current.ignoredPlayerIds.size >= settings().maxIgnoredPlayers) {
            return IgnoreUpdateResult.IgnoreListFull
        }

        val updated = current.copy(ignoredPlayerIds = current.ignoredPlayerIds + targetId)
        if (!persist(playerId, playerName, updated)) {
            return IgnoreUpdateResult.StorageUnavailable
        }

        runner.run { online[playerId] = PreferenceState.Ready(updated) }
        return IgnoreUpdateResult.Updated(updated)
    }

    suspend fun unignorePlayer(
        playerId: UUID,
        playerName: String,
        targetId: UUID,
    ): IgnoreUpdateResult {
        if (stopping) {
            return IgnoreUpdateResult.RuntimeStopping
        }

        val current = runner.run { current(playerId) }
            ?: return IgnoreUpdateResult.StorageUnavailable
        if (targetId !in current.ignoredPlayerIds) {
            return IgnoreUpdateResult.NotIgnored
        }

        val updated = current.copy(ignoredPlayerIds = current.ignoredPlayerIds - targetId)
        if (!persist(playerId, playerName, updated)) {
            return IgnoreUpdateResult.StorageUnavailable
        }

        runner.run { online[playerId] = PreferenceState.Ready(updated) }
        return IgnoreUpdateResult.Updated(updated)
    }

    fun clear(playerId: UUID) {
        online.remove(playerId)
    }

    fun shutdown() {
        stopping = true
        online.clear()
    }

    private suspend fun persist(
        playerId: UUID,
        playerName: String,
        preferences: MessagingPreferences,
    ): Boolean =
        try {
            repository.save(playerId, playerName, preferences)
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error(
                "failed to persist communication preferences for {}",
                playerId,
                exception,
            )
            false
        }

    private companion object {

        private val LOGGER = LogUtils.getLogger()

    }

}
