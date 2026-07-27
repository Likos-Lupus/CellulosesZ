package top.likoslupus.cellulosesz.modules.playerstate.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class CompassAndRealNameCommandTest {

    @Test
    void compassNormalizesNegativeAndOversizedYaw() {
        assertEquals(359.0D, CompassCommand.normalizeDegrees(-1.0D));
        assertEquals(1.0D, CompassCommand.normalizeDegrees(721.0D));
        assertEquals(0.0D, CompassCommand.normalizeDegrees(720.0D));
    }

    @Test
    void compassCoversEightDirectionBoundaries() {
        assertEquals("south", CompassCommand.directionKey(0.0D));
        assertEquals("south-west", CompassCommand.directionKey(22.5D));
        assertEquals("west", CompassCommand.directionKey(67.5D));
        assertEquals("north-west", CompassCommand.directionKey(112.5D));
        assertEquals("north", CompassCommand.directionKey(157.5D));
        assertEquals("north-east", CompassCommand.directionKey(202.5D));
        assertEquals("east", CompassCommand.directionKey(247.5D));
        assertEquals("south-east", CompassCommand.directionKey(292.5D));
        assertEquals("south", CompassCommand.directionKey(337.5D));
    }

    @Test
    void realNameNormalizationRemovesLegacyAndMiniMessageFormatting() {
        assertEquals("alice the brave", RealNameCommand.normalize("<gold>§lAlice</gold> the &aBrave"));
        assertEquals("éclair", RealNameCommand.normalize("Ｅ́clair"));
    }

}
