package top.likoslupus.cellulosesz.api.player

import top.likoslupus.cellulosesz.api.platform.CellPlayer
import java.util.*
import java.util.concurrent.CompletableFuture

public interface PlayerResolver {

    public fun resolveKnown(input: String, viewer: CellPlayer?): ResolvedPlayer

    public fun resolveKnown(uuid: UUID, viewer: CellPlayer?): ResolvedPlayer

    public fun resolve(input: String, viewer: CellPlayer?): CompletableFuture<ResolvedPlayer>

}
