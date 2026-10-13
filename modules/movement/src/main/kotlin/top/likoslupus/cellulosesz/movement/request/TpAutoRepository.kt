package top.likoslupus.cellulosesz.movement.request

import java.util.*

/** Durable auto-accept state: per-player mode plus the per-owner allow-list. */
internal data class TpAutoSnapshot(
    val modes: Map<UUID, TpAutoMode>,
    val allowed: Map<UUID, Set<UUID>>,
)

/**
 * Persistence boundary for `/tpauto`. The blocking load is used by the bounded startup bootstrap,
 * exactly like the identity and language indexes; mutations are suspending and own their own I/O.
 */
internal interface TpAutoRepository {

    fun loadAllBlocking(): TpAutoSnapshot

    suspend fun setMode(owner: UUID, mode: TpAutoMode)

    suspend fun addAllowed(owner: UUID, allowed: UUID)

    suspend fun removeAllowed(owner: UUID, allowed: UUID): Boolean

}

/** Raised when auto-accept rows cannot be read or written; corrupt rows are never overwritten. */
internal class TpAutoDataException(
    message: String,
    cause: Exception? = null,
) : RuntimeException(message, cause)
