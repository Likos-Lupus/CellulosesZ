package top.likoslupus.cellulosesz.core.command

import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.player.PlayerResolver
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.i18n.renderFor
import java.util.*

/** A resolved target for a self/other toggle command. */
public data class ToggleTarget(
    public val id: UUID,
    public val name: String,
)

/**
 * Resolves the optional target of a toggle command: the executing player when [targetName] is null,
 * otherwise an online player. The caller supplies the unknown-player message key because economy and
 * movement phrase that failure differently; everything else is identical.
 */
public fun resolveToggleTarget(
    source: CommandSourceStack,
    actorId: UUID?,
    targetName: String?,
    messages: LocalizedMessages,
    adventure: AdventureRuntime,
    unknownPlayerKey: MessageKey,
): ToggleTarget? {
    if (targetName == null) {
        val player = source.player
            ?: run {
                source.replyError(
                    adventure,
                    messages.renderFor(
                        null,
                        MessageKey("core.error.player_required")
                    ),
                )
                return null
            }
        return ToggleTarget(player.uuid, player.gameProfile.name)
    }
    val resolved = PlayerResolver.onlineByName(source.server, targetName)
        ?: run {
            source.replyError(
                adventure,
                messages.renderFor(actorId, unknownPlayerKey, targetName),
            )
            return null
        }
    return ToggleTarget(resolved.uuid, resolved.name.string)
}

/** Online player names for tab completion, in server list order. */
public fun onlineNames(source: CommandSourceStack): List<String> =
    source.server.playerList.players.map { it.gameProfile.name }
