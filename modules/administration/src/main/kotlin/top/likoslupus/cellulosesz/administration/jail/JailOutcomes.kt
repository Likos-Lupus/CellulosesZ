package top.likoslupus.cellulosesz.administration.jail

import top.likoslupus.cellulosesz.administration.moderation.PlayerIdentity
import java.time.Instant

/** Outcome of a jail or release request. Expected failures are values. */
internal sealed interface JailActionOutcome {

    data class Jailed(
        val target: PlayerIdentity,
        val jailName: JailName,
        val transferred: Boolean,
        val auditRecorded: Boolean,
        val expiresAt: Instant?,
        val remainingSeconds: Long?,
        val returnRecorded: Boolean,
        val actionId: String,
    ) : JailActionOutcome

    data class ReleaseRequested(
        val target: PlayerIdentity,
        val auditRecorded: Boolean,
        val actionId: String? = null,
    ) : JailActionOutcome

    data object TargetUnknown : JailActionOutcome
    data object SelfTarget : JailActionOutcome
    data object TargetProtected : JailActionOutcome
    data object AlreadyJailed : JailActionOutcome
    data object NotJailed : JailActionOutcome
    data object InvalidJailName : JailActionOutcome
    data class JailUnknown(val jailName: JailName) : JailActionOutcome
    data object NoDefaultJail : JailActionOutcome
    data object Disabled : JailActionOutcome
    data object StorageUnavailable : JailActionOutcome
    data object RuntimeStopping : JailActionOutcome

}

internal sealed interface SetJailOutcome {

    data class Saved(val name: JailName) : SetJailOutcome

    data object InvalidName : SetJailOutcome
    data object LimitReached : SetJailOutcome
    data object Disabled : SetJailOutcome
    data object StorageUnavailable : SetJailOutcome
    data object RuntimeStopping : SetJailOutcome

}

internal sealed interface DeleteJailOutcome {

    data object Deleted : DeleteJailOutcome
    data object NotFound : DeleteJailOutcome
    data object HasSentences : DeleteJailOutcome
    data object StorageUnavailable : DeleteJailOutcome
    data object RuntimeStopping : DeleteJailOutcome

}
