package top.likoslupus.cellulosesz.api.teleport

import top.likoslupus.cellulosesz.api.platform.CellPlayer
import java.util.*
import java.util.concurrent.CompletableFuture

public interface TeleportService {

    public fun teleport(
        player: CellPlayer,
        target: CellLocation,
        options: TeleportOptions
    ): CompletableFuture<TeleportResult>

    public fun cancelWarmup(uuid: UUID, status: TeleportStatus): Boolean

    public fun warmingUp(uuid: UUID): Boolean

    public fun rememberBackLocation(player: CellPlayer): CompletableFuture<Void>

    public fun rememberBackLocation(uuid: UUID, location: CellLocation): CompletableFuture<Void>

    public fun backLocation(uuid: UUID): CellLocation?

    public fun shutdown()

}
