package top.likoslupus.cellulosesz.api.item;

import java.util.Locale;

public enum WorkstationKind {

    ANVIL,
    CARTOGRAPHY,
    DISPOSAL,
    GRINDSTONE,
    LOOM,
    SMITHING,
    STONECUTTER,
    WORKBENCH;

    public String permissionSegment() {
        return name().toLowerCase(Locale.ROOT);
    }

}
