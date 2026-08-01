package top.likoslupus.cellulosesz.api.item;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ItemAutomationService {

    List<String> powerTool(UUID uuid, String itemId);

    Map<String, List<String>> powerTools(UUID uuid);

    CompletableFuture<PlatformResult<Void>> setPowerTool(
            UUID uuid,
            String itemId,
            String command
    );

    CompletableFuture<PlatformResult<Void>> addPowerTool(
            UUID uuid,
            String itemId,
            String command
    );

    CompletableFuture<PlatformResult<Boolean>> removePowerTool(
            UUID uuid,
            String itemId,
            String command
    );

    CompletableFuture<PlatformResult<Void>> clearPowerTool(UUID uuid, String itemId);

    CompletableFuture<PlatformResult<Void>> clearAllPowerTools(UUID uuid);

    default boolean executePowerTool(CellPlayer player) {
        return executePowerTool(player, "");
    }

    boolean executePowerTool(CellPlayer player, String clickedPlayerName);

    boolean powerToolsEnabled(UUID uuid);

    CompletableFuture<PlatformResult<Void>> setPowerToolsEnabled(UUID uuid, boolean enabled);

    boolean unlimited(UUID uuid, String itemId);

    CompletableFuture<PlatformResult<Void>> setUnlimited(
            UUID uuid,
            String itemId,
            boolean enabled
    );

    CompletableFuture<PlatformResult<Void>> clearUnlimited(UUID uuid);

    default void maintainUnlimited(CellPlayer player) {
        unlimitedItems(player.uuid())
                .forEach(itemId -> maintainUnlimited(player, itemId));
    }

    Set<String> unlimitedItems(UUID uuid);

    void maintainUnlimited(CellPlayer player, String itemId);

}
