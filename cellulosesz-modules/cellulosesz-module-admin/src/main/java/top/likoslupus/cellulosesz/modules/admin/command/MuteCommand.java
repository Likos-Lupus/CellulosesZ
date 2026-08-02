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
import top.likoslupus.cellulosesz.modules.admin.application.ModerationCommandService;
import top.likoslupus.cellulosesz.modules.admin.command.argument.DurationArgument;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class MuteCommand implements CommandContributor {

    private final ModerationCommandService service;
    private final PlayerDirectory players;
    private final Duration maximum;

    public MuteCommand(
            ModerationCommandService service,
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
                "mute",
                "cellulosesz.admin.mute",
                CommandSourceKind.ANY
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
                .executes(command -> mute(
                        context,
                        command,
                        descriptor,
                        Optional.empty(),
                        ""
                ));

        player.then(Commands.literal("off")
                .executes(command -> AdminCommandResults.async(
                        context,
                        command,
                        descriptor,
                        "mute off",
                        policy -> service.unmute(
                                StringArgumentType.getString(command, "player"),
                                AdminCommandResults.actor(
                                        policy,
                                        players
                                )
                        )
                ))
        );

        player.then(Commands.literal("reason")
                .then(Commands.argument(
                                        "reason",
                                        StringArgumentType.greedyString()
                                )
                                .executes(command -> mute(
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

        player.then(Commands.argument(
                                "duration",
                                DurationArgument.duration(maximum)
                        )
                        .executes(command -> mute(
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
                                        .executes(command -> mute(
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

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.mute",
                "/mute <player> [off|duration|reason <text>]",
                Commands.literal("mute").then(player)
        );
    }

    private int mute(
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
                "mute duration=%s reason-present=%s".formatted(
                        duration.isPresent(),
                        !reason.isBlank()
                ),
                policy -> service.mute(
                        StringArgumentType.getString(command, "player"),
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
