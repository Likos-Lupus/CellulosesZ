package top.likoslupus.cellulosesz.modules.admin.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.modules.admin.application.BanCommandService;
import top.likoslupus.cellulosesz.modules.admin.command.argument.AdminDurations;
import top.likoslupus.cellulosesz.modules.admin.command.argument.NetworkTargets;

import java.time.Duration;
import java.util.List;

import static java.util.Objects.requireNonNull;

public final class TempBanIpCommand implements CommandContributor {

    private final BanCommandService service;
    private final PlayerDirectory players;
    private final Duration maximum;

    public TempBanIpCommand(
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
                "tempbanip",
                "cellulosesz.admin.tempbanip",
                CommandSourceKind.ANY
        );

        var duration = Commands.argument(
                        "duration",
                        StringArgumentType.word()
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

        var target = Commands.argument(
                        "target",
                        StringArgumentType.string()
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
                "commands.description.tempbanip",
                "/tempbanip <address-or-player|\"ipv6\"> <duration> [reason]",
                Commands.literal("tempbanip").then(target)
        );
    }

    private int execute(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            String reason
    ) throws CommandSyntaxException {
        var target = NetworkTargets.addressOrPlayer(
                StringArgumentType.getString(command, "target")
        );
        var duration = AdminDurations.parse(
                StringArgumentType.getString(command, "duration"),
                maximum
        );
        return AdminCommandResults.async(
                registration,
                command,
                descriptor,
                "tempbanip reason-present=" + !reason.isBlank(),
                policy -> service.tempBanIp(
                        target,
                        AdminCommandResults.actor(policy, players),
                        duration,
                        reason
                )
        );
    }

    @Override
    public String moduleId() {
        return AdminCommandResults.MODULE;
    }

}
