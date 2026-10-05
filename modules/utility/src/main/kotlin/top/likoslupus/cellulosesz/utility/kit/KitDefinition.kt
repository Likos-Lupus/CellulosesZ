package top.likoslupus.cellulosesz.utility.kit

import net.minecraft.world.item.ItemStack

/**
 * A server-owned kit definition. [items] are immutable snapshots: delivery always [ItemStack.copy]s
 * them, and updates replace the whole value rather than mutating the list.
 */
internal data class KitDefinition(
    val id: KitId,
    val name: KitName,
    val reuse: KitReusePolicy,
    val items: List<ItemStack>,
)
