package top.likoslupus.cellulosesz.player.nickname

import com.mojang.logging.LogUtils
import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.player.identity.PlayerIdentityRepository
import top.likoslupus.cellulosesz.core.runtime.ServerThreadRunner
import top.likoslupus.cellulosesz.player.config.NicknameSettings
import java.time.Clock
import java.util.*
import kotlinx.coroutines.CancellationException

/** The outcome of a `/nick` set or clear. Expected failures are typed, never exceptions. */
internal sealed interface NicknameSetResult {

    data class Set(val display: String) : NicknameSetResult

    data object Cleared : NicknameSetResult

    data object Disabled : NicknameSetResult

    data object Empty : NicknameSetResult

    data class TooShort(val minimum: Int) : NicknameSetResult

    data class TooLong(val maximum: Int) : NicknameSetResult

    data object InvalidCharacters : NicknameSetResult

    data object ColorsNotAllowed : NicknameSetResult

    data object PatternMismatch : NicknameSetResult

    data object Blacklisted : NicknameSetResult

    data object Taken : NicknameSetResult

    data object Reserved : NicknameSetResult

    data object StorageUnavailable : NicknameSetResult

}

/** The outcome of a `/realname` resolution. */
internal sealed interface NicknameLookup {

    data class Found(val playerId: UUID, val actualName: String) : NicknameLookup

    data object NotFound : NicknameLookup

    data object StorageUnavailable : NicknameLookup

}

/** The caller's own nickname, for the bare `/nick` status line. */
internal sealed interface NicknameCurrent {

    data class Set(val display: String) : NicknameCurrent

    data object None : NicknameCurrent

    data object StorageUnavailable : NicknameCurrent

}

/**
 * The outcome of a staff `<setfor|clearfor>` nickname change. The underlying [NicknameSetResult] is
 * reused so validation failures present identically whether the target is self or another player.
 */
internal sealed interface NicknameForResult {

    data class Applied(
        val targetName: String,
        val result: NicknameSetResult,
    ) : NicknameForResult

    data object TargetNotFound : NicknameForResult

}

/**
 * Owns nickname validation, uniqueness and resolution. The database unique index is the backstop;
 * the service also rejects a colliding or reserved nickname up front so the player gets a clear
 * result instead of a storage error.
 */
