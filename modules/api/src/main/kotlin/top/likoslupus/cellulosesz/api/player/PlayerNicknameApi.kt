package top.likoslupus.cellulosesz.api.player

import top.likoslupus.cellulosesz.api.common.ApiCallback
import top.likoslupus.cellulosesz.api.common.ApiRequestHandle
import java.util.*

/** The persisted nickname of a player. */
public sealed interface NicknameQueryOutcome {

    public data class Found(public val name: String) : NicknameQueryOutcome

    public data object None : NicknameQueryOutcome

    public data object Unknown : NicknameQueryOutcome

    public data object StorageUnavailable : NicknameQueryOutcome

}

/** The outcome of a nickname mutation. Every branch maps a real validation/limit reason. */
public sealed interface NicknameChangeOutcome {

    public data class Changed(public val name: String) : NicknameChangeOutcome

    public data object Cleared : NicknameChangeOutcome

    public data object Disabled : NicknameChangeOutcome

    public data object InvalidName : NicknameChangeOutcome

    public data object NameTaken : NicknameChangeOutcome

    public data object Reserved : NicknameChangeOutcome

    public data object NotAuthorized : NicknameChangeOutcome

    public data object StorageUnavailable : NicknameChangeOutcome

}

/**
 * Reads and (for the actor's own nickname) mutates nicknames through the existing nickname rules.
 * Changing another player's nickname is a privileged operation and is deliberately not exposed here.
 */
public interface PlayerNicknameApi {

    public fun current(
        playerId: UUID,
        callback: ApiCallback<NicknameQueryOutcome>,
    ): ApiRequestHandle

    public fun changeOwn(
        actorId: UUID,
        newName: String?,
        callback: ApiCallback<NicknameChangeOutcome>,
    ): ApiRequestHandle

}
