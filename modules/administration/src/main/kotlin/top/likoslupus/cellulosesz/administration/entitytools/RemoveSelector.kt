package top.likoslupus.cellulosesz.administration.entitytools

/**
 * A named entity category accepted by `/remove`. The mapping onto concrete Minecraft classes lives
 * in the Minecraft-side service so this parser stays pure and unit-testable.
 */
internal enum class EntitySelector(val token: String) {

    ALL("all"),
    TAMED("tamed"),
    NAMED("named"),
    DROPS("drops"),
    ARROWS("arrows"),
    BOATS("boats"),
    MINECARTS("minecarts"),
    XP("xp"),
    PAINTINGS("paintings"),
    ITEM_FRAMES("itemframes"),
    ENDER_CRYSTALS("endercrystals"),
    MONSTERS("monsters"),
    ANIMALS("animals"),
    AMBIENT("ambient"),
    MOBS("mobs");

    companion object {

        private val BY_NAME: Map<String, EntitySelector> = entries.associateBy { it.token }

        val tokens: List<String> = entries.map { it.token }

        fun parse(raw: String): EntitySelector? =
            BY_NAME[raw.lowercase()]

    }

}

/** What `/remove` should target: a named category or one concrete entity type id. */
internal sealed interface RemoveTarget {

    data class Category(val selector: EntitySelector) : RemoveTarget

    data class EntityType(val id: String) : RemoveTarget

}

internal object RemoveSelectorParser {

    private val ENTITY_TYPE: Regex = Regex("[a-z0-9_.-]+:[a-z0-9_./-]+")
    private val BARE_ENTITY_TYPE: Regex = Regex("[a-z0-9_./-]+")

    /**
     * Parses one `/remove` selector. Named categories win over entity-type ids, so `all` is always
     * the category. A bare id (`zombie`) is namespaced to `minecraft:zombie`.
     */
    fun parse(raw: String): RemoveTarget? {
        val text = raw.trim().lowercase()
        if (text.isEmpty()) {
            return null
        }
        EntitySelector.parse(text)?.let { return RemoveTarget.Category(it) }
        return when {
            ENTITY_TYPE.matches(text) -> RemoveTarget.EntityType(text)
            BARE_ENTITY_TYPE.matches(text) -> RemoveTarget.EntityType("minecraft:$text")
            else -> null
        }
    }

}
