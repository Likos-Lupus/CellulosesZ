package top.likoslupus.cellulosesz.utility.itemtools

import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.Filterable
import net.minecraft.world.InteractionHand
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.item.component.ResolvableProfile
import net.minecraft.world.item.component.WritableBookContent
import net.minecraft.world.item.component.WrittenBookContent
import net.minecraft.world.item.crafting.display.SlotDisplayContext
import net.minecraft.world.item.enchantment.ItemEnchantments
import top.likoslupus.cellulosesz.core.player.PlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.utility.inventory.InventoryInsertion
import java.util.*

/**
 * The only class that mutates a player's items for the Phase 5 item tools. These operations are
 * immediate and synchronous; command handlers already run on the server thread.
 */
internal class MinecraftItemToolsBackend(
    private val kernel: RuntimeKernel,
) : ItemToolsBackend {

    override fun give(
        targetId: UUID,
        base: ItemStack,
        amount: Int,
        dropIfFull: Boolean,
    ): GiveBackendResult {
        val player = player(targetId)
            ?: return GiveBackendResult.TargetOffline
        return when (val insertion = insert(player, expand(base, amount), dropIfFull)) {
            Insertion.Full -> GiveBackendResult.InventoryFull
            is Insertion.Inserted -> GiveBackendResult.Given(
                itemName = base.hoverName.string,
                amount = amount,
                dropped = insertion.dropped,
            )
        }
    }

    override fun enchantmentMaxLevel(raw: String): Int? {
        val id = identifierOf(raw) ?: return null
        val server = kernel.requireServer()
        val key = ResourceKey.create(Registries.ENCHANTMENT, id)
        return server.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(key)
                .map { it.value().maxLevel }
                .orElse(null)
    }

    override fun applyEnchantment(
        targetId: UUID,
        raw: String,
        level: Int,
    ): EnchantBackendResult {
        val player = player(targetId)
            ?: return EnchantBackendResult.TargetOffline
        val stack = player.mainHandItem
        if (stack.isEmpty) {
            return EnchantBackendResult.NoHeldItem
        }
        val id = identifierOf(raw)
            ?: return EnchantBackendResult.NotSupported
        val server = kernel.requireServer()
        val key = ResourceKey.create(Registries.ENCHANTMENT, id)
        val holder = server.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(key)
                .orElse(null)
            ?: return EnchantBackendResult.NotSupported

        val mutable = ItemEnchantments.Mutable(stack.enchantments)
        val name = holder.value().description.string
        if (level <= 0) {
            mutable.removeIf { enchant -> enchant.unwrapKey() == holder.unwrapKey() }
            stack.set(DataComponents.ENCHANTMENTS, mutable.toImmutable())
            player.inventory.setChanged()
            return EnchantBackendResult.Removed(name)
        }
        if (!holder.value().canEnchant(stack)) {
            return EnchantBackendResult.NotSupported
        }
        mutable.set(holder, level)
        stack.set(DataComponents.ENCHANTMENTS, mutable.toImmutable())
        player.inventory.setChanged()
        return EnchantBackendResult.Applied(name, level)
    }

    override fun editPotion(
        targetId: UUID,
        spec: PotionSpec,
    ): PotionEditBackendResult {
        val player = player(targetId)
            ?: return PotionEditBackendResult.TargetOffline
        val stack = player.mainHandItem
        if (stack.isEmpty) {
            return PotionEditBackendResult.NoHeldItem
        }
        if (!isPotion(stack)) {
            return PotionEditBackendResult.NotPotion
        }
        return when (spec) {
            PotionSpec.Clear -> {
                stack.set(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                player.inventory.setChanged()
                PotionEditBackendResult.Cleared(stack.hoverName.string)
            }

            is PotionSpec.Effect -> {
                val id = identifierOf(spec.effect)
                    ?: return PotionEditBackendResult.UnknownEffect
                val holder = BuiltInRegistries.MOB_EFFECT.get(id).orElse(null)
                    ?: return PotionEditBackendResult.UnknownEffect
                val current = stack.get(DataComponents.POTION_CONTENTS) ?: PotionContents.EMPTY
                val instance = MobEffectInstance(
                    holder,
                    spec.seconds * TICKS_PER_SECOND,
                    spec.amplifier,
                )
                stack.set(
                    DataComponents.POTION_CONTENTS,
                    current.withEffectAdded(instance),
                )
                player.inventory.setChanged()
                PotionEditBackendResult.Edited(
                    itemName = stack.hoverName.string,
                    effectCount = current.allEffects.count() + 1,
                )
            }

            PotionSpec.Apply, PotionSpec.Invalid ->
                PotionEditBackendResult.NotPotion
        }
    }

    override fun applyPotion(targetId: UUID): PotionEditBackendResult {
        val player = player(targetId)
            ?: return PotionEditBackendResult.TargetOffline
        val stack = player.mainHandItem
        if (stack.isEmpty) {
            return PotionEditBackendResult.NoHeldItem
        }
        val contents = stack.get(DataComponents.POTION_CONTENTS)
            ?: return PotionEditBackendResult.NotPotion
        var applied = 0
        contents.allEffects.forEach { effect ->
            player.addEffect(MobEffectInstance(effect))
            applied++
        }
        if (applied == 0) {
            return PotionEditBackendResult.NotPotion
        }
        return PotionEditBackendResult.Applied(
            targetName = player.gameProfile.name,
            effectCount = applied,
        )
    }

    override fun skull(
        actorId: UUID,
        recipientId: UUID,
        ownerName: String,
        spawn: Boolean,
    ): SkullBackendResult {
        val recipient = player(recipientId)
            ?: return SkullBackendResult.TargetOffline
        if (!spawn) {
            val held = recipient.mainHandItem
            if (held.isEmpty) {
                return SkullBackendResult.NoHeldItem
            }
            held.set(DataComponents.PROFILE, ResolvableProfile.createUnresolved(ownerName))
            recipient.inventory.setChanged()
            return SkullBackendResult.Edited(ownerName)
        }
        val head = ItemStack(Items.PLAYER_HEAD)
        head.set(DataComponents.PROFILE, ResolvableProfile.createUnresolved(ownerName))
        return when (val insertion = insert(recipient, listOf(head), dropIfFull = false)) {
            Insertion.Full -> SkullBackendResult.InventoryFull
            is Insertion.Inserted -> SkullBackendResult.Spawned(
                owner = ownerName,
                recipient = recipient.gameProfile.name,
                dropped = insertion.dropped,
            )
        }
    }

    override fun editBook(
        targetId: UUID,
        action: BookAction,
    ): BookBackendResult {
        val player = player(targetId)
            ?: return BookBackendResult.TargetOffline
        val stack = player.mainHandItem
        if (stack.isEmpty) {
            return BookBackendResult.NoHeldItem
        }
        return when (action) {
            BookAction.Convert -> convertBook(player, stack)
            is BookAction.SetAuthor -> setAuthor(player, stack, action.author)
            is BookAction.SetTitle -> setTitle(player, stack, action.title)
        }
    }

    private fun convertBook(
        player: ServerPlayer,
        stack: ItemStack,
    ): BookBackendResult {
        val item = stack.item
        return when {
            item == Items.WRITTEN_BOOK -> {
                val content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT)
                    ?: WrittenBookContent.EMPTY
                val pages = content.pages().map { Filterable.passThrough(it.raw().string) }
                val writable = ItemStack(Items.WRITABLE_BOOK)
                writable.set(
                    DataComponents.WRITABLE_BOOK_CONTENT,
                    WritableBookContent(pages),
                )
                player.setItemInHand(InteractionHand.MAIN_HAND, writable)
                player.inventory.setChanged()
                BookBackendResult.Converted(nowWritten = false)
            }

            item == Items.WRITABLE_BOOK -> {
                val content = stack.get(DataComponents.WRITABLE_BOOK_CONTENT)
                    ?: WritableBookContent.EMPTY
                val pages: List<Filterable<Component>> = content.pages().map { page ->
                    val text: Component = Component.literal(page.raw())
                    Filterable.passThrough(text)
                }
                val written = ItemStack(Items.WRITTEN_BOOK)
                written.set(
                    DataComponents.WRITTEN_BOOK_CONTENT,
                    WrittenBookContent(
                        Filterable.passThrough("Written Book"),
                        player.gameProfile.name,
                        0,
                        pages,
                        true,
                    ),
                )
                player.setItemInHand(InteractionHand.MAIN_HAND, written)
                player.inventory.setChanged()
                BookBackendResult.Converted(nowWritten = true)
            }

            else -> BookBackendResult.NotBook
        }
    }

    private fun setAuthor(
        player: ServerPlayer,
        stack: ItemStack,
        author: String,
    ): BookBackendResult {
        if (stack.item != Items.WRITTEN_BOOK) {
            return BookBackendResult.NotBook
        }
        val content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT)
            ?: return BookBackendResult.NotWrittenBook
        stack.set(
            DataComponents.WRITTEN_BOOK_CONTENT,
            WrittenBookContent(
                content.title(),
                author,
                content.generation(),
                content.pages(),
                content.resolved(),
            ),
        )
        player.inventory.setChanged()
        return BookBackendResult.AuthorSet(author)
    }

    private fun setTitle(
        player: ServerPlayer,
        stack: ItemStack,
        title: String,
    ): BookBackendResult {
        if (stack.item != Items.WRITTEN_BOOK) {
            return BookBackendResult.NotBook
        }
        val content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT)
            ?: return BookBackendResult.NotWrittenBook
        stack.set(
            DataComponents.WRITTEN_BOOK_CONTENT,
            WrittenBookContent(
                Filterable.passThrough(title),
                content.author(),
                content.generation(),
                content.pages(),
                content.resolved(),
            ),
        )
        player.inventory.setChanged()
        return BookBackendResult.TitleSet(title)
    }

    override fun info(
        playerId: UUID?,
        provided: ItemStack?,
        includeDurability: Boolean,
    ): ItemInfo? {
        val stack = when (provided) {
            null -> player(playerId ?: return null)?.mainHandItem ?: return null
            else -> provided
        }
        if (stack.isEmpty) {
            return null
        }
        val item = stack.item
        val registryId = BuiltInRegistries.ITEM.getKey(item).toString()
        val aliases = item.builtInRegistryHolder()
                .tags()
                .limit(MAX_TAGS.toLong())
                .map { "#" + it.location() }
                .toList()
        val componentIds = stack.components.keySet()
                .map { type ->
                    BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type)?.toString()
                        ?: type.toString()
                }
                .sorted()
        return ItemInfo(
            displayName = stack.hoverName.string,
            registryId = registryId,
            aliases = aliases,
            components = componentIds.take(MAX_COMPONENTS),
            componentCount = componentIds.size,
            maxStackSize = stack.maxStackSize,
            durability = when {
                includeDurability && stack.isDamageableItem ->
                    DurabilityView(stack.damageValue, stack.maxDamage)

                else -> null
            },
        )
    }

    override fun recipes(
        playerId: UUID,
        item: ItemStack,
    ): List<RecipeView> {
        val player = player(playerId)
            ?: return emptyList()
        val target = item.item
        val context = SlotDisplayContext.fromLevel(player.level())
        val manager = kernel.requireServer().recipeManager
        return manager.recipes.mapNotNull { holder ->
            val recipe = holder.value()
            val outputs = runCatching {
                recipe.display().flatMap { display ->
                    display.result().resolveForStacks(context)
                }
            }.getOrDefault(emptyList())
            val match = outputs.firstOrNull { it.item == target }
                ?: return@mapNotNull null
            val ingredients = runCatching {
                recipe.placementInfo().ingredients().map { ingredient ->
                    // FIXME: 'fun items(): Stream<Holder<Item>>' is deprecated. Deprecated in Java.
                    ingredient.items().findFirst()
                            .map { BuiltInRegistries.ITEM.getKey(it.value()).toString() }
                            .orElse("")
                }
            }.getOrDefault(emptyList())
            RecipeView(
                id = holder.id().identifier().toString(),
                typeId = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.type)?.toString() ?: "",
                result = match.hoverName.string,
                resultCount = match.count,
                // FIXME: Java type mismatch: inferred type is 'List<String?>', but 'List<String>'
                //  was expected.
                ingredients = ingredients,
            )
        }
    }

    /** Expands one base stack into a list of max-stack-sized stacks. */
    private fun expand(base: ItemStack, amount: Int): List<ItemStack> {
        if (amount <= 0) {
            return emptyList()
        }
        val maxStack = base.maxStackSize.coerceAtLeast(1)
        return buildList {
            var remaining = amount
            while (remaining > 0) {
                val chunk = minOf(remaining, maxStack)
                add(base.copyWithCount(chunk))
                remaining -= chunk
            }
        }
    }

    private sealed interface Insertion {

        data class Inserted(val dropped: Boolean) : Insertion

        data object Full : Insertion

    }

    /**
     * All-or-nothing insertion over the 36 main slots: the working copy is only committed when the
     * whole batch fits, unless [dropIfFull] allows dropping the remainder.
     */
    private fun insert(
        player: ServerPlayer,
        incoming: List<ItemStack>,
        dropIfFull: Boolean,
    ): Insertion {
        if (incoming.isEmpty()) {
            return Insertion.Inserted(dropped = false)
        }
        val working = player.inventory.nonEquipmentItems
                .map { it.copy() }
                .toMutableList()
        val leftover = InventoryInsertion.simulate(working, incoming)
        if (leftover.isNotEmpty() && !dropIfFull) {
            return Insertion.Full
        }
        player.inventory.nonEquipmentItems.forEachIndexed { index, _ ->
            player.inventory.setItem(index, working[index])
        }
        player.inventory.setChanged()
        leftover.forEach { player.drop(it, false) }
        return Insertion.Inserted(dropped = leftover.isNotEmpty())
    }

    private fun isPotion(stack: ItemStack): Boolean =
        stack.get(DataComponents.POTION_CONTENTS) != null
                || stack.item == Items.POTION
                || stack.item == Items.SPLASH_POTION
                || stack.item == Items.LINGERING_POTION

    private fun player(playerId: UUID): ServerPlayer? =
        PlayerResolver.onlineById(kernel.requireServer(), playerId)

    private fun identifierOf(raw: String): Identifier? {
        val trimmed = raw.trim().lowercase(Locale.ROOT)
        if (trimmed.isEmpty()) {
            return null
        }
        return when {
            trimmed.contains(':') -> Identifier.tryParse(trimmed)
            else -> Identifier.tryBuild("minecraft", trimmed)
        }
    }

    private companion object {

        private const val TICKS_PER_SECOND: Int = 20
        private const val MAX_TAGS: Int = 8
        private const val MAX_COMPONENTS: Int = 12

    }

}
