package top.likoslupus.cellulosesz.modules.sign;

import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.item.InventoryMutation;
import top.likoslupus.cellulosesz.api.item.InventoryPlatformService;
import top.likoslupus.cellulosesz.api.item.ItemDescriptor;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;
import top.likoslupus.cellulosesz.api.sign.SynchronousSignHandler;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.modules.sign.handler.TradeSignHandler;
import top.likoslupus.cellulosesz.modules.sign.service.DefaultSignService;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import static org.junit.jupiter.api.Assertions.*;

final class SignTransactionBehaviorTest {

    @Test
    @NullMarked
    void cooldownIsScopedToPlayerSignSideAndHandler() {
        var config = new SignConfig();
        config.interaction.cooldownTicks = 200;

        var service = new DefaultSignService(
                config,
                allowAllPermissions(),
                new MemoryStorage(),
                Path.of("signs.json")
        );
        service.register(new SynchronousSignHandler() {
            @Override
            public String id() {
                return "Balance";
            }

            @Override
            public SignUseResult useSynchronously(SignUseContext context) {
                return SignUseResult.success("test.success");
            }
        });

        var player = new CellPlayer(UUID.randomUUID(), "tester", new Object());
        var first = new CellLocation("minecraft:overworld", 1, 64, 1, 0, 0);
        var second = new CellLocation("minecraft:overworld", 2, 64, 1, 0, 0);
        var raw = List.of("[Balance]", "", "", "");
        var formatted = service.formattedLines(raw);

        create(service, player, first, raw);
        create(service, player, second, raw);

        assertTrue(service.use(
                player,
                first,
                true,
                formatted,
                false
        ).result().join().success());
        assertEquals(
                "service.sign.cooldown",
                key(service.use(
                        player,
                        first,
                        true,
                        formatted,
                        false
                ).result().join())
        );
        assertTrue(service.use(
                player,
                second,
                true,
                formatted,
                false
        ).result().join().success());
    }

    @NullMarked
    private static PermissionService allowAllPermissions() {
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

    private static void create(
            DefaultSignService service,
            CellPlayer player,
            CellLocation location,
            List<String> lines
    ) {
        var execution = service.create(player, location, true, lines);
        assertTrue(execution.handled());

        var commit = execution.preparation().join();
        assertTrue(commit.result().success());
        assertTrue(commit.complete(true).join().success());
    }

    private static String key(SignUseResult result) {
        return result.optionalMessage().orElseThrow().key();
    }

    @Test
    void tradeUsesOneAtomicExchangeAndDoesNotReportSuccessWhenCommitFails() {
        var mutation = new InventoryMutation() {
            @Override
            public boolean commit() {
                return false;
            }

            @Override
            public boolean rollback() {
                fail("A failed atomic commit must not require rollback");
                return false;
            }
        };
        var inventory = proxy(
                InventoryPlatformService.class,
                (method, _) -> method.getName()
                        .equals("prepareExchange")
                        ? PlatformResult.success(mutation)
                        : defaultValue(method)
        );
        var items = proxy(
                ItemService.class,
                (method, args) -> switch (method.getName()) {
                    case "parse" -> Optional.of(new ItemDescriptor(String.valueOf(args[0]), 1));
                    case "valid" -> true;
                    case "blacklisted" -> false;
                    case "commandArgument" -> ((ItemDescriptor) args[0]).normalizedItem();
                    default -> defaultValue(method);
                }
        );
        var handler = new TradeSignHandler(items, inventory);
        var context = new SignUseContext(
                new CellPlayer(UUID.randomUUID(), "trader", new Object()),
                new CellLocation("minecraft:overworld", 0, 64, 0, 0, 0),
                true,
                List.of("[Trade]", "minecraft:stone", "minecraft:diamond", ""),
                false
        );

        var result = handler.useSynchronously(context);
        assertFalse(result.success());
        assertEquals("service.sign.trade-inventory-changed", key(result));
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(
            Class<T> type,
            BiFunction<Method, Object[], Object> behavior
    ) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (instance, method, rawArgs) -> {
                    var args = rawArgs == null
                            ? new Object[0]
                            : rawArgs;
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> type.getSimpleName() + "TestProxy";
                            case "hashCode" -> System.identityHashCode(instance);
                            case "equals" -> instance == args[0];
                            default -> throw new UnsupportedOperationException(method.getName());
                        };
                    }
                    return behavior.apply(method, args);
                }
        );
    }

    private static Object defaultValue(Method method) {
        var type = method.getReturnType();
        if (type == void.class) return Boolean.TRUE;
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == String.class) return "";
        if (type == Optional.class) return Optional.empty();
        if (type == List.class) return List.of();
        if (type == Set.class) return Set.of();
        if (type == Collection.class) return List.of();
        if (type == Map.class) return Map.of();
        if (type == CompletableFuture.class) return CompletableFuture.completedFuture(Boolean.TRUE);
        throw new UnsupportedOperationException(method.toString());
    }

    @NullMarked
    private static final class MemoryStorage implements StorageService {

        private @Nullable Object document;

        @Override
        public <T> CompletableFuture<T> loadOrDefault(
                Path path,
                Class<T> type,
                Supplier<T> defaults
        ) {
            if (document == null) return CompletableFuture.completedFuture(defaults.get());
            return CompletableFuture.completedFuture(type.cast(document));
        }

        @Override
        public <T> CompletableFuture<T> createIfMissing(
                Path path,
                Class<T> type,
                Supplier<T> defaults
        ) {
            if (document == null) document = defaults.get();
            return CompletableFuture.completedFuture(type.cast(document));
        }

        @Override
        public <T> CompletableFuture<Void> save(Path path, T value) {
            document = value;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Boolean> exists(Path path) {
            return CompletableFuture.completedFuture(document != null);
        }

        @Override
        public CompletableFuture<Boolean> delete(Path path) {
            document = null;
            return CompletableFuture.completedFuture(true);
        }

        @Override
        public <T> CompletableFuture<List<T>> loadDirectory(Path directory, Class<T> type) {
            return CompletableFuture.completedFuture(List.of());
        }

    }

}
