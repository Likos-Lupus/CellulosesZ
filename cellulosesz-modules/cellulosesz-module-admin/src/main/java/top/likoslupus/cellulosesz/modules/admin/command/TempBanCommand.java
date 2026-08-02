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
import top.likoslupus.cellulosesz.modules.admin.command.argument.DurationArgument;

import java.time.Duration;
import java.util.List;

import static java.util.Objects.requireNonNull;

public final class TempBanCommand implements CommandContributor {

    private final BanCommandService service;
    private final PlayerDirectory players;
    private final Duration maximum;

    public TempBanCommand(
            BanCommandService service,
            PlayerDirectory players,
            Duration maximum
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
        this.maximum = requireNonNull(maximum, "maximum");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = AdminCommandResults.descriptor(
                "tempban",
                "cellulosesz.admin.tempban",
                CommandSourceKind.ANY
        );

        var duration = Commands.argument(
                        "duration",
                        DurationArgument.duration(maximum)
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

        var player = Commands.argument(
                        "player",
                        StringArgumentType.word()
                )
                .suggests((_, builder) ->
                        CommandSuggestionSupport.suggest(
                                players::onlinePlayerNames,
                                builder
                        )
                )
                .then(duration);

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.tempban",
                "/tempban <player> <duration> [reason]",
                Commands.literal("tempban").then(player)
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
                "tempban reason-present=" + !reason.isBlank(),
                policy -> service.tempBan(
                        StringArgumentType.getString(command, "player"),
                        AdminCommandResults.actor(policy, players),
                        DurationArgument.get(command, "duration"),
                        reason
                )
        );
    }

    @Override
    public String moduleId() {
        return AdminCommandResults.MODULE;
    }

}
