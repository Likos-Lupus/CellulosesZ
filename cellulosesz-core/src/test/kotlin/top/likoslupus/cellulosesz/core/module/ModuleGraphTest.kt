package top.likoslupus.cellulosesz.core.module

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import top.likoslupus.cellulosesz.api.module.*

class ModuleGraphTest {

    private class TestModule : CellulosesZModule

    private fun testDefinition(
        key: String,
        phase: ModulePhase = ModulePhase.FEATURE,
        priority: Int = 0,
        requires: Set<String> = emptySet(),
        optional: Set<String> = emptySet(),
    ): ModuleDefinition = ModuleDefinition(
        descriptor = ModuleDescriptor(
            key = ModuleKey(key),
            name = key.replaceFirstChar { it.uppercase() },
            description = "$key module",
            phase = phase,
            priority = priority,
            requires = requires.map(::ModuleKey).toSet(),
            optional = optional.map(::ModuleKey).toSet(),
        ),
        factory = ::TestModule,
    )

    @Test
    fun `required dependency ordering orders providers before consumers`() {
        // A <- B <- C (C requires B, B requires A)
        val catalog = ModuleCatalog.of(
            listOf(
                testDefinition("c", requires = setOf("b")),
                testDefinition("b", requires = setOf("a")),
                testDefinition("a"),
            )
        )
        val graph = ModuleGraph(catalog)
        val resolved = graph.resolve(
            setOf(
                ModuleKey("a"),
                ModuleKey("b"),
                ModuleKey("c")
            )
        )

        assertEquals(
            listOf("a", "b", "c"),
            resolved.startKeys.map { it.value }
        )
        assertEquals(
            listOf("c", "b", "a"),
            resolved.stopKeys.map { it.value }
        )
    }

    @Test
    fun `missing required dependency fails resolution before mutation`() {
        val catalog = ModuleCatalog.of(
            listOf(
                testDefinition("b", requires = setOf("a")),
                testDefinition("a"),
            )
        )
        val graph = ModuleGraph(catalog)
        val ex = assertThrows<ModuleLoadException> {
            graph.resolve(setOf(ModuleKey("b"))) // 'a' is not enabled
        }
        assertTrue(ex.message!!.contains("Module 'b' requires enabled module 'a'"))
    }

    @Test
    fun `catalog with missing required dependency fails construction`() {
        val catalog = ModuleCatalog.of(
            listOf(
                testDefinition("b", requires = setOf("missing")),
            )
        )
        val ex = assertThrows<ModuleLoadException> {
            ModuleGraph(catalog)
        }
        assertTrue(ex.message!!.contains("Module 'b' requires missing module 'missing'"))
    }

    @Test
    fun `optional absent dependency still resolves consumer`() {
        val catalog = ModuleCatalog.of(
            listOf(
                testDefinition("b", optional = setOf("a")),
                testDefinition("a"),
            )
        )
        val graph = ModuleGraph(catalog)
        val resolved = graph.resolve(setOf(ModuleKey("b"))) // 'a' is absent/disabled

        assertEquals(
            listOf("b"),
            resolved.startKeys.map { it.value }
        )
        assertEquals(
            mapOf(ModuleKey("a") to false),
            resolved.optionalAvailability(ModuleKey("b"))
        )
    }

    @Test
    fun `optional present dependency orders provider before consumer`() {
        val catalog = ModuleCatalog.of(
            listOf(
                testDefinition("b", priority = 0, optional = setOf("a")),
                testDefinition("a", priority = 10), // normally higher priority would be later
            )
        )
        val graph = ModuleGraph(catalog)
        val resolved = graph.resolve(setOf(ModuleKey("a"), ModuleKey("b")))

        assertEquals(
            listOf("a", "b"),
            resolved.startKeys.map { it.value }
        )
        assertEquals(
            mapOf(ModuleKey("a") to true),
            resolved.optionalAvailability(ModuleKey("b"))
        )
    }

    @Test
    fun `duplicate key in sort fails deterministically`() {
        val descA1 = testDefinition("a").descriptor
        val descA2 = testDefinition("a").descriptor

        val catalog = ModuleCatalog.of(listOf(testDefinition("a")))
        val graph = ModuleGraph(catalog)

        val ex = assertThrows<ModuleLoadException> {
            graph.sort(listOf(descA1, descA2))
        }
        assertTrue(ex.message!!.contains("Duplicate module id: a"))
    }

    @Test
    fun `required cycle reports full cycle path in diagnostics`() {
        val catalog = ModuleCatalog.of(
            listOf(
                testDefinition("a", requires = setOf("b")),
                testDefinition("b", requires = setOf("c")),
                testDefinition("c", requires = setOf("a")),
            )
        )
        val graph = ModuleGraph(catalog)
        val ex = assertThrows<ModuleLoadException> {
            graph.resolve(
                setOf(
                    ModuleKey("a"),
                    ModuleKey("b"),
                    ModuleKey("c")
                )
            )
        }
        assertTrue(ex.message!!.contains("Module dependency cycle:"))
        assertTrue(
            ex.message!!.contains("a -> b -> c -> a")
                    || ex.message!!.contains("b -> c -> a -> b")
                    || ex.message!!.contains("c -> a -> b -> c")
        )
    }

    @Test
    fun `optional cycle when both active is detected`() {
        val catalog = ModuleCatalog.of(
            listOf(
                testDefinition("a", optional = setOf("b")),
                testDefinition("b", optional = setOf("a")),
            )
        )
        val graph = ModuleGraph(catalog)
        val ex = assertThrows<ModuleLoadException> {
            graph.resolve(
                setOf(
                    ModuleKey("a"),
                    ModuleKey("b")
                )
            )
        }
        assertTrue(ex.message!!.contains("Module dependency cycle:"))
    }

    @Test
    fun `base ordering respects phase then priority then key`() {
        val catalog = ModuleCatalog.of(
            listOf(
                testDefinition("z_feat", phase = ModulePhase.FEATURE, priority = 5),
                testDefinition("a_feat", phase = ModulePhase.FEATURE, priority = 5),
                testDefinition("b_core", phase = ModulePhase.CORE, priority = 10),
                testDefinition("a_core", phase = ModulePhase.CORE, priority = 0),
                testDefinition("found", phase = ModulePhase.FOUNDATION, priority = 0),
            )
        )
        val graph = ModuleGraph(catalog)
        val keys = setOf(
            ModuleKey("z_feat"),
            ModuleKey("a_feat"),
            ModuleKey("b_core"),
            ModuleKey("a_core"),
            ModuleKey("found")
        )

        val firstRun = graph.resolve(keys).startKeys.map { it.value }
        val secondRun = graph.resolve(keys).startKeys.map { it.value }

        val expected = listOf("found", "a_core", "b_core", "a_feat", "z_feat")
        assertEquals(expected, firstRun)
        assertEquals(expected, secondRun)
    }

}
