package top.likoslupus.cellulosesz.modules.playerstate.command;

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
import top.likoslupus.cellulosesz.common.player.MinecraftPlayers;
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
                .then(Commands.literal("on")
                        .executes(command -> self(
                                context,
                                command,
                                descriptor,
                                Optional.of(true)
                        ))
                )
                .then(Commands.literal("off")
                        .executes(command -> self(
                                context,
                                command,
                                descriptor,
                                Optional.of(false)
                        ))
                )
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(source -> context.permissions().has(
                                source,
                                "cellulosesz.playerstate.fly.other"
                        ))
                        .executes(command -> other(
                                context,
                                command,
                                descriptor,
                                Optional.empty()
                        ))
                        .then(Commands.literal("on")
                                .executes(command -> other(
                                        context,
                                        command,
                                        descriptor,
                                        Optional.of(true)
                                ))
                        )
                        .then(Commands.literal("off")
                                .executes(command -> other(
                                        context,
                                        command,
                                        descriptor,
                                        Optional.of(false)
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
    ) throws CommandSyntaxException {
        var target = MinecraftPlayers.wrap(
                EntityArgument.getPlayer(command, "player")
        );

        return PlayerStateCommandSupport.async(
                context,
                command,
                descriptor,
                "fly other",
                _ -> service.fly(
                        target,
                        state
                )
        );
    }

    @Override
    public String moduleId() {
        return PlayerStateCommandSupport.MODULE;
    }

}
