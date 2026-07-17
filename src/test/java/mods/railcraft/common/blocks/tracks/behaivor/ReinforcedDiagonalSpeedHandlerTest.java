package mods.railcraft.common.blocks.tracks.behaivor;

import mods.railcraft.common.util.misc.Vec2D;
import net.minecraft.block.BlockRailBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReinforcedDiagonalSpeedHandlerTest {
    private static final double DELTA = 1.0E-9;
    private static final double TARGET_SPEED = 0.6;

    @Test
    void longDiagonalBlockTransitionDoesNotRaiseVectorMagnitude() {
        Vec2D prepared = ReinforcedDiagonalSpeedHandler.prepareMotion(
                new Vec2D(0.6, 0.6), TARGET_SPEED, 1.0);

        assertEquals(TARGET_SPEED, prepared.magnitude(), DELTA);
        assertEquals(TARGET_SPEED / Math.sqrt(2.0), prepared.getX(), DELTA);
        assertEquals(TARGET_SPEED / Math.sqrt(2.0), prepared.getY(), DELTA);
    }

    @Test
    void diagonalTravelDirectionIsPreserved() {
        Vec2D prepared = ReinforcedDiagonalSpeedHandler.prepareMotion(
                new Vec2D(-0.3, 0.3), TARGET_SPEED, 1.0);

        assertEquals(TARGET_SPEED, prepared.magnitude(), DELTA);
        assertEquals(-TARGET_SPEED / Math.sqrt(2.0), prepared.getX(), DELTA);
        assertEquals(TARGET_SPEED / Math.sqrt(2.0), prepared.getY(), DELTA);
    }

    @Test
    void vanillaRiderSlowdownIsPreCompensated() {
        Vec2D prepared = ReinforcedDiagonalSpeedHandler.prepareMotion(
                new Vec2D(0.2, 0.2), TARGET_SPEED, 0.75);

        assertEquals(TARGET_SPEED, prepared.magnitude() * 0.75, DELTA);
    }

    @Test
    void stoppedCartIsNotStartedWithoutATravelDirection() {
        Vec2D prepared = ReinforcedDiagonalSpeedHandler.prepareMotion(
                new Vec2D(), TARGET_SPEED, 1.0);

        assertEquals(0.0, prepared.magnitude(), DELTA);
    }

    @Test
    void allCornerShapesUsedBySingleAndLongDiagonalsAreDetected() {
        assertTrue(ReinforcedDiagonalSpeedHandler.isDiagonal(BlockRailBase.EnumRailDirection.SOUTH_EAST));
        assertTrue(ReinforcedDiagonalSpeedHandler.isDiagonal(BlockRailBase.EnumRailDirection.SOUTH_WEST));
        assertTrue(ReinforcedDiagonalSpeedHandler.isDiagonal(BlockRailBase.EnumRailDirection.NORTH_WEST));
        assertTrue(ReinforcedDiagonalSpeedHandler.isDiagonal(BlockRailBase.EnumRailDirection.NORTH_EAST));
        assertFalse(ReinforcedDiagonalSpeedHandler.isDiagonal(BlockRailBase.EnumRailDirection.EAST_WEST));
        assertFalse(ReinforcedDiagonalSpeedHandler.isDiagonal(BlockRailBase.EnumRailDirection.ASCENDING_EAST));
    }
}
