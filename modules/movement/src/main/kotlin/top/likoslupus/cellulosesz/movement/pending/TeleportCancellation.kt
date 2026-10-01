package top.likoslupus.cellulosesz.movement.pending

/** Why a delayed teleport was cancelled before it committed. */
internal enum class TeleportCancellation {

    MOVED,
    DAMAGED,
    DISCONNECTED,
    DIMENSION_CHANGED,
    SERVER_STOPPING,

}
