package top.likoslupus.cellulosesz.warp

internal interface WarpRepository {

    suspend fun list(): List<Warp>

    suspend fun get(name: WarpName): Warp?

    suspend fun put(warp: Warp)

    suspend fun remove(name: WarpName): Boolean

}

internal class WarpDataException(
    message: String,
    cause: Throwable? = null
) : Exception(
    message,
    cause
)
