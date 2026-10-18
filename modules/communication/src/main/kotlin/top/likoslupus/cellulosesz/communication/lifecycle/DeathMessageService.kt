package top.likoslupus.cellulosesz.communication.lifecycle

import net.kyori.adventure.text.Component
import net.minecraft.server.level.ServerPlayer
import top.likoslupus.cellulosesz.communication.config.MessagingSettings
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.model.LocalizedText
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.core.text.model.NoticeTone
import top.likoslupus.cellulosesz.core.text.model.TextArgument
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation

/**
 * Production death-message policy. The mixin redirects the vanilla broadcast here: a non-blank
 * `customFormat` suppresses and replaces it, while `showCoordinates` always tells the dead player
 * where they died. All work runs on the server thread.
 */
internal class DeathMessageService(
    private val kernel: RuntimeKernel,
    private val settings: () -> MessagingSettings,
    private val adventure: AdventureRuntime,
    private val presentation: TextPresentation,
) : DeathMessageController {

    fun install() {
        DeathMessageState.install(this)
    }

    fun uninstall() {
        DeathMessageState.uninstall()
    }

    override fun suppressesVanilla(): Boolean {
        val messaging = settings()
        val config = messaging.death
        return messaging.enabled
                && config.enabled
                && !config.customFormat.isNullOrBlank()
    }

    override fun onDeath(
        player: ServerPlayer,
        vanillaMessage: String?,
    ) {
        val messaging = settings()
        val config = messaging.death
        if (!messaging.enabled || !config.enabled) {
            return
        }

        val playerId = player.uuid
        val worldId = player.level().dimension().identifier().toString()
        val x = player.x
        val y = player.y
        val z = player.z

        if (config.showCoordinates) {
            sendCoordinates(
                playerId,
                worldId,
                x,
                y,
                z,
                config.coordinatesFormat
            )
        }

        val format = config.customFormat
        if (format.isNullOrBlank()) {
            return
        }
        val values = mapOf(
            "PLAYER" to JoinQuitRenderer.escape(player.displayName.string),
            "DISPLAYNAME" to JoinQuitRenderer.escape(player.displayName.string),
            "USERNAME" to JoinQuitRenderer.escape(player.gameProfile.name),
            "WORLD" to worldId,
            "X" to DeathCoordinates.coordinate(x),
            "Y" to DeathCoordinates.coordinate(y),
            "Z" to DeathCoordinates.coordinate(z),
            "MESSAGE" to JoinQuitRenderer.escape(vanillaMessage.orEmpty()),
        )
        broadcast(
            JoinQuitRenderer.parse(
                JoinQuitRenderer.resolve(format, values)
            )
        )
    }

    private fun sendCoordinates(
        playerId: java.util.UUID,
        worldId: String,
        x: Double,
        y: Double,
        z: Double,
        template: String,
    ) {
        val target = CommandFeedbackTarget.Player(playerId)
        if (template.isNotBlank()) {
            val component = JoinQuitRenderer.parse(
                JoinQuitRenderer.resolve(
                    template,
                    mapOf(
                        "WORLD" to worldId,
                        "X" to DeathCoordinates.coordinate(x),
                        "Y" to DeathCoordinates.coordinate(y),
                        "Z" to DeathCoordinates.coordinate(z),
                    ),
                ),
            )
            adventure.audience(target).sendMessage(component)
            return
        }

        val notice = Notice(
            LocalizedText(
                MessageKey("communication.death.coordinates"),
                listOf(
                    TextArgument.Literal(DeathCoordinates.coordinate(x)),
                    TextArgument.Literal(DeathCoordinates.coordinate(y)),
                    TextArgument.Literal(DeathCoordinates.coordinate(z)),
                    TextArgument.Literal(worldId),
                ),
            ),
            NoticeTone.INFO,
        )
        presentation.renderFor(playerId, notice).forEach {
            adventure.audience(target).sendMessage(it)
        }
    }

    private fun broadcast(component: Component) {
        val server = kernel.requireServer()
        server.playerList.players.forEach { online ->
            adventure
                    .audience(CommandFeedbackTarget.Player(online.uuid))
                    .sendMessage(component)
        }
        adventure
                .audience(CommandFeedbackTarget.Server)
                .sendMessage(component)
    }

}
