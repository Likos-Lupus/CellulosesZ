package top.likoslupus.cellulosesz.application.config

import top.likoslupus.cellulosesz.administration.config.AdministrationSettingsValidation
import top.likoslupus.cellulosesz.communication.config.MessagingSettingsValidation
import top.likoslupus.cellulosesz.foundation.config.ValidationError
import top.likoslupus.cellulosesz.foundation.database.DatabaseDescriptorResolver
import top.likoslupus.cellulosesz.foundation.database.DatabaseSettingsValidation
import top.likoslupus.cellulosesz.foundation.database.ResolvedDatabaseDescriptor
import top.likoslupus.cellulosesz.movement.config.MovementSettingsValidation
import top.likoslupus.cellulosesz.utility.config.UtilitySettingsValidation
import java.nio.file.Path

/** Aggregates root-schema rules and each feature owner's own validation. */
internal object ConfigValidation {

    fun validate(config: CellulosesConfig): List<ValidationError> =
        buildList {
            addAll(DatabaseSettingsValidation.validate(config.database))
            addAll(MovementSettingsValidation.validate(config.movement))
            addAll(MessagingSettingsValidation.validate(config.messaging))
            addAll(AdministrationSettingsValidation.validate(config.administration))
            addAll(UtilitySettingsValidation.validate(config.utility))
        }

    /**
     * A reload may change any inactive backend section, but a change to the *active* connection
     * (identity, credentials, SSL, pool) needs a pool swap and therefore a restart.
     */
    fun validateDatabaseTransition(
        previous: ResolvedDatabaseDescriptor,
        candidate: ResolvedDatabaseDescriptor,
    ): List<ValidationError> =
        if (previous.signature == candidate.signature) {
            emptyList()
        } else {
            listOf(
                ValidationError(
                    "database",
                    "active database connection settings changed; restart the server to apply",
                )
            )
        }

    /**
     * Builds a transition validator for [top.likoslupus.cellulosesz.foundation.config.ConfigStore].
     * The active descriptor is recomputed for both snapshots against [storageRoot].
     */
    fun databaseTransition(
        storageRoot: () -> Path,
    ): (CellulosesConfig, CellulosesConfig) -> List<ValidationError> =
        { previous, candidate ->
            validateDatabaseTransition(
                previous = DatabaseDescriptorResolver.resolve(
                    previous.database,
                    storageRoot()
                ),
                candidate = DatabaseDescriptorResolver.resolve(
                    candidate.database,
                    storageRoot()
                ),
            )
        }

}
