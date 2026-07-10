package top.likoslupus.cellulosesz.api.world;

import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.Optional;

public interface EntityRemoveService {

    AdminResult remove(
            String selector,
            Optional<CellPlayer> origin,
            int radius
    );

}
