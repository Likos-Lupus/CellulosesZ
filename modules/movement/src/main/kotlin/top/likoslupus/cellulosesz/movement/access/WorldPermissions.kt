package top.likoslupus.cellulosesz.movement.access

import top.likoslupus.cellulosesz.core.permission.PermissionSpec
import top.likoslupus.cellulosesz.core.permission.VanillaPermissionFallback
import java.util.*

/**
 * The dynamic movement permission nodes: per-world teleport access, per-world home access and
 * per-warp access. Every node is deny-by-default because the owning config toggle already decides
 * whether the check runs at all, so an undefined node must mean "not granted".
 */
internal object WorldPermissions {

    private const val WORLD_PREFIX: String = "cellulosesz.world."
    private const val HOME_WORLD_PREFIX: String = "cellulosesz.world.home."
    private const val WARP_PREFIX: String = "cellulosesz.warp."

    private const val WARP_SAFE_CHARS: String =
        "abcdefghijklmnopqrstuvwxyz0123456789._-"

    private val ALIAS_SAFE: Regex = Regex("[a-z0-9._-]+")

    /** `cellulosesz.world.<dimension>` — may this player be teleported into [dimension]? */
    fun teleportNode(dimension: String): String =
        WORLD_PREFIX + normalizeDimension(dimension)

    /** `cellulosesz.world.home.<dimension>` — may this player use a home in [dimension]? */
    fun homeNode(dimension: String): String =
        HOME_WORLD_PREFIX + normalizeDimension(dimension)

    /** `cellulosesz.warp.<name>` — may this player use the warp [name]? */
    fun warpNode(name: String): String =
        WARP_PREFIX + normalizeWarpName(name)

    /** Lowercases and maps every character outside `[a-z0-9._-]` to `_`, so the node is stable. */
    fun normalizeWarpName(name: String): String =
        buildString(name.length) {
            name.lowercase(Locale.ROOT).forEach { character ->
                append(if (character in WARP_SAFE_CHARS) character else '_')
            }
        }

    /**
     * Permission nodes are identifier-safe on every loader: the dimension's `namespace:path`
     * separator becomes a dot, so `minecraft:the_nether` reads `minecraft.the_nether`.
     */
    fun normalizeDimension(dimension: String): String =
        dimension.lowercase(Locale.ROOT).replace(':', '.')

    /** True when [value] is a usable alias key. */
    fun isAliasKey(value: String): Boolean =
        ALIAS_SAFE.matches(value)

    fun spec(node: String, description: String): PermissionSpec =
        PermissionSpec(
            node = node,
            fallback = VanillaPermissionFallback.DENY_ALL,
            description = description,
        )

}
