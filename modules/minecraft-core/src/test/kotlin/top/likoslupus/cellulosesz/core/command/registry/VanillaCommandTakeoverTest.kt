package top.likoslupus.cellulosesz.core.command.registry

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.tree.CommandNode
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
 * Proves the EssentialsX-style takeover with pure Brigadier: the plain literal becomes ours, the
 * original node stays reachable through `/minecraft:<literal>`, and a failed takeover leaves vanilla
 * untouched. The removal seam is injected so the flow is testable without mixins.
 */
class VanillaCommandTakeoverTest {

    private val permissions = PermissionService(VanillaFallbackPermissionBridge)

    private val buildContext: CommandBuildContext = CommandBuildContext.simple(
        HolderLookup.Provider.create(Stream.empty()),
        FeatureFlagSet.of(),
    )

    private val pureBuilder = CommandRootBuilder { _, literal, _, _, _, _ ->
        LiteralArgumentBuilder
                .literal<CommandSourceStack>(literal)
                .executes { 1 }.build()
    }

    /** Test-only removal: the production path is the Brigadier mixin. */
    private val workingMutator = CommandTreeMutator { dispatcher, literal ->
        val children = dispatcher.root.childrenField()
        children.remove(literal)
        dispatcher.root.getChild(literal) == null
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

    private fun vanilla(
        literal: String,
        result: Int,
        requirement: () -> Boolean = { true }
    ) =
        LiteralArgumentBuilder
                .literal<CommandSourceStack>(literal)
                .requires { requirement() }
                .executes { result }
                .build()

    private fun registry(mutator: CommandTreeMutator = workingMutator): CommandRegistry =
        CommandRegistry(
            listOf(definition("me")),
            pureBuilder,
            mutator
        )

    private fun policy(): CommandRoutingPolicy =
        CommandRoutingPolicy(vanillaTakeovers = setOf("me"))

    @Test
    fun `the vanilla literal is replaced and stays reachable at minecraft`() {
        val dispatcher = CommandDispatcher<CommandSourceStack>()
        val vanillaMe = vanilla("me", 7)
        dispatcher.root.addChild(vanillaMe)

        val report = registry().register(
            dispatcher,
            buildContext,
            permissions,
            policy()
        )

        val installed = dispatcher.root.getChild("me")
        assertNotSame(vanillaMe, installed)
        assertEquals(
            "minecraft:me",
            report.resultFor("me")?.vanillaAlias,
        )
        assertSame(
            vanillaMe,
            dispatcher.root.getChild("minecraft:me")?.redirect
        )
        assertEquals(
            CommandRouteStatus.INSTALLED,
            report.resultFor("me")?.status
        )
        assertTrue(report.owns("czme"))
    }

    @Test
    fun `the alias copies the vanilla requirement`() {
        val dispatcher = CommandDispatcher<CommandSourceStack>()
        val vanillaMe = vanilla("me", 7) { false }
        dispatcher.root.addChild(vanillaMe)

        registry().register(
            dispatcher,
            buildContext,
            permissions,
            policy()
        )

        val alias = dispatcher.root.getChild("minecraft:me")
        assertEquals(vanillaMe.requirement, alias?.requirement)
    }

    @Test
    fun `a failed removal keeps vanilla and reports an unconfirmed takeover`() {
        val dispatcher = CommandDispatcher<CommandSourceStack>()
        val vanillaMe = vanilla("me", 7)
        dispatcher.root.addChild(vanillaMe)

        val report = registry { _, _ -> false }
                .register(dispatcher, buildContext, permissions, policy())

        assertSame(
            vanillaMe,
            dispatcher.root.getChild("me")
        )
        assertNull(dispatcher.root.getChild("minecraft:me"))
        assertEquals(
            CommandRouteStatus.TAKEOVER_NOT_CONFIRMED,
            report.resultFor("me")?.status,
        )
    }

    @Test
    fun `an existing minecraft alias blocks the takeover`() {
        val dispatcher = CommandDispatcher<CommandSourceStack>()
        val vanillaMe = vanilla("me", 7)
        dispatcher.root.addChild(vanillaMe)
        dispatcher.root.addChild(vanilla("minecraft:me", 9))

        val report = registry().register(
            dispatcher,
            buildContext,
            permissions,
            policy()
        )

        assertSame(
            vanillaMe,
            dispatcher.root.getChild("me")
        )
        assertEquals(
            CommandRouteStatus.TAKEOVER_NOT_CONFIRMED,
            report.resultFor("me")?.status,
        )
    }

    private fun CommandNode<CommandSourceStack>.childrenField(): MutableMap<String, CommandNode<CommandSourceStack>> {
        val field = CommandNode::class.java.getDeclaredField("children")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return field.get(this) as MutableMap<String, CommandNode<CommandSourceStack>>
    }

}
