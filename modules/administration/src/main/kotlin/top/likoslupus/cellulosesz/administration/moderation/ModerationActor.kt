package top.likoslupus.cellulosesz.administration.moderation

import net.minecraft.commands.CommandSourceStack
import java.util.*

/** The immutable origin of a moderation action. */
internal sealed interface ModerationActor {

    data object Console : ModerationActor

    data class Player(
        val id: UUID,
        val name: String,
    ) : ModerationActor

    /** An authorized addon acting through the public API; the id is recorded in the audit trail. */
    data class Addon(
        val id: String,
    ) : ModerationActor

}

/** Snapshots the command source once so services never hold a [CommandSourceStack]. */
internal fun CommandSourceStack.moderationActor(): ModerationActor =
    player?.let {
        ModerationActor.Player(
            id = it.uuid,
            name = it.gameProfile.name
        )
    } ?: ModerationActor.Console
