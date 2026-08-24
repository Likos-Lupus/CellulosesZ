package top.likoslupus.cellulosesz.api.user

import java.util.*
import java.util.concurrent.CompletableFuture

public interface NameCacheService {

    public fun remember(uuid: UUID, name: String)

    public fun findUuid(name: String): UUID?

    public fun findName(uuid: UUID): String?

    public fun entries(): Map<UUID, String>

    public fun save(): CompletableFuture<Void>

}
