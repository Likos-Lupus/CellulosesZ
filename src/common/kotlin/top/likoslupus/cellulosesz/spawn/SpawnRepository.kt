package top.likoslupus.cellulosesz.spawn

import top.likoslupus.cellulosesz.teleport.StoredPosition

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
