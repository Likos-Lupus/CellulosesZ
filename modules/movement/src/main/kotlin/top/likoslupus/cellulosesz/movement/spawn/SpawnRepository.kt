package top.likoslupus.cellulosesz.movement.spawn

import top.likoslupus.cellulosesz.movement.teleport.StoredPosition

internal interface SpawnRepository {

    suspend fun read(): StoredPosition?

    suspend fun write(position: StoredPosition?)

}

internal class SpawnDataException(
    message: String,
    cause: Throwable? = null
) : Exception(
    message,
    cause
)
