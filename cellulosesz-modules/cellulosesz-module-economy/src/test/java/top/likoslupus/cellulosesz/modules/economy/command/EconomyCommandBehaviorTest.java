package top.likoslupus.cellulosesz.modules.economy.command;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.economy.WorthService;
import top.likoslupus.cellulosesz.api.item.InventoryItemSnapshot;
import top.likoslupus.cellulosesz.api.item.InventoryMutation;
import top.likoslupus.cellulosesz.api.item.ItemDescriptor;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.player.ResolvedPlayer;
import top.likoslupus.cellulosesz.api.player.ResolvedPlayerState;
import top.likoslupus.cellulosesz.api.text.RichText;
import top.likoslupus.cellulosesz.api.user.NameCacheService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.economy.EconomyConfig;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;

final class EconomyCommandBehaviorTest {

    @Test
    void sellRollsBackExactPreparedMutationWhenBalanceSaveFails() {
        var player = new CellPlayer(UUID.randomUUID(), "seller", new Object());
        var snapshot = new InventoryItemSnapshot(4, "lossless-stack");
        var mutation = new TrackingMutation();
        var platform = proxy(
                PlatformService.class,
                (method, args) -> switch (method.getName()) {
                    case "player" -> Optional.of(player);
                    case "inventorySnapshot" -> Optional.of(List.of(snapshot));
                    case "plainInventoryItem" -> true;
                    case "describeInventoryItem" -> Optional.of(new ItemDescriptor("minecraft:stone", 3));
                    case "prepareInventoryRemoval" -> Optional.of(mutation);
                    case "runOnServerThread" -> {
                        ((Runnable) args[0]).run();
                        yield null;
                    }
                    default -> defaultValue(method);
                }
        );
        var worth = proxy(
                WorthService.class,
                (method, _) -> method.getName().equals("worth")
                        ? Optional.of(new BigDecimal("2.00"))
                        : defaultValue(method)
        );
        var economy = proxy(
                EconomyService.class,
                (method, args) -> switch (method.getName()) {
                    case "deposit" -> CompletableFuture.failedFuture(new IllegalStateException("disk failure"));
                    case "format" -> ((BigDecimal) args[0]).toPlainString();
                    default -> defaultValue(method);
                }
        );
        var items = proxy(
                ItemService.class,
                (method, _) -> defaultValue(method)
        );
        var invocation = new TestInvocation("all");

        assertEquals(
                1,
                new SellCommand(platform, items, worth, economy, new NoopLogger()).execute(invocation)
        );
        assertTrue(mutation.committed);
        assertTrue(mutation.rolledBack);
        assertEquals("service.economy.persistence-failed", invocation.errorKey);
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
        return switch (type) {
            case Class<?> t when t == void.class -> null;
            case Class<?> t when t == boolean.class -> false;
            case Class<?> t when t == int.class -> 0;
            case Class<?> t when t == long.class -> 0L;
            case Class<?> t when t == double.class -> 0.0D;
            case Class<?> t when t == String.class -> "";
            case Class<?> t when Optional.class.isAssignableFrom(t) -> Optional.empty();
            case Class<?> t when List.class.isAssignableFrom(t) -> List.of();
            case Class<?> t when Set.class.isAssignableFrom(t) -> Set.of();
            case Class<?> t when Collection.class.isAssignableFrom(t) -> List.of();
            case Class<?> t when Map.class.isAssignableFrom(t) -> Map.of();
            case Class<?> t when CompletableFuture.class.isAssignableFrom(t) -> CompletableFuture.completedFuture(null);
            case Class<?> t when t == BigDecimal.class -> BigDecimal.ZERO;
            default -> throw new UnsupportedOperationException(method.toString());
        };
    }

