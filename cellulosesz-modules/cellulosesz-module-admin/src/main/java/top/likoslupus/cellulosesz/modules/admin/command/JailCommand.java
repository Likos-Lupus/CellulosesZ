package top.likoslupus.cellulosesz.modules.admin.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.admin.Jail;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.common.command.argument.PlayerNameArgument;
import top.likoslupus.cellulosesz.modules.admin.application.JailCommandService;
import top.likoslupus.cellulosesz.modules.admin.command.argument.DurationArgument;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class JailCommand implements CommandContributor {

    private final JailCommandService service;
    private final PlayerDirectory players;
    private final Duration maximum;

    public JailCommand(
            JailCommandService service,
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
                "jail",
                "cellulosesz.admin.jail",
                CommandSourceKind.ANY
        );

        var player = Commands.argument(
                        "player",
                        PlayerNameArgument.playerName()
                )
                .suggests((_, builder) ->
                        CommandSuggestionSupport.suggest(
                                players::onlinePlayerNames,
                                builder
                        )
                );

        player.then(Commands.literal("off")
                .executes(command -> AdminCommandResults.async(
                        context,
                        command,
                        descriptor,
                        "jail off",
                        policy -> service.unjail(
                                PlayerNameArgument.get(
                                        command,
                                        "player"
                                ),
                                AdminCommandResults.actor(
                                        policy,
                                        players
                                )
                        )
                ))
        );

        var jail = Commands.argument(
                        "jail",
                        StringArgumentType.word()
                )
                .suggests((_, builder) ->
                        CommandSuggestionSupport.suggest(
                                () -> service.jails()
                                        .stream()
                                        .map(Jail::name)
                                        .toList(),
                                builder
                        )
                )
                .executes(command -> jail(
                        context,
                        command,
                        descriptor,
                        Optional.empty(),
                        ""
                ));

        jail.then(Commands.literal("reason")
                .then(Commands.argument(
                                        "reason",
                                        StringArgumentType.greedyString()
                                )
                                .executes(command -> jail(
                                        context,
                                        command,
                                        descriptor,
                                        Optional.empty(),
                                        StringArgumentType.getString(
                                                command,
                                                "reason"
                                        )
                                ))
                )
        );

        jail.then(Commands.argument(
                                "duration",
                                DurationArgument.duration(maximum)
                        )
                        .executes(command -> jail(
                                context,
                                command,
                                descriptor,
                                Optional.of(
                                        DurationArgument.get(
                                                command,
                                                "duration"
                                        )
                                ),
                                ""
                        ))
                        .then(Commands.argument(
                                                "reason",
                                                StringArgumentType.greedyString()
                                        )
                                        .executes(command -> jail(
                                                context,
                                                command,
                                                descriptor,
                                                Optional.of(
                                                        DurationArgument.get(
                                                                command,
                                                                "duration"
                                                        )
                                                ),
                                                StringArgumentType.getString(
                                                        command,
                                                        "reason"
                                                )
                                        ))
                        )
        );

        player.then(jail);

        var root = context.registerDirect(
                moduleId(),
                descriptor,
                List.of("togglejail"),
                "commands.description.jail",
                "/jail <player> <off|jail> [duration] [reason]",
                Commands.literal("jail").then(player)
        );

        context.registerAlias(
                moduleId(),
                descriptor,
                "togglejail",
                root
        );
    }

    private int jail(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            Optional<Duration> duration,
            String reason
    ) {
        return AdminCommandResults.async(
                registration,
                command,
                descriptor,
                "jail duration="
                        + duration.isPresent()
                        + " reason-present="
                        + !reason.isBlank(),
                policy -> service.jail(
                        PlayerNameArgument.get(command, "player"),
                        StringArgumentType.getString(command, "jail"),
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
