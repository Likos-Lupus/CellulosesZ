package top.likoslupus.cellulosesz.api.teleport;

import java.util.Optional;

public interface RandomTeleportService {

    Optional<CellLocation> randomLocation(
            String world,
            int minRadius,
            int maxRadius
    );

}
