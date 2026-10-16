package top.likoslupus.cellulosesz.utility.kit

import java.time.Instant

/** Coarse catalog mutation kind used by the internal integration hook. */
internal enum class KitCatalogChange {

    CREATED,
    UPDATED,
    DELETED,

}

/**
 * Receives one kit-catalog mutation fact on the server thread. A throwing listener is isolated and
 * never stops the others.
 */
internal fun interface KitCatalogListener {

    fun onChanged(
        name: KitName,
        change: KitCatalogChange,
        at: Instant
    )

}

/** Cancellable registration returned by [KitService.observeCatalogChanges]. */
internal fun interface KitCatalogRegistration {

    fun close()

}
