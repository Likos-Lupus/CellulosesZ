package top.likoslupus.cellulosesz.administration

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.administration.moderation.ModerationCommands
import top.likoslupus.cellulosesz.administration.playerstate.PlayerStateCommands

/**
 * Public surface of the administration bounded context. Internals stay `internal`.
 */
public class AdministrationFeature internal constructor() {

    public fun registerCommands(dispatcher: CommandDispatcher<CommandSourceStack>) {
        ModerationCommands.register(dispatcher)
        PlayerStateCommands.register(dispatcher)
    }

}

public fun createAdministrationFeature(): AdministrationFeature =
    AdministrationFeature()
