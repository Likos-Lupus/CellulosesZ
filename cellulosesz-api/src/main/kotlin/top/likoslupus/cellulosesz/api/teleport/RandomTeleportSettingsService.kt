package top.likoslupus.cellulosesz.api.teleport

import java.util.concurrent.CompletableFuture

public interface RandomTeleportSettingsService {

    public fun settings(world: String): RandomTeleportSettings

    public fun setCenter(world: String, x: Double, z: Double): CompletableFuture<Void>

    public fun setMinimumRadius(world: String, radius: Int): CompletableFuture<Void>

    public fun setMaximumRadius(world: String, radius: Int): CompletableFuture<Void>

}
