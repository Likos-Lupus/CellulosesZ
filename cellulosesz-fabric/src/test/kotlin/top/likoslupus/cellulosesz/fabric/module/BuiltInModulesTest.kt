package top.likoslupus.cellulosesz.fabric.module

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.api.module.ModuleKeys
import top.likoslupus.cellulosesz.api.module.ModulePhase
import top.likoslupus.cellulosesz.core.module.ModuleGraph

class BuiltInModulesTest {

    private val expectedKeys = setOf(
        "admin",
        "command",
        "economy",
        "home",
        "item",
        "kit",
        "messaging",
        "permission",
        "playerstate",
        "sign",
        "teleport",
        "text",
        "user",
        "warp",
        "world",
    )

    @Test
    fun `built-in catalog contains exact expected 15 keys without duplicates`() {
        val catalog = BuiltInModules.catalog()
        val actualKeys = catalog.descriptors.map { it.key.value }.toSet()

        assertEquals(15, catalog.definitions.size)
        assertEquals(expectedKeys, actualKeys)
        assertEquals(ModuleKeys.ALL.map { it.value }.toSet(), actualKeys)
    }

    @Test
    fun `built-in catalog resolves deterministic start and stop order`() {
        val catalog = BuiltInModules.catalog()
        val graph = ModuleGraph(catalog)

        val resolved = graph.resolve(ModuleKeys.ALL)
        assertEquals(15, resolved.startOrder.size)
        assertEquals(15, resolved.stopOrder.size)
        assertEquals(resolved.startOrder.reversed(), resolved.stopOrder)

        val startKeyValues = resolved.startKeys.map { it.value }

        // Core modules must start before feature modules that require them
        val commandIndex = startKeyValues.indexOf("command")
        val permissionIndex = startKeyValues.indexOf("permission")
        val userIndex = startKeyValues.indexOf("user")
        val teleportIndex = startKeyValues.indexOf("teleport")
        val signIndex = startKeyValues.indexOf("sign")

        assertTrue(commandIndex < userIndex, "command must start before user")
        assertTrue(permissionIndex < userIndex, "permission must start before user")
        assertTrue(userIndex < teleportIndex, "user must start before teleport")

        // Sign requires almost all modules and must start after its dependencies
        catalog.require(ModuleKeys.SIGN).descriptor.requires.forEach {
            val reqIndex = startKeyValues.indexOf(it.value)
            assertTrue(
                reqIndex < signIndex,
                "Required dependency '$it' ($reqIndex) must start before 'sign' ($signIndex)"
            )
        }

        // Check phase ordering
        val coreDescriptors = resolved.startOrder.filter { it.phase == ModulePhase.CORE }
        val featureDescriptors = resolved.startOrder.filter { it.phase == ModulePhase.FEATURE }
        assertEquals(2, coreDescriptors.size)
        assertEquals(13, featureDescriptors.size)
    }

    @Test
    fun `built-in factories instantiate valid module objects`() {
        val catalog = BuiltInModules.catalog()
        catalog.definitions.forEach { (descriptor, factory) ->
            val instance = factory.create()
            assertNotNull(
                instance,
                "Factory for ${descriptor.key} must return non-null module instance"
            )
        }
    }

}
