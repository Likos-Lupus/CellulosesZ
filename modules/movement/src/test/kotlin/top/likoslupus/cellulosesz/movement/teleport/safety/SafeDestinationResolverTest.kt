package top.likoslupus.cellulosesz.movement.teleport.safety

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.movement.config.TeleportSafetySettings
import top.likoslupus.cellulosesz.movement.teleport.SafetyMode
import top.likoslupus.cellulosesz.movement.teleport.StoredPosition
import kotlin.math.floor

class SafeDestinationResolverTest {

    private val resolver = SafeDestinationResolver()
    private val settings = TeleportSafetySettings(
        enabled = true,
        searchHorizontalRadius = 3,
        searchVerticalRadius = 8,
        allowWater = false,
    )

    private fun position(
        x: Int,
        y: Int,
        z: Int
    ) =
        StoredPosition(
            dimension = "minecraft:overworld",
            x = x.toDouble(),
            y = y.toDouble(),
            z = z.toDouble(),
            yaw = 0f,
            pitch = 0f
        )

    @Test
    fun `exact safe position is returned unchanged`() {
        val probe = FakeProbe(safe = setOf(key(10, 64, 10)))

        val result = resolver.resolve(
            position(10, 64, 10),
            SafetyMode.FIND_NEAREST_SAFE,
            settings,
            probe
        )

        assertEquals(
            position(10, 64, 10),
            assertInstanceOf(
                SafetyResult.Safe::class.java,
                result
            ).position
        )
    }

    @Test
    fun `requires exact safety in REQUIRE_SAFE mode`() {
        val probe = FakeProbe(safe = setOf(key(10, 65, 10)))

        val result = resolver.resolve(
            position(10, 64, 10),
            SafetyMode.REQUIRE_SAFE,
            settings,
            probe
        )

        assertInstanceOf(
            SafetyResult.Unsafe::class.java,
            result
        )
    }

    @Test
    fun `nearest safe search prefers vertical offsets`() {
        val probe = FakeProbe(safe = setOf(key(10, 66, 10)))

        val result = resolver.resolve(
            position(10, 64, 10),
            SafetyMode.FIND_NEAREST_SAFE,
            settings,
            probe
        )

        assertEquals(
            position(10, 66, 10),
            assertInstanceOf(
                SafetyResult.Safe::class.java,
                result
            ).position
        )
    }

    @Test
    fun `allow unsafe bypasses checks`() {
        val probe = FakeProbe(safe = emptySet(), withinBorder = false)

        val result = resolver.resolve(
            position(10, 64, 10),
            SafetyMode.ALLOW_UNSAFE,
            settings,
            probe
        )

        assertEquals(
            position(10, 64, 10),
            assertInstanceOf(
                SafetyResult.Safe::class.java,
                result
            ).position
        )
    }

    @Test
    fun `disabled safety accepts requested position`() {
        val probe = FakeProbe(safe = emptySet())

        val result = resolver.resolve(
            position(10, 64, 10),
            SafetyMode.FIND_NEAREST_SAFE,
            settings.copy(enabled = false),
            probe,
        )

        assertEquals(
            position(10, 64, 10),
            assertInstanceOf(
                SafetyResult.Safe::class.java,
                result
            ).position
        )
    }

    @Test
    fun `water is rejected unless allowed`() {
        val probe = FakeProbe(safe = emptySet(), surface = SurfaceKind.WATER)

        assertInstanceOf(
            SafetyResult.Unsafe::class.java,
            resolver.resolve(
                position(10, 64, 10),
                SafetyMode.FIND_NEAREST_SAFE,
                settings,
                probe
            ),
        )
        assertInstanceOf(
            SafetyResult.Safe::class.java,
            resolver.resolve(
                position(10, 64, 10),
                SafetyMode.FIND_NEAREST_SAFE,
                settings.copy(allowWater = true),
                probe,
            ),
        )
    }

    @Test
    fun `outside barrier is unsafe`() {
        val probe = FakeProbe(
            safe = setOf(key(10, 64, 10)),
            withinBorder = false
        )

        assertInstanceOf(
            SafetyResult.Unsafe::class.java,
            resolver.resolve(
                position(10, 64, 10),
                SafetyMode.FIND_NEAREST_SAFE,
                settings,
                probe
            ),
        )
    }

    @Test
    fun `search is bounded and does not hang when nothing is safe`() {
        val probe = FakeProbe(safe = emptySet())

        assertInstanceOf(
            SafetyResult.Unsafe::class.java,
            resolver.resolve(
                position(0, 0, 0),
                SafetyMode.FIND_NEAREST_SAFE,
                settings,
                probe
            ),
        )
    }

    private fun key(
        x: Int,
        y: Int,
        z: Int
    ) =
        Triple(
            x,
            y,
            z
        )

    private class FakeProbe(
        private val safe: Set<Triple<Int, Int, Int>>,
        private val surface: SurfaceKind = SurfaceKind.BLOCKED,
        private val withinBorder: Boolean = true,
        private val withinHeight: Boolean = true,
        private val loaded: Boolean = true,
    ) : TeleportProbe {

        override fun isWithinBorder(x: Double, z: Double): Boolean =
            withinBorder

        override fun isWithinHeight(y: Double): Boolean =
            withinHeight

        override fun isChunkLoaded(blockX: Int, blockZ: Int): Boolean =
            loaded

        override fun classify(
            x: Double,
            y: Double,
            z: Double
        ): SurfaceKind {
            val key = Triple(
                floor(x).toInt(),
                floor(y).toInt(),
                floor(z).toInt()
            )
            return if (key in safe) {
                SurfaceKind.SAFE
            } else {
                surface
            }
        }

    }

}
