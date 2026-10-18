package top.likoslupus.cellulosesz.movement.request

/**
 * The single rule behind `/tptoggle`. A player whose teleport setting is off cannot be teleported to
 * or moved, unless the actor holds the bypass capability. The decision is pure so it can be
 * unit-tested without Minecraft.
 */
internal object TeleportPreferenceGate {

    fun allowsTarget(targetAccepts: Boolean, actorBypasses: Boolean): Boolean =
        actorBypasses || targetAccepts

}
