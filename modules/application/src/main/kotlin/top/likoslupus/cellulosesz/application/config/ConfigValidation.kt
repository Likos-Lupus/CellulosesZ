package top.likoslupus.cellulosesz.application.config

import top.likoslupus.cellulosesz.foundation.config.ValidationError

internal object ConfigValidation {

    fun validate(config: CellulosesConfig): List<ValidationError> = buildList {
        if (config.schemaVersion != CURRENT_SCHEMA_VERSION) {
            add(
                ValidationError(
                    "schemaVersion",
                    "unsupported schema version ${config.schemaVersion}, expected $CURRENT_SCHEMA_VERSION",
                )
            )
        }
        if (config.homes.maxPerPlayer < 0) {
            add(ValidationError("homes.maxPerPlayer", "must be >= 0"))
        }
        if (config.homes.defaultName.isBlank()) {
            add(ValidationError("homes.defaultName", "must not be blank"))
        }
        if (config.teleportRequests.timeoutSeconds <= 0) {
            add(ValidationError("teleportRequests.timeoutSeconds", "must be > 0"))
        }
    }

}
