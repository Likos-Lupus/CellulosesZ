package top.likoslupus.cellulosesz.modules.sign;

import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.economy.TransactionResult;
import top.likoslupus.cellulosesz.api.item.InventoryMutation;
import top.likoslupus.cellulosesz.api.item.InventoryPlatformService;
import top.likoslupus.cellulosesz.api.item.ItemDescriptor;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;
import top.likoslupus.cellulosesz.api.sign.SynchronousSignHandler;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.modules.sign.handler.SellSignHandler;
import top.likoslupus.cellulosesz.modules.sign.handler.TradeSignHandler;
import top.likoslupus.cellulosesz.modules.sign.service.DefaultSignService;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
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

        var player = new CellPlayer(UUID.randomUUID(), "tester");
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
            public boolean has(CellPlayer source, String permission) {
                return true;
            }

            @Override
            public int intOption(
                    CellPlayer source,
                    String key,
                    int fallback
            ) {
                return fallback;
            }

            @Override
            public boolean boolOption(
                    CellPlayer source,
                    String key,
                    boolean fallback
            ) {
                return fallback;
            }

            @Override
            public Optional<String> stringOption(CellPlayer source, String key) {
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
            public PlatformResult<Void> commit() {
                return PlatformResult.failure(
                        PlatformOperationStatus.CONFLICT,
                        "inventory changed"
                );
            }

            @Override
            public PlatformResult<Void> rollback() {
                fail("A failed atomic commit must not require rollback");
                return PlatformResult.failure(
                        PlatformOperationStatus.INVALID_STATE,
                        "rollback should not run"
                );
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
                    case "parse" ->
                            PlatformResult.success(new ItemDescriptor(String.valueOf(args[0]), 1));
                    case "valid" -> PlatformResult.success(true);
                    case "blacklisted" -> false;
                    case "commandArgument" -> ((ItemDescriptor) args[0]).normalizedItem();
                    default -> defaultValue(method);
                }
        );
        var handler = new TradeSignHandler(items, inventory);
        var context = new SignUseContext(
                new CellPlayer(UUID.randomUUID(), "trader"),
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
        if (type == void.class) {
            return Boolean.TRUE;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == String.class) {
            return "";
        }
        if (type == Optional.class) {
            return Optional.empty();
        }
        if (type == List.class) {
            return List.of();
        }
        if (type == Set.class) {
            return Set.of();
        }
        if (type == Collection.class) {
            return List.of();
        }
        if (type == Map.class) {
            return Map.of();
        }
        if (type == CompletableFuture.class) {
            return CompletableFuture.completedFuture(Boolean.TRUE);
        }
        throw new UnsupportedOperationException(method.toString());
    }

    @Test
    void sellReportsRollbackFailureSeparatelyFromDepositFailure() {
        var mutation = new InventoryMutation() {
            @Override
            public PlatformResult<Void> commit() {
                return PlatformResult.success();
            }

            @Override
            public PlatformResult<Void> rollback() {
                return PlatformResult.failure(
                        PlatformOperationStatus.ROLLBACK_FAILED,
                        "inventory changed after commit"
                );
            }
        };
        var inventory = proxy(
                InventoryPlatformService.class,
                (method, _) -> method.getName().equals("prepareExchange")
                        ? PlatformResult.success(mutation)
                        : defaultValue(method)
        );
        var items = proxy(
                ItemService.class,
                (method, args) -> switch (method.getName()) {
                    case "parse" ->
                            PlatformResult.success(new ItemDescriptor("minecraft:stone", 2));
                    case "valid" -> PlatformResult.success(true);
                    case "blacklisted" -> false;
                    case "commandArgument" -> ((ItemDescriptor) args[0]).normalizedArgument();
                    default -> defaultValue(method);
                }
        );
        var economy = proxy(
                EconomyService.class,
                (method, _) -> switch (method.getName()) {
                    case "deposit" -> CompletableFuture.completedFuture(TransactionResult.failure(
                            "economy.deposit-failed",
                            BigDecimal.TEN,
                            BigDecimal.ZERO
                    ));
                    case "balance" -> BigDecimal.ZERO;
                    case "format" -> "10";
                    case "topBalances" -> List.of();
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
        var handler = new SellSignHandler(
                items,
                economy,
                inventory,
                immediateServerThread(),
                new NoopLogger()
        );
        var context = new SignUseContext(
                new CellPlayer(UUID.randomUUID(), "seller"),
                new CellLocation("minecraft:overworld", 0, 64, 0, 0, 0),
                true,
                List.of("[Sell]", "2", "minecraft:stone", "10"),
                false
        );

        var result = handler.use(context).join();

        assertFalse(result.success());
        assertEquals("service.sign.sell-rollback-failed", key(result));
    }

    private static ServerThreadExecutor immediateServerThread() {
        return new ServerThreadExecutor() {
            @Override
            public boolean isServerThread() {
                return true;
            }

            @Override
            public void execute(Runnable task) {
                task.run();
            }

            @Override
            public <T> CompletableFuture<T> submit(Supplier<T> task) {
                return CompletableFuture.completedFuture(task.get());
            }
        };
    }

    private static final class NoopLogger implements CellulosesZLogger {

        @Override
        public void warn(String message) {
        }

        @Override
        public void error(String message) {
        }

        @Override
        public void error(String message, Throwable throwable) {
        }

        @Override
        public void info(String message) {
        }

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
            if (document == null) {
                return CompletableFuture.completedFuture(defaults.get());
            }
            return CompletableFuture.completedFuture(type.cast(document));
        }

        @Override
        public <T> CompletableFuture<T> createIfMissing(
                Path path,
                Class<T> type,
                Supplier<T> defaults
        ) {
            if (document == null) {
                document = defaults.get();
            }
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
