package top.likoslupus.cellulosesz.movement.home

import top.likoslupus.cellulosesz.movement.config.HomeSettings

/**
 * The pure home-allowance rule. The highest matching permission-scoped entry wins; when none of the
 * configured permissions is held, the plain [HomeSettings.maxPerPlayer] default applies.
 */
internal object HomeLimits {

    fun maxFor(
        settings: HomeSettings,
        hasPermission: (String) -> Boolean,
    ): Int {
        val best = settings.limits
                .filter { hasPermission(it.permission) }
                .maxOfOrNull { it.max }
        return (best ?: settings.maxPerPlayer).coerceAtLeast(0)
    }

}
