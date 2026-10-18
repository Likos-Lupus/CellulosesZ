package top.likoslupus.cellulosesz.utility.powertool

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerPlayer

/**
 * Process-wide bridge between the packet mixin and the powertool service. The mixin has no reference
 * to the composition graph, so the composition root installs the active service here. This class
 * lives outside the `mixin` package because Mixin forbids normal code from referencing classes in a
 * declared mixin package.
 */
internal object PowertoolInteractState {

    @Volatile private var service: PowertoolService? = null

    fun install(value: PowertoolService?) {
        service = value
    }

    /** Server thread: runs a bound powertool for the interaction, true when one matched. */
    fun handle(player: ServerPlayer, leftClick: Boolean): Boolean {
        val active = service ?: return false
        val stack = player.mainHandItem
        if (stack.isEmpty) {
            return false
        }
        val itemId = BuiltInRegistries.ITEM.getKey(stack.item).toString()
        val trigger = when (leftClick) {
            true -> PowertoolTrigger.LEFT
            false -> PowertoolTrigger.RIGHT
        }
        return active.dispatch(
            player.uuid,
            itemId,
            trigger
        )
    }

}
