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
    private const val MAX_LOGIN_ATTACK_DELAY_SECONDS: Int = 3_600
    private const val MAX_HEAL_COOLDOWN_SECONDS: Long = 86_400
    private const val MAX_SPAWN_AMOUNT: Int = 1_024
    private const val MAX_REMOVE_RADIUS: Double = 1_024.0
    private const val MAX_FIREWORK_POWER: Int = 32
    private const val MAX_FIREWORK_AMOUNT: Int = 128
    private const val MAX_BURN_SECONDS: Double = 3_600.0
    private const val MAX_TREE_RANGE: Double = 1_024.0
    private const val MAX_PROJECTILE_SPEED: Double = 64.0
    private const val MAX_SIGN_LINE_LENGTH: Int = 4_096
    private const val MAX_SIGN_DISTANCE: Double = 64.0

    private val JAIL_COMMAND_ENTRY: Regex = Regex("[a-z0-9_:-]+")

    private val DIMENSION_ID: Regex = Regex("[a-z0-9_.-]+:[a-z0-9_./-]+")

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

            moderation.maxMuteSeconds?.let { seconds ->
                if (seconds <= 0) {
                    add(
                        ValidationError(
                            "administration.moderation.maxMuteSeconds",
                            "must be > 0 when set",
                        )
                    )
                }
            }

            if (moderation.loginAttackDelaySeconds !in 0..MAX_LOGIN_ATTACK_DELAY_SECONDS) {
                add(
                    ValidationError(
                        "administration.moderation.loginAttackDelaySeconds",
                        "must be between 0 and $MAX_LOGIN_ATTACK_DELAY_SECONDS",
                    )
                )
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

            if (adjustments.healCooldownSeconds !in 0..MAX_HEAL_COOLDOWN_SECONDS) {
                add(
                    ValidationError(
                        "administration.playerAdjustments.healCooldownSeconds",
                        "must be between 0 and $MAX_HEAL_COOLDOWN_SECONDS",
                    )
                )
            }
            adjustments.noGodWorlds.forEach { world ->
                if (!DIMENSION_ID.matches(world)) {
                    add(
                        ValidationError(
                            "administration.playerAdjustments.noGodWorlds",
                            "entry '$world' must be a namespaced dimension id",
                        )
                    )
                }
            }

            val entityTools = settings.entityTools
            if (entityTools.maxSpawnAmount !in 1..MAX_SPAWN_AMOUNT) {
                add(
                    ValidationError(
                        "administration.entityTools.maxSpawnAmount",
                        "must be between 1 and $MAX_SPAWN_AMOUNT",
                    )
                )
            }
            if (!entityTools.maxRemoveRadius.isFinite()
                || entityTools.maxRemoveRadius <= 0.0
                || entityTools.maxRemoveRadius > MAX_REMOVE_RADIUS
            ) {
                add(
                    ValidationError(
                        "administration.entityTools.maxRemoveRadius",
                        "must be a finite value in (0, $MAX_REMOVE_RADIUS]",
                    )
                )
            }

            val worldTools = settings.worldTools
            if (worldTools.maxFireworkPower !in 1..MAX_FIREWORK_POWER) {
                add(
                    ValidationError(
                        "administration.worldTools.maxFireworkPower",
                        "must be between 1 and $MAX_FIREWORK_POWER",
                    )
                )
            }
            if (worldTools.maxFireworkAmount !in 1..MAX_FIREWORK_AMOUNT) {
                add(
                    ValidationError(
                        "administration.worldTools.maxFireworkAmount",
                        "must be between 1 and $MAX_FIREWORK_AMOUNT",
                    )
                )
            }
            if (!worldTools.maxBurnSeconds.isFinite()
                || worldTools.maxBurnSeconds <= 0.0
                || worldTools.maxBurnSeconds > MAX_BURN_SECONDS
            ) {
                add(
                    ValidationError(
                        "administration.worldTools.maxBurnSeconds",
                        "must be a finite value in (0, $MAX_BURN_SECONDS]",
                    )
                )
            }
            if (!worldTools.treeCommandRangeLimit.isFinite()
                || worldTools.treeCommandRangeLimit <= 0.0
                || worldTools.treeCommandRangeLimit > MAX_TREE_RANGE
            ) {
                add(
                    ValidationError(
                        "administration.worldTools.treeCommandRangeLimit",
                        "must be a finite value in (0, $MAX_TREE_RANGE]",
                    )
                )
            }
            if (!worldTools.maxProjectileSpeed.isFinite()
                || worldTools.maxProjectileSpeed <= 0.0
                || worldTools.maxProjectileSpeed > MAX_PROJECTILE_SPEED
            ) {
                add(
                    ValidationError(
                        "administration.worldTools.maxProjectileSpeed",
                        "must be a finite value in (0, $MAX_PROJECTILE_SPEED]",
                    )
                )
            }

            val signEditing = settings.signEditing
            if (signEditing.maxLineLength !in 1..MAX_SIGN_LINE_LENGTH) {
                add(
                    ValidationError(
                        "administration.signEditing.maxLineLength",
                        "must be between 1 and $MAX_SIGN_LINE_LENGTH",
                    )
                )
            }
            if (!signEditing.maxDistance.isFinite()
                || signEditing.maxDistance <= 0.0
                || signEditing.maxDistance > MAX_SIGN_DISTANCE
            ) {
                add(
                    ValidationError(
                        "administration.signEditing.maxDistance",
                        "must be a finite value in (0, $MAX_SIGN_DISTANCE]",
                    )
                )
            }
        }

}
