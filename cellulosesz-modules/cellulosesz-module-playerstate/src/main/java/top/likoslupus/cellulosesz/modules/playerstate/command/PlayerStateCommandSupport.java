package top.likoslupus.cellulosesz.modules.playerstate.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.command.execution.CommandOutcome;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandExecutions;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.source.MinecraftCommandPolicyContext;
import top.likoslupus.cellulosesz.modules.playerstate.application.PlayerStateCommandResult;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

final class PlayerStateCommandSupport {

    static final String MODULE = "playerstate";

    private PlayerStateCommandSupport() {
    }

    static CommandDescriptor descriptor(
            String root,
            String permission,
            CommandSourceKind kind
    ) {
        return new CommandDescriptor(
                MODULE,
                root,
                permission,
                kind
        );
    }

    static int requirePlayer(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            String audit,
            PlayerDirectory players,
            Function<
                    CellPlayer,
                    CompletableFuture<PlayerStateCommandResult>
                    > operation
    ) {
        return async(
                registration,
                command,
                descriptor,
                audit,
                policy -> currentPlayer(policy, players)
                        .map(operation)
                        .orElseGet(() ->
                                CompletableFuture.completedFuture(
                                        PlayerStateCommandResult.failure(
                                                "common.player-only"
                                        )
                                )
                        )
        );
    }

    static int async(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            String audit,
            Function<
                    MinecraftCommandPolicyContext,
                    CompletableFuture<PlayerStateCommandResult>
                    > operation
    ) {
        return CommandExecutions.async(
                registration,
                command,
                descriptor,
                audit,
                operation,
                (policy, result) -> {
                    policy.respondAll(result.success(), result.messages());
                    return CommandOutcome.fromStatus(result.status());
                }
        );
    }

    static Optional<CellPlayer> currentPlayer(
            MinecraftCommandPolicyContext policy,
            PlayerDirectory players
    ) {
        return policy.playerUuid()
                .flatMap(players::onlinePlayer);
    }

    static Optional<CellPlayer> online(
            PlayerDirectory players,
            String name
    ) {
        return players.onlinePlayer(name);
    }

    static CompletableFuture<PlayerStateCommandResult> offline(
            String name
    ) {
        return CompletableFuture.completedFuture(
                PlayerStateCommandResult.failure(
                        "commands.common.player-offline",
                        Map.of("player", name)
                )
        );
    }

}
