package top.likoslupus.cellulosesz.utility.itemtools

import com.mojang.brigadier.exceptions.CommandSyntaxException
import net.minecraft.commands.arguments.item.ItemInput
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.ItemStack
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.utility.config.GiveSettings
import java.util.*

internal sealed interface GiveResult {

    data class Given(
        val targetName: String,
        val itemName: String,
        val amount: Int,
        val dropped: Boolean,
    ) : GiveResult

    data object Blacklisted : GiveResult

    data object InvalidAmount : GiveResult

    /** The item syntax was rejected by the vanilla item argument. */
    data object InvalidItem : GiveResult

    data object TargetOffline : GiveResult

    data object InventoryFull : GiveResult

}

internal sealed interface EnchantResult {

    data class Applied(
        val targetName: String,
        val enchantment: String,
        val level: Int,
    ) : EnchantResult

    data class Removed(
        val targetName: String,
        val enchantment: String,
    ) : EnchantResult

    data object UnknownEnchantment : EnchantResult

    /** A level above the vanilla maximum without the unsafe permission. */
    data object UnsafeDenied : EnchantResult

    data object InvalidLevel : EnchantResult

    data object NoHeldItem : EnchantResult

    data object NotSupported : EnchantResult

    data object TargetOffline : EnchantResult

}

internal sealed interface PotionResult {

    data class Cleared(val itemName: String) : PotionResult

    data class Edited(
        val itemName: String,
        val effectCount: Int,
    ) : PotionResult

    data class Applied(
        val targetName: String,
        val effectCount: Int,
    ) : PotionResult

    data object UnknownEffect : PotionResult

    data object NotPotion : PotionResult

    data object InvalidSpec : PotionResult

    data object NoHeldItem : PotionResult

    data object TargetOffline : PotionResult

}

internal sealed interface SkullResult {

    data class Spawned(
        val owner: String,
        val recipient: String,
        val dropped: Boolean,
    ) : SkullResult

    data class Edited(val owner: String) : SkullResult

    data object NoHeldItem : SkullResult

    data object TargetOffline : SkullResult

    data object InventoryFull : SkullResult

}

internal sealed interface BookResult {

    data class Converted(val nowWritten: Boolean) : BookResult

    data class AuthorSet(val author: String) : BookResult

    data class TitleSet(val title: String) : BookResult

    data object NoHeldItem : BookResult

    data object NotBook : BookResult

    data object NotWrittenBook : BookResult

    data object TargetOffline : BookResult

}

/**
 * Orchestrates the Phase 5 item tools over the server-thread-confined [ItemToolsBackend]. Product
 * policy (blacklist, amount bounds, unsafe enchantment levels) lives here; Minecraft interaction
 * stays in the backend.
 */
