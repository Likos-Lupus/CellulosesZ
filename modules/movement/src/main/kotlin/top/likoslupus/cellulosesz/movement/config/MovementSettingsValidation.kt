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

    public fun validate(settings: MovementSettings): List<ValidationError> =
        buildList {
            val teleport = settings.teleport
            if (teleport.delaySeconds < 0) {
                add(ValidationError("movement.teleport.delaySeconds", "must be >= 0"))
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

            if (settings.homes.maxPerPlayer < 0) {
                add(ValidationError("movement.homes.maxPerPlayer", "must be >= 0"))
            }
            if (settings.homes.defaultName.isBlank()) {
                add(ValidationError("movement.homes.defaultName", "must not be blank"))
            }
        }

}
