package top.likoslupus.cellulosesz.modules.playerstate.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.common.command.argument.PlayerNameArgument;
import top.likoslupus.cellulosesz.common.command.argument.ToggleArgument;
import top.likoslupus.cellulosesz.modules.playerstate.application.PlayerAbilityCommandService;
import top.likoslupus.cellulosesz.modules.playerstate.application.PlayerStateCommandResult;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class FlyCommand implements CommandContributor {

    private final PlayerAbilityCommandService service;
    private final PlayerDirectory players;

    public FlyCommand(
            PlayerAbilityCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = PlayerStateCommandSupport.descriptor(
                "fly",
                "cellulosesz.playerstate.fly",
                CommandSourceKind.ANY
        );

        var root = Commands.literal("fly")
                .executes(command -> self(
                        context,
                        command,
                        descriptor,
                        Optional.empty()
                ))
                .then(Commands.argument(
                                        "state",
                                        ToggleArgument.toggle()
                                )
                                .executes(command -> self(
                                        context,
                                        command,
                                        descriptor,
                                        Optional.of(
                                                ToggleArgument.get(
                                                        command,
                                                        "state"
                                                ).enabled()
                                        )
                                ))
                )
                .then(Commands.argument(
                                        "player",
                                        PlayerNameArgument.playerNameWithoutToggleWords()
                                )
                                .requires(source -> context.permissions().has(
                                        source,
                                        "cellulosesz.playerstate.fly.other"
                                ))
                                .suggests((_, builder) ->
                                        CommandSuggestionSupport.suggest(
                                                players::onlinePlayerNames,
                                                builder
                                        )
                                )
                                .executes(command -> other(
                                        context,
                                        command,
                                        descriptor,
                                        Optional.empty()
                                ))
                                .then(Commands.argument(
                                                        "state",
                                                        ToggleArgument.toggle()
                                                )
                                                .executes(command -> other(
                                                        context,
                                                        command,
                                                        descriptor,
                                                        Optional.of(
                                                                ToggleArgument.get(
                                                                        command,
                                                                        "state"
                                                                ).enabled()
                                                        )
                                                ))
                                )
                );

        var node = context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.fly",
                "/fly [player] [on|off]",
                root
        );
    }

    private int self(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            Optional<Boolean> state
    ) {
        return PlayerStateCommandSupport.async(
                context,
                command,
                descriptor,
                "fly self",
                policy -> PlayerStateCommandSupport.currentPlayer(
                                policy,
                                players
                        )
                        .map(player -> service.fly(player, state))
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
            Optional<Boolean> state
    ) {
        return PlayerStateCommandSupport.async(
                context,
                command,
                descriptor,
                "fly other",
                _ -> {
                    var name = PlayerNameArgument.get(
                            command,
                            "player"
                    );

                    return players.onlinePlayer(name)
                            .map(player -> service.fly(player, state))
                            .orElseGet(() ->
                                    PlayerStateCommandSupport.offline(name)
                            );
                }
        );
    }

    @Override
    public String moduleId() {
        return PlayerStateCommandSupport.MODULE;
    }

}
