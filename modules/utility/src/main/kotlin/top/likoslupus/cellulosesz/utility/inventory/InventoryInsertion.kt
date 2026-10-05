package top.likoslupus.cellulosesz.utility.inventory

import net.minecraft.world.item.ItemStack

/**
 * Pure, deterministic inventory insertion shared by kit delivery and item condensing. It merges into
 * matching stacks first, then fills empty slots, and returns whatever could not be placed. It never
 * touches a live inventory, so callers can preflight and commit from the same result.
 */
internal object InventoryInsertion {

    fun simulate(
        slots: MutableList<ItemStack>,
        incoming: List<ItemStack>,
    ): List<ItemStack> {
        val leftover = mutableListOf<ItemStack>()
        incoming.forEach { source ->
            var remaining = source.copy()
            slots.indices.forEach {
                if (remaining.isEmpty) return@forEach

                val current = slots[it]
                if (current.isEmpty
                    || !ItemStack.isSameItemSameComponents(current, remaining)
                    || current.count >= current.maxStackSize
                ) {
                    return@forEach
                }

                val space = current.maxStackSize - current.count
                val moved = minOf(space, remaining.count)
                slots[it] = current.copyWithCount(current.count + moved)
                remaining = remaining.copyWithCount(remaining.count - moved)
            }

            slots.indices.forEach {
                if (remaining.isEmpty) return@forEach
                if (!slots[it].isEmpty) return@forEach

                val moved = minOf(remaining.maxStackSize, remaining.count)
                slots[it] = remaining.copyWithCount(moved)
                remaining = remaining.copyWithCount(remaining.count - moved)
            }

            if (!remaining.isEmpty) {
                leftover += remaining
            }
        }

        return leftover
    }

}
