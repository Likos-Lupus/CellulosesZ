package top.likoslupus.cellulosesz.api.teleport;

import java.util.concurrent.CompletableFuture;

public interface RandomTeleportSettingsService {

    RandomTeleportSettings settings(String world);

    CompletableFuture<Void> setCenter(String world, double x, double z);

    CompletableFuture<Void> setMinimumRadius(String world, int radius);

    CompletableFuture<Void> setMaximumRadius(String world, int radius);

}
