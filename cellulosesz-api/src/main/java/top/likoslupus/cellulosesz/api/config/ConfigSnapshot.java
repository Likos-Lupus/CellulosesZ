package top.likoslupus.cellulosesz.api.config;

import java.util.Optional;

public interface ConfigSnapshot {

    <T> T require(String key, Class<T> type);

    <T> Optional<T> optional(String key, Class<T> type);

}
