package top.likoslupus.cellulosesz.modules.admin.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.modules.admin.application.BanCommandService;
import top.likoslupus.cellulosesz.modules.admin.command.argument.NetworkTargetArgument;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class BanIpCommand implements CommandContributor {

    private final BanCommandService service;
    private final PlayerDirectory players;

    public BanIpCommand(
            BanCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = AdminCommandResults.descriptor(
                "banip",
                "cellulosesz.admin.banip",
                CommandSourceKind.ANY
        );

        var argument = Commands.argument(
                        "target",
                        NetworkTargetArgument.addressOrPlayer()
                )
                .suggests((_, builder) ->
                        CommandSuggestionSupport.suggest(
                                players::onlinePlayerNames,
                                builder
                        )
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
                "commands.description.banip",
                "/banip <address-or-player> [reason]",
                Commands.literal("banip").then(argument)
        );
    }

    private int execute(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            String reason
    ) {
        return AdminCommandResults.async(
                registration,
                command,
                descriptor,
                "banip reason-present=" + !reason.isBlank(),
                policy -> service.banIp(
                        NetworkTargetArgument.get(command, "target"),
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
