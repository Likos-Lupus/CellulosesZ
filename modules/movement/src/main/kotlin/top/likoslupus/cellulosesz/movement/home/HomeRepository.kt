package top.likoslupus.cellulosesz.movement.home

import java.util.*

internal interface HomeRepository {

    suspend fun list(owner: UUID): List<Home>

    suspend fun put(owner: UUID, home: Home)

    suspend fun remove(owner: UUID, name: HomeName): Boolean

    /** Renames in one transaction; refuses to overwrite an existing destination. */
    suspend fun rename(owner: UUID, from: HomeName, to: HomeName): HomeRenameOutcome

}

/** Repository-level result of an atomic rename. */
internal sealed interface HomeRenameOutcome {

    data object Renamed : HomeRenameOutcome

    data object SourceMissing : HomeRenameOutcome

    data object DestinationExists : HomeRenameOutcome

}

internal class HomeDataException(
    message: String,
    cause: Throwable? = null
) : Exception(
    message,
    cause
)
