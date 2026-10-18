package top.likoslupus.cellulosesz.movement.teleport.history

import top.likoslupus.cellulosesz.movement.teleport.StoredPosition

/**
 * Pure `/back` destination choice. The death location wins only when death recording is enabled and
 * the player holds `cellulosesz.back.ondeath`; otherwise the previous teleport location is used.
 */
internal object BackSelection {

    fun select(
        teleport: StoredPosition?,
        death: StoredPosition?,
        recordDeath: Boolean,
        hasDeathPermission: Boolean,
    ): StoredPosition? =
        when {
            recordDeath && hasDeathPermission -> death ?: teleport
            else -> teleport
        }

}
