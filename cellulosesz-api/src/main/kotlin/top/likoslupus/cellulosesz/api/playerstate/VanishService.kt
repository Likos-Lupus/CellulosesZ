package top.likoslupus.cellulosesz.api.playerstate

import top.likoslupus.cellulosesz.api.platform.CellPlayer
import java.util.*
import java.util.concurrent.CompletableFuture

public interface VanishService {

    public fun vanished(uuid: UUID): Boolean

    public fun setVanished(
        player: CellPlayer,
        vanished: Boolean
    ): CompletableFuture<PlayerStateResult>

    public fun canSee(viewer: CellPlayer, target: UUID): Boolean

    public fun synchronizeViewer(viewer: CellPlayer)

}
