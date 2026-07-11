package top.likoslupus.cellulosesz.api.item;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ItemAutomationService {

    Optional<String> powerTool(UUID uuid, String itemId);

    Map<String, String> powerTools(UUID uuid);

    void setPowerTool(UUID uuid, String itemId, String command);

    void clearPowerTool(UUID uuid, String itemId);

    boolean executePowerTool(CellPlayer player);

    boolean unlimited(UUID uuid, String itemId);

    Set<String> unlimitedItems(UUID uuid);

    void setUnlimited(UUID uuid, String itemId, boolean enabled);

    void maintainUnlimited(CellPlayer player);

}
