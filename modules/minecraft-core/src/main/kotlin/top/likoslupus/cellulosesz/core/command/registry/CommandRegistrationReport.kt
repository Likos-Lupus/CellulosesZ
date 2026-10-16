package top.likoslupus.cellulosesz.core.command.registry

/** The category of a single command path CellulosesZ attempted to own. */
public enum class CommandRouteKind {

    ADMIN_ROOT,
    NAMESPACED,

    /** A protected single-segment fallback for a vanilla conflict, e.g. `/cztp`. */
    CZ_FALLBACK,

    /** A vanilla-conflicting literal CellulosesZ was allowed to take over, e.g. `/tp`. */
    VANILLA_OVERRIDE,

    CANONICAL,
    ALIAS,

}

/** The outcome of one route attempt within a registration generation. */
public enum class CommandRouteStatus {

    /** The route was free and CellulosesZ installed its own node. */
    INSTALLED,

    /** The route was disabled by configuration (never requested from the dispatcher). */
    SKIPPED_DISABLED,

    /** A short name or alias was already present; CellulosesZ did not merge into it. */
    SKIPPED_ALREADY_PRESENT,

    /** A `cz` fallback literal was already owned by vanilla/another mod; CellulosesZ did not merge. */
    FALLBACK_COLLISION,

    /** A configured vanilla takeover could not be confirmed (the literal was already present). */
    TAKEOVER_NOT_CONFIRMED,

    /** A required route was occupied, or the installed node was not the one we built. */
    INSTALL_MISMATCH,

}

/**
 * A stable, Brigadier-free projection of one route attempt. It carries only strings and enums, so a
 * report can be held by the application and rendered to admins without leaking a live node.
 */
public class CommandRouteResult(
    public val literal: String,
    public val kind: CommandRouteKind,
    public val commandName: String,
    public val status: CommandRouteStatus,
    public val fallback: String?,
)

/**
 * The result of one registration pass, keyed to a monotonic dispatcher generation. The composition
 * root keeps only the latest report; it never holds a node, a dispatcher or a player.
 */
public class CommandRegistrationReport(
    public val generation: Int,
    public val results: List<CommandRouteResult>,
) {

    /** Every literal CellulosesZ actually installed this generation. */
    public val installedLiterals: Set<String> =
        results.filter { it.status == CommandRouteStatus.INSTALLED }
                .mapTo(LinkedHashSet()) { it.literal }

    /** Skipped short names/aliases, fallback collisions and any required-route mismatch. */
    public val conflicts: List<CommandRouteResult> =
        results.filter {
            it.status == CommandRouteStatus.SKIPPED_ALREADY_PRESENT ||
                    it.status == CommandRouteStatus.FALLBACK_COLLISION ||
                    it.status == CommandRouteStatus.TAKEOVER_NOT_CONFIRMED ||
                    it.status == CommandRouteStatus.INSTALL_MISMATCH
        }

    public val installedCount: Int
        get() = results.count { it.status == CommandRouteStatus.INSTALLED }

    public val skippedCount: Int
        get() = results.size - installedCount

    public fun resultFor(literal: String): CommandRouteResult? =
        results.firstOrNull { it.literal == literal }

    /** True only when CellulosesZ installed this literal in this generation. */
    public fun owns(literal: String): Boolean =
        resultFor(literal)?.status == CommandRouteStatus.INSTALLED

}
