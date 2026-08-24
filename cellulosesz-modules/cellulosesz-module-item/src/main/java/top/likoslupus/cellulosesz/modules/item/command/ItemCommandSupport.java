package top.likoslupus.cellulosesz.modules.item.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.common.command.CommandExecutions;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.source.MinecraftCommandPolicyContext;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

final class ItemCommandSupport {

    static final String MODULE = "item";

    private ItemCommandSupport() {
    }

    static CommandDescriptor descriptor(
            String root,
            String permission,
            CommandSourceKind sourceKind
    ) {
        return new CommandDescriptor(
                MODULE,
                root,
                permission,
                sourceKind
        );
    }

    static int sync(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            String audit,
            Function<MinecraftCommandPolicyContext, PlatformResult<?>> operation
    ) {
        return CommandExecutions.syncOutcome(
                registration,
                command,
                descriptor,
                audit,
                policy -> {
                    var result = operation.apply(policy);
                    respond(policy, descriptor.canonicalName(), result);
                    return result.status().toCommandOutcome();
                }
        );
    }

    static int respond(
            MinecraftCommandPolicyContext policy,
            String command,
            PlatformResult<?> result
    ) {
        var key = result.successful()
                ? "commands.item.operation.success"
                : "commands.item.operation.failed";

        var arguments = result.successful()
                ?
                MessageArguments.builder()
                        .add(command)
                        .build()
                : MessageArguments.builder()
                        .add(command)
                        .add(result.status().name().toLowerCase())
                        .add(result.detail().isBlank()
                                ? "-"
                                : result.detail()
                        )
                        .build();
        policy.respond(
                result.successful(),
                LocalizedMessage.of(key, arguments)
        );

        return result.successful()
                ? 1
                : 0;
    }

    static <T> int async(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            String audit,
            Function<
                    MinecraftCommandPolicyContext,
                    CompletableFuture<PlatformResult<T>>
                    > operation
    ) {
        return CommandExecutions.async(
                registration,
                command,
                descriptor,
                audit,
                operation,
                (policy, result) -> {
                    respond(policy, descriptor.canonicalName(), result);
                    return result.status().toCommandOutcome();
                }
        );
    }

    static Optional<CellPlayer> current(
            MinecraftCommandPolicyContext policy
    ) {
        var player = policy.currentPlayer();

        if (player.isEmpty()) {
            policy.error(
                    LocalizedMessage.of("commands.item.player-only")
            );
        }

        return player;
    }

}
