package top.likoslupus.cellulosesz.api.warp

import top.likoslupus.cellulosesz.api.teleport.CellLocation
import java.util.*
import java.util.concurrent.CompletableFuture

public interface WarpService {

    public fun warps(): CompletableFuture<List<Warp>>

    public fun cachedWarps(): List<Warp>

    public fun warp(name: String): CompletableFuture<Warp?>

    public fun cachedWarp(name: String): Warp?

    public fun setWarp(
        name: String,
        location: CellLocation,
        creator: UUID
    ): CompletableFuture<Warp>

    public fun deleteWarp(name: String): CompletableFuture<Boolean>

    public fun requiredPermission(warp: Warp): String?

}
