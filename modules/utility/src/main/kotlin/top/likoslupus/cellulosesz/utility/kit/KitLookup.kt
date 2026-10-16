package top.likoslupus.cellulosesz.utility.kit

/**
 * The result of resolving a kit name against the in-memory catalog. It distinguishes a catalog that
 * is still loading from a failed/absent one and from a genuinely unknown name, so callers never guess.
 */
internal sealed interface KitLookup {

    data class Found(val definition: KitDefinition) : KitLookup

    data object NotFound : KitLookup

    data object Loading : KitLookup

    data object StorageUnavailable : KitLookup

}
