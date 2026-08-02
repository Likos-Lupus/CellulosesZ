package top.likoslupus.cellulosesz.modules.admin.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.admin.application.ModerationCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class KickCommand implements CommandContributor {

    private final ModerationCommandService service;
    private final PlayerDirectory players;

    public KickCommand(
            ModerationCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = AdminCommandResults.descriptor(
                "kick",
                "cellulosesz.admin.kick",
                CommandSourceKind.ANY
        );

        var target = Commands.argument(
                        "player",
                        EntityArgument.player()
                )
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        ""
                ))
                .then(Commands.argument(
                                        "reason",
                                        StringArgumentType.greedyString()
                                )
                                .executes(command -> execute(
                                        context,
                                        command,
                                        descriptor,
                                        StringArgumentType.getString(
                                                command,
                                                "reason"
                                        )
                                ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.kick",
                "/kick <player> [reason]",
                Commands.literal("kick").then(target)
        );
    }

    private int execute(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            String reason
    ) throws CommandSyntaxException {
        var targetName = EntityArgument.getPlayer(command, "player")
                .getGameProfile()
                .name();

        return AdminCommandResults.async(
                registration,
                command,
                descriptor,
                "kick reason-present=" + !reason.isBlank(),
                policy -> service.kick(
                        targetName,
                        AdminCommandResults.actor(policy, players),
                        reason
                )
        );
    }

    @Override
    public String moduleId() {
        return AdminCommandResults.MODULE;
    }

}
