package top.likoslupus.cellulosesz.utility.kit

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import net.minecraft.resources.RegistryOps
import net.minecraft.world.item.ItemStack

/**
 * The single version-sensitive item boundary. Item components can reference registries (for example
 * enchantments), so encoding/decoding must run against a registry-aware [RegistryOps] rather than a
 * bare [JsonOps]. The [ops] supplier is captured from the running server's registry access.
 */
internal class KitItemCodec(private val ops: () -> RegistryOps<JsonElement>) {

    fun encode(stack: ItemStack): String =
        ItemStack.CODEC
                .encodeStart(ops(), stack)
                .getOrThrow()
                .toString()

    fun decode(text: String): ItemStack =
        ItemStack.CODEC
                .parse(ops(), JsonParser.parseString(text))
                .getOrThrow()

}
