package top.likoslupus.cellulosesz.api.user;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface NameCacheService {

    void remember(UUID uuid, String name);

    Optional<UUID> findUuid(String name);

    Optional<String> findName(UUID uuid);

    Map<UUID, String> entries();

    CompletableFuture<Void> save();

}
