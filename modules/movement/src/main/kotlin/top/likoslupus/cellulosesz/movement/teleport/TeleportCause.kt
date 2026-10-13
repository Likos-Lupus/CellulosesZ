package top.likoslupus.cellulosesz.movement.teleport

/** Why a teleport happens. Drives policy selection, never command-name branching. */
internal enum class TeleportCause {

    HOME,
    WARP,
    SPAWN,
    BACK,
    REQUEST,
    DIRECT,
    ADMIN,
    JAIL_CONFINEMENT,
    JAIL_CORRECTION,
    JAIL_RELEASE,
    RANDOM,
    TOP,
    BOTTOM,
    JUMP,

    ;

    /**
     * Administrative and jail transfers bypass the confinement gate: a jailed player can be moved
     * into the cell, re-anchored after escaping, and released. Every player-instigated move stays
     * denied while confined.
     */
    val bypassesConfinementGate: Boolean
        get() = this == ADMIN
                || this == JAIL_CONFINEMENT
                || this == JAIL_CORRECTION
                || this == JAIL_RELEASE

}
