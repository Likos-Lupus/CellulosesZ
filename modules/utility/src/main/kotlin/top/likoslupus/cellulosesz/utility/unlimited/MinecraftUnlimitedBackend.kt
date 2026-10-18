package top.likoslupus.cellulosesz.utility.unlimited

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerPlayer
import top.likoslupus.cellulosesz.core.player.PlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import java.util.*

/** Applies the unlimited top-up rule to a live player's main inventory. */
internal class MinecraftUnlimitedBackend(
    private val kernel: RuntimeKernel,
) : UnlimitedBackend {

    override fun heldItemId(playerId: UUID): String? {
        val player = player(playerId) ?: return null
        val held = player.mainHandItem
        if (held.isEmpty) {
            return null
        }
        return BuiltInRegistries.ITEM.getKey(held.item).toString()
    }

    override fun topUp(playerId: UUID, unlimited: Set<String>): Int {
        if (unlimited.isEmpty()) {
            return 0
        }
        val player = player(playerId) ?: return 0
        val slots = player.inventory.nonEquipmentItems.mapIndexed { index, stack ->
            UnlimitedSlot(
                slot = index,
                itemId = when {
                    stack.isEmpty -> ""
                    else -> BuiltInRegistries.ITEM.getKey(stack.item).toString() ?: ""
                },
                count = stack.count,
            )
        }
        val plan = UnlimitedTopUp.plan(slots, unlimited)
        if (plan.isEmpty()) {
            return 0
        }
        plan.forEach { entry ->
            player.inventory.getItem(entry.slot).let { stack ->
                if (!stack.isEmpty) {
                    stack.copyWithCount(entry.count)
                            .let { player.inventory.setItem(entry.slot, it) }
                }
            }
        }
        player.inventory.setChanged()
        return plan.size
    }

    private fun player(playerId: UUID): ServerPlayer? =
        PlayerResolver.onlineById(kernel.requireServer(), playerId)

}
