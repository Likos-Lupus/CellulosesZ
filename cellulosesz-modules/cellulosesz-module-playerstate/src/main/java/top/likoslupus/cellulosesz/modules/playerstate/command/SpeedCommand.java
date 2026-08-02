package top.likoslupus.cellulosesz.modules.playerstate.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.platform.MovementSpeedType;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayers;
import top.likoslupus.cellulosesz.modules.playerstate.application.PlayerAbilityCommandService;
import top.likoslupus.cellulosesz.modules.playerstate.application.PlayerStateCommandResult;
import top.likoslupus.cellulosesz.modules.playerstate.application.PlayerStateCommandSettings;
import top.likoslupus.cellulosesz.modules.playerstate.command.argument.FiniteSpeedArgument;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class SpeedCommand implements CommandContributor {

    private final PlayerAbilityCommandService service;
    private final PlayerDirectory players;
    private final PlayerStateCommandSettings settings;

    public SpeedCommand(
            PlayerAbilityCommandService service,
            PlayerDirectory players,
            PlayerStateCommandSettings settings
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
        this.settings = requireNonNull(settings, "settings");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = PlayerStateCommandSupport.descriptor(
                "speed",
                "cellulosesz.playerstate.speed",
                CommandSourceKind.ANY
        );

        var root = Commands.literal("speed")
                .then(Commands.argument(
                                        "speed",
                                        FiniteSpeedArgument.speed(
                                                settings.minimumSpeed(),
                                                settings.maximumSpeed()
                                        )
                                )
                                .executes(command ->
                                        PlayerStateCommandSupport.async(
                                                context,
                                                command,
                                                descriptor,
                                                "speed current",
                                                policy -> PlayerStateCommandSupport.currentPlayer(
                                                                policy,
                                                                players
                                                        )
                                                        .map(player ->
                                                                service.speedForCurrentMovement(
                                                                        player,
                                                                        FiniteSpeedArgument.get(
                                                                                command,
                                                                                "speed"
                                                                        )
                                                                )
                                                        )
                                                        .orElseGet(() ->
                                                                CompletableFuture.completedFuture(
                                                                        PlayerStateCommandResult.failure(
                                                                                "common.player-only"
                                                                        )
                                                                )
                                                        )
                                        )
                                )
                );

        Arrays.stream(MovementSpeedType.values())
                .forEach(type -> {
                    var typeName = type.name().toLowerCase(Locale.ROOT);
                    root.then(Commands.literal(typeName)
                            .requires(source -> context.permissions().has(
                                    source,
                                    "cellulosesz.playerstate.speed." + typeName
                            ))
                            .then(Commands.argument(
                                                    "speed",
                                                    FiniteSpeedArgument.speed(
                                                            settings.minimumSpeed(),
                                                            settings.maximumSpeed()
                                                    )
                                            )
                                            .executes(command -> self(
                                                    context,
                                                    command,
                                                    descriptor,
                                                    type
                                            ))
                                            .then(Commands.argument(
                                                                    "player",
                                                                    EntityArgument.player()
                                                            )
                                                            .requires(source ->
                                                                    context.permissions().has(
                                                                            source,
                                                                            "cellulosesz.playerstate.speed.others"
                                                                    )
                                                            )
                                                            .executes(command -> other(
                                                                    context,
                                                                    command,
                                                                    descriptor,
                                                                    type
                                                            ))
                                            )
                            )
                    );
                });

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.speed",
                "/speed <speed> | /speed <walk|fly> <speed> [player]",
                root
        );
    }

    private int self(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            MovementSpeedType type
    ) {
        return PlayerStateCommandSupport.async(
                context,
                command,
                descriptor,
                "speed self",
                policy -> PlayerStateCommandSupport.currentPlayer(
                                policy,
                                players
                        )
                        .map(player -> service.speed(
                                player,
                                type,
                                FiniteSpeedArgument.get(
                                        command,
                                        "speed"
                                )
                        ))
                        .orElseGet(() ->
                                CompletableFuture.completedFuture(
                                        PlayerStateCommandResult.failure(
                                                "common.player-only"
                                        )
                                )
                        )
        );
    }

    private int other(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            MovementSpeedType type
    ) {
        return PlayerStateCommandSupport.async(
                context,
                command,
                descriptor,
                "speed other",
                _ -> service.speed(
                        MinecraftPlayers.wrap(EntityArgument.getPlayer(command, "player")),
                        type,
                        FiniteSpeedArgument.get(command, "speed")
                )
        );
    }

    @Override
    public String moduleId() {
        return PlayerStateCommandSupport.MODULE;
    }

}
