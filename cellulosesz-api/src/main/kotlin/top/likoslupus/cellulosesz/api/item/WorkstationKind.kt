package top.likoslupus.cellulosesz.api.item

import java.util.*

public enum class WorkstationKind {

    ANVIL,
    CARTOGRAPHY,
    DISPOSAL,
    GRINDSTONE,
    LOOM,
    SMITHING,
    STONECUTTER,
    WORKBENCH;

    public fun permissionSegment(): String = name.lowercase(Locale.ROOT)

}
