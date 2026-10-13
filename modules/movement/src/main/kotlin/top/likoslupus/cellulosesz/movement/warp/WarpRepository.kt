package top.likoslupus.cellulosesz.movement.warp

internal interface WarpRepository : WarpMetadataRepository {

    suspend fun list(): List<Warp>

    suspend fun get(name: WarpName): Warp?

    suspend fun put(warp: Warp, metadata: WarpMetadata)

    suspend fun remove(name: WarpName): Boolean

}

internal class WarpDataException(
    message: String,
    cause: Throwable? = null
) : Exception(
    message,
    cause
)
