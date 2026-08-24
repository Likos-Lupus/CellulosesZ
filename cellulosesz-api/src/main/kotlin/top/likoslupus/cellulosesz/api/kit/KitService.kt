package top.likoslupus.cellulosesz.api.kit

import top.likoslupus.cellulosesz.api.platform.CellPlayer
import java.util.*
import java.util.concurrent.CompletableFuture

public interface KitService {

    public fun kits(): List<KitDefinition>

    public fun kit(id: String): KitDefinition?

    public fun save(kit: KitDefinition): CompletableFuture<Void>

    public fun delete(id: String): CompletableFuture<Boolean>

    public fun claim(player: CellPlayer, kit: KitDefinition): CompletableFuture<KitClaimResult>

    public fun resetCooldown(uuid: UUID, kitId: String): CompletableFuture<Void>

}
