package top.likoslupus.cellulosesz.api.teleport;

import java.util.Optional;

public interface SafeLocationFinder {

    Optional<CellLocation> safeLocation(CellLocation location);

}
