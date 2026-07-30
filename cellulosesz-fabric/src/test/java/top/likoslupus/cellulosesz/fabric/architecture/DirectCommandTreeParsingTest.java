package top.likoslupus.cellulosesz.fabric.architecture;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.service.ServiceRegistry;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.source.MinecraftCommandPolicyContext;
import top.likoslupus.cellulosesz.modules.home.application.HomeCommandService;
import top.likoslupus.cellulosesz.modules.home.command.HomeCommand;
import top.likoslupus.cellulosesz.modules.kit.application.KitCommandService;
import top.likoslupus.cellulosesz.modules.kit.command.KitCommand;
import top.likoslupus.cellulosesz.modules.text.application.TextCommandService;
import top.likoslupus.cellulosesz.modules.text.command.TextCommand;
import top.likoslupus.cellulosesz.modules.warp.application.WarpCommandService;
import top.likoslupus.cellulosesz.modules.warp.command.WarpCommand;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class DirectCommandTreeParsingTest {

    private CommandDispatcher<CommandSourceStack> dispatcher;

    @BeforeEach
    void registerActualModuleTrees() {
        dispatcher = new CommandDispatcher<>();
        var context = new TreeContext(dispatcher);
        new TextCommand(proxy(TextCommandService.class)).register(context);
        new HomeCommand(proxy(HomeCommandService.class)).register(context);
        new WarpCommand(proxy(WarpCommandService.class)).register(context);
        new KitCommand(proxy(KitCommandService.class)).register(context);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (_, method, _) -> {
                    var returnType = method.getReturnType();
                    if (returnType == boolean.class) return false;
                    if (returnType == int.class) return 0;
                    if (returnType == long.class) return 0L;
                    if (returnType == double.class) return 0D;
                    if (returnType == Optional.class) return Optional.empty();
                    if (returnType == List.class) return List.of();
                    if (returnType == Set.class) return Set.of();
                    if (returnType == CompletableFuture.class)
                        return new CompletableFuture<>();
                    return null;
                }
        );
    }

    @Test
    void textShapesParse() {
        valid(
                "info",
                "info 2",
                "motd 2",
                "rules 2",
                "customtext name",
                "customtext name 2"
        );
        invalid(
                "info 0",
                "info 2 extra",
                "customtext",
                "customtext name 2 extra"
        );
    }

    private void valid(String... commands) {
        Arrays.stream(commands)
                .forEach(command ->
                        assertDoesNotThrow(
                                () -> dispatcher.execute(command, null),
                                command
                        )
                );
    }

    private void invalid(String... commands) {
        Arrays.stream(commands)
                .forEach(command ->
                        assertThrows(
                                CommandSyntaxException.class,
                                () -> dispatcher.execute(command, null),
                                command
                        )
                );
    }

    @Test
    void homeShapesParse() {
        valid(
                "home",
                "home main",
                "homes",
                "sethome",
                "sethome main",
                "delhome main",
                "renamehome old new"
        );
        invalid(
                "home main extra",
                "delhome",
                "renamehome old",
                "renamehome old new extra"
        );
    }

    @Test
    void warpShapesHaveDistinctPageAndNameBranches() {
        valid(
                "warp",
                "warp 2",
                "warp spawn",
                "warps",
                "warps 2",
                "setwarp spawn",
                "delwarp spawn",
                "warpinfo spawn"
        );
        invalid(
                "warp 0",
                "warp -1",
                "warps 0",
                "setwarp",
                "warpinfo spawn extra"
        );
    }

    @Test
    void kitShapesAndTypedCooldownParse() {
        valid(
                "kit",
                "kit starter",
                "kits",
                "showkit starter",
                "createkit starter once",
                "createkit starter 0",
                "createkit starter 300",
                "delkit starter",
                "kitreset starter",
                "kitreset starter Player"
        );
        invalid(
                "createkit starter -1",
                "createkit starter 999999999999999999999999",
                "createkit starter later",
                "delkit",
                "kitreset",
                "kitreset starter Player extra"
        );
    }

    @NullMarked
    private record TreeContext(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) implements CommandRegistrationContext {

        @Override
        public ServiceRegistry services() {
            throw new UnsupportedOperationException();
        }

        @Override
        public PermissionService permissions() {
            return new PermissionService() {
                @Override
                public boolean has(Object source, String permission) {
                    return true;
                }

                @Override
                public int intOption(
                        Object source,
                        String key,
                        int fallback
                ) {
                    return fallback;
                }

                @Override
                public boolean boolOption(
                        Object source,
                        String key,
                        boolean fallback
                ) {
                    return fallback;
                }

                @Override
                public Optional<String> stringOption(Object source, String key) {
                    return Optional.empty();
                }
            };
        }

        @Override
        public boolean moduleEnabled(String moduleId) {
            return true;
        }

        @Override
        public Optional<CellPlayer> player(CommandSourceStack source) {
            return Optional.empty();
        }

        @Override
        public List<String> onlinePlayerNames() {
            return List.of();
        }

        @Override
        public CommandNode<CommandSourceStack> registerDirect(
                String owner,
                CommandDescriptor descriptor,
                List<String> semanticRoots,
                String description,
                String usage,
                LiteralArgumentBuilder<CommandSourceStack> root
        ) {
            return dispatcher.register(root);
        }

        @Override
        public CommandNode<CommandSourceStack> registerSemantic(
                String owner,
                CommandDescriptor descriptor,
                String label,
                LiteralArgumentBuilder<CommandSourceStack> root
        ) {
            return dispatcher.register(root);
        }

        @Override
        public void registerAlias(
                String owner,
                CommandDescriptor descriptor,
                String label,
                CommandNode<CommandSourceStack> target
        ) {
            dispatcher.register(
                    Commands.literal(label).redirect(target)
            );
        }

        @Override
        public int execute(
                CommandContext<CommandSourceStack> command,
                CommandDescriptor descriptor,
                String auditSummary,
                Function<MinecraftCommandPolicyContext, Integer> terminal
        ) {
            return 1;
        }

        @Override
        public void internalFailure(
                MinecraftCommandPolicyContext policy,
                Throwable failure
        ) {
            throw new AssertionError(failure);
        }

    }

}
