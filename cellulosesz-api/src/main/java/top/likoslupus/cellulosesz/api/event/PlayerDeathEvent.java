package top.likoslupus.cellulosesz.api.event;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;

public record PlayerDeathEvent(
        CellPlayer player,
        CellLocation location,
        String source
) {

}
