package top.likoslupus.cellulosesz.modules.messaging.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.messaging.MessageResult;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.common.command.CommandExecutions;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.source.MinecraftCommandPolicyContext;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

final class MessagingCommandSupport {

    static final String MODULE = "messaging";

    private MessagingCommandSupport() {
    }

    static CommandDescriptor descriptor(
            String name,
            String permission,
            CommandSourceKind kind
    ) {
        return new CommandDescriptor(
                MODULE,
                name,
                permission,
                kind
        );
    }

    static Optional<CellPlayer> playerFromSource(
            CommandSourceStack source,
            PlayerDirectory players
    ) {
        return source.getEntity() instanceof ServerPlayer nativePlayer
                ? players.onlinePlayer(nativePlayer.getUUID())
                : Optional.empty();
    }

    static int requirePlayer(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            String audit,
            PlayerDirectory players,
            Function<
                    CellPlayer,
                    CompletableFuture<MessageResult>
                    > operation
    ) {
        return async(
                registration,
                command,
                descriptor,
                audit,
                policy -> player(policy, players)
                        .map(operation)
                        .orElseGet(() ->
                                CompletableFuture.completedFuture(
                                        MessageResult.failure(
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
                    CompletableFuture<MessageResult>
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

    static Optional<CellPlayer> player(
            MinecraftCommandPolicyContext policy,
            PlayerDirectory players
    ) {
        return policy.playerUuid()
                .flatMap(players::onlinePlayer);
    }

}
