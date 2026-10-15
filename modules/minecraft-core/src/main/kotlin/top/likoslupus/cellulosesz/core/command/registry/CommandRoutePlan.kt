package top.likoslupus.cellulosesz.core.command.registry

import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition

/**
 * A single literal path CellulosesZ wants to own. A [CommandDefinition] can produce several routes
 * (its namespaced form, its canonical name and each alias); none of them is registered until the
 * planner has produced the plan and the registry has checked the live dispatcher.
 */
internal data class CommandRoute(
    val literal: String,
    val kind: CommandRouteKind,
    val definition: CommandDefinition,
)

/**
 * Immutable, startup-bound routing policy. It is captured once per registration pass and never
 * reads a live configuration snapshot, so a reload never half-applies a command tree.
 *
 * [suppressedCanonicals] removes a canonical short name without touching its namespaced route (used
 * for `/me` in preserve-vanilla mode). [ADMIN_ROOT] and [NAMESPACED] routes are not configurable.
 */
public data class CommandRoutingPolicy(
    public val shortNamesEnabled: Boolean = true,
    public val aliasesEnabled: Boolean = true,
    public val suppressedCanonicals: Set<String> = emptySet(),
)

/**
 * Derives and validates namespaced route literals. The namespace is a fixed prefix plus the DSL's
 * own `[a-z0-9_-]+` local part; a literal is never accepted merely because it contains a colon.
 */
internal object CommandRouteName {

    /** The product namespace, which is also the admin root literal. */
    const val NAMESPACE: String = "cellulosesz"

    private val LOCAL_PATTERN: Regex = Regex("[a-z0-9_-]+")

    fun isAdminRoot(literal: String): Boolean =
        literal == NAMESPACE

    fun namespaced(local: String): String =
        "$NAMESPACE:$local"

    fun isNamespaced(literal: String): Boolean =
        literal.startsWith("$NAMESPACE:")

    /** Returns a problem description, or null when [literal] is a valid namespaced route. */
    fun validateNamespaced(literal: String): String? {
        if (!literal.startsWith("$NAMESPACE:")) {
            return "namespaced route '$literal' must start with '$NAMESPACE:'"
        }
        val local = literal.substring(NAMESPACE.length + 1)
        if (local.isEmpty()) {
            return "namespaced route '$literal' has an empty local part"
        }
        if (':' in local) {
            return "namespaced route '$literal' must contain a single ':'"
        }
        if (!LOCAL_PATTERN.matches(local)) {
            return "namespaced route local part '$local' must match ${LOCAL_PATTERN.pattern}"
        }
        return null
    }

}

/**
 * True when a route must always be available: the admin root and the namespaced fallback. A conflict
 * on one of these is an error, not an ordinary short-name skip.
 */
internal fun CommandRouteKind.requiredRoute(): Boolean =
    this == CommandRouteKind.ADMIN_ROOT || this == CommandRouteKind.NAMESPACED

/** The status recorded when a route literal is already present in the dispatcher. */
internal fun statusForOccupiedRoute(kind: CommandRouteKind): CommandRouteStatus =
    when {
        kind.requiredRoute() -> CommandRouteStatus.INSTALL_MISMATCH
        else -> CommandRouteStatus.SKIPPED_ALREADY_PRESENT
    }

/**
 * Pure route planning. It has no reference to a dispatcher and no side effects, so the plan can be
 * unit-tested without Minecraft and compared for reproducibility. Order is fixed: admin root, then
 * every definition's namespaced route, then canonical short names, then sorted aliases.
 */
internal object CommandRoutePlanner {

    fun plan(
        definitions: List<CommandDefinition>,
        policy: CommandRoutingPolicy,
    ): List<CommandRoute> {
        val routes = mutableListOf<CommandRoute>()

        definitions.firstOrNull { CommandRouteName.isAdminRoot(it.name) }?.let { root ->
            routes += CommandRoute(
                root.name,
                CommandRouteKind.ADMIN_ROOT,
                root
            )
        }

        val featureDefinitions = definitions.filterNot {
            CommandRouteName.isAdminRoot(it.name)
        }

        featureDefinitions.forEach { definition ->
            routes += CommandRoute(
                literal = CommandRouteName.namespaced(definition.name),
                kind = CommandRouteKind.NAMESPACED,
                definition = definition,
            )
        }

        if (policy.shortNamesEnabled) {
            featureDefinitions
                    .filterNot { it.name in policy.suppressedCanonicals }
                    .forEach { definition ->
                        routes += CommandRoute(
                            definition.name,
                            CommandRouteKind.CANONICAL,
                            definition
                        )
                    }
        }

        if (policy.aliasesEnabled) {
            featureDefinitions
                    .flatMap { definition ->
                        definition.aliases
                                .sorted()
                                .map { alias ->
                                    CommandRoute(
                                        alias,
                                        CommandRouteKind.ALIAS,
                                        definition
                                    )
                                }
                    }
                    .forEach(routes::add)
        }

        return routes
    }

}