internal class NicknameService(
    private val runner: ServerThreadRunner,
    private val known: KnownPlayerResolver,
    private val identities: PlayerIdentityRepository,
    private val repository: NicknameRepository,
    private val settings: () -> NicknameSettings,
    private val clock: Clock,
) {

    /**
     * [colorsAllowed] is the composition of the `nicknames.allowColors` toggle and the actor's
     * `NICK_COLORS` capability, resolved by the command so the service stays permission-free.
     */
    suspend fun set(
        playerId: UUID,
        raw: String,
        updatedBy: UUID? = null,
        colorsAllowed: Boolean = false,
    ): NicknameSetResult {
        val config = settings()
        if (!config.enabled) {
            return NicknameSetResult.Disabled
        }

        val nickname = when (
            val parsed = Nickname.parse(
                raw,
                config,
                colorsAllowed && config.allowColors
            )
        ) {
            is NicknameParse.Valid ->
                parsed.nickname

            NicknameParse.Empty ->
                return NicknameSetResult.Empty

            NicknameParse.TooShort ->
                return NicknameSetResult.TooShort(config.minimumVisibleLength)

            NicknameParse.TooLong ->
                return NicknameSetResult.TooLong(config.maximumVisibleLength)

            NicknameParse.InvalidCharacters ->
                return NicknameSetResult.InvalidCharacters

            NicknameParse.ColorsNotAllowed ->
                return NicknameSetResult.ColorsNotAllowed

            NicknameParse.PatternMismatch ->
                return NicknameSetResult.PatternMismatch

            NicknameParse.Blacklisted ->
                return NicknameSetResult.Blacklisted
        }

        val reserved = runner.run { known.knownByName(nickname.normalized) }
        if (reserved != null && reserved.id != playerId) {
            return NicknameSetResult.Reserved
        }

        return try {
            val existing = repository.findByNormalized(nickname.normalized)
            if (existing != null && existing.playerId != playerId) {
                return NicknameSetResult.Taken
            }
            repository.save(
                StoredNickname(
                    playerId = playerId,
                    display = nickname.display,
                    normalized = nickname.normalized,
                    ownerName = resolveOwnerName(playerId),
                    updatedAt = clock.instant(),
                    updatedBy = updatedBy,
                )
            )
            NicknameState.update(
                playerId,
                Nickname.component(nickname.display, config.prefix)
            )
            NicknameSetResult.Set(nickname.display)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error("failed to save nickname for {}", playerId, exception)
            NicknameSetResult.StorageUnavailable
        }
    }

    /** The caller's durable nickname, for the bare `/nick` status line. */
    suspend fun current(playerId: UUID): NicknameCurrent =
        try {
            when (val stored = repository.find(playerId)) {
                null -> NicknameCurrent.None
                else -> NicknameCurrent.Set(stored.display)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error("failed to read nickname for {}", playerId, exception)
            NicknameCurrent.StorageUnavailable
        }

    /**
     * Staff-only: sets the nickname of another player, resolved by account name. Offline targets are
     * supported through the durable identity index; [updatedBy] records the acting staff member.
     */
    suspend fun setFor(
        updatedBy: UUID?,
        targetName: String,
        raw: String,
        colorsAllowed: Boolean = false,
    ): NicknameForResult {
        val target = resolveTarget(targetName)
            ?: return NicknameForResult.TargetNotFound
        return NicknameForResult.Applied(
            targetName = target.name,
            result = set(
                target.id,
                raw,
                updatedBy,
                colorsAllowed
            ),
        )
    }

    /** Staff-only: clears the nickname of another player, resolved by account name. */
    suspend fun clearFor(targetName: String): NicknameForResult {
        val target = resolveTarget(targetName)
            ?: return NicknameForResult.TargetNotFound
        return NicknameForResult.Applied(
            targetName = target.name,
            result = clear(target.id),
        )
    }

    /** Resolves an account name to a known player, falling back to the durable identity index. */
    private suspend fun resolveTarget(raw: String): KnownPlayerIdentity? {
        val name = raw.trim()
        if (name.isEmpty()) {
            return null
        }

        runner.run { known.knownByName(name) }?.let { return it }
        return identities.findCandidatesByName(name)
                .firstOrNull { it.name.equals(name, ignoreCase = true) }
                ?.let { KnownPlayerIdentity(it.id, it.name) }
    }

    private suspend fun resolveOwnerName(playerId: UUID): String? =
        runner.run { known.knownById(playerId)?.name }
            ?: identities.findById(playerId)?.name

    suspend fun clear(playerId: UUID): NicknameSetResult =
        try {
            repository.delete(playerId)
            NicknameState.clear(playerId)
            NicknameSetResult.Cleared
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error("failed to clear nickname for {}", playerId, exception)
            NicknameSetResult.StorageUnavailable
        }

    /**
     * Hydrates the display snapshot for a joined player from durable storage. A player without a
     * stored nickname is cleared so a reused UUID can never keep a stale display. When
     * `resetOnNameChange` is set and the account name differs from the one recorded when the
     * nickname was set, the nickname is discarded.
     */
    suspend fun loadDisplay(
        playerId: UUID,
        currentName: String? = null,
    ) {
        try {
            val stored = repository.find(playerId)
            if (stored == null) {
                NicknameState.clear(playerId)
                return
            }
            val config = settings()
            if (config.resetOnNameChange
                && currentName != null
                && stored.ownerName != null
                && !stored.ownerName.equals(currentName, ignoreCase = true)
            ) {
                repository.delete(playerId)
                NicknameState.clear(playerId)
                return
            }
            NicknameState.update(
                playerId,
                Nickname.component(stored.display, config.prefix)
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error(
                "failed to load nickname display for {}",
                playerId,
                exception
            )
        }
    }

    /** Drops the display snapshot for a departing player. */
    fun clearDisplay(playerId: UUID) {
        NicknameState.clear(playerId)
    }

    /** Drops every display snapshot when the server stops. */
    fun clearAllDisplays() {
        NicknameState.clearAll()
    }

    suspend fun resolve(raw: String): NicknameLookup {
        val normalized = Nickname.normalize(raw)
        if (normalized.isEmpty()) {
            return NicknameLookup.NotFound
        }

        return try {
            val stored = repository.findByNormalized(normalized)
                ?: return NicknameLookup.NotFound
            val name = runner.run { known.knownById(stored.playerId)?.name }
                ?: identities.findById(stored.playerId)?.name
            when (name) {
                null -> NicknameLookup.NotFound
                else -> NicknameLookup.Found(stored.playerId, name)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error("failed to resolve nickname '$normalized'", exception)
            NicknameLookup.StorageUnavailable
        }
    }

    private companion object {

        private val LOGGER = LogUtils.getLogger()

    }

}
