package top.likoslupus.cellulosesz.movement.teleport.safety

import top.likoslupus.cellulosesz.movement.teleport.StoredPosition

/** Result of resolving a requested position against the safety policy. */
internal sealed interface SafetyResult {

    data class Safe(val position: StoredPosition) : SafetyResult

    data object Unsafe : SafetyResult

}