internal class ItemToolsService(
    private val backend: ItemToolsBackend,
    private val known: KnownPlayerResolver,
    private val settings: () -> GiveSettings,
) {

    fun give(
        targetName: String,
        input: ItemInput,
        amount: Int?
    ): GiveResult {
        val base = materialize(input)
            ?: return GiveResult.InvalidItem
        val config = settings()
        if (ItemToolsPolicy.isBlacklisted(registryId(base), config.blacklist)) {
            return GiveResult.Blacklisted
        }
        val resolvedAmount = ItemToolsPolicy.resolveAmount(
            amount,
            config.defaultAmount,
            config.maxAmount
        )
            ?: return GiveResult.InvalidAmount
        val target = known.onlineByName(targetName)
            ?: return GiveResult.TargetOffline
        return when (val result = backend.give(
            target.id,
            base,
            resolvedAmount,
            config.dropIfFull
        )) {
            is GiveBackendResult.Given -> GiveResult.Given(
                targetName = target.name,
                itemName = result.itemName,
                amount = result.amount,
                dropped = result.dropped,
            )

            GiveBackendResult.TargetOffline -> GiveResult.TargetOffline
            GiveBackendResult.InventoryFull -> GiveResult.InventoryFull
        }
    }

    fun item(
        actorId: UUID,
        input: ItemInput,
        amount: Int?
    ): GiveResult {
        val base = materialize(input)
            ?: return GiveResult.InvalidItem
        val config = settings()
        if (ItemToolsPolicy.isBlacklisted(registryId(base), config.blacklist)) {
            return GiveResult.Blacklisted
        }
        val resolvedAmount = ItemToolsPolicy.resolveAmount(
            amount,
            config.defaultAmount,
            config.maxAmount
        )
            ?: return GiveResult.InvalidAmount
        val actor = known.onlineById(actorId)
            ?: return GiveResult.TargetOffline
        return when (
            val result = backend.give(
                actorId,
                base,
                resolvedAmount,
                config.dropIfFull
            )
        ) {
            is GiveBackendResult.Given -> GiveResult.Given(
                targetName = actor.name,
                itemName = result.itemName,
                amount = result.amount,
                dropped = result.dropped,
            )

            GiveBackendResult.TargetOffline -> GiveResult.TargetOffline
            GiveBackendResult.InventoryFull -> GiveResult.InventoryFull
        }
    }

    fun enchant(
        targetName: String,
        enchantment: String,
        level: Int?,
        unsafeAllowed: Boolean,
    ): EnchantResult {
        val target = known.onlineByName(targetName)
            ?: return EnchantResult.TargetOffline
        val resolvedLevel = level ?: DEFAULT_ENCHANT_LEVEL
        if (resolvedLevel < 0) {
            return EnchantResult.InvalidLevel
        }
        if (resolvedLevel > 0 && !unsafeAllowed) {
            val max = backend.enchantmentMaxLevel(enchantment)
                ?: return EnchantResult.UnknownEnchantment
            if (resolvedLevel > max) {
                return EnchantResult.UnsafeDenied
            }
        }
        return when (
            val result = backend.applyEnchantment(
                target.id,
                enchantment,
                resolvedLevel
            )
        ) {
            is EnchantBackendResult.Applied -> EnchantResult.Applied(
                targetName = target.name,
                enchantment = result.enchantmentName,
                level = result.level,
            )

            is EnchantBackendResult.Removed -> EnchantResult.Removed(
                target.name,
                result.enchantmentName
            )

            EnchantBackendResult.NoHeldItem -> EnchantResult.NoHeldItem
            EnchantBackendResult.TargetOffline -> EnchantResult.TargetOffline
            EnchantBackendResult.NotSupported -> EnchantResult.NotSupported
        }
    }

    fun potion(targetId: UUID, spec: PotionSpec): PotionResult =
        when (spec) {
            PotionSpec.Invalid -> PotionResult.InvalidSpec
            PotionSpec.Apply -> map(backend.applyPotion(targetId))
            PotionSpec.Clear,
            is PotionSpec.Effect -> map(backend.editPotion(targetId, spec))
        }

    fun skull(
        actorId: UUID,
        recipientName: String?,
        ownerName: String?,
        spawn: Boolean,
    ): SkullResult {
        val recipient = when (recipientName) {
            null -> known.onlineById(actorId)
            else -> known.onlineByName(recipientName)
        } ?: return SkullResult.TargetOffline
        val owner = when (ownerName) {
            null -> recipient.name
            else -> known.knownByName(ownerName)?.name ?: ownerName
        }
        return when (
            val result = backend.skull(
                actorId,
                recipient.id,
                owner,
                spawn
            )
        ) {
            is SkullBackendResult.Spawned -> SkullResult.Spawned(
                owner = result.owner,
                recipient = result.recipient,
                dropped = result.dropped,
            )

            is SkullBackendResult.Edited -> SkullResult.Edited(result.owner)
            SkullBackendResult.NoHeldItem -> SkullResult.NoHeldItem
            SkullBackendResult.TargetOffline -> SkullResult.TargetOffline
            SkullBackendResult.InventoryFull -> SkullResult.InventoryFull
        }
    }

    fun book(actorId: UUID, action: BookAction): BookResult =
        when (val result = backend.editBook(actorId, action)) {
            is BookBackendResult.Converted -> BookResult.Converted(result.nowWritten)
            is BookBackendResult.AuthorSet -> BookResult.AuthorSet(result.author)
            is BookBackendResult.TitleSet -> BookResult.TitleSet(result.title)
            BookBackendResult.NoHeldItem -> BookResult.NoHeldItem
            BookBackendResult.NotBook -> BookResult.NotBook
            BookBackendResult.TargetOffline -> BookResult.TargetOffline
            BookBackendResult.NotWrittenBook -> BookResult.NotWrittenBook
        }

    fun itemInfo(actorId: UUID?, input: ItemInput?): ItemInfo? {
        val provided = input?.let { materialize(it) }
        if (input != null && provided == null) {
            return null
        }
        return backend.info(actorId, provided, includeDurability = input == null)
    }

    fun recipes(actorId: UUID, input: ItemInput): List<RecipeView> {
        val stack = materialize(input) ?: return emptyList()
        return backend.recipes(actorId, stack)
    }

    private fun map(result: PotionEditBackendResult): PotionResult =
        when (result) {
            is PotionEditBackendResult.Cleared -> PotionResult.Cleared(result.itemName)
            is PotionEditBackendResult.Edited -> PotionResult.Edited(
                result.itemName,
                result.effectCount
            )

            is PotionEditBackendResult.Applied -> PotionResult.Applied(
                result.targetName,
                result.effectCount
            )

            PotionEditBackendResult.NoHeldItem -> PotionResult.NoHeldItem
            PotionEditBackendResult.TargetOffline -> PotionResult.TargetOffline
            PotionEditBackendResult.UnknownEffect -> PotionResult.UnknownEffect
            PotionEditBackendResult.NotPotion -> PotionResult.NotPotion
        }

    private fun materialize(input: ItemInput): ItemStack? =
        try {
            input.createItemStack(1)
        } catch (_: CommandSyntaxException) {
            null
        }

    private fun registryId(stack: ItemStack): String =
        BuiltInRegistries.ITEM.getKey(stack.item).toString()

    private companion object {

        private const val DEFAULT_ENCHANT_LEVEL: Int = 1

    }

}
