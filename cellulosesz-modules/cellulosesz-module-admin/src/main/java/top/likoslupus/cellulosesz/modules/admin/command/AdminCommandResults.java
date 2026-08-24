package top.likoslupus.cellulosesz.modules.admin.command;

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
import top.likoslupus.cellulosesz.modules.admin.domain.AdminActor;
import top.likoslupus.cellulosesz.modules.admin.domain.AdminResult;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

final class AdminCommandResults {

    static final String MODULE = "admin";

    private AdminCommandResults() {
    }

    static CommandDescriptor descriptor(
            String root,
            String permission,
            CommandSourceKind source
    ) {
        return new CommandDescriptor(MODULE, root, permission, source);
    }

    static int async(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            String audit,
            Function<MinecraftCommandPolicyContext, CompletableFuture<AdminResult>> operation
    ) {
        return CommandExecutions.async(
                registration,
                command,
                descriptor,
                audit,
                operation,
                (policy, result) -> {
                    policy.respond(result.success(), result.message());
                    return switch (result.status()) {
                        case SUCCESS -> CommandOutcome.success();
                        case PARTIAL_SUCCESS -> CommandOutcome.partial();
                        case PERSISTENCE_FAILURE,
                             NATIVE_COMMAND_FAILURE,
                             PLATFORM_FAILURE,
                             ROLLBACK_FAILURE,
                             FAILURE -> CommandOutcome.failed();
                        case NOT_FOUND, ALREADY_EXISTS, INVALID_INPUT -> CommandOutcome.rejected();
                    };
                }
        );
    }

    static AdminActor actor(
            MinecraftCommandPolicyContext policy,
            PlayerDirectory players
    ) {
        return current(policy, players)
                .map(AdminActor::player)
                .orElseGet(() -> new AdminActor(
                        Optional.ofNullable(policy.playerUuid()),
                        policy.playerName() == null
                                ? "console"
                                : policy.playerName()
                ));
    }

    static Optional<CellPlayer> current(
            MinecraftCommandPolicyContext policy,
            PlayerDirectory players
    ) {
        var uuid = policy.playerUuid();
        return uuid == null
                ? Optional.empty()
                : Optional.ofNullable(players.onlinePlayer(uuid));
    }

}
