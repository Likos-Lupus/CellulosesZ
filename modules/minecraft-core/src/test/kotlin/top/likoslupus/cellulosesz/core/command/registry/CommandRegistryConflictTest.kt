package top.likoslupus.cellulosesz.core.command.registry

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.HolderLookup
import net.minecraft.world.flag.FeatureFlagSet
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.permission.PermissionSpec
import top.likoslupus.cellulosesz.core.permission.VanillaFallbackPermissionBridge
import top.likoslupus.cellulosesz.core.permission.VanillaPermissionFallback
import java.util.stream.Stream

/**
 * Exercises the real Brigadier dispatcher with pure-Brigadier root nodes, so the no-accidental-merge
 * contract is proven without bootstrapping Minecraft (`net.minecraft.commands.Commands` would require
 * a full registry bootstrap, which unit tests must not do).
 */
class CommandRegistryConflictTest {

    private val permissions = PermissionService(VanillaFallbackPermissionBridge)

    private val buildContext: CommandBuildContext =
        CommandBuildContext.simple(
            HolderLookup.Provider.create(Stream.empty()),
            FeatureFlagSet.of(),
        )

    private val pureBuilder = CommandRootBuilder { _, literal, _, _ ->
        LiteralArgumentBuilder
                .literal<CommandSourceStack>(literal)
                .executes { 1 }.build()
    }

    private fun definition(name: String): CommandDefinition =
        command(
            name = name,
            category = CommandCategory.UTILITY,
            permission = PermissionSpec(
                "cellulosesz.command.$name",
                VanillaPermissionFallback.ALLOW_ALL,
                name,
            ),
            documentation = "core/$name",
        ) {
            executes { 1 }
        }

    private fun foreign(literal: String, result: Int) =
        LiteralArgumentBuilder
                .literal<CommandSourceStack>(literal)
                .executes { result }
                .build()

    @Test
    fun `an occupied short name is skipped and the foreign node is never merged`() {
        val dispatcher = CommandDispatcher<CommandSourceStack>()
        val foreignHome = foreign("home", 7)
        dispatcher.root.addChild(foreignHome)

        val registry = CommandRegistry(
            listOf(definition("home")),
            pureBuilder
        )
        val report = registry.register(
            dispatcher,
            buildContext,
            permissions
        )

        assertEquals(
            CommandRouteStatus.SKIPPED_ALREADY_PRESENT,
            report.resultFor("home")?.status
        )
        assertEquals(
            CommandRouteStatus.INSTALLED,
            report.resultFor("cellulosesz:home")?.status
        )
        assertEquals(
            "cellulosesz:home",
            report.resultFor("home")?.fallback
        )
        assertSame(
            foreignHome,
            dispatcher.root.getChild("home")
        )
        assertNotNull(dispatcher.root.getChild("cellulosesz:home"))
    }

    @Test
    fun `an occupied namespaced route is a mismatch and is never overwritten`() {
        val dispatcher = CommandDispatcher<CommandSourceStack>()
        val foreignNamespaced = foreign("cellulosesz:home", 11)
        dispatcher.root.addChild(foreignNamespaced)

        val registry = CommandRegistry(
            listOf(definition("home")),
            pureBuilder
        )
        val report = registry.register(dispatcher, buildContext, permissions)

        assertEquals(
            CommandRouteStatus.INSTALL_MISMATCH,
            report.resultFor("cellulosesz:home")?.status
        )
        assertSame(
            foreignNamespaced,
            dispatcher.root.getChild("cellulosesz:home")
        )
    }

    @Test
    fun `an occupied admin root is a mismatch and is never overwritten`() {
        val dispatcher = CommandDispatcher<CommandSourceStack>()
        val foreignRoot = foreign("cellulosesz", 3)
        dispatcher.root.addChild(foreignRoot)

        val registry = CommandRegistry(
            listOf(definition("cellulosesz")),
            pureBuilder
        )
        val report = registry.register(dispatcher, buildContext, permissions)

        assertEquals(
            CommandRouteStatus.INSTALL_MISMATCH,
            report.resultFor("cellulosesz")?.status
        )
        assertSame(
            foreignRoot,
            dispatcher.root.getChild("cellulosesz")
        )
    }

    @Test
    fun `re-registering the same dispatcher is idempotent across generations`() {
        val dispatcher = CommandDispatcher<CommandSourceStack>()
        val registry = CommandRegistry(
            listOf(definition("home")),
            pureBuilder
        )

        val first = registry.register(dispatcher, buildContext, permissions)
        val firstHome = dispatcher.root.getChild("home")
        val second = registry.register(dispatcher, buildContext, permissions)

        assertEquals(1, first.generation)
        assertEquals(2, second.generation)
        assertEquals(
            CommandRouteStatus.INSTALLED,
            second.resultFor("home")?.status
        )
        assertEquals(
            CommandRouteStatus.INSTALLED,
            second.resultFor("cellulosesz:home")?.status
        )
        assertSame(
            firstHome,
            dispatcher.root.getChild("home")
        )
    }

    @Test
    fun `a fresh dispatcher produces an independent report with no stale nodes`() {
        val registry = CommandRegistry(
            listOf(definition("home")),
            pureBuilder
        )

        val firstDispatcher = CommandDispatcher<CommandSourceStack>()
        registry.register(firstDispatcher, buildContext, permissions)

        val secondDispatcher = CommandDispatcher<CommandSourceStack>()
        val report = registry.register(secondDispatcher, buildContext, permissions)

        assertTrue(report.owns("home"))
        assertTrue(report.owns("cellulosesz:home"))
        assertNull(secondDispatcher.root.getChild("missing"))
    }

}
