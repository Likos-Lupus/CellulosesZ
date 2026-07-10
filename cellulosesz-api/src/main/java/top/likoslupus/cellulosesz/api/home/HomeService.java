package top.likoslupus.cellulosesz.api.home;

import top.likoslupus.cellulosesz.api.teleport.CellLocation;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface HomeService {

    CompletableFuture<Map<String, CellLocation>> homes(
            UUID uuid
    );

    CompletableFuture<Optional<CellLocation>> home(
            UUID uuid,
            String name
    );

    CompletableFuture<Boolean> setHome(
            UUID uuid,
            String name,
            CellLocation location
    );

    CompletableFuture<Boolean> deleteHome(
            UUID uuid,
            String name
    );

    CompletableFuture<Boolean> renameHome(
            UUID uuid,
            String oldName,
            String newName
    );

}
