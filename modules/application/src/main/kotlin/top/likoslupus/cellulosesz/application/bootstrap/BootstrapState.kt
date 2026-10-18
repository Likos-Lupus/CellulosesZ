package top.likoslupus.cellulosesz.application.bootstrap

import top.likoslupus.cellulosesz.administration.AdministrationFeature
import top.likoslupus.cellulosesz.api.extension.ExtensionsApi
import top.likoslupus.cellulosesz.application.api.ApiRequestDispatcher
import top.likoslupus.cellulosesz.application.api.ApplicationApiProvider
import top.likoslupus.cellulosesz.application.api.extension.AddonMessageOverlay
import top.likoslupus.cellulosesz.application.api.extension.ApplicationExtensionHub
import top.likoslupus.cellulosesz.application.backup.BackupService
import top.likoslupus.cellulosesz.application.command.CommandCooldownStore
import top.likoslupus.cellulosesz.application.command.CommandGate
import top.likoslupus.cellulosesz.application.command.CommandHelpCatalog
import top.likoslupus.cellulosesz.application.command.CommandRegistrationDiagnostics
import top.likoslupus.cellulosesz.application.command.help.SelectiveVanillaHelpOverlay
import top.likoslupus.cellulosesz.application.config.CellulosesConfig
import top.likoslupus.cellulosesz.application.health.ApplicationHealth
import top.likoslupus.cellulosesz.communication.CommunicationFeature
import top.likoslupus.cellulosesz.communication.CommunicationIntegration
import top.likoslupus.cellulosesz.core.command.help.CommandDocumentationService
import top.likoslupus.cellulosesz.core.command.help.CommandHelpResolver
import top.likoslupus.cellulosesz.core.command.registry.CommandRegistry
import top.likoslupus.cellulosesz.core.command.registry.CommandRoutingPolicy
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.player.MinecraftKnownPlayerResolver
import top.likoslupus.cellulosesz.core.player.identity.JdbcPlayerIdentityRepository
import top.likoslupus.cellulosesz.core.preference.JdbcPlayerPreferenceRepository
import top.likoslupus.cellulosesz.core.preference.PlayerPreferenceService
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.i18n.LanguageResolver
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.i18n.TranslationCatalog
import top.likoslupus.cellulosesz.core.text.i18n.preference.JdbcPlayerLanguagePreferenceRepository
import top.likoslupus.cellulosesz.core.text.i18n.preference.PlayerLanguagePreferences
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.theme.TextTheme
import top.likoslupus.cellulosesz.economy.EconomyFeature
import top.likoslupus.cellulosesz.foundation.config.ConfigHandle
import top.likoslupus.cellulosesz.foundation.database.DeferredDatabaseRuntime
import top.likoslupus.cellulosesz.movement.MovementFeature
import top.likoslupus.cellulosesz.player.PlayerFeature
import top.likoslupus.cellulosesz.utility.UtilityFeature
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference

/**
 * The graph assembled by the composition root. Each bootstrap phase fills the fields it owns in the
 * same order the original single-method bootstrap did, so collaborators can read the graph without
 * long parameter lists.
 */
internal class BootstrapState(
    val platform: PlatformServices,
    val health: ApplicationHealth,
    val kernel: RuntimeKernel,
    val permissions: PermissionService,
    val configDirectory: Path,
    val config: ConfigHandle<CellulosesConfig>,
    val database: DeferredDatabaseRuntime,
    val namespace: String,
    val known: MinecraftKnownPlayerResolver,
    val identityRepository: JdbcPlayerIdentityRepository,
    val catalog: TranslationCatalog,
    val languageRepository: JdbcPlayerLanguagePreferenceRepository,
    val languagePreferences: PlayerLanguagePreferences,
    val preferenceRepository: JdbcPlayerPreferenceRepository,
    val preferences: PlayerPreferenceService,
    val languageResolver: LanguageResolver,
    val presentationTheme: () -> TextTheme,
    val addonText: AddonMessageOverlay,
    val messages: LocalizedMessages,
    val presentation: TextPresentation,
    val adventure: AdventureRuntime,
) {

    lateinit var administration: AdministrationFeature

    lateinit var movement: MovementFeature

    lateinit var communication: CommunicationFeature

    lateinit var utility: UtilityFeature

    lateinit var playerFeature: PlayerFeature

    lateinit var economy: EconomyFeature

    lateinit var apiDispatcher: ApiRequestDispatcher

    lateinit var extensions: ApplicationExtensionHub

    lateinit var apiProvider: ApplicationApiProvider

    lateinit var extensionsApi: ExtensionsApi

    val sessionEvents: AtomicReference<ApiSessionEvents?> = AtomicReference(null)

    lateinit var integration: CommunicationIntegration

    lateinit var diagnostics: CommandRegistrationDiagnostics

    lateinit var commandCooldowns: CommandCooldownStore

    lateinit var commandGate: CommandGate

    lateinit var backup: BackupService

    lateinit var helpCatalog: CommandHelpCatalog

    lateinit var commandRegistry: CommandRegistry

    lateinit var helpResolver: CommandHelpResolver

    lateinit var documentation: CommandDocumentationService

    lateinit var helpOverlay: SelectiveVanillaHelpOverlay

    lateinit var routingPolicy: CommandRoutingPolicy

    var helpOverlayEnabled: Boolean = false

}
