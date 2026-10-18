package top.likoslupus.cellulosesz.administration

import top.likoslupus.cellulosesz.administration.config.AdministrationSettings
import top.likoslupus.cellulosesz.administration.moderation.ModerationActor
import top.likoslupus.cellulosesz.administration.moderation.PlayerIdentity
import top.likoslupus.cellulosesz.administration.moderation.audit.ModerationAuditAction
import top.likoslupus.cellulosesz.administration.moderation.audit.ModerationAuditService
import top.likoslupus.cellulosesz.administration.socialspy.SocialSpyService
import top.likoslupus.cellulosesz.administration.vanish.VanishService
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.player.MinecraftKnownPlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.message
import top.likoslupus.cellulosesz.core.text.model.LocalizedText
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.core.text.model.NoticeTone
import top.likoslupus.cellulosesz.core.text.model.TextArgument
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import java.util.*

/**
 * Side-channel observations (social spy, audit, forced vanish) that other features trigger through
 * the facade. Every member must run on the server thread.
 */
internal class AdministrationObservation(
    private val kernel: RuntimeKernel,
    private val settings: () -> AdministrationSettings,
    private val spies: SocialSpyService,
    private val vanish: VanishService,
    private val known: MinecraftKnownPlayerResolver,
    private val audit: ModerationAuditService,
    private val adventure: AdventureRuntime,
    private val presentation: TextPresentation,
) {

    /** Social-spy command observation; the root command only, never its arguments. Server thread. */
    fun observeCommand(actorId: UUID?, command: String) {
        if (actorId == null || !settings().moderation.socialSpyCommands) {
            return
        }
        val recipients = spies.recipients(setOf(actorId))
        if (recipients.isEmpty()) {
            return
        }
        val actorName = kernel.requireServer().playerList
                .getPlayer(actorId)?.gameProfile?.name
            ?: actorId.toString()
        val notice = Notice(
            LocalizedText(
                MessageKey("administration.socialspy.command"),
                listOf(
                    TextArgument.PlayerName(actorName),
                    TextArgument.Literal(command)
                ),
            ),
            NoticeTone.INFO,
        )
        kernel.launch {
            recipients.forEach { recipientId ->
                presentation.renderFor(recipientId, notice).forEach { line ->
                    kernel.message(
                        CommandFeedbackTarget.Player(recipientId),
                        adventure,
                        line
                    )
                }
            }
        }
    }

    /** Forces vanish on for a silent join. Server-thread confined. */
    fun forceVanish(playerId: UUID) {
        vanish.forceVanish(playerId)
    }

    /**
     * Best-effort audit for an administrative utility action that mutates another player (for
     * example clearing their inventory). Given as primitives so utility never imports
     * administration. The game action already happened, so a failed write is only logged.
     */
    suspend fun recordInventoryClear(
        actorId: UUID,
        targetId: UUID,
        targetName: String,
        cleared: Boolean,
    ): Boolean =
        audit.record(
            actor = ModerationActor.Player(
                id = actorId,
                name = known.onlineById(actorId)?.name ?: actorId.toString(),
            ),
            action = ModerationAuditAction.CLEAR_INVENTORY,
            target = PlayerIdentity(targetId, targetName),
            reason = if (cleared) "cleared" else "failed",
        )

    /**
     * Called by the composition root after a private message is delivered. Given as primitives so
     * administration never imports communication types. Must be called on the server thread. The
     * spy copy is rendered once per recipient so each moderator sees their own language.
     */
    fun observePrivateMessage(
        senderId: UUID,
        senderName: String,
        targetId: UUID,
        targetName: String,
        text: String,
    ) {
        val recipients = spies.recipients(setOf(senderId, targetId))
        if (recipients.isEmpty()) return

        val copy = Notice(
            LocalizedText(
                MessageKey("administration.socialspy.copy"),
                listOf(
                    TextArgument.PlayerName(senderName),
                    TextArgument.PlayerName(targetName),
                    TextArgument.Literal(text),
                ),
            ),
            NoticeTone.INFO,
        )
        kernel.launch {
            recipients.forEach { recipientId ->
                val target = CommandFeedbackTarget.Player(recipientId)
                presentation.renderFor(recipientId, copy).forEach { line ->
                    kernel.message(target, adventure, line)
                }
            }
        }
    }

}
