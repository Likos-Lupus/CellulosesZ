package top.likoslupus.cellulosesz.core.permission

import net.minecraft.commands.CommandSourceStack

/**
 * The permission backends a node may be resolved through. The standard loader permission API stays
 * the sole authority for effective authorization; LuckPerms native values are diagnostics only.
 */
public enum class PermissionBackendStatus {

    VANILLA,
    PLATFORM,
    LUCKPERMS_NOT_READY,
    LUCKPERMS_ACTIVE,
    LUCKPERMS_UNAVAILABLE,

}

/** LuckPerms' raw opinion for a node in the queried context. [UNKNOWN] means it could not be read. */
public enum class PermissionOpinion {

    ALLOW,
    DENY,
    UNDEFINED,
    UNKNOWN,

}

/**
 * A read-only diagnostic snapshot for one permission node. It records the platform's effective
 * decision (the only thing that authorizes anything), the vanilla fallback, and, when a native
 * LuckPerms provider is present, its raw tri-state opinion for the current context. It never changes
 * any authorization result.
 *
 * This is an internal composition-root seam, not part of the public `:modules:api` contract; it uses
 * the live [CommandSourceStack] so the online player context is accurate.
 */
public data class PermissionInspection(
    public val node: String,
    public val effectiveAllowed: Boolean,
    public val vanillaDefaultAllowed: Boolean,
    public val platformBackend: String,
    public val luckPermsOpinion: PermissionOpinion,
    public val contextSummary: Map<String, Set<String>>,
    public val status: PermissionBackendStatus,
)

/** Optional, loader-supplied permission diagnostics. Present only when a native provider is loaded. */
public interface PermissionInspector {

    public fun inspect(
        source: CommandSourceStack,
        spec: PermissionSpec,
    ): PermissionInspection

}
