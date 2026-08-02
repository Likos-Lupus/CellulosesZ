package top.likoslupus.cellulosesz.modules.economy.command;

import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.economy.WorthService;
import top.likoslupus.cellulosesz.api.item.*;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.text.MessageArgument;
import top.likoslupus.cellulosesz.modules.economy.application.ItemValueCommandService;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.jspecify.annotations.NullMarked;

import static org.junit.jupiter.api.Assertions.*;

final class EconomyCommandBehaviorTest {

    @Test
    void sellRollsBackExactPreparedMutationWhenBalanceSaveFails() {
        var player = new CellPlayer(
                UUID.randomUUID(),
                "seller"
        );
        var snapshot = new InventoryItemSnapshot(
                4,
                "lossless-stack"
        );
        var slot = new InventorySlotView(
                snapshot,
                new ItemDescriptor("minecraft:stone", 3),
                InventorySlotKind.MAIN,
                true
        );
        var mutation = new TrackingMutation();
        var inventories = proxy(
                InventoryPlatformService.class,
                (method, _) -> switch (method.getName()) {
                    case "inventorySlots" -> PlatformResult.success(List.of(slot));
                    case "heldSlot" -> PlatformResult.success(slot);
                    case "prepareRemoval" -> PlatformResult.success(mutation);
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
                    case "deposit" ->
                            CompletableFuture.failedFuture(new IllegalStateException("disk failure"));
                    case "format" -> ((BigDecimal) args[0]).toPlainString();
                    default -> defaultValue(method);
                }
        );
        var service = new ItemValueCommandService(
                inventories,
                proxy(ItemService.class, (method, _) -> defaultValue(method)),
                worth,
                economy,
                new ImmediateServerThreadExecutor()
        );

        var result = service.sell(
                player,
                ItemValueCommandService.SellSelector.ALL,
                Optional.empty(),
                1
        ).join();

        assertFalse(result.success());
        assertEquals("service.economy.persistence-failed", result.messages().getFirst().key());
        assertTrue(mutation.committed);
        assertTrue(mutation.rolledBack);
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
                    return method.getDeclaringClass() != Object.class
                            ? behavior.apply(method, args)
                            : switch (method.getName()) {
                                case "toString" -> type.getSimpleName() + "TestProxy";
                                case "hashCode" -> System.identityHashCode(instance);
                                case "equals" -> instance == args[0];
                                default ->
                                        throw new UnsupportedOperationException(method.getName());
                            };
                }
        );
    }

    private static Object defaultValue(Method method) {
        var type = method.getReturnType();
        if (type == void.class) {
            return null;
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
        if (type == double.class) {
            return 0.0D;
        }
        if (type == String.class) {
            return "";
        }
        if (Optional.class.isAssignableFrom(type)) {
            return Optional.empty();
        }
        if (List.class.isAssignableFrom(type)) {
            return List.of();
        }
        if (CompletableFuture.class.isAssignableFrom(type)) {
            return CompletableFuture.completedFuture(null);
        }
        if (BigDecimal.class.isAssignableFrom(type)) {
            return BigDecimal.ZERO;
        }
        if (PlatformResult.class.isAssignableFrom(type)) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INTERNAL_ERROR,
                    "unsupported test operation"
            );
        }
        throw new UnsupportedOperationException(method.toString());
    }

    @Test
    void worthInventoryUsesEveryStackQuantity() {
        var player = new CellPlayer(
                UUID.randomUUID(),
                "owner"
        );
        var first = new InventorySlotView(
                new InventoryItemSnapshot(1, "first"),
                new ItemDescriptor("minecraft:stone", 2),
                InventorySlotKind.MAIN,
                true
        );
        var second = new InventorySlotView(
                new InventoryItemSnapshot(7, "second"),
                new ItemDescriptor("minecraft:stone", 3),
                InventorySlotKind.MAIN,
                true
        );
        var inventories = proxy(
                InventoryPlatformService.class,
                (method, _) -> method.getName().equals("inventorySlots")
                        ? PlatformResult.success(List.of(first, second))
                        : defaultValue(method)
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
        var service = new ItemValueCommandService(
                inventories,
                proxy(ItemService.class, (method, _) -> defaultValue(method)),
                worth,
                economy,
                new ImmediateServerThreadExecutor()
        );

        var result = service.worth(
                player,
                ItemValueCommandService.WorthSelector.INVENTORY
        ).join();

        assertTrue(result.success());
        assertEquals(
                "10.00",
                (
                        (MessageArgument.Text) result.messages()
                                .getLast()
                                .placeholders()
                                .values()
                                .get("total")
                ).value()
        );
        assertEquals(
                5L,
                (
                        (MessageArgument.Number) result.messages()
                                .getFirst()
                                .placeholders()
                                .values()
                                .get("count")
                ).value().longValueExact()
        );
    }

    @Test
    void componentBearingInventoryIsRejectedWithoutPreparingMutation() {
        var player = new CellPlayer(
                UUID.randomUUID(),
                "owner"
        );
        var slot = new InventorySlotView(
                new InventoryItemSnapshot(1, "component-stack"),
                new ItemDescriptor("minecraft:stone", 1),
                InventorySlotKind.MAIN,
                false
        );
        var inventories = proxy(
                InventoryPlatformService.class,
                (method, _) -> switch (method.getName()) {
                    case "inventorySlots" -> PlatformResult.success(List.of(slot));
                    case "prepareRemoval" ->
                            throw new AssertionError("mutation must not be prepared");
                    default -> defaultValue(method);
                }
        );
        var service = new ItemValueCommandService(
                inventories,
                proxy(ItemService.class, (method, _) -> defaultValue(method)),
                proxy(WorthService.class, (_, _) -> Optional.of(BigDecimal.ONE)),
                proxy(EconomyService.class, (method, _) -> defaultValue(method)),
                new ImmediateServerThreadExecutor()
        );

        var result = service.sell(
                player,
                ItemValueCommandService.SellSelector.ALL,
                Optional.empty(),
                1
        ).join();

        assertFalse(result.success());
        assertEquals(
                "commands.economy.component-item-unsupported",
                result.messages().getFirst().key()
        );
    }

    @NullMarked
    private static final class TrackingMutation implements InventoryMutation {

        private boolean committed;
        private boolean rolledBack;

        @Override
        public PlatformResult<Void> commit() {
            committed = true;
            return PlatformResult.success();
        }

        @Override
        public PlatformResult<Void> rollback() {
            rolledBack = true;
            return PlatformResult.success();
        }

    }

    @NullMarked
    private static final class ImmediateServerThreadExecutor implements ServerThreadExecutor {

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

    }

}
