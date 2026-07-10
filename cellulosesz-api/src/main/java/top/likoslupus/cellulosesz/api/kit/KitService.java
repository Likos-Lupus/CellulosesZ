package top.likoslupus.cellulosesz.api.kit;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface KitService {

    CompletableFuture<Void> reload();

    List<KitDefinition> kits();

    Optional<KitDefinition> kit(String id);

    CompletableFuture<Void> save(KitDefinition kit);

    CompletableFuture<Boolean> delete(String id);

    CompletableFuture<KitClaimResult> claim(CellPlayer player, KitDefinition kit);

    CompletableFuture<Void> resetCooldown(UUID uuid, String kitId);

}
