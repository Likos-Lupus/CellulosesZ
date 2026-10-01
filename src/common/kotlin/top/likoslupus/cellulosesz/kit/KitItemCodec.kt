package top.likoslupus.cellulosesz.kit

import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import net.minecraft.world.item.ItemStack

internal object KitItemCodec {

    fun encode(stack: ItemStack): String =
        ItemStack.CODEC.encodeStart(
            JsonOps.INSTANCE,
            stack
        ).getOrThrow().toString()

    fun decode(text: String): ItemStack =
        ItemStack.CODEC.parse(
            JsonOps.INSTANCE,
            JsonParser.parseString(text)
        ).getOrThrow()

}
