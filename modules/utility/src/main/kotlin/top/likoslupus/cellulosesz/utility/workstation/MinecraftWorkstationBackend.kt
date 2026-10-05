package top.likoslupus.cellulosesz.utility.workstation

import net.minecraft.network.chat.Component
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.inventory.*
import top.likoslupus.cellulosesz.core.player.PlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import java.util.*

/**
 * Opens the vanilla workstation menus with a null level access so they behave as portable interfaces
 * without placing a block. All version-sensitive menu constructors are confined to this class.
 */
internal class MinecraftWorkstationBackend(
    private val kernel: RuntimeKernel,
) : WorkstationBackend {

    override fun open(playerId: UUID, type: WorkstationType): WorkstationOpenResult {
        val player = PlayerResolver.onlineById(kernel.requireServer(), playerId)
            ?: return WorkstationOpenResult.PlayerOffline

        val provider = SimpleMenuProvider(
            { id, inventory, _ ->
                val access = ContainerLevelAccess.NULL
                when (type) {
                    WorkstationType.CRAFTING -> CraftingMenu(id, inventory, access)
                    WorkstationType.ANVIL -> AnvilMenu(id, inventory, access)
                    WorkstationType.GRINDSTONE -> GrindstoneMenu(id, inventory, access)
                    WorkstationType.STONECUTTER -> StonecutterMenu(id, inventory, access)
                    WorkstationType.LOOM -> LoomMenu(id, inventory, access)
                    WorkstationType.CARTOGRAPHY -> CartographyTableMenu(id, inventory, access)
                    WorkstationType.SMITHING -> SmithingMenu(id, inventory, access)
                }
            },
            title(type),
        )

        return when {
            player.openMenu(provider).isPresent -> WorkstationOpenResult.Opened
            else -> WorkstationOpenResult.Failed
        }
    }

    private fun title(type: WorkstationType): Component =
        Component.translatable(
            when (type) {
                WorkstationType.CRAFTING -> "container.crafting"
                WorkstationType.ANVIL -> "container.repair"
                WorkstationType.GRINDSTONE -> "container.grindstone_title"
                WorkstationType.STONECUTTER -> "container.stonecutter"
                WorkstationType.LOOM -> "container.loom"
                WorkstationType.CARTOGRAPHY -> "container.cartography_table"
                WorkstationType.SMITHING -> "container.upgrade"
            }
        )

}
