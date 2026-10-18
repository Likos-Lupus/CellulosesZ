package top.likoslupus.cellulosesz.movement.config

import top.likoslupus.cellulosesz.foundation.config.ValidationError

/**
 * Movement-owned configuration rules. The application aggregates these errors and attaches them to
 * the root schema path instead of re-implementing feature validation.
 */
public object MovementSettingsValidation {

    private const val MAX_TOLERANCE: Double = 64.0
    private const val MAX_HORIZONTAL_RADIUS: Int = 8
    private const val MAX_VERTICAL_RADIUS: Int = 32
    private const val MAX_JUMP_DISTANCE: Int = 256
    private val ALIAS_PATTERN: Regex = Regex("[a-z0-9._-]+")
    private val RESOURCE_PATTERN: Regex = Regex("[a-z0-9_.-]+:[a-z0-9_./-]+")

    public fun validate(settings: MovementSettings): List<ValidationError> =
        buildList {
            val teleport = settings.teleport
            if (teleport.delaySeconds < 0) {
                add(ValidationError("movement.teleport.delaySeconds", "must be >= 0"))
            }
            if (teleport.invulnerabilitySeconds < 0) {
                add(
                    ValidationError(
                        "movement.teleport.invulnerabilitySeconds",
                        "must be >= 0"
                    )
                )
            }
            if (teleport.cooldownSeconds < 0) {
                add(ValidationError("movement.teleport.cooldownSeconds", "must be >= 0"))
            }
            if (!teleport.movementTolerance.isFinite() || teleport.movementTolerance < 0.0) {
                add(
                    ValidationError(
                        "movement.teleport.movementTolerance",
                        "must be a finite value >= 0"
                    )
                )
            } else if (teleport.movementTolerance > MAX_TOLERANCE) {
                add(
                    ValidationError(
                        "movement.teleport.movementTolerance",
                        "must be <= $MAX_TOLERANCE"
                    )
                )
            }

            val safety = teleport.safety
            if (safety.searchHorizontalRadius !in 0..MAX_HORIZONTAL_RADIUS) {
                add(
                    ValidationError(
                        "movement.teleport.safety.searchHorizontalRadius",
                        "must be between 0 and $MAX_HORIZONTAL_RADIUS",
                    )
                )
            }
            if (safety.searchVerticalRadius !in 0..MAX_VERTICAL_RADIUS) {
                add(
                    ValidationError(
                        "movement.teleport.safety.searchVerticalRadius",
                        "must be between 0 and $MAX_VERTICAL_RADIUS",
                    )
                )
            }

            if (settings.requests.timeoutSeconds <= 0) {
                add(ValidationError("movement.requests.timeoutSeconds", "must be > 0"))
            }
            if (settings.requests.maxIncomingPerPlayer < 1) {
                add(ValidationError("movement.requests.maxIncomingPerPlayer", "must be >= 1"))
            }
            if (settings.requests.maxOutgoingPerPlayer < 1) {
                add(ValidationError("movement.requests.maxOutgoingPerPlayer", "must be >= 1"))
            }
            if (settings.requests.maxRequestsServerWide < 1) {
                add(ValidationError("movement.requests.maxRequestsServerWide", "must be >= 1"))
            }
            if (settings.requests.maxBatchRecipients < 1) {
                add(ValidationError("movement.requests.maxBatchRecipients", "must be >= 1"))
            }
            if (settings.requests.batchCooldownSeconds < 0) {
                add(ValidationError("movement.requests.batchCooldownSeconds", "must be >= 0"))
            }
            if (settings.requests.maxAutoAllowedPlayers < 0) {
                add(ValidationError("movement.requests.maxAutoAllowedPlayers", "must be >= 0"))
            }

            if (settings.homes.maxPerPlayer < 0) {
                add(ValidationError("movement.homes.maxPerPlayer", "must be >= 0"))
            }
            if (settings.homes.defaultName.isBlank()) {
                add(ValidationError("movement.homes.defaultName", "must not be blank"))
            }
            settings.homes.limits.forEachIndexed { index, limit ->
                if (limit.permission.isBlank()) {
                    add(
                        ValidationError(
                            "movement.homes.limits[$index].permission",
                            "must not be blank",
                        )
                    )
                }
                if (limit.max < 0) {
                    add(
                        ValidationError(
                            "movement.homes.limits[$index].max",
                            "must be >= 0",
                        )
                    )
                }
            }

            settings.worlds.aliases.forEach { (alias, dimension) ->
                if (!ALIAS_PATTERN.matches(alias)) {
                    add(
                        ValidationError(
                            "movement.worlds.aliases.$alias",
                            "alias must match ${ALIAS_PATTERN.pattern}",
                        )
                    )
                }
                if (!RESOURCE_PATTERN.matches(dimension)) {
                    add(
                        ValidationError(
                            "movement.worlds.aliases.$alias",
                            "dimension id '$dimension' must be a resource location",
                        )
                    )
                }
            }

            val random = settings.randomTeleport
            if (random.maxAttempts < 1) {
                add(ValidationError("movement.randomTeleport.maxAttempts", "must be >= 1"))
            }
            if (random.maxRadius < 1) {
                add(ValidationError("movement.randomTeleport.maxRadius", "must be >= 1"))
            }
            if (random.maxRegions < 1) {
                add(ValidationError("movement.randomTeleport.maxRegions", "must be >= 1"))
            }
            if (random.searchTimeoutSeconds <= 0) {
                add(ValidationError("movement.randomTeleport.searchTimeoutSeconds", "must be > 0"))
            }
            if (random.cooldownSeconds < 0) {
                add(ValidationError("movement.randomTeleport.cooldownSeconds", "must be >= 0"))
            }
            if (random.maxConcurrentServerSearches < 1) {
                add(
                    ValidationError(
                        "movement.randomTeleport.maxConcurrentServerSearches",
                        "must be >= 1",
                    )
                )
            }
            if (random.maxConcurrentPerPlayer < 1) {
                add(
                    ValidationError(
                        "movement.randomTeleport.maxConcurrentPerPlayer",
                        "must be >= 1",
                    )
                )
            }

            val terrain = settings.terrain
            if (terrain.maxJumpDistance !in 1..MAX_JUMP_DISTANCE) {
                add(
                    ValidationError(
                        "movement.terrain.maxJumpDistance",
                        "must be between 1 and $MAX_JUMP_DISTANCE",
                    )
                )
            }
        }

}
