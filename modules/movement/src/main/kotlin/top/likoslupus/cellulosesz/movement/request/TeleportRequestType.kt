package top.likoslupus.cellulosesz.movement.request

/** Direction of a teleport request. `/tpa` moves the sender; `/tpahere` moves the target. */
internal enum class TeleportRequestType {

    TO_TARGET,
    BRING_TARGET,

}
