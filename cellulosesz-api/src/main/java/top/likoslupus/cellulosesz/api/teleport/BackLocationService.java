package top.likoslupus.cellulosesz.api.teleport;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.Optional;
import java.util.UUID;

public interface BackLocationService {

    void remember(CellPlayer player);

    void remember(UUID uuid, CellLocation location);

    Optional<CellLocation> location(UUID uuid);

}
