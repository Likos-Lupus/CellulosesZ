package top.likoslupus.cellulosesz.fabric.architecture;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import org.jspecify.annotations.NullMarked;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class DirectCommandSuggestionTest {

    @Test
    void moduleSnapshotsAndDynamicPermissionsDriveSuggestions() {
        var dispatcher = new CommandDispatcher<CommandSourceStack>();
        var context = new TreeContext(dispatcher, permission -> !permission.endsWith(".private")
                && !permission.endsWith(".secret"));
        new TextCommand(textService(false)).register(context);
        new HomeCommand(homeService()).register(context);
        new WarpCommand(warpService()).register(context);
        new KitCommand(kitService()).register(context);

        assertEquals(List.of("alpha"), suggestions(dispatcher, "customtext a"));
        assertEquals(List.of("main"), suggestions(dispatcher, "home m"));
        assertEquals(List.of("public"), suggestions(dispatcher, "warp p"));
        assertEquals(List.of("starter"), suggestions(dispatcher, "kit s"));
        assertEquals(List.of("PlayerOne"), suggestions(dispatcher, "kitreset starter PlayerO"));
    }

    private static TextCommandService textService(boolean failSuggestions) {
        return proxy(
                TextCommandService.class,
                (method, _) -> switch (method) {
                    case "customNames" -> {
                        if (failSuggestions) throw new IllegalStateException("snapshot unavailable");
                        yield Set.of("alpha", "beta");
                    }
                    default -> null;
                }
        );
    }

    private static HomeCommandService homeService() {
        return proxy(
                HomeCommandService.class,
                (method, _) -> method.equals("cachedNames")
                        ? Set.of("main", "mine")
                        : null
        );
    }

    private static WarpCommandService warpService() {
        return proxy(WarpCommandService.class, (method, args) -> switch (method) {
            case "cachedNames" -> List.of("public", "private");
            case "usableNames" -> {
                @SuppressWarnings("unchecked")
                var allowed = (Predicate<String>) args[0];
                yield Stream.of("public", "private")
                        .filter(name -> allowed.test("cellulosesz.warp." + name))
                        .toList();
            }
            default -> null;
        });
    }

    private static KitCommandService kitService() {
        return proxy(KitCommandService.class, (method, args) -> switch (method) {
            case "kitNames" -> List.of("starter", "secret");
            case "claimableNames" -> {
                @SuppressWarnings("unchecked")
                var allowed = (Predicate<String>) args[0];
                yield Stream.of("starter", "secret")
                        .filter(name -> allowed.test("cellulosesz.kit." + name))
                        .toList();
            }
            default -> null;
        });
    }

    private static List<String> suggestions(CommandDispatcher<CommandSourceStack> dispatcher, String input) {
        return dispatcher.getCompletionSuggestions(dispatcher.parse(input, null)).join()
                .getList().stream()
                .map(Suggestion::getText)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, ProxyAnswer answer) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (_, method, args) -> {
                    var answered = answer.answer(method.getName(), args == null ? new Object[0] : args);
                    if (answered != null) return answered;
                    var returnType = method.getReturnType();
                    if (returnType == boolean.class) return false;
                    if (returnType == int.class) return 0;
                    if (returnType == long.class) return 0L;
                    if (returnType == double.class) return 0D;
                    if (returnType == Optional.class) return Optional.empty();
                    if (returnType == List.class) return List.of();
                    if (returnType == Set.class) return Set.of();
                    if (returnType == CompletableFuture.class) return new CompletableFuture<>();
                    return null;
                }
        );
    }

    @Test
    void snapshotFailureReturnsEmptySuggestionsWithoutDamagingTheTree() {
        var dispatcher = new CommandDispatcher<CommandSourceStack>();
        var context = new TreeContext(dispatcher, _ -> true);
        new TextCommand(textService(true)).register(context);

        assertEquals(List.of(), suggestions(dispatcher, "customtext a"));
        assertDoesNotThrow(() -> dispatcher.execute("info", null));
    }

    @FunctionalInterface
    private interface ProxyAnswer {

        Object answer(String method, Object[] args);

    }

    @NullMarked
    private record TreeContext(
            CommandDispatcher<CommandSourceStack> dispatcher,
            Predicate<String> permissionPredicate
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
                    return permissionPredicate.test(permission);
                }

                @Override
                public int intOption(Object source, String key, int fallback) {
                    return fallback;
                }

                @Override
                public boolean boolOption(Object source, String key, boolean fallback) {
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
            return Optional.of(new CellPlayer(UUID.fromString("00000000-0000-0000-0000-000000000001"), "PlayerOne", new Object()));
        }

        @Override
        public List<String> onlinePlayerNames() {
            return List.of("PlayerOne", "PlayerTwo");
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
        public int execute(
                CommandContext<CommandSourceStack> command,
                CommandDescriptor descriptor,
                String auditSummary,
                Function<MinecraftCommandPolicyContext, Integer> terminal
        ) {
            return 1;
        }

        @Override
        public void internalFailure(MinecraftCommandPolicyContext policy, Throwable failure) {
            throw new AssertionError(failure);
        }

    }

}
