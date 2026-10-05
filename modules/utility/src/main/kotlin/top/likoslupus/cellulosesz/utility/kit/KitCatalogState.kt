package top.likoslupus.cellulosesz.utility.kit

/** Lifecycle of the in-memory kit catalog. */
internal sealed interface KitCatalogState {

    data object Loading : KitCatalogState

    data object Ready : KitCatalogState

    data class Failed(val reason: String) : KitCatalogState

}
