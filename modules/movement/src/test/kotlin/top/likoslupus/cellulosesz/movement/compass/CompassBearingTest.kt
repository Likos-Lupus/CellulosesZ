package top.likoslupus.cellulosesz.movement.compass

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CompassBearingTest {

    @Test
    fun `cardinal yaws map to the four compass points`() {
        assertEquals(CompassPoint.SOUTH, CompassBearing.read(0f).point)
        assertEquals(CompassPoint.WEST, CompassBearing.read(90f).point)
        assertEquals(CompassPoint.NORTH, CompassBearing.read(180f).point)
        assertEquals(CompassPoint.EAST, CompassBearing.read(270f).point)
    }

    @Test
    fun `diagonal yaws map to the four intermediate points`() {
        assertEquals(CompassPoint.NORTH_EAST, CompassBearing.read(225f).point)
        assertEquals(CompassPoint.SOUTH_EAST, CompassBearing.read(315f).point)
        assertEquals(CompassPoint.SOUTH_WEST, CompassBearing.read(45f).point)
        assertEquals(CompassPoint.NORTH_WEST, CompassBearing.read(135f).point)
    }

    @Test
    fun `the bearing is 0 at north and 180 at south`() {
        assertEquals(0, CompassBearing.read(180f).bearingDegrees)
        assertEquals(180, CompassBearing.read(0f).bearingDegrees)
        assertEquals(90, CompassBearing.read(270f).bearingDegrees)
        assertEquals(270, CompassBearing.read(90f).bearingDegrees)
    }

    @Test
    fun `the bearing wraps for negative and beyond-full yaw`() {
        assertEquals(90, CompassBearing.read(-90f).bearingDegrees)
        assertEquals(0, CompassBearing.read(540f).bearingDegrees)
    }

}
