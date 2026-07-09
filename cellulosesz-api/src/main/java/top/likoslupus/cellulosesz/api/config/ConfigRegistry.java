package top.likoslupus.cellulosesz.api.config;

import java.util.Optional;
import java.util.function.Supplier;

public interface ConfigRegistry {

    <T> T register(
            String key,
            Class<T> type,
            String relativePath,
            Supplier<T> defaultSupplier
    );

    <T> T require(String key, Class<T> type);

    <T> Optional<T> optional(String key, Class<T> type);

    void reload();

}
