package top.likoslupus.cellulosesz.application.config

import top.likoslupus.cellulosesz.administration.config.AdministrationSettingsValidation
import top.likoslupus.cellulosesz.communication.config.MessagingSettingsValidation
import top.likoslupus.cellulosesz.communication.config.meRouteMode
import top.likoslupus.cellulosesz.core.text.TextSettingsValidation
import top.likoslupus.cellulosesz.core.text.i18n.LocalizationSettingsValidation
import top.likoslupus.cellulosesz.foundation.config.ValidationError
import top.likoslupus.cellulosesz.foundation.database.DatabaseDescriptorResolver
import top.likoslupus.cellulosesz.foundation.database.DatabaseSettingsValidation
import top.likoslupus.cellulosesz.foundation.database.ResolvedDatabaseDescriptor
import top.likoslupus.cellulosesz.movement.config.MovementSettingsValidation
import top.likoslupus.cellulosesz.player.config.PlayerSettingsValidation
import top.likoslupus.cellulosesz.utility.config.UtilitySettingsValidation
import java.nio.file.Path

/** Aggregates root-schema rules and each feature owner's own validation. */
internal object ConfigValidation {

    fun validate(config: CellulosesConfig): List<ValidationError> =
        buildList {
            addAll(DatabaseSettingsValidation.validate(config.database))
            addAll(LocalizationSettingsValidation.validate(config.localization))
            addAll(TextSettingsValidation.validate(config.text))
            addAll(MovementSettingsValidation.validate(config.movement))
            addAll(MessagingSettingsValidation.validate(config.messaging))
            addAll(AdministrationSettingsValidation.validate(config.administration))
            addAll(UtilitySettingsValidation.validate(config.utility))
            addAll(PlayerSettingsValidation.validate(config.players))
        }

    /**
     * A reload may change any inactive backend section, but a change to the *active* connection
     * (identity, credentials, SSL, pool) needs a pool swap and therefore a restart.
     */
    fun validateDatabaseTransition(
        previous: ResolvedDatabaseDescriptor,
        candidate: ResolvedDatabaseDescriptor,
    ): List<ValidationError> =
        when (previous.signature) {
            candidate.signature -> emptyList()
            else -> listOf(
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

    /**
     * A reload may not change a registration-time binding: the command routing settings and the
     * effective `/me` ownership mode are decided while the dispatcher is built and cannot be
     * hot-swapped. This compares the *effective* mode, not raw booleans, so disabling the action
     * service while `overrideVanillaMe` is already false stays allowed.
     */
    fun validateCommandTransition(
        previous: CellulosesConfig,
        candidate: CellulosesConfig,
    ): List<ValidationError> =
        buildList {
            if (previous.commands != candidate.commands) {
                add(
                    ValidationError(
                        "commands",
                        "command registration settings changed; restart the server to apply",
                    )
                )
            }
            if (meRouteMode(previous.messaging.actionMessage) !=
                meRouteMode(candidate.messaging.actionMessage)
            ) {
                add(
                    ValidationError(
                        "messaging.actionMessage",
                        "vanilla /me ownership changed; restart the server to apply",
                    )
                )
            }
        }

    /**
     * The full reload transition validator: the existing database check plus the command-binding
     * check. Both must pass for a reload to publish.
     */
    fun rootTransition(
        storageRoot: () -> Path,
    ): (CellulosesConfig, CellulosesConfig) -> List<ValidationError> =
        { previous, candidate ->
            databaseTransition(storageRoot)(previous, candidate) +
                    validateCommandTransition(previous, candidate)
        }

}
