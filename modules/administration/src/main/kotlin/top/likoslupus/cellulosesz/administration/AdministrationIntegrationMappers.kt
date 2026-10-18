package top.likoslupus.cellulosesz.administration

import top.likoslupus.cellulosesz.administration.integration.ModerationIntegrationOutcome
import top.likoslupus.cellulosesz.administration.jail.JailActionOutcome
import top.likoslupus.cellulosesz.administration.moderation.mute.MuteResult
import top.likoslupus.cellulosesz.administration.moderation.mute.UnmuteResult

/** Maps the internal mute outcome onto the integration port's result set. */
internal fun MuteResult.toIntegration(): ModerationIntegrationOutcome =
    when (this) {
        is MuteResult.Applied ->
            when {
                auditRecorded -> ModerationIntegrationOutcome.Applied(actionId)
                else -> ModerationIntegrationOutcome.PartiallyApplied
            }

        MuteResult.AlreadyMuted -> ModerationIntegrationOutcome.AlreadyInState
        MuteResult.SelfTarget -> ModerationIntegrationOutcome.ProtectedTarget
        MuteResult.TargetProtected -> ModerationIntegrationOutcome.ProtectedTarget
        MuteResult.TargetUnknown -> ModerationIntegrationOutcome.TargetUnknown
        MuteResult.DurationTooLong -> ModerationIntegrationOutcome.InvalidDuration
        MuteResult.StorageUnavailable -> ModerationIntegrationOutcome.StorageUnavailable
        MuteResult.RuntimeStopping -> ModerationIntegrationOutcome.RuntimeStopping
    }

/** Maps the internal unmute outcome onto the integration port's result set. */
internal fun UnmuteResult.toIntegration(): ModerationIntegrationOutcome =
    when (this) {
        is UnmuteResult.Unmuted ->
            when {
                auditRecorded -> ModerationIntegrationOutcome.Applied(actionId)
                else -> ModerationIntegrationOutcome.PartiallyApplied
            }

        UnmuteResult.TargetUnknown -> ModerationIntegrationOutcome.TargetUnknown
        UnmuteResult.NotMuted -> ModerationIntegrationOutcome.AlreadyInState
        UnmuteResult.StorageUnavailable -> ModerationIntegrationOutcome.StorageUnavailable
        UnmuteResult.RuntimeStopping -> ModerationIntegrationOutcome.RuntimeStopping
    }

/** Maps the internal jail outcome onto the integration port's result set. */
internal fun JailActionOutcome.toIntegration(): ModerationIntegrationOutcome =
    when (this) {
        is JailActionOutcome.Jailed ->
            when {
                auditRecorded -> ModerationIntegrationOutcome.Applied(actionId)
                else -> ModerationIntegrationOutcome.PartiallyApplied
            }

        is JailActionOutcome.ReleaseRequested ->
            actionId.let {
                when {
                    it == null -> ModerationIntegrationOutcome.AlreadyInState
                    !auditRecorded -> ModerationIntegrationOutcome.PartiallyApplied
                    else -> ModerationIntegrationOutcome.Applied(it)
                }
            }

        JailActionOutcome.AlreadyJailed -> ModerationIntegrationOutcome.AlreadyInState
        JailActionOutcome.NotJailed -> ModerationIntegrationOutcome.AlreadyInState
        JailActionOutcome.TargetUnknown -> ModerationIntegrationOutcome.TargetUnknown
        JailActionOutcome.SelfTarget -> ModerationIntegrationOutcome.ProtectedTarget
        JailActionOutcome.TargetProtected -> ModerationIntegrationOutcome.ProtectedTarget
        JailActionOutcome.InvalidJailName -> ModerationIntegrationOutcome.Unavailable
        is JailActionOutcome.JailUnknown -> ModerationIntegrationOutcome.Unavailable
        JailActionOutcome.NoDefaultJail -> ModerationIntegrationOutcome.Unavailable
        JailActionOutcome.Disabled -> ModerationIntegrationOutcome.Unavailable
        JailActionOutcome.StorageUnavailable -> ModerationIntegrationOutcome.StorageUnavailable
        JailActionOutcome.RuntimeStopping -> ModerationIntegrationOutcome.RuntimeStopping
    }
