package top.likoslupus.cellulosesz.core.text.presentation

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.model.LocalizedText
import top.likoslupus.cellulosesz.core.text.model.TextArgument
import top.likoslupus.cellulosesz.core.text.model.UiAction
import top.likoslupus.cellulosesz.core.text.theme.TextRole

/**
 * Renders a constrained [UiAction] into a component. Only well-known intents produce a click event;
 * arbitrary strings never become a runnable command. A player-name argument that does not satisfy the
 * command token grammar degrades to plain text. On a console channel there is no click: the label is
 * shown with the equivalent command text so RCON/console output stays readable.
 */
internal object ActionRenderer {

    private val NAME_PATTERN = Regex("[A-Za-z0-9_]{1,16}")

    fun render(
        messages: LocalizedMessages,
        context: RenderContext,
        action: UiAction,
    ): Component {
        val color = context.theme.palette.color(TextRole.ACTION)
        val label = messages.render(context.language, action.label).color(color)
        val command = commandOf(action)

        if (context.channel == DisplayChannel.CONSOLE) {
            return if (command == null) {
                label
            } else {
                label.append(
                    messages.renderColored(
                        context.language,
                        LocalizedText(
                            MessageKey("core.action.run_hint"),
                            listOf(TextArgument.Literal(command))
                        ),
                        color,
                    )
                )
            }
        }

        return when (action) {
            is UiAction.SuggestCommand ->
                clickable(
                    label,
                    ClickEvent.suggestCommand(action.command),
                    action.command
                )

            is UiAction.HelpPage ->
                clickable(
                    label,
                    ClickEvent.suggestCommand("/help ${action.commandPath} --page ${action.page}"),
                    "/help ${action.commandPath} --page ${action.page}",
                )

            is UiAction.TeleportDecision -> {
                val base = if (action.accept) "/tpaccept" else "/tpdeny"
                when {
                    NAME_PATTERN.matches(action.senderName) ->
                        clickable(
                            label,
                            ClickEvent.suggestCommand("$base ${action.senderName}"),
                            "$base ${action.senderName}"
                        )

                    else -> label.append(
                        messages.renderColored(
                            context.language,
                            LocalizedText(
                                MessageKey("core.action.run_hint"),
                                listOf(TextArgument.Literal(base))
                            ),
                            color,
                        )
                    )
                }
            }

            is UiAction.OpenHttps ->
                when {
                    action.url.startsWith("https://") -> label.clickEvent(ClickEvent.openUrl(action.url))
                    else -> label
                }
        }
    }

    private fun commandOf(action: UiAction): String? =
        when (action) {
            is UiAction.SuggestCommand -> action.command
            is UiAction.HelpPage -> "/help ${action.commandPath} --page ${action.page}"
            is UiAction.TeleportDecision -> {
                val base = if (action.accept) "/tpaccept" else "/tpdeny"
                if (NAME_PATTERN.matches(action.senderName)) "$base ${action.senderName}" else base
            }

            is UiAction.OpenHttps -> action.url
        }

    private fun clickable(
        label: Component,
        click: ClickEvent,
        hint: String,
    ): Component =
        label
                .clickEvent(click)
                .hoverEvent(HoverEvent.showText(Component.text(hint)))

}
