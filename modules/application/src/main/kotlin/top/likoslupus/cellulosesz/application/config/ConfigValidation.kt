package top.likoslupus.cellulosesz.application.config

import top.likoslupus.cellulosesz.administration.config.AdministrationSettingsValidation
import top.likoslupus.cellulosesz.communication.config.MessagingSettingsValidation
import top.likoslupus.cellulosesz.core.command.registry.VanillaCommandCatalog
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
            addAll(validateVanillaOverrides(config.commands))
            addAll(validateApiIntegrations(config.api))
        }

    /** Addon integration entries must be well-formed: non-blank ids and capability names. */
    private fun validateApiIntegrations(
        api: ApiIntegrationSettings,
    ): List<ValidationError> =
        buildList {
            api.addons.forEach { (addonId, settings) ->
                if (addonId.isBlank()) {
                    add(
                        ValidationError(
                            "api.integrations.addons",
                            "addon id must not be blank",
                        )
                    )
                }
                settings.allowedCapabilities.forEach { capability ->
                    if (capability.isBlank()) {
                        add(
                            ValidationError(
                                "api.integrations.addons.$addonId.allowedCapabilities",
                                "capability must not be blank",
                            )
                        )
                    }
                }
            }
        }

    /**
     * The vanilla-override allowlist is validated regardless of the global switch: a target that is
     * unknown, unverified for this version, or duplicated must fail loudly rather than be silently
     * ignored (see [VanillaCommandCatalog]).
     */
    private fun validateVanillaOverrides(
        commands: CommandRoutingSettings,
    ): List<ValidationError> =
        buildList {
            val overrides = commands.vanillaOverrides
            overrides.commands
                    .groupingBy { it }
                    .eachCount()
                    .filterValues { it > 1 }
                    .keys
                    .forEach { literal ->
                        add(
                            ValidationError(
                                "commands.vanillaOverrides.commands",
                                "duplicate vanilla override '$literal'",
                            )
                        )
                    }
            overrides.commands.forEach { literal ->
                VanillaCommandCatalog.takeoverProblem(literal)?.let { problem ->
                    add(ValidationError("commands.vanillaOverrides.commands", problem))
                }
            }
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
     * A reload may not change a registration-time binding: the command routing settings (including
     * the vanilla-override allowlist) and whether the action-message `/me` feature is registered at
     * all are decided while the dispatcher is built and cannot be hot-swapped.
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
            if (previous.messaging.actionMessage.enabled != candidate.messaging.actionMessage.enabled) {
                add(
                    ValidationError(
                        "messaging.actionMessage",
                        "enabling or disabling the action-message service changes command " +
                                "registration; restart the server to apply",
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
