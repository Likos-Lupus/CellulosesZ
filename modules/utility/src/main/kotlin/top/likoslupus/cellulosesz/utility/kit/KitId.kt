package top.likoslupus.cellulosesz.utility.kit

import java.util.*

/**
 * Stable identity of a kit definition. Deleting and recreating a kit with the same [KitName]
 * produces a new [KitId], so old claim history can never leak onto the new definition.
 */
@JvmInline
internal value class KitId(val value: UUID)
