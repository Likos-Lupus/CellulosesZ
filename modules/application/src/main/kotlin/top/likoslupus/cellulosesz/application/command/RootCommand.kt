package top.likoslupus.cellulosesz.application.command

import top.likoslupus.cellulosesz.application.config.CellulosesConfig
import top.likoslupus.cellulosesz.application.health.ApplicationHealth
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionInspector
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.i18n.LanguageResolver
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.i18n.TranslationCatalog
import top.likoslupus.cellulosesz.core.text.i18n.preference.PlayerLanguagePreferences
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.foundation.config.ConfigHandle

/**
 * The `/cellulosesz` root: metadata plus the sub-command groups, each declared in its own file.
 * The declaration order of the groups is the declaration order of the root's children.
 */
internal object RootCommand {

    const val ROOT_LITERAL: String = "cellulosesz"

    fun definition(
        config: ConfigHandle<CellulosesConfig>,
        kernel: RuntimeKernel,
        health: ApplicationHealth,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        languages: PlayerLanguagePreferences,
        resolver: LanguageResolver,
        catalog: TranslationCatalog,
        modVersion: String,
        loaderName: String,
        diagnostics: CommandRegistrationDiagnostics,
        permissions: PermissionService,
        inspector: PermissionInspector?,
        helpCatalog: CommandHelpCatalog,
        onReloadApplied: suspend () -> Unit = {},
    ): CommandDefinition =
        command(
            name = ROOT_LITERAL,
            category = CommandCategory.ROOT,
            permission = CommandPermissions.ROOT,
            documentation = "core/cellulosesz",
        ) {
            infoCommands(
                modVersion = modVersion,
                loaderName = loaderName,
                health = health,
                messages = messages,
                adventure = adventure,
                presentation = presentation,
            )
            reloadCommand(
                config = config,
                kernel = kernel,
                messages = messages,
                adventure = adventure,
                onReloadApplied = onReloadApplied,
            )
            diagnosticsCommands(
                diagnostics = diagnostics,
                permissions = permissions,
                inspector = inspector,
                messages = messages,
                adventure = adventure,
            )
            languageCommands(
                kernel = kernel,
                messages = messages,
                adventure = adventure,
                languages = languages,
                resolver = resolver,
                catalog = catalog,
            )
            helpCommands(
                helpCatalog = helpCatalog,
                permissions = permissions,
                messages = messages,
                adventure = adventure,
            )
        }

}
