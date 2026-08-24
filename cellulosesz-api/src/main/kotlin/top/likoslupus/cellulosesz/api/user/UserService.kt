package top.likoslupus.cellulosesz.api.user

import top.likoslupus.cellulosesz.api.platform.CellPlayer
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Function
import java.util.function.UnaryOperator

public interface UserService {

    public fun load(uuid: UUID): CompletableFuture<CellUser>

    public fun loadFromPlayer(player: CellPlayer): CompletableFuture<CellUser>

    public fun cached(uuid: UUID): CellUser?

    public fun cachedUsers(): Collection<CellUser>

    public fun findUuidByName(name: String): UUID?

    public fun knownUuids(): Collection<UUID>

    public fun updateVoid(
        uuid: UUID,
        mutation: UnaryOperator<CellUser>
    ): CompletableFuture<Void> =
        update(uuid) { user ->
            UserUpdate.replacing(mutation.apply(user))
        }.thenAccept { }

    public fun <T> update(
        uuid: UUID,
        mutation: Function<CellUser, UserUpdate<T>>
    ): CompletableFuture<T>

    public fun save(uuid: UUID): CompletableFuture<Void>

    public fun saveAll(): CompletableFuture<Void>

}
