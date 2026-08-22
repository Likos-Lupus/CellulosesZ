package top.likoslupus.cellulosesz.core.module

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import top.likoslupus.cellulosesz.api.module.*

class ModuleCatalogTest {

    private class TestModule : CellulosesZModule

    @Test
    fun `catalog construction stores immutable definitions and provides lookup`() {
        val cat = moduleCatalog {
            core(
                key = ModuleKey("core_a"),
                name = "Core A",
                description = "Core A Description",
                factory = ::TestModule
            )
            feature(
                key = ModuleKey("feature_b"),
                name = "Feature B",
                description = "Feature B Description",
                factory = ::TestModule
            ) {
                requires(ModuleKey("core_a"))
            }
        }

        assertEquals(2, cat.definitions.size)
        val coreA = cat[ModuleKey("core_a")]
        assertNotNull(coreA)
        assertEquals("Core A", coreA!!.descriptor.name)
        assertEquals(ModulePhase.CORE, coreA.descriptor.phase)
        assertTrue(coreA.descriptor.requires.isEmpty())
        assertTrue(coreA.factory.create() is TestModule)

        val featureB = cat.require(ModuleKey("feature_b"))
        assertEquals("Feature B", featureB.descriptor.name)
        assertEquals(ModulePhase.FEATURE, featureB.descriptor.phase)
        assertEquals(setOf(ModuleKey("core_a")), featureB.descriptor.requires)

        assertNull(cat[ModuleKey("unknown")])
        assertThrows<NoSuchElementException> {
            cat.require(ModuleKey("unknown"))
        }
    }

    @Test
    fun `duplicate key is rejected`() {
        val ex = assertThrows<IllegalArgumentException> {
            moduleCatalog {
                core(
                    key = ModuleKey("dup"),
                    name = "First",
                    description = "First",
                    factory = ::TestModule
                )
                feature(
                    key = ModuleKey("dup"),
                    name = "Second",
                    description = "Second",
                    factory = ::TestModule
                )
            }
        }
        assertTrue(ex.message!!.contains("Duplicate module key"))
    }

    @Test
    fun `self-required dependency is rejected`() {
        val ex = assertThrows<IllegalArgumentException> {
            moduleCatalog {
                feature(
                    key = ModuleKey("self_req"),
                    name = "Self Req",
                    description = "Desc",
                    factory = ::TestModule
                ) {
                    requires(ModuleKey("self_req"))
                }
            }
        }
        assertTrue(ex.message!!.contains("cannot require itself"))
    }

    @Test
    fun `self-optional dependency is rejected`() {
        val ex = assertThrows<IllegalArgumentException> {
            moduleCatalog {
                feature(
                    key = ModuleKey("self_opt"),
                    name = "Self Opt",
                    description = "Desc",
                    factory = ::TestModule
                ) {
                    optional(ModuleKey("self_opt"))
                }
            }
        }
        assertTrue(ex.message!!.contains("cannot optionally depend on itself"))
    }

    @Test
    fun `required and optional overlap is rejected`() {
        val ex = assertThrows<IllegalArgumentException> {
            moduleCatalog {
                feature(
                    key = ModuleKey("overlap"),
                    name = "Overlap",
                    description = "Desc",
                    factory = ::TestModule
                ) {
                    requires(ModuleKey("dep"))
                    optional(ModuleKey("dep"))
                }
            }
        }
        assertTrue(ex.message!!.contains("overlapping"))
    }

    @Test
    fun `blank name is rejected`() {
        assertThrows<IllegalArgumentException> {
            ModuleDescriptor(
                key = ModuleKey("test"),
                name = "",
                description = "desc",
                phase = ModulePhase.FEATURE
            )
        }
    }

    @Test
    fun `blank key is rejected`() {
        assertThrows<IllegalArgumentException> {
            ModuleKey("")
        }
        assertThrows<IllegalArgumentException> {
            ModuleKey("   ")
        }
    }

}
