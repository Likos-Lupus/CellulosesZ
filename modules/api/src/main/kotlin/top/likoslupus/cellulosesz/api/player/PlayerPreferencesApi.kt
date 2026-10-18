package top.likoslupus.cellulosesz.api.player

import top.likoslupus.cellulosesz.api.actor.AddonAccess
import top.likoslupus.cellulosesz.api.common.ApiCallback
import top.likoslupus.cellulosesz.api.common.ApiRequestHandle
import java.util.*

/**
 * A boolean per-player preference, named after the runtime preference it mirrors. The API never
 * exposes the storage key; a feature-renamed preference stays addressable by this stable name.
 */
public enum class PlayerPreference {

    TELEPORT_ENABLED,
    ACCEPTING_PAY,
    PAY_CONFIRMATION,
    REPLY_TO_LAST_RECIPIENT,
    CLEAR_INVENTORY_CONFIRMATION,

}

/**
 * A read-only snapshot of one player's preference values. Every field resolves an absent stored row
 * to the server's configured default, so the snapshot is always fully populated.
 */
public class PlayerPreferencesView(
    public val playerId: UUID,
    public val teleportEnabled: Boolean,
    public val acceptingPay: Boolean,
    public val payConfirmation: Boolean,
    public val replyToLastRecipient: Boolean,
    public val clearInventoryConfirmation: Boolean,
)

/** The result of reading a player's preferences. [UnknownPlayer] is distinct from "all defaults". */
public sealed interface PlayerPreferencesOutcome {

    public data class Available(
        public val preferences: PlayerPreferencesView
    ) : PlayerPreferencesOutcome

    public data object UnknownPlayer : PlayerPreferencesOutcome

    public data object NotAuthorized : PlayerPreferencesOutcome

    public data object Disabled : PlayerPreferencesOutcome

}

/** A trusted addon changing one boolean preference of one player. */
public class PlayerPreferenceChangeRequest(
    public val access: AddonAccess,
    public val playerId: UUID,
    public val preference: PlayerPreference,
    public val enabled: Boolean,
)

/** The outcome of a preference change. Every branch maps a real write result. */
public sealed interface PlayerPreferenceChangeOutcome {

    /** The stored value changed. */
    public data class Changed(
        public val playerId: UUID,
        public val preference: PlayerPreference,
        public val enabled: Boolean,
    ) : PlayerPreferenceChangeOutcome

    /** The stored value already had this value; nothing was written. */
    public data class Unchanged(
        public val playerId: UUID,
        public val preference: PlayerPreference,
        public val enabled: Boolean,
    ) : PlayerPreferenceChangeOutcome

    public data object UnknownPlayer : PlayerPreferenceChangeOutcome

    public data object NotAuthorized : PlayerPreferenceChangeOutcome

    public data object Disabled : PlayerPreferenceChangeOutcome

}

/**
 * Reads and (for an authorized addon) changes per-player boolean preferences. Both operations are
 * authorized server-side against the granted capability, never against the [AddonAccess] alone.
 */
public interface PlayerPreferencesApi {

    public fun read(
        access: AddonAccess,
        playerId: UUID,
        callback: ApiCallback<PlayerPreferencesOutcome>,
    ): ApiRequestHandle

    public fun change(
        request: PlayerPreferenceChangeRequest,
        callback: ApiCallback<PlayerPreferenceChangeOutcome>,
    ): ApiRequestHandle

}
