package top.likoslupus.cellulosesz.common.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.service.ServiceRegistry;
import top.likoslupus.cellulosesz.common.command.source.MinecraftCommandPolicyContext;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Direct Brigadier registration surface exposed to feature modules. It contains no route/spec model.
 */
public interface CommandRegistrationContext {

    ServiceRegistry services();

    PermissionService permissions();

    boolean moduleEnabled(String moduleId);

    Optional<CellPlayer> player(CommandSourceStack source);

    List<String> onlinePlayerNames();

    CommandNode<CommandSourceStack> registerDirect(
            String owner,
            CommandDescriptor descriptor,
            List<String> semanticRoots,
            String description,
            String usage,
            LiteralArgumentBuilder<CommandSourceStack> root
    );

    CommandNode<CommandSourceStack> registerSemantic(
            String owner,
            CommandDescriptor descriptor,
            String label,
            LiteralArgumentBuilder<CommandSourceStack> root
    );

    void registerAlias(
            String owner,
            CommandDescriptor descriptor,
            String label,
            CommandNode<CommandSourceStack> target
    );

    int execute(
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            String auditSummary,
            Function<MinecraftCommandPolicyContext, Integer> terminal
    );

    void internalFailure(MinecraftCommandPolicyContext policy, Throwable failure);

}
