package top.likoslupus.cellulosesz.api.playerstate;

import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.UUID;

public interface VanishService {

    boolean vanished(UUID uuid);

    AdminResult setVanished(CellPlayer player, boolean vanished);

    boolean canSee(CellPlayer viewer, UUID target);

    void synchronizeViewer(CellPlayer viewer);

}
