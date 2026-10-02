package top.likoslupus.cellulosesz.administration.config

import top.likoslupus.cellulosesz.administration.moderation.ModerationReason
import top.likoslupus.cellulosesz.foundation.config.ValidationError

/**
 * Administration-owned configuration rules. The application aggregates these errors and attaches
 * them to the root schema path instead of re-implementing feature validation.
 */
public object AdministrationSettingsValidation {

    private const val MIN_REASON_LENGTH: Int = 1
    private const val MAX_REASON_LENGTH: Int = 1024

    public fun validate(settings: AdministrationSettings): List<ValidationError> =
        buildList {
            val moderation = settings.moderation

            if (moderation.maxReasonLength !in MIN_REASON_LENGTH..MAX_REASON_LENGTH) {
                add(
                    ValidationError(
                        "administration.moderation.maxReasonLength",
                        "must be between $MIN_REASON_LENGTH and $MAX_REASON_LENGTH",
                    )
                )
            }

            moderation.maxTemporaryBanSeconds?.let { seconds ->
                if (seconds <= 0) {
                    add(
                        ValidationError(
                            "administration.moderation.maxTemporaryBanSeconds",
                            "must be > 0 when set",
                        )
                    )
                }
            }

            moderation.maxTemporaryMuteSeconds?.let { seconds ->
                if (seconds <= 0) {
                    add(
                        ValidationError(
                            "administration.moderation.maxTemporaryMuteSeconds",
                            "must be > 0 when set",
                        )
                    )
                }
            }

            val reasonLength = moderation.maxReasonLength.coerceIn(
                MIN_REASON_LENGTH,
                MAX_REASON_LENGTH,
            )
            if (
                ModerationReason.parse(
                    moderation.defaultKickReason,
                    reasonLength
                ) == null
            ) {
                add(
                    ValidationError(
                        "administration.moderation.defaultKickReason",
                        "must be a non-empty reason without control characters",
                    )
                )
            }
            if (
                ModerationReason.parse(
                    moderation.defaultBanReason,
                    reasonLength
                ) == null
            ) {
                add(
                    ValidationError(
                        "administration.moderation.defaultBanReason",
                        "must be a non-empty reason without control characters",
                    )
                )
            }
            if (
                ModerationReason.parse(
                    moderation.defaultMuteReason,
                    reasonLength
                ) == null
            ) {
                add(
                    ValidationError(
                        "administration.moderation.defaultMuteReason",
                        "must be a non-empty reason without control characters",
                    )
                )
            }
        }

}
