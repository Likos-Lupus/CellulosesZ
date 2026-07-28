package top.likoslupus.cellulosesz.modules.admin.service;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.admin.AdminStatus;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JsonTempBanServiceTest {

    @Test
    void expirationAdditionOverflowIsRejectedWithoutSaving() {
        var storage = new MemoryStorage();
        var service = service(storage);
        var result = service.tempBanIp(
                "192.0.2.1",
                "console",
                Long.MAX_VALUE,
                "test"
        ).join();

        assertEquals(AdminStatus.INVALID_INPUT, result.status());
        assertEquals(0, storage.saves);
        assertTrue(service.activeIp("192.0.2.1").isEmpty());
    }

    private static JsonTempBanService service(MemoryStorage storage) {
        return new JsonTempBanService(
                storage,
                Path.of("temp-bans.json"),
                proxy(PlatformService.class, (method, args) -> defaultValue(method)),
                proxy(UserService.class, (method, args) -> defaultValue(method)),
                proxy(MessageRenderer.class, (method, args) -> defaultValue(method)),
                proxy(LocaleResolver.class, (method, args) -> "en"),
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

    @Test
    void expiredBanIsRemovedByExplicitPurge() throws Exception {
        var storage = new MemoryStorage();
        var service = service(storage);
        assertTrue(service.tempBanIp("192.0.2.2", "console", 1L, "test").join().success());
        Thread.sleep(5L);

        assertTrue(service.activeIp("192.0.2.2").isEmpty());
        assertEquals(1, service.purgeExpired().join());
        assertTrue(storage.saves >= 2);
    }

    @NullMarked
    private static final class MemoryStorage implements StorageService {

        private @Nullable Object document;
        private int saves;

        @Override
        public <T> CompletableFuture<T> loadOrDefault(Path path, Class<T> type, Supplier<T> defaults) {
            if (document == null) return CompletableFuture.completedFuture(defaults.get());
            return CompletableFuture.completedFuture(type.cast(document));
        }

        @Override
        public <T> CompletableFuture<T> createIfMissing(Path path, Class<T> type, Supplier<T> defaults) {
            if (document == null) document = defaults.get();
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
        public <T> CompletableFuture<List<T>> loadDirectory(Path directory, Class<T> type) {
            return CompletableFuture.completedFuture(List.of());
        }

    }

}
