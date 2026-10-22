package top.likoslupus.cellulosesz.application.bootstrap

import dev.architectury.event.events.common.CommandRegistrationEvent
import top.likoslupus.cellulosesz.application.backup.BackupCommand
import top.likoslupus.cellulosesz.application.backup.BackupService
import top.likoslupus.cellulosesz.application.command.*
import top.likoslupus.cellulosesz.application.command.help.SelectiveVanillaHelpOverlay
import top.likoslupus.cellulosesz.core.command.help.CommandDocumentationService
import top.likoslupus.cellulosesz.core.command.help.CommandHelpResolver
import top.likoslupus.cellulosesz.core.command.registry.*
import top.likoslupus.cellulosesz.core.text.document.ClasspathCommandDocumentSource
import top.likoslupus.cellulosesz.core.text.document.DocumentTheme
import top.likoslupus.cellulosesz.core.text.document.MarkdownDocumentLoader
import top.likoslupus.cellulosesz.economy.EconomyFeature
import top.likoslupus.cellulosesz.player.PlayerFeature

/**
 * Bootstrap phase for command definitions, help and routing. The routing policy and the effective
 * `/me` ownership are startup bindings, so they are computed once here and published through the
 * single registration event.
 */
internal object BootstrapCommands {

    fun install(state: BootstrapState) {
        val diagnostics = CommandRegistrationDiagnostics()
        state.diagnostics = diagnostics
        val commandCooldowns = CommandCooldownStore(
            JdbcCommandCooldownRepository(
                state.database,
                state.namespace
            )
        )
        state.commandCooldowns = commandCooldowns
        val commandGate = CommandGate(
            kernel = state.kernel,
            settings = { state.config.current.commands },
            permissions = state.permissions,
            administration = state.administration,
            economy = state.economy.integration,
            cooldowns = commandCooldowns,
            presentation = state.presentation,
            adventure = state.adventure,
        )
        state.commandGate = commandGate
        val backup = BackupService(
            kernel = state.kernel,
            settings = { state.config.current.backup },
            onlineCount = { state.kernel.requireServer().playerList.playerCount },
        )
        state.backup = backup
        val helpCatalog = CommandHelpCatalog()
        state.helpCatalog = helpCatalog
        val disabledCommands = state.config.current.commands.disabled.toSet()
        val definitions = buildList {
            add(
                RootCommand.definition(
                    config = state.config,
                    kernel = state.kernel,
                    health = state.health,
                    messages = state.messages,
                    adventure = state.adventure,
                    presentation = state.presentation,
                    languages = state.languagePreferences,
                    resolver = state.languageResolver,
                    catalog = state.catalog,
                    modVersion = BootstrapConfiguration.modVersion(),
                    loaderName = state.platform.loaderName,
                    diagnostics = diagnostics,
                    permissions = state.permissions,
                    inspector = state.platform.permissionInspector,
                    helpCatalog = helpCatalog,
                    onReloadApplied = {
                        state.kernel.onServerThread {
                            state.playerFeature.applyNicknameDisplay()
                            state.playerFeature.applyAfkPolicy()
                            state.administration.applyJailCommandRestrictions()
                            state.backup.reload()
                        }
                    },
                )
            )
            addAll(state.movement.commands())
            addAll(state.communication.commands(state.integration))
            addAll(state.administration.commands())
            addAll(state.utility.commands())
            addAll(state.playerFeature.commands())
            addAll(state.economy.commands())
            add(
                BackupCommand.definition(
                    state.kernel,
                    backup,
                    state.messages,
                    state.adventure
                )
            )
        }.filterNot { it.name in disabledCommands }
        val commandRegistry = CommandRegistry(definitions)
        state.commandRegistry = commandRegistry
        helpCatalog.definitions = commandRegistry.definitions
        val helpResolver = CommandHelpResolver(
            definitions = commandRegistry.definitions,
            permissions = state.permissions,
            effectiveRoutes = diagnostics.effectiveRoutes,
        )
        state.helpResolver = helpResolver
        val documentation = CommandDocumentationService(
            loader = MarkdownDocumentLoader(
                listOf(
                    ClasspathCommandDocumentSource(state.movement::class.java),
                    ClasspathCommandDocumentSource(state.communication::class.java),
                    ClasspathCommandDocumentSource(state.administration::class.java),
                    ClasspathCommandDocumentSource(state.utility::class.java),
                    ClasspathCommandDocumentSource(PlayerFeature::class.java),
                    ClasspathCommandDocumentSource(EconomyFeature::class.java),
                    ClasspathCommandDocumentSource(CellulosesZ::class.java),
                )
            ),
            messages = state.messages,
            languages = state.languageResolver,
            permissions = state.permissions,
        )
        state.documentation = documentation
        val helpOverlay = SelectiveVanillaHelpOverlay(
            helpResolver,
            documentation,
            state.adventure
        ) {
            DocumentTheme.from(state.presentationTheme())
        }
        state.helpOverlay = helpOverlay
        // The routing policy is a startup binding: `commands` and the takeover settings cannot
        // reload. Takeovers are derived from one place, so `/me`, `/tp`, ... never disagree about
        // who owns their literal.
        val routingSettings = state.config.current.commands
        val overrides = routingSettings.vanillaOverrides
        val takeovers = VanillaCommandCatalog.all.mapNotNull { command ->
            val featureEnabled = command != VanillaCommandId.ME || state.config.current.messaging.actionMessage.enabled
            val route = effectiveVanillaRoute(
                command = command,
                featureEnabled = featureEnabled,
                overridesEnabled = overrides.enabled,
                overrides = overrides.commands.toSet(),
            )
            command.literal.takeIf {
                route.mode == VanillaRouteMode.TAKE_OVER_VANILLA &&
                        command.takeoverSupport == TakeoverSupport.VERIFIED
            }
        }.toSet()
        val routingPolicy = CommandRoutingPolicy(
            shortNamesEnabled = routingSettings.shortNamesEnabled,
            aliasesEnabled = routingSettings.aliasesEnabled,
            vanillaTakeovers = takeovers,
        )
        state.routingPolicy = routingPolicy
        state.helpOverlayEnabled = routingSettings.vanillaHelpOverlayEnabled

        CommandRegistrationEvent.EVENT.register { dispatcher, buildContext, _ ->
            val report = commandRegistry.register(
                dispatcher,
                buildContext,
                state.permissions,
                routingPolicy,
                interceptor = commandGate,
            )
            diagnostics.publish(report)
            state.health.recordCommands(report)
            if (state.helpOverlayEnabled) {
                helpOverlay.install(dispatcher)
            }
        }
    }

}
