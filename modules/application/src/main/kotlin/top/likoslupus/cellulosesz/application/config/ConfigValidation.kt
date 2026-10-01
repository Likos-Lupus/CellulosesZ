package top.likoslupus.cellulosesz.application.config

import top.likoslupus.cellulosesz.foundation.config.ValidationError
import top.likoslupus.cellulosesz.movement.config.MovementSettingsValidation

/** Aggregates root-schema rules and each feature owner's own validation. */
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
        addAll(MovementSettingsValidation.validate(config.movement))
    }

}
