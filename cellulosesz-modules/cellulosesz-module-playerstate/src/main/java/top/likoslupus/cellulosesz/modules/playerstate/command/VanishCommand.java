package top.likoslupus.cellulosesz.modules.playerstate.command;

import com.mojang.brigadier.context.CommandContext;
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

public final class VanishCommand implements CommandContributor {

    private final PlayerAbilityCommandService service;
    private final PlayerDirectory players;

    public VanishCommand(
            PlayerAbilityCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = PlayerStateCommandSupport.descriptor(
                "vanish",
                "cellulosesz.playerstate.vanish",
                CommandSourceKind.PLAYER_ONLY
        );

        var root = Commands.literal("vanish")
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
                                "cellulosesz.playerstate.vanish.other"
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
                List.of("v"),
                "commands.description.vanish",
                "/vanish [player] [on|off]",
                root
        );

        context.registerAlias(
                moduleId(),
                descriptor,
                "v",
                node
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
                "vanish self",
                policy -> PlayerStateCommandSupport.currentPlayer(
                                policy,
                                players
                        )
                        .map(player ->
                                service.vanish(
                                        player,
                                        state
                                )
                        )
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
                "vanish other",
                _ -> service.vanish(
                        MinecraftPlayers.wrap(EntityArgument.getPlayer(command, "player")),
                        state
                )
        );
    }

    @Override
    public String moduleId() {
        return PlayerStateCommandSupport.MODULE;
    }

}
