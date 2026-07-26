package top.likoslupus.cellulosesz.api.item;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.Optional;
import java.util.Set;

public interface ItemService {

    Optional<ItemDescriptor> parse(String input);

    String commandArgument(ItemDescriptor item);

    boolean valid(ItemDescriptor item);

    boolean blacklisted(ItemDescriptor item);

    int maxStackSize(ItemDescriptor item);

    Set<String> names();

    boolean give(CellPlayer player, ItemDescriptor item);

    int count(CellPlayer player, ItemDescriptor item);

    boolean take(CellPlayer player, ItemDescriptor item);

    Optional<String> heldItemId(CellPlayer player);

}
