package top.likoslupus.cellulosesz.economy.sale

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mojang.serialization.DynamicOps
import net.minecraft.world.item.ItemStack

/** One inventory slot and the exact stack recovered from it; used for both capture and restore. */
internal data class RemovedStack(
    val slot: Int,
    val stack: ItemStack,
)

/**
 * Serializes the exact removed stacks into the sale-intent payload, and decodes them again for
 * recovery. Item components may reference registries (enchantments, potion contents), so encoding
 * and decoding run against a registry-aware [DynamicOps] captured from the running server — the same
 * technique the kit codec uses, copied locally so economy never depends on the utility module.
 *
 * Format: a JSON array of `{"slot": <int>, "stack": <ItemStack.CODEC JSON>}`. Only non-empty stacks
 * are ever written, so a decode can trust every entry.
 */
internal class SaleItemCodec(
    private val ops: () -> DynamicOps<JsonElement>,
) {

    fun encode(stacks: List<RemovedStack>): String {
        val array = JsonArray()
        stacks.forEach { removed ->
            val element = ItemStack.CODEC
                    .encodeStart(ops(), removed.stack)
                    .getOrThrow()
            val entry = JsonObject()
            entry.addProperty("slot", removed.slot)
            entry.add("stack", element)
            array.add(entry)
        }
        return array.toString()
    }

    fun decode(payload: String): List<RemovedStack> {
        val array = JsonParser.parseString(payload)
        if (!array.isJsonArray) {
            return emptyList()
        }
        return array.asJsonArray.mapNotNull { element ->
            if (!element.isJsonObject) {
                return@mapNotNull null
            }
            val entry = element.asJsonObject
            val slot = entry.get("slot")?.takeIf { it.isJsonPrimitive }?.asInt
                ?: return@mapNotNull null
            val stack = entry.get("stack")
                ?: return@mapNotNull null
            val decoded = ItemStack.CODEC.parse(
                ops(),
                stack
            ).getOrThrow()
            when {
                decoded.isEmpty -> null
                else -> RemovedStack(slot, decoded)
            }
        }
    }

}
