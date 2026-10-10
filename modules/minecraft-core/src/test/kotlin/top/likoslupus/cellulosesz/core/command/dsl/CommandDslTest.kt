package top.likoslupus.cellulosesz.core.command.dsl

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.permission.PermissionSpec
import top.likoslupus.cellulosesz.core.permission.VanillaPermissionFallback

class CommandDslTest {

    private val permission = PermissionSpec(
        "cellulosesz.command.home",
        VanillaPermissionFallback.ALLOW_ALL,
        "test",
    )

    @Test
    fun `a command declaration builds an immutable nested tree`() {
        val definition = command(
            name = "home",
            category = CommandCategory.MOVEMENT,
            permission = permission,
            documentation = "movement/home",
        ) {
            executesPlayer { 1 }
            argument("name", word()) { name ->
                executesPlayer { if (get(name).isEmpty()) 0 else 1 }
            }
        }

        assertEquals("home", definition.name)
        assertEquals("movement/home", definition.documentation.value)
        assertTrue(definition.playerOnly)
        assertNotNull(definition.root.execution)

        val child = definition.root.children.single()
        assertTrue(child is ArgumentNodeSpec<*>)
        assertEquals("name", child.name)
        assertNotNull(child.execution)
        assertTrue(child.children.isEmpty())
    }

    @Test
    fun `aliases and source access are preserved`() {
        val definition = command(
            name = "sudo",
            category = CommandCategory.ADMINISTRATION,
            permission = permission,
            documentation = "administration/sudo",
            aliases = setOf("runas"),
            sourceAccess = SourceAccess.NON_PLAYER,
        ) {
            argument("player", word()) { target ->
                argument("command", greedyString()) { input ->
                    executes { if (get(target).isEmpty() || get(input).isEmpty()) 0 else 1 }
                }
            }
        }

        assertEquals(setOf("runas"), definition.aliases)
        assertEquals(SourceAccess.NON_PLAYER, definition.sourceAccess)
    }

    @Test
    fun `documentation ids must be relative lowercase paths`() {
        assertThrows<IllegalArgumentException> { CommandDocumentId.of("Movement/Home") }
        assertThrows<IllegalArgumentException> { CommandDocumentId.of("../home") }
        assertEquals("core/language", CommandDocumentId.of("core/language").value)
    }

    @Test
    fun `the dsl rejects a dangling literal without an executor`() {
        assertThrows<IllegalArgumentException> {
            command(
                name = "broken",
                category = CommandCategory.UTILITY,
                permission = permission,
                documentation = "utility/broken",
            ) {
                literal("empty") { }
            }
        }
    }

    @Test
    fun `the dsl rejects a player executor on a non-player path`() {
        assertThrows<IllegalArgumentException> {
            command(
                name = "bad",
                category = CommandCategory.ADMINISTRATION,
                permission = permission,
                documentation = "administration/bad",
                sourceAccess = SourceAccess.NON_PLAYER,
            ) {
                executesPlayer { 1 }
            }
        }
    }

    @Test
    fun `the dsl rejects a greedy argument that has children`() {
        assertThrows<IllegalArgumentException> {
            command(
                name = "bad2",
                category = CommandCategory.UTILITY,
                permission = permission,
                documentation = "utility/bad2",
            ) {
                argument("tail", greedyString()) { _ ->
                    literal("later") { executes { 1 } }
                }
            }
        }
    }

    @Test
    fun `the dsl rejects a command with no executor`() {
        assertThrows<IllegalArgumentException> {
            command(
                name = "bad3",
                category = CommandCategory.UTILITY,
                permission = permission,
                documentation = "utility/bad3",
            ) {
                literal("x") { }
            }
        }
    }

    @Test
    fun `sibling arguments get distinct keys of the declared name`() {
        val definition = command(
            name = "path",
            category = CommandCategory.UTILITY,
            permission = permission,
            documentation = "utility/path",
        ) {
            argument("a", word()) { _ ->
                executes { 1 }
            }
            argument("b", integer(1, 5)) { _ ->
                executes { 2 }
            }
        }
        val args = definition.root.children.filterIsInstance<ArgumentNodeSpec<*>>()
        assertEquals(listOf("a", "b"), args.map { it.name })
        assertTrue(args[0].key !== args[1].key)
    }

}
