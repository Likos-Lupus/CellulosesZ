package top.likoslupus.cellulosesz.administration.jail

/** Jail location storage. Locations are durable and shared by every sentence. */
internal interface JailLocationRepository {

    suspend fun loadAll(): List<JailLocation>

    suspend fun put(location: JailLocation)

    suspend fun remove(name: JailName): Boolean

}

/** Raised when a jail record cannot be persisted or read back. */
internal class JailDataException(
    message: String,
    cause: Throwable? = null,
) : Exception(
    message,
    cause,
)
