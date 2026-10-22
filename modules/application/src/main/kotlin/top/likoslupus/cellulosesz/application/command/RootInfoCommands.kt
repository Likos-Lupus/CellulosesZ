package top.likoslupus.cellulosesz.application.command

import net.minecraft.SharedConstants
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.application.health.ApplicationHealth
import top.likoslupus.cellulosesz.core.command.dsl.CommandSyntax
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.text.MessageArgument
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.reply
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.model.DetailField
import top.likoslupus.cellulosesz.core.text.model.DetailView
import top.likoslupus.cellulosesz.core.text.model.LocalizedText
import top.likoslupus.cellulosesz.core.text.model.TextArgument
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation

/** `/cellulosesz` (bare) and `/cellulosesz status`. */
internal fun CommandSyntax.infoCommands(
    modVersion: String,
    loaderName: String,
    health: ApplicationHealth,
    messages: LocalizedMessages,
    adventure: AdventureRuntime,
    presentation: TextPresentation,
) {
    executes {
        showInfo(
            source,
            modVersion,
            loaderName,
            messages,
            adventure
        )
    }
    literal("status", permission = CommandPermissions.ROOT_STATUS) {
        executes {
            showStatus(
                source,
                health,
                presentation,
                adventure
            )
        }
    }
}

private fun showInfo(
    source: CommandSourceStack,
    modVersion: String,
    loaderName: String,
    messages: LocalizedMessages,
    adventure: AdventureRuntime,
): Int {
    val playerId = source.player?.uuid
    val minecraftVersion = SharedConstants.getCurrentVersion().name()
    source.reply(
        adventure,
        messages.renderFor(
            playerId,
            MessageKey("core.info.version"),
            listOf(MessageArgument(modVersion))
        )
    )
    source.reply(
        adventure,
        messages.renderFor(
            playerId,
            MessageKey("core.info.loader"),
            listOf(MessageArgument(loaderName))
        )
    )
    source.reply(
        adventure,
        messages.renderFor(
            playerId,
            MessageKey("core.info.minecraft"),
            listOf(MessageArgument(minecraftVersion))
        )
    )
    return source.reply(
        adventure,
        messages.renderFor(playerId, MessageKey("core.info.hint"))
    )
}

private fun showStatus(
    source: CommandSourceStack,
    health: ApplicationHealth,
    presentation: TextPresentation,
    adventure: AdventureRuntime,
): Int {
    val detail = DetailView(
        title = LocalizedText(MessageKey("core.status.title")),
        fields = listOf(
            field(MessageKey("core.status.state"), health.state.name),
            field(MessageKey("core.status.loader"), health.loader),
            field(MessageKey("core.status.permissions"), health.permissionBackend),
            field(MessageKey("core.status.storage"), health.storageType ?: "-"),
            field(MessageKey("core.status.endpoint"), health.storageEndpoint ?: "-"),
            field(MessageKey("core.status.driver"), health.driverSource ?: "-"),
            field(MessageKey("core.status.identity"), health.identityRecords.toString()),
            field(MessageKey("core.status.migration"), health.migration),
        ),
    )
    val lines = presentation.renderFor(source.player?.uuid, detail)
    lines.forEach { source.reply(adventure, it) }
    return if (lines.isEmpty()) 0 else 1
}

private fun field(label: MessageKey, value: String): DetailField =
    DetailField(
        label = LocalizedText(label),
        value = LocalizedText(
            MessageKey("core.status.value"),
            listOf(TextArgument.Literal(value))
        ),
    )
