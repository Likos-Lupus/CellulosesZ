package top.likoslupus.cellulosesz.api.item;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.text.RichText;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Loader-neutral Minecraft item operations. All methods must run on the server thread.
 */
public interface ItemPlatformService {

    Set<String> itemIds();

    Set<String> enchantmentIds();

    Set<String> potionEffectIds();

    boolean validItem(String itemId);

    int maxStackSize(String itemId);

    PlatformResult<ItemGrantResult> grant(CellPlayer player, ItemDescriptor descriptor);

    PlatformResult<Integer> count(CellPlayer player, ItemDescriptor descriptor);

    PlatformResult<Void> take(CellPlayer player, ItemDescriptor descriptor);

    PlatformResult<String> heldItemId(CellPlayer player);

    PlatformResult<Void> setHeldName(CellPlayer player, Optional<RichText> name);

    PlatformResult<Void> setHeldLore(CellPlayer player, List<RichText> lore);

    PlatformResult<Void> enchant(
            CellPlayer player,
            String enchantmentId,
            int level,
            boolean unsafe
    );

    PlatformResult<Integer> repair(CellPlayer player, RepairScope scope);

    PlatformResult<Void> applyPotion(CellPlayer player, PotionItemRequest request);

    PlatformResult<Void> applyFirework(CellPlayer player, FireworkItemRequest request);

    PlatformResult<Void> maintainCount(
            CellPlayer player,
            String itemId,
            int minimum
    );

}
