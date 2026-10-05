package top.likoslupus.cellulosesz.utility.item

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.RecipeType
import top.likoslupus.cellulosesz.core.player.PlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.utility.inventory.InventoryInsertion
import java.util.*

/**
 * The only class that mutates a player's items for the utility features. Recipe-based condensing is
 * isolated here because the recipe and crafting-input APIs are the most version-sensitive surface.
 */
internal class MinecraftItemUtilityBackend(
    private val kernel: RuntimeKernel,
) : ItemUtilityBackend {

    override fun repair(
        playerId: UUID,
        scope: RepairScope,
        allowEnchanted: Boolean,
        includeArmor: Boolean,
    ): RepairBackendResult {
        val player = player(playerId)
            ?: return RepairBackendResult.TargetOffline
        val stacks = repairTargets(player, scope, includeArmor)
        var repaired = 0
        var skipped = 0
        stacks.forEach { stack ->
            if (stack.isEmpty
                || !stack.isDamageableItem
                || stack.damageValue == 0
            ) {
                return@forEach
            }

            if (stack.isEnchanted && !allowEnchanted) {
                skipped++
            } else {
                stack.damageValue = 0
                repaired++
            }
        }

        if (repaired > 0) {
            player.inventory.setChanged()
        }

        return when (repaired) {
            0 if skipped == 0 -> RepairBackendResult.NothingToRepair
            else -> RepairBackendResult.Repaired(
                repaired,
                skipped
            )
        }
    }

    override fun more(
        playerId: UUID,
        amount: Int?,
    ): MoreBackendResult {
        val player = player(playerId)
            ?: return MoreBackendResult.TargetOffline
        val stack = player.mainHandItem
        if (stack.isEmpty)
            return MoreBackendResult.NothingToFill

        val current = stack.count
        val maximum = stack.maxStackSize
        val target = when (amount) {
            null -> maximum
            else -> (current + amount).coerceAtMost(maximum)
        }

        if (target <= current) {
            return MoreBackendResult.NothingToFill
        }
        stack.count = target
        player.inventory.setChanged()
        return MoreBackendResult.Filled(stack.hoverName.string)
    }

    override fun condense(playerId: UUID): CondenseBackendResult {
        val player = player(playerId) ?: return CondenseBackendResult.TargetOffline
        val level = player.level()
        val size = player.inventory.nonEquipmentItems.size
        var conversions = 0

        var index = 0
        while (index < size) {
            val sample = player.inventory.getItem(index)
            val recipe = when {
                isPlain(sample) -> findCondenseRecipe(player, sample)
                else -> null
            }
            if (recipe == null) {
                index++
                continue
            }

            val available = countMatching(player, sample)
            val batches = available / recipe.inputCount
            if (batches <= 0) {
                index++
                continue
            }

            val working = player.inventory.nonEquipmentItems
                    .map { it.copy() }
                    .toMutableList()
            removeMatching(working, sample, recipe.inputCount * batches)
            val leftover = InventoryInsertion.simulate(
                working,
                List(batches) { recipe.result.copy() },
            )
            if (leftover.isNotEmpty()) {
                index++
                continue
            }

            (0 until size).forEach {
                player.inventory.setItem(it, working[it])
            }
            player.inventory.setChanged()
            conversions += batches
            index++
        }

        return when {
            conversions > 0 -> CondenseBackendResult.Condensed(conversions)
            else -> CondenseBackendResult.NothingToCondense
        }
    }

    private fun player(playerId: UUID): ServerPlayer? =
        PlayerResolver.onlineById(
            kernel.requireServer(),
            playerId
        )

    private fun repairTargets(
        player: ServerPlayer,
        scope: RepairScope,
        includeArmor: Boolean,
    ): List<ItemStack> =
        when (scope) {
            RepairScope.HAND -> listOf(player.mainHandItem)
            RepairScope.ALL -> buildList {
                player.inventory.nonEquipmentItems.forEach { add(it) }
                add(player.offhandItem)
                if (includeArmor) {
                    add(player.getItemBySlot(EquipmentSlot.HEAD))
                    add(player.getItemBySlot(EquipmentSlot.CHEST))
                    add(player.getItemBySlot(EquipmentSlot.LEGS))
                    add(player.getItemBySlot(EquipmentSlot.FEET))
                }
            }
        }

    private fun isPlain(stack: ItemStack): Boolean =
        !stack.isEmpty
                && !stack.isEnchanted
                && stack.customName == null
                && stack.damageValue == 0

    private fun countMatching(player: ServerPlayer, sample: ItemStack): Int =
        player.inventory.nonEquipmentItems.sumOf { stack ->
            when {
                !stack.isEmpty
                        && ItemStack.isSameItemSameComponents(stack, sample) ->
                    stack.count

                else -> 0
            }
        }

    private fun removeMatching(
        slots: MutableList<ItemStack>,
        sample: ItemStack,
        amount: Int,
    ) {
        var remaining = amount
        slots.indices.forEach { index ->
            if (remaining <= 0) return@forEach

            val stack = slots[index]
            if (stack.isEmpty || !ItemStack.isSameItemSameComponents(stack, sample))
                return@forEach

            val removed = minOf(stack.count, remaining)
            val next = stack.count - removed
            slots[index] = when (next) {
                0 -> ItemStack.EMPTY
                else -> stack.copyWithCount(next)
            }
            remaining -= removed
        }
    }

    private data class CondenseRecipe(
        val inputCount: Int,
        val result: ItemStack,
    )

    private fun findCondenseRecipe(
        player: ServerPlayer,
        sample: ItemStack
    ): CondenseRecipe? {
        val level = player.level()
        listOf(9 to 3, 4 to 2).forEach { (count, side) ->
            val input = CraftingInput.of(
                side,
                side,
                List(count) { sample.copyWithCount(1) }
            )
            val holder = level
                    .recipeAccess()
                    .getRecipeFor(RecipeType.CRAFTING, input, level)
                    .orElse(null)
                ?: return@forEach

            val result = holder.value().assemble(input)
            if (result.isEmpty
                || result.count != 1
                || result.item == sample.item
            ) return@forEach

            // Require a deterministic reverse recipe so condensation never destroys an item.
            val reverseInput = CraftingInput.of(
                1,
                1,
                listOf(result.copyWithCount(1))
            )
            val reverse = level
                    .recipeAccess()
                    .getRecipeFor(RecipeType.CRAFTING, reverseInput, level)
                    .orElse(null)
                ?: return@forEach
            val back = reverse.value().assemble(reverseInput)
            if (back.isEmpty
                || back.item != sample.item
            ) return@forEach

            return CondenseRecipe(count, result)
        }

        return null
    }

}
