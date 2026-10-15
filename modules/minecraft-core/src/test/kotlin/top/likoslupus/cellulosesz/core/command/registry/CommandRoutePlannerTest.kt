package top.likoslupus.cellulosesz.core.command.registry

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.permission.PermissionSpec
import top.likoslupus.cellulosesz.core.permission.VanillaPermissionFallback

class CommandRoutePlannerTest {

    private fun spec(name: String): PermissionSpec =
        PermissionSpec(
            "cellulosesz.command.$name",
            VanillaPermissionFallback.ALLOW_ALL,
            name,
        )

    private fun definition(
        name: String,
        aliases: Set<String> = emptySet(),
    ): CommandDefinition =
        command(
            name = name,
            category = CommandCategory.UTILITY,
            permission = spec(name),
            documentation = "core/$name",
            aliases = aliases,
        ) {
            executes { 1 }
        }

    private val root = definition("cellulosesz")
    private val home = definition("home", aliases = setOf("homes"))

    @Test
    fun `the admin root is planned first and is never namespaced`() {
        val plan = CommandRoutePlanner.plan(
            listOf(home, root),
            CommandRoutingPolicy()
        )

        assertEquals(CommandRouteKind.ADMIN_ROOT, plan.first().kind)
        assertEquals("cellulosesz", plan.first().literal)
        assertFalse(plan.any { it.literal == "cellulosesz:cellulosesz" })
    }

    @Test
    fun `every feature definition gets a mandatory namespaced route`() {
        val plan = CommandRoutePlanner.plan(
            listOf(root, home),
            CommandRoutingPolicy()
        )

        assertTrue(
            plan.any {
                it.literal == "cellulosesz:home" && it.kind == CommandRouteKind.NAMESPACED
            }
        )
    }

    @Test
    fun `canonical names and sorted aliases are planned`() {
        val plan = CommandRoutePlanner.plan(
            listOf(
                root,
                definition("reply", aliases = setOf("r", "answer"))
            ),
            CommandRoutingPolicy(),
        )

        assertEquals(
            listOf("answer", "r"),
            plan.filter { it.kind == CommandRouteKind.ALIAS }.map { it.literal },
        )
        assertTrue(plan.any { it.literal == "reply" && it.kind == CommandRouteKind.CANONICAL })
    }

    @Test
    fun `short names can be disabled without losing namespaced routes`() {
        val plan = CommandRoutePlanner.plan(
            listOf(root, home),
            CommandRoutingPolicy(
                shortNamesEnabled = false,
                aliasesEnabled = false
            ),
        )

        assertFalse(plan.any { it.kind == CommandRouteKind.CANONICAL })
        assertFalse(plan.any { it.kind == CommandRouteKind.ALIAS })
        assertTrue(plan.any { it.literal == "cellulosesz:home" })
    }

    @Test
    fun `a suppressed canonical keeps its namespaced and alias routes`() {
        val me = definition("me", aliases = setOf("czme"))

        val plan = CommandRoutePlanner.plan(
            listOf(root, me),
            CommandRoutingPolicy(suppressedCanonicals = setOf("me")),
        )

        assertFalse(plan.any { it.literal == "me" })
        assertTrue(plan.any { it.literal == "cellulosesz:me" })
        assertTrue(plan.any { it.literal == "czme" })
    }

    @Test
    fun `namespaced names only accept the fixed prefix and a lowercase local part`() {
        assertNull(CommandRouteName.validateNamespaced("cellulosesz:home"))
        assertNull(CommandRouteName.validateNamespaced("cellulosesz:kit_v2"))
        assertNull(CommandRouteName.validateNamespaced("cellulosesz:a-b"))

        assertNotNull(CommandRouteName.validateNamespaced("cellulosesz:"))
        assertNotNull(CommandRouteName.validateNamespaced("cellulosesz:Home"))
        assertNotNull(CommandRouteName.validateNamespaced("cellulosesz:a:b"))
        assertNotNull(CommandRouteName.validateNamespaced("other:home"))
        assertNotNull(CommandRouteName.validateNamespaced("home"))
    }

    @Test
    fun `occupied route status distinguishes required from optional routes`() {
        assertEquals(
            CommandRouteStatus.INSTALL_MISMATCH,
            statusForOccupiedRoute(CommandRouteKind.ADMIN_ROOT),
        )
        assertEquals(
            CommandRouteStatus.INSTALL_MISMATCH,
            statusForOccupiedRoute(CommandRouteKind.NAMESPACED),
        )
        assertEquals(
            CommandRouteStatus.SKIPPED_ALREADY_PRESENT,
            statusForOccupiedRoute(CommandRouteKind.CANONICAL),
        )
        assertEquals(
            CommandRouteStatus.SKIPPED_ALREADY_PRESENT,
            statusForOccupiedRoute(CommandRouteKind.ALIAS),
        )
    }

}
