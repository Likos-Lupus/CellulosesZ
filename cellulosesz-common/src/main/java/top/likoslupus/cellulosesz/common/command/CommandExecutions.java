package top.likoslupus.cellulosesz.common.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.command.execution.CommandOutcome;
import top.likoslupus.cellulosesz.common.command.source.MinecraftCommandPolicyContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class CommandExecutions {

    private CommandExecutions() {
    }

    public static int sync(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            String auditSummary,
            Function<MinecraftCommandPolicyContext, Integer> operation
    ) {
        return registration.execute(
                command,
                descriptor,
                auditSummary,
                policy -> {
                    var result = (int) operation.apply(policy);
                    return CompletableFuture.completedFuture(
                            result > 0
                                    ? CommandOutcome.success(result)
                                    : CommandOutcome.rejected(result)
                    );
                }
        );
    }

    public static int syncOutcome(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            String auditSummary,
            Function<MinecraftCommandPolicyContext, CommandOutcome> operation
    ) {
        return registration.execute(
                command,
                descriptor,
                auditSummary,
                policy -> CompletableFuture.completedFuture(operation.apply(policy))
        );
    }

    public static <T> int async(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            String auditSummary,
            Function<
                    MinecraftCommandPolicyContext,
                    ? extends CompletionStage<T>
                    > operation,
            BiFunction<
                    MinecraftCommandPolicyContext,
                    T,
                    CommandOutcome
                    > completion
    ) {
        return registration.execute(
                command,
                descriptor,
                auditSummary,
                policy -> operation
                        .apply(policy)
                        .thenApply(result -> completion.apply(policy, result))
        );
    }

}
