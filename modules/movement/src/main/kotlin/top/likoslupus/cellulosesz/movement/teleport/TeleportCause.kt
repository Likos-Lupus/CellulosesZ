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
    RANDOM,

}
