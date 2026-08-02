package top.likoslupus.cellulosesz.api.item;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;

/** Item application service that preserves validation and platform failure states. */
public interface ItemService {

    PlatformResult<ItemDescriptor> parse(String input);

    PlatformResult<Boolean> valid(ItemDescriptor item);

    boolean blacklisted(ItemDescriptor item);

    PlatformResult<Integer> maxStackSize(ItemDescriptor item);

    PlatformResult<ItemGrantResult> give(CellPlayer player, ItemDescriptor item);

    PlatformResult<Integer> count(CellPlayer player, ItemDescriptor item);

    PlatformResult<Void> take(CellPlayer player, ItemDescriptor item);

    PlatformResult<String> heldItemId(CellPlayer player);

}
