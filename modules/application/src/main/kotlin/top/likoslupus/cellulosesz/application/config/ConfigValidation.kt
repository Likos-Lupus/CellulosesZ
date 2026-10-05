package top.likoslupus.cellulosesz.application.config

import top.likoslupus.cellulosesz.administration.config.AdministrationSettingsValidation
import top.likoslupus.cellulosesz.communication.config.MessagingSettingsValidation
import top.likoslupus.cellulosesz.foundation.config.ValidationError
import top.likoslupus.cellulosesz.movement.config.MovementSettingsValidation
import top.likoslupus.cellulosesz.utility.config.UtilitySettingsValidation

/** Aggregates root-schema rules and each feature owner's own validation. */
internal object ConfigValidation {

    fun validate(config: CellulosesConfig): List<ValidationError> =
        buildList {
            if (config.schemaVersion != CURRENT_SCHEMA_VERSION) {
                add(
                    ValidationError(
                        "schemaVersion",
                        "unsupported schema version ${config.schemaVersion}, " +
                                "expected $CURRENT_SCHEMA_VERSION",
                    )
                )
            }
            addAll(MovementSettingsValidation.validate(config.movement))
            addAll(MessagingSettingsValidation.validate(config.messaging))
            addAll(AdministrationSettingsValidation.validate(config.administration))
            addAll(UtilitySettingsValidation.validate(config.utility))
        }

}
