package top.likoslupus.cellulosesz.core.config;

import top.likoslupus.cellulosesz.api.service.Registration;

import java.util.Optional;
import java.util.function.Supplier;

public interface ConfigRegistry {

    default Registration register(
            String key,
            Class<?> type,
            String relativePath,
            Supplier<?> defaultSupplier
    ) {
        return register(key, type, relativePath, defaultSupplier, "global");
    }

    Registration register(
            String key,
            Class<?> type,
            String relativePath,
            Supplier<?> defaultSupplier,
            String owner
    );

    <T> T require(String key, Class<T> type);

    <T> Optional<T> optional(String key, Class<T> type);

    void reload();

}