    @Test
    void worthInventoryUsesEveryStackQuantity() {
        var player = new CellPlayer(UUID.randomUUID(), "owner", new Object());
        var first = new InventoryItemSnapshot(1, "first");
        var second = new InventoryItemSnapshot(7, "second");
        var platform = proxy(
                PlatformService.class,
                (method, args) -> switch (method.getName()) {
                    case "player" -> Optional.of(player);
                    case "inventorySnapshot" -> Optional.of(List.of(first, second));
                    case "plainInventoryItem" -> true;
                    case "describeInventoryItem" -> Optional.of(new ItemDescriptor(
                            "minecraft:stone",
                            ((InventoryItemSnapshot) args[0]).validatedStack().equals("first") ? 2 : 3
                    ));
                    default -> defaultValue(method);
                }
        );
        var worth = proxy(
                WorthService.class,
                (method, _) -> method.getName().equals("worth")
                        ? Optional.of(new BigDecimal("2.00"))
                        : defaultValue(method)
        );
        var economy = proxy(
                EconomyService.class,
                (method, args) -> method.getName().equals("format")
                        ? ((BigDecimal) args[0]).toPlainString()
                        : defaultValue(method)
        );
        var items = proxy(
                ItemService.class,
                (method, _) -> defaultValue(method)
        );
        var invocation = new TestInvocation("inventory");

        assertEquals(1, new WorthCommand(platform, items, worth, economy).execute(invocation));
        assertEquals("10.00", invocation.replyPlaceholders.get("total"));
        assertTrue(String.valueOf(invocation.replyPlaceholders.get("rows")).contains("x5"));
    }

    @Test
    void balanceTopRejectsPageZeroBeforeQueryingServices() {
        var platform = proxy(
                PlatformService.class,
                (method, _) -> defaultValue(method)
        );
        var users = proxy(
                UserService.class,
                (method, _) -> defaultValue(method)
        );
        var economy = proxy(
                EconomyService.class,
                (method, _) -> {
                    fail("Economy must not be queried for an invalid page");
                    return defaultValue(method);
                }
        );
        var invocation = new TestInvocation("0");

        assertEquals(
                0,
                new BalanceTopCommand(
                        platform,
                        users,
                        names(),
                        economy,
                        new EconomyConfig()
                ).execute(invocation)
        );
        assertEquals("commands.economy.balance-top-command.error.page-number-must-integer", invocation.errorKey);
    }

    @NullMarked
    private static NameCacheService names() {
        return new NameCacheService() {
            @Override
            public void remember(UUID uuid, String name) {
            }

            @Override
            public Optional<UUID> findUuid(String name) {
                return Optional.empty();
            }

            @Override
            public Optional<String> findName(UUID uuid) {
                return Optional.empty();
            }

            @Override
            public Map<UUID, String> entries() {
                return Map.of();
            }

            @Override
            public CompletableFuture<Void> save() {
                return CompletableFuture.completedFuture(null);
            }
        };
    }

    @NullMarked
    private static final class TrackingMutation implements InventoryMutation {

        private boolean committed;
        private boolean rolledBack;

        @Override
        public boolean commit() {
            committed = true;
            return true;
        }

        @Override
        public boolean rollback() {
            rolledBack = true;
            return true;
        }

    }

    @NullMarked
    private static final class TestInvocation implements CommandInvocation {

        private final String[] args;
        private String errorKey = "";
        private Map<String, ?> replyPlaceholders = Map.of();

        private TestInvocation(String... args) {
            this.args = args;
        }

        @Override
        public Object nativeSource() {
            return this;
        }

        @Override
        public String label() {
            return "test";
        }

        @Override
        public String[] args() {
            return args.clone();
        }

        @Override
        public boolean player() {
            return true;
        }

        @Override
        public Optional<String> playerName() {
            return Optional.of("tester");
        }

        @Override
        public boolean hasPermission(String permission) {
            return true;
        }

        @Override
        public ResolvedPlayer resolvePlayer(String input) {
            return new ResolvedPlayer(
                    ResolvedPlayerState.UNKNOWN,
                    null,
                    input,
                    null,
                    false
            );
        }

        @Override
        public String locale() {
            return "en";
        }

        @Override
        public void reply(String message) {
        }

        @Override
        public void reply(RichText message) {
        }

        @Override
        public void replyKey(String key, Map<String, ?> placeholders) {
            replyPlaceholders = Map.copyOf(placeholders);
        }

        @Override
        public void error(String message) {
        }

        @Override
        public void error(RichText message) {
        }

        @Override
        public void errorKey(String key, Map<String, ?> placeholders) {
            errorKey = key;
        }

    }

    @NullMarked
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

}
