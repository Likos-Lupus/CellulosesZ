package top.likoslupus.cellulosesz.api.item;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.Optional;

public interface ItemService {

    Optional<ItemDescriptor> parse(String input);

    boolean give(CellPlayer player, ItemDescriptor item);

}
