package top.likoslupus.cellulosesz.modules.economy.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.common.command.CommandExecutions;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.source.MinecraftCommandPolicyContext;
import top.likoslupus.cellulosesz.modules.economy.application.EconomyCommandResult;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

final class EconomyCommandSupport {

    static final String MODULE = "economy";

    private EconomyCommandSupport() {
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
                    CompletableFuture<EconomyCommandResult>
                    > operation
    ) {
        return async(
                registration,
                command,
                descriptor,
                audit,
                policy -> currentPlayer(
                        policy.playerUuid(),
                        players
                )
                        .map(operation)
                        .orElseGet(() ->
                                CompletableFuture.completedFuture(
                                        EconomyCommandResult.failure(
                                                LocalizedMessage.of(
                                                        "common.player-only"
                                                )
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
                    CompletableFuture<EconomyCommandResult>
                    > operation
    ) {
        return CommandExecutions.async(
                registration,
                command,
                descriptor,
                audit,
                operation,
                (policy, result) -> policy.respondAll(
                        result.success(),
                        result.messages()
                )
        );
    }

    static Optional<CellPlayer> currentPlayer(
            Optional<UUID> uuid,
            PlayerDirectory players
    ) {
        return uuid.flatMap(players::onlinePlayer);
    }

}
