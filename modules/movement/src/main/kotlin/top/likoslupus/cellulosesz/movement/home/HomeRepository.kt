package top.likoslupus.cellulosesz.movement.home

import java.util.*

internal interface HomeRepository {

    suspend fun list(owner: UUID): List<Home>

    suspend fun put(owner: UUID, home: Home)

    suspend fun remove(owner: UUID, name: HomeName): Boolean

}

internal class HomeDataException(
    message: String,
    cause: Throwable? = null
) : Exception(
    message,
    cause
)
