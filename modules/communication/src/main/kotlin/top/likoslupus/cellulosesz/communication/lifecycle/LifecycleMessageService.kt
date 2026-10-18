package top.likoslupus.cellulosesz.communication.lifecycle

import net.kyori.adventure.text.Component
import top.likoslupus.cellulosesz.communication.config.MessagingSettings
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.permission.PermissionSpec
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import java.util.*

/**
 * Production lifecycle-announcement policy. It decides silent/vanished/threshold suppression, renders
 * the configured strict MiniMessage template, and performs the custom broadcast through the adventure
 * audience seam (the vanilla message is cancelled by the mixin when [handle] returns true).
 */
internal class LifecycleMessageService(
    private val kernel: RuntimeKernel,
    private val settings: () -> MessagingSettings,
    private val permissions: PermissionService,
    private val known: KnownPlayerResolver,
    private val adventure: AdventureRuntime,
    private val groupResolver: (UUID) -> String?,
    private val onlineCount: () -> Int,
    private val uniqueCount: () -> Int,
    private val uptime: () -> String,
    private val silentJoinVanish: (UUID) -> Unit,
    private val hiddenJoinQuit: (UUID) -> Boolean,
) : LifecycleMessageController {

    fun install() {
        LifecycleMessageState.install(this)
    }

    fun uninstall() {
        LifecycleMessageState.uninstall()
    }

    override fun prepareJoin(
        playerId: UUID,
        username: String,
        displayName: String,
        worldId: String,
    ): LifecyclePlayer {
        val previous = known.knownById(playerId)
        return LifecyclePlayer(
            playerId = playerId,
            username = username,
            displayName = displayName,
            worldId = worldId,
            firstJoin = previous == null,
            newUsername = previous != null
                    && !previous.name.equals(username, ignoreCase = true),
            previousName = previous?.name,
        )
    }

    override fun prepareQuit(
        playerId: UUID,
        username: String,
        displayName: String,
        worldId: String,
    ): LifecyclePlayer =
        LifecyclePlayer(
            playerId = playerId,
            username = username,
            displayName = displayName,
            worldId = worldId,
            firstJoin = false,
            newUsername = false,
            previousName = null,
        )

    override fun handle(
        kind: LifecycleKind,
        player: LifecyclePlayer,
    ): Boolean {
        val messaging = settings()
        val config = messaging.joinQuit
        if (!messaging.enabled || !config.enabled) {
            return false
        }
        if (hiddenJoinQuit(player.playerId)) {
            return true
        }
        if (config.allowSilentJoinQuit && allowed(
                player.playerId,
                CommandPermissions.SILENT_JOIN
            )
        ) {
            if (kind == LifecycleKind.JOIN
                && allowed(player.playerId, CommandPermissions.SILENT_JOIN_VANISH)
            ) {
                silentJoinVanish(player.playerId)
            }
            return true
        }
        if (
            JoinQuitRenderer.hideAbovePlayerCount(
                onlineCount(),
                config.hideAbovePlayerCount
            )
        ) {
            return true
        }

        val template = when (kind) {
            LifecycleKind.JOIN ->
                JoinQuitRenderer.selectJoinTemplate(
                    config,
                    player.firstJoin,
                    player.newUsername
                )

            LifecycleKind.QUIT -> config.quitFormat
        }
        broadcast(render(template, player))
        return true
    }

    private fun render(
        template: String,
        player: LifecyclePlayer,
    ): Component {
        val values = mapOf(
            "PLAYER" to JoinQuitRenderer.escape(player.displayName),
            "USERNAME" to JoinQuitRenderer.escape(player.username),
            "DISPLAYNAME" to JoinQuitRenderer.escape(player.displayName),
            "PREVIOUS_NAME" to JoinQuitRenderer.escape(player.previousName.orEmpty()),
            "WORLD" to player.worldId,
            "ONLINE" to onlineCount().toString(),
            "UNIQUE" to uniqueCount().toString(),
            "UPTIME" to uptime(),
            "GROUP" to groupResolver(player.playerId).orEmpty(),
        )
        return JoinQuitRenderer.parse(JoinQuitRenderer.resolve(template, values))
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

    private fun allowed(playerId: UUID, spec: PermissionSpec): Boolean {
        val player = kernel.requireServer().playerList.getPlayer(playerId)
            ?: return false
        return permissions.allows(player.createCommandSourceStack(), spec)
    }

}
