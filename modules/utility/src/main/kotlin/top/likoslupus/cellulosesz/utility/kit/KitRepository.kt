package top.likoslupus.cellulosesz.utility.kit

/**
 * The durable kit catalog. [create] and [replace] are deliberately distinct: create reports an
 * existing kit, replace reports a missing one, and neither silently overwrites the other's intent.
 */
internal interface KitRepository {

    suspend fun loadAll(): Map<KitName, KitDefinition>

    /** Persists a new kit; returns false if the name already exists. */
    suspend fun create(definition: KitDefinition): Boolean

    /** Replaces an existing kit; returns false if the name does not exist. */
    suspend fun replace(definition: KitDefinition): Boolean

    /** Removes a kit; returns false if the name does not exist. */
    suspend fun remove(name: KitName): Boolean

}

/** Raised when kit machine data is missing, corrupt, or unsafe to load. Never silently repaired. */
internal class KitDataException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
