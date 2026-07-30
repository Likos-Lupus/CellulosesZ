package top.likoslupus.cellulosesz.common.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.common.command.source.MinecraftCommandPolicyContext;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
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
        return registration.execute(command, descriptor, auditSummary, operation);
    }

    public static <T> int async(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            String auditSummary,
            Function<MinecraftCommandPolicyContext, CompletableFuture<T>> operation,
            BiConsumer<MinecraftCommandPolicyContext, T> completion
    ) {
        return registration.execute(
                command,
                descriptor,
                auditSummary,
                policy -> {
                    final CompletableFuture<T> future;
                    try {
                        future = operation.apply(policy);
                    } catch (RuntimeException failure) {
                        registration.internalFailure(policy, failure);
                        return 0;
                    }

                    future.whenComplete((result, failure) -> {
                        if (failure != null) registration.internalFailure(policy, failure);
                        else completion.accept(policy, result);
                    });
                    return 1;
                }
        );
    }

}
