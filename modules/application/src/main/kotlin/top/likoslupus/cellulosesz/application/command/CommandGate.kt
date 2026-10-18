package top.likoslupus.cellulosesz.application.command

import net.minecraft.network.chat.Component
import top.likoslupus.cellulosesz.administration.AdministrationFeature
import top.likoslupus.cellulosesz.application.config.CommandRoutingSettings
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.command.CommandInterceptor
import top.likoslupus.cellulosesz.core.command.CommandInvocation
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.permission.PermissionSpec
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.model.LocalizedText
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.core.text.model.NoticeTone
import top.likoslupus.cellulosesz.core.text.model.TextArgument
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.economy.integration.*
import java.util.*

/**
 * The composition-root command gate, applied to every CellulosesZ execution in this order:
 * mute -> cooldown -> social spy -> cost -> body. A gate may defer the body (cost charging is a
 * suspension) but calls `proceed` at most once, on the server thread. Permissions are still evaluated
 * by Brigadier before this runs.
 */
internal class CommandGate(
    private val kernel: RuntimeKernel,
    private val settings: () -> CommandRoutingSettings,
    private val permissions: PermissionService,
    private val administration: AdministrationFeature,
    private val economy: EconomyIntegrationPort,
    private val cooldowns: CommandCooldownStore,
    private val presentation: TextPresentation,
    private val adventure: AdventureRuntime,
) : CommandInterceptor {

    override fun intercept(invocation: CommandInvocation, proceed: () -> Unit) {
        val config = settings()
        val playerId = invocation.playerId
        val name = invocation.commandName

        if (playerId != null
            && administration.isMuted(playerId)
            && mutedBlocks(name)
        ) {
            deny(invocation, "core.command.muted")
            return
        }

        if (playerId != null &&
            !exempt(invocation, CommandPermissions.COMMAND_COOLDOWN_EXEMPT)
        ) {
            val remaining = cooldowns.remainingSeconds(
                playerId,
                name,
                System.currentTimeMillis()
            )
            if (remaining > 0) {
                deny(invocation, "core.command.cooldown", remaining)
                return
            }
        }

        administration.observeCommand(playerId, name)

        val cost = config.costs[name]?.toBigDecimalOrNull()
        if (cost == null || cost.signum() <= 0 || playerId == null ||
            exempt(invocation, CommandPermissions.COMMAND_COST_EXEMPT)
        ) {
            markAndProceed(invocation, config, proceed)
            return
        }

        kernel.launch {
            val result = economy.take(
                EconomyTakeRequest(
                    actor = EconomyActorDescriptor(
                        EconomyActorKind.INTERNAL,
                        "cellulosesz:command",
                    ),
                    playerId = playerId,
                    amount = cost,
                    originId = "command",
                    requestKey = UUID.randomUUID().toString(),
                    reasonCode = "command.$name",
                )
            )
            kernel.onServerThread {
                when (result) {
                    is EconomyTransactionResult.Committed,
                    is EconomyTransactionResult.AlreadyCommitted ->
                        markAndProceed(
                            invocation,
                            config,
                            proceed
                        )

                    else ->
                        deny(
                            invocation,
                            "core.command.cost"
                        )
                }
            }
        }
    }

    private fun markAndProceed(
        invocation: CommandInvocation,
        config: CommandRoutingSettings,
        proceed: () -> Unit,
    ) {
        val playerId = invocation.playerId
        val seconds = config.cooldowns[invocation.commandName]
            ?: config.cooldowns["*"] ?: 0
        if (playerId != null && seconds > 0) {
            val expiresAt = System.currentTimeMillis() + seconds * 1_000L
            cooldowns.mark(playerId, invocation.commandName, expiresAt)
            if (config.cooldownPersistence) {
                kernel.launch {
                    cooldowns.persist(
                        playerId,
                        invocation.commandName,
                        expiresAt
                    )
                }
            }
        }
        // Re-check: the player may have disconnected while a cost was being charged, and the server
        // may already be stopping.
        val stillOnline = playerId == null ||
                runCatching {
                    kernel.requireServer().playerList.getPlayer(playerId) != null
                }.getOrDefault(false)
        if (stillOnline) {
            proceed()
        }
    }

    private fun mutedBlocks(command: String): Boolean {
        val roots = administration.mutedCommandRoots()
        return roots.any { it == "*" || it == command }
    }

    private fun exempt(
        invocation: CommandInvocation,
        spec: PermissionSpec
    ): Boolean =
        invocation.source.player != null
                && permissions.allows(invocation.source, spec)

    private fun deny(
        invocation: CommandInvocation,
        key: String,
        remaining: Long? = null
    ) {
        val playerId = invocation.playerId
        if (playerId == null) {
            invocation.source.sendFailure(Component.literal(key))
            return
        }
        val arguments = remaining
                ?.let { listOf(TextArgument.Literal(it.toString())) }
            ?: emptyList()
        val notice = Notice(
            LocalizedText(MessageKey(key), arguments),
            NoticeTone.WARNING
        )
        presentation.renderFor(playerId, notice).forEach { line ->
            adventure
                    .audience(CommandFeedbackTarget.Player(playerId))
                    .sendMessage(line)
        }
    }

}
