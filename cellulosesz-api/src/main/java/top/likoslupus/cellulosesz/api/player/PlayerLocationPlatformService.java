package top.likoslupus.cellulosesz.api.player;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;

public interface PlayerLocationPlatformService {

    CellLocation currentLocation(CellPlayer player);

}
