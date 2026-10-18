package top.likoslupus.cellulosesz.utility.itemtools

import net.minecraft.world.item.ItemStack
import java.util.*

internal sealed interface GiveBackendResult {

    data class Given(
        val itemName: String,
        val amount: Int,
        val dropped: Boolean,
    ) : GiveBackendResult

    data object TargetOffline : GiveBackendResult

    data object InventoryFull : GiveBackendResult

}

internal sealed interface EnchantBackendResult {

    data class Applied(
        val enchantmentName: String,
        val level: Int,
    ) : EnchantBackendResult

    data class Removed(val enchantmentName: String) : EnchantBackendResult

    data object NoHeldItem : EnchantBackendResult

    data object TargetOffline : EnchantBackendResult

    /** The held item cannot carry this enchantment. */
    data object NotSupported : EnchantBackendResult

}

internal sealed interface PotionEditBackendResult {

    data class Cleared(val itemName: String) : PotionEditBackendResult

    data class Edited(
        val itemName: String,
        val effectCount: Int,
    ) : PotionEditBackendResult

    data class Applied(
        val targetName: String,
        val effectCount: Int,
    ) : PotionEditBackendResult

    data object NoHeldItem : PotionEditBackendResult

    data object TargetOffline : PotionEditBackendResult

    data object UnknownEffect : PotionEditBackendResult

    /** The held item is not a potion, splash potion or lingering potion. */
    data object NotPotion : PotionEditBackendResult

}

internal sealed interface SkullBackendResult {

    data class Spawned(
        val owner: String,
        val recipient: String,
        val dropped: Boolean,
    ) : SkullBackendResult

    data class Edited(val owner: String) : SkullBackendResult

    data object NoHeldItem : SkullBackendResult

    data object TargetOffline : SkullBackendResult

    data object InventoryFull : SkullBackendResult

}

/** What `/book` should do with the held book. */
internal sealed interface BookAction {

    /** Convert a written book to a writable one, or the other way around. */
    data object Convert : BookAction

    data class SetAuthor(val author: String) : BookAction

    data class SetTitle(val title: String) : BookAction

}

internal sealed interface BookBackendResult {

    data class Converted(val nowWritten: Boolean) : BookBackendResult

    data class AuthorSet(val author: String) : BookBackendResult

    data class TitleSet(val title: String) : BookBackendResult

    data object NoHeldItem : BookBackendResult

    data object NotBook : BookBackendResult

    data object TargetOffline : BookBackendResult

    /** Author/title only exist on a written book. */
    data object NotWrittenBook : BookBackendResult

}

/** The current and maximum damage of a held, damageable item. */
internal data class DurabilityView(
    val damage: Int,
    val maxDamage: Int,
)

/** A registry-backed summary of one item, produced for `/itemdb`. */
internal data class ItemInfo(
    val displayName: String,
    val registryId: String,
    val aliases: List<String>,
    val components: List<String>,
    val componentCount: Int,
    val maxStackSize: Int,
    val durability: DurabilityView?,
)

/** One recipe whose result matches the queried item, produced for `/recipe`. */
internal data class RecipeView(
    val id: String,
    val typeId: String,
    val result: String,
    val resultCount: Int,
    val ingredients: List<String>,
)

/**
 * All item/inventory mutation for the Phase 5 item tools. Implementations are server-thread confined;
 * the service hops onto the server thread through its runner before calling them.
 */
internal interface ItemToolsBackend {

    /**
     * Inserts [amount] of [base] into the target's inventory, splitting into max-stack-sized stacks.
     * [dropIfFull] drops the remainder at the target's feet instead of failing.
     */
    fun give(
        targetId: UUID,
        base: ItemStack,
        amount: Int,
        dropIfFull: Boolean,
    ): GiveBackendResult

    /** The vanilla maximum level of an enchantment id, or null when it is unknown. */
    fun enchantmentMaxLevel(raw: String): Int?

    /** Applies or removes an enchantment on the target's held item. Level 0 removes it. */
    fun applyEnchantment(
        targetId: UUID,
        raw: String,
        level: Int,
    ): EnchantBackendResult

    /** Applies a `/potion clear` or `/potion effect:...` edit to the target's held item. */
    fun editPotion(
        targetId: UUID,
        spec: PotionSpec,
    ): PotionEditBackendResult

    /** Applies the target's held potion effects to the target. */
    fun applyPotion(targetId: UUID): PotionEditBackendResult

    /** Writes the owner profile onto the held head, or spawns a fresh head for the recipient. */
    fun skull(
        actorId: UUID,
        recipientId: UUID,
        ownerName: String,
        spawn: Boolean,
    ): SkullBackendResult

    /** Applies a `/book` action to the target's held book. */
    fun editBook(
        targetId: UUID,
        action: BookAction,
    ): BookBackendResult

    /** A registry/component summary of [provided], or of the player's held item when it is null. */
    fun info(
        playerId: UUID?,
        provided: ItemStack?,
        includeDurability: Boolean,
    ): ItemInfo?

    /** Recipes whose result matches [item], with their ingredient list. */
    fun recipes(
        playerId: UUID,
        item: ItemStack,
    ): List<RecipeView>

}
