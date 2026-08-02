package top.likoslupus.cellulosesz.modules.world.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.command.execution.CommandOutcome;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.api.world.WorldDirectory;
import top.likoslupus.cellulosesz.common.command.CommandExecutions;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.source.MinecraftCommandPolicyContext;

import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

final class WorldCommandSupport {

    static final String MODULE = "world";

    private WorldCommandSupport() {
    }

    static CommandDescriptor descriptor(
            String root,
            String permission,
            CommandSourceKind sourceKind
    ) {
        return new CommandDescriptor(MODULE, root, permission, sourceKind);
    }

    static Optional<CellPlayer> current(MinecraftCommandPolicyContext policy) {
        var player = policy.currentPlayer();
        if (player.isEmpty()) {
            policy.error(LocalizedMessage.of("commands.world.player-only"));
        }
        return player;
    }

    static Optional<String> world(
            MinecraftCommandPolicyContext policy,
            WorldDirectory worlds,
            PlayerLocationPlatformService locations,
            Optional<String> explicit
    ) {
        if (explicit.isPresent()) {
            return explicit;
        }

        var player = policy.currentPlayer();
        if (player.isPresent()) {
            return Optional.of(locations.currentLocation(player.orElseThrow()).world());
        }

        return worlds.loadedWorldIds().stream()
                .min(Comparator.naturalOrder());
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
                    return CommandOutcome.fromPlatformStatus(result.status());
                }
        );
    }

    static int respond(
            MinecraftCommandPolicyContext policy,
            String command,
            PlatformResult<?> result
    ) {
        policy.respond(
                result.successful(),
                LocalizedMessage.of(
                        result.successful()
                                ? "commands.world.operation.success"
                                : "commands.world.operation.failed",
                        MessageArguments.builder()
                                .put("command", command)
                                .put("status", result.status().name().toLowerCase())
                                .put(
                                        "detail", result.detail().isBlank()
                                                ? "-"
                                                : result.detail()
                                )
                                .build()
                )
        );

        return result.successful()
                ? 1
                : 0;
    }

    static int admin(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            String audit,
            Function<MinecraftCommandPolicyContext, AdminResult> operation
    ) {
        return CommandExecutions.sync(
                registration,
                command,
                descriptor,
                audit,
                policy -> {
                    var result = operation.apply(policy);
                    policy.respond(result.success(), result.message());
                    return result.success()
                            ? 1
                            : 0;
                }
        );
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
                    return CommandOutcome.fromPlatformStatus(result.status());
                }
        );
    }

}
