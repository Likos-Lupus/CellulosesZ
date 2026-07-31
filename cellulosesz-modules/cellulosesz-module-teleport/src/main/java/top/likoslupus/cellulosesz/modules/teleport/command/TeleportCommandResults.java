package top.likoslupus.cellulosesz.modules.teleport.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandExecutions;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.source.MinecraftCommandPolicyContext;
import top.likoslupus.cellulosesz.modules.teleport.application.TeleportCommandResult;
import top.likoslupus.cellulosesz.modules.teleport.application.TeleportCommandStatus;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

final class TeleportCommandResults {

    static final String MODULE = "teleport";

    private TeleportCommandResults() {
    }

    static CommandDescriptor descriptor(
            String root,
            String permission,
            CommandSourceKind source
    ) {
        return new CommandDescriptor(MODULE, root, permission, source);
    }

    static int player(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            String audit,
            PlayerDirectory players,
            Function<CellPlayer, CompletableFuture<TeleportCommandResult>> operation
    ) {
        return async(
                registration,
                command,
                descriptor,
                audit,
                policy -> current(policy, players)
                        .map(operation)
                        .orElseGet(() -> CompletableFuture.completedFuture(
                                TeleportCommandResult.failure(
                                        TeleportCommandStatus.INVALID_INPUT,
                                        "common.player-only"
                                )
                        ))
        );
    }

    static int async(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            String audit,
            Function<
                    MinecraftCommandPolicyContext,
                    CompletableFuture<TeleportCommandResult>
                    > operation
    ) {
        return CommandExecutions.async(
                registration,
                command,
                descriptor,
                audit,
                operation,
                (policy, result) -> policy.respond(
                        result.success(),
                        result.message()
                )
        );
    }

    static Optional<CellPlayer> current(
            MinecraftCommandPolicyContext policy,
            PlayerDirectory players
    ) {
        return policy.playerUuid()
                .flatMap(players::onlinePlayer);
    }

}
