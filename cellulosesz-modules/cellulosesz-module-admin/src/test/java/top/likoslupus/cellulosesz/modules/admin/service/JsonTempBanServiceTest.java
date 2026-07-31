package top.likoslupus.cellulosesz.modules.admin.service;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.admin.AdminActor;
import top.likoslupus.cellulosesz.api.admin.AdminStatus;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.player.PlayerConnectionService;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.player.PlayerNetworkService;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.text.PlayerAudienceService;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JsonTempBanServiceTest {

    @Test
    void expirationAdditionOverflowIsRejectedWithoutSaving() throws Exception {
        var storage = new MemoryStorage();
        var clock = new MutableClock(Instant.MAX.minusSeconds(1));
        var service = service(storage, clock);

        var result = service.tempBanIp(
                InetAddress.getByAddress(new byte[]{(byte) 192, 0, 2, 1}),
                AdminActor.console("console"),
                Duration.ofSeconds(2),
                "test"
        ).join();

        assertEquals(AdminStatus.INVALID_INPUT, result.status());
        assertEquals(0, storage.saves);
    }

    private static JsonTempBanService service(MemoryStorage storage, Clock clock) {
        return new JsonTempBanService(
                storage,
                Path.of("temp-bans.json"),
                proxy(PlayerDirectory.class, (method, args) -> defaultValue(method)),
                proxy(PlayerConnectionService.class, (method, args) -> defaultValue(method)),
                proxy(PlayerAudienceService.class, (method, args) -> defaultValue(method)),
                proxy(PlayerNetworkService.class, (method, args) -> Optional.empty()),
                proxy(MessageRenderer.class, (method, args) -> defaultValue(method)),
                directExecutor(),
                clock,
                false
        );
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, BiFunction<Method, Object[], Object> behavior) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (instance, method, rawArgs) -> {
            var args = rawArgs == null ? new Object[0] : rawArgs;
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "toString" -> type.getSimpleName() + "TestProxy";
                    case "hashCode" -> System.identityHashCode(instance);
                    case "equals" -> instance == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                };
            }
            if (method.isDefault()) return InvocationHandler.invokeDefault(instance, method, args);
            return behavior.apply(method, args);
        });
    }

    private static Object defaultValue(Method method) {
        var type = method.getReturnType();
        if (type == void.class) return null;
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == String.class) return "";
        if (type == Optional.class) return Optional.empty();
        if (type == List.class) return List.of();
        if (type == Set.class) return Set.of();
        if (type == Collection.class) return List.of();
        if (type == Map.class) return Map.of();
        if (type == CompletableFuture.class) return CompletableFuture.completedFuture(null);
        throw new UnsupportedOperationException(method.toString());
    }

    private static ServerThreadExecutor directExecutor() {
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
                try {
                    return CompletableFuture.completedFuture(task.get());
                } catch (Throwable failure) {
                    return CompletableFuture.failedFuture(failure);
                }
            }
        };
    }

    @Test
    void expiredBanIsRemovedByExplicitPurge() throws Exception {
        var storage = new MemoryStorage();
        var clock = new MutableClock(Instant.parse("2026-07-30T00:00:00Z"));
        var service = service(storage, clock);
        var address = InetAddress.getByAddress(new byte[]{(byte) 192, 0, 2, 2});

        assertTrue(service.tempBanIp(address, AdminActor.console("console"), Duration.ofSeconds(1), "test")
                .join()
                .success());
        assertTrue(service.activeIp(address).isPresent());
        clock.advance(Duration.ofSeconds(2));

        assertTrue(service.activeIp(address).isEmpty());
        assertEquals(1, service.purgeExpired().join().intValue());
        assertEquals(2, storage.saves);
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

    }

    @NullMarked
    private static final class MemoryStorage implements StorageService {

        private @Nullable Object document;
        private int saves;

        @Override
        public <T> CompletableFuture<T> loadOrDefault(
                Path path,
                Class<T> type,
                Supplier<T> defaults
        ) {
            return CompletableFuture.completedFuture(
                    document == null
                            ? defaults.get()
                            : type.cast(document)
            );
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
            saves++;
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
        public <T> CompletableFuture<List<T>> loadDirectory(
                Path directory,
                Class<T> type
        ) {
            return CompletableFuture.completedFuture(List.of());
        }

    }

}
