package top.likoslupus.cellulosesz.administration.config

import top.likoslupus.cellulosesz.administration.jail.JailName
import top.likoslupus.cellulosesz.administration.moderation.ModerationReason
import top.likoslupus.cellulosesz.foundation.config.ValidationError

/**
 * Administration-owned configuration rules. The application aggregates these errors and attaches
 * them to the root schema path instead of re-implementing feature validation.
 */
public object AdministrationSettingsValidation {

    private const val MIN_REASON_LENGTH: Int = 1
    private const val MAX_REASON_LENGTH: Int = 1024
    private const val MAX_JAILS: Int = 256
    private const val MAX_JAIL_RADIUS: Double = 256.0
    private const val MAX_CONFINEMENT_INTERVAL_TICKS: Int = 1200
    private const val MAX_JAIL_ALLOWED_COMMANDS: Int = 64
    private const val MAX_EXPERIENCE_ADJUSTMENT: Int = 100_000_000
    private const val MAX_SPEED_MULTIPLIER: Double = 10.0

    private val JAIL_COMMAND_ENTRY: Regex = Regex("[a-z0-9_:-]+")

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

            val jail = settings.jail
            if (jail.defaultJail.isNotBlank() && JailName.parse(jail.defaultJail) == null) {
                add(
                    ValidationError(
                        "administration.jail.defaultJail",
                        "must be a jail name matching [a-z0-9_-]{1,32}, or empty",
                    )
                )
            }
            if (jail.maxJails !in 1..MAX_JAILS) {
                add(
                    ValidationError(
                        "administration.jail.maxJails",
                        "must be between 1 and $MAX_JAILS",
                    )
                )
            }
            if (!jail.allowedRadius.isFinite() || jail.allowedRadius < 0.0) {
                add(
                    ValidationError(
                        "administration.jail.allowedRadius",
                        "must be a finite value >= 0",
                    )
                )
            } else if (jail.allowedRadius > MAX_JAIL_RADIUS) {
                add(
                    ValidationError(
                        "administration.jail.allowedRadius",
                        "must be <= $MAX_JAIL_RADIUS",
                    )
                )
            }
            if (jail.confinementCheckIntervalTicks !in 1..MAX_CONFINEMENT_INTERVAL_TICKS) {
                add(
                    ValidationError(
                        "administration.jail.confinementCheckIntervalTicks",
                        "must be between 1 and $MAX_CONFINEMENT_INTERVAL_TICKS",
                    )
                )
            }
            if (jail.allowedCommands.size > MAX_JAIL_ALLOWED_COMMANDS) {
                add(
                    ValidationError(
                        "administration.jail.allowedCommands",
                        "must have at most $MAX_JAIL_ALLOWED_COMMANDS entries",
                    )
                )
            }
            jail.allowedCommands.forEach { entry ->
                if (!JAIL_COMMAND_ENTRY.matches(entry)) {
                    add(
                        ValidationError(
                            "administration.jail.allowedCommands",
                            "entry '$entry' must match [a-z0-9_:-]+ (no blanks, spaces or wildcards)",
                        )
                    )
                }
            }

            val adjustments = settings.playerAdjustments
            if (adjustments.maximumExperienceAdjustment !in 1..MAX_EXPERIENCE_ADJUSTMENT) {
                add(
                    ValidationError(
                        "administration.playerAdjustments.maximumExperienceAdjustment",
                        "must be between 1 and $MAX_EXPERIENCE_ADJUSTMENT",
                    )
                )
            }
            val minimum = adjustments.minimumSpeedMultiplier
            val maximum = adjustments.maximumSpeedMultiplier
            if (!minimum.isFinite() || minimum < 0.0) {
                add(
                    ValidationError(
                        "administration.playerAdjustments.minimumSpeedMultiplier",
                        "must be a finite value >= 0",
                    )
                )
            }
            if (!maximum.isFinite() || maximum > MAX_SPEED_MULTIPLIER) {
                add(
                    ValidationError(
                        "administration.playerAdjustments.maximumSpeedMultiplier",
                        "must be a finite value <= $MAX_SPEED_MULTIPLIER",
                    )
                )
            }
            if (minimum.isFinite() && maximum.isFinite() && minimum > maximum) {
                add(
                    ValidationError(
                        "administration.playerAdjustments.minimumSpeedMultiplier",
                        "must be <= maximumSpeedMultiplier",
                    )
                )
            }
        }

}
