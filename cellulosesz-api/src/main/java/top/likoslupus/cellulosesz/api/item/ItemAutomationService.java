package top.likoslupus.cellulosesz.api.item;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface ItemAutomationService {

    List<String> powerTool(UUID uuid, String itemId);

    Map<String, List<String>> powerTools(UUID uuid);

    void setPowerTool(
            UUID uuid,
            String itemId,
            String command
    );

    void addPowerTool(
            UUID uuid,
            String itemId,
            String command
    );

    boolean removePowerTool(
            UUID uuid,
            String itemId,
            String command
    );

    void clearPowerTool(
            UUID uuid,
            String itemId
    );

    default boolean executePowerTool(CellPlayer player) {
        return executePowerTool(player, "");
    }

    boolean executePowerTool(CellPlayer player, String clickedPlayerName);

    boolean powerToolsEnabled(UUID uuid);

    void setPowerToolsEnabled(UUID uuid, boolean enabled);

    boolean unlimited(UUID uuid, String itemId);

    void setUnlimited(UUID uuid, String itemId, boolean enabled);

    default void maintainUnlimited(CellPlayer player) {
        unlimitedItems(player.uuid())
                .forEach(itemId -> maintainUnlimited(player, itemId));
    }

    Set<String> unlimitedItems(UUID uuid);

    void maintainUnlimited(CellPlayer player, String itemId);

}
