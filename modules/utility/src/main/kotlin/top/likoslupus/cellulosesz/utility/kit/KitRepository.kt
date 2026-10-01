package top.likoslupus.cellulosesz.utility.kit

import net.minecraft.world.item.ItemStack

internal interface KitRepository {

    suspend fun names(): List<KitName>

    suspend fun load(name: KitName): List<ItemStack>?

    suspend fun save(name: KitName, items: List<ItemStack>)

    suspend fun remove(name: KitName): Boolean

}

internal class KitDataException(
    message: String,
    cause: Throwable? = null
) : Exception(
    message,
    cause
)
