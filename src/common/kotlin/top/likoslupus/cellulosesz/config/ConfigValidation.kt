package top.likoslupus.cellulosesz.config

internal sealed interface ConfigValidationResult {

    data object Valid : ConfigValidationResult

    data class Invalid(val errors: List<ConfigValidationError>) : ConfigValidationResult

}

internal data class ConfigValidationError(
    val path: String,
    val message: String
)

internal object ConfigValidation {

    fun validate(config: CellulosesConfig): ConfigValidationResult {
        val errors = buildList {
            if (config.schemaVersion != CURRENT_SCHEMA_VERSION) {
                add(
                    ConfigValidationError(
                        "schemaVersion",
                        "unsupported schema version ${config.schemaVersion}, expected $CURRENT_SCHEMA_VERSION",
                    )
                )
            }
            if (config.homes.maxPerPlayer < 0) {
                add(ConfigValidationError("homes.maxPerPlayer", "must be >= 0"))
            }
            if (config.homes.defaultName.isBlank()) {
                add(ConfigValidationError("homes.defaultName", "must not be blank"))
            }
            if (config.teleportRequests.timeoutSeconds <= 0) {
                add(ConfigValidationError("teleportRequests.timeoutSeconds", "must be > 0"))
            }
        }
        return if (errors.isEmpty()) ConfigValidationResult.Valid else ConfigValidationResult.Invalid(
            errors
        )
    }

}
