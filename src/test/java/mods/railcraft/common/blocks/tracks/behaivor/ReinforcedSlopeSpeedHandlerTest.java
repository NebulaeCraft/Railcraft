package mods.railcraft.common.blocks.tracks.behaivor;

import mods.railcraft.common.util.misc.Vec2D;
import net.minecraft.block.BlockRailBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReinforcedSlopeSpeedHandlerTest {
    private static final double DELTA = 1.0E-9;
    private static final double TARGET_SPEED = 0.6;
    private static final double SLOPE_ADJUSTMENT = 0.0078125;

    @Test
    void eastSlopeHasTheSameSpeedUphillAndDownhill() {
        Vec2D uphill = ReinforcedSlopeSpeedHandler.prepareMotion(
                new Vec2D(0.3, 0.1), BlockRailBase.EnumRailDirection.ASCENDING_EAST,
                TARGET_SPEED, SLOPE_ADJUSTMENT, 1.0);
        Vec2D downhill = ReinforcedSlopeSpeedHandler.prepareMotion(
                new Vec2D(-0.3, 0.1), BlockRailBase.EnumRailDirection.ASCENDING_EAST,
                TARGET_SPEED, SLOPE_ADJUSTMENT, 1.0);

        assertEquals(TARGET_SPEED, uphill.getX() - SLOPE_ADJUSTMENT, DELTA);
        assertEquals(-TARGET_SPEED, downhill.getX() - SLOPE_ADJUSTMENT, DELTA);
        assertEquals(0.0, uphill.getY(), DELTA);
        assertEquals(0.0, downhill.getY(), DELTA);
    }

    @Test
    void northSlopeHasTheSameSpeedUphillAndDownhill() {
        Vec2D uphill = ReinforcedSlopeSpeedHandler.prepareMotion(
                new Vec2D(0.1, -0.3), BlockRailBase.EnumRailDirection.ASCENDING_NORTH,
                TARGET_SPEED, SLOPE_ADJUSTMENT, 1.0);
        Vec2D downhill = ReinforcedSlopeSpeedHandler.prepareMotion(
                new Vec2D(0.1, 0.3), BlockRailBase.EnumRailDirection.ASCENDING_NORTH,
                TARGET_SPEED, SLOPE_ADJUSTMENT, 1.0);

        assertEquals(-TARGET_SPEED, uphill.getY() + SLOPE_ADJUSTMENT, DELTA);
        assertEquals(TARGET_SPEED, downhill.getY() + SLOPE_ADJUSTMENT, DELTA);
        assertEquals(0.0, uphill.getX(), DELTA);
        assertEquals(0.0, downhill.getX(), DELTA);
    }

    @Test
    void westAndSouthSlopesHaveTheSameSpeedInBothDirections() {
        Vec2D westUphill = ReinforcedSlopeSpeedHandler.prepareMotion(
                new Vec2D(-0.3, 0.0), BlockRailBase.EnumRailDirection.ASCENDING_WEST,
                TARGET_SPEED, SLOPE_ADJUSTMENT, 1.0);
        Vec2D westDownhill = ReinforcedSlopeSpeedHandler.prepareMotion(
                new Vec2D(0.3, 0.0), BlockRailBase.EnumRailDirection.ASCENDING_WEST,
                TARGET_SPEED, SLOPE_ADJUSTMENT, 1.0);
        Vec2D southUphill = ReinforcedSlopeSpeedHandler.prepareMotion(
                new Vec2D(0.0, 0.3), BlockRailBase.EnumRailDirection.ASCENDING_SOUTH,
                TARGET_SPEED, SLOPE_ADJUSTMENT, 1.0);
        Vec2D southDownhill = ReinforcedSlopeSpeedHandler.prepareMotion(
                new Vec2D(0.0, -0.3), BlockRailBase.EnumRailDirection.ASCENDING_SOUTH,
                TARGET_SPEED, SLOPE_ADJUSTMENT, 1.0);

        assertEquals(-TARGET_SPEED, westUphill.getX() + SLOPE_ADJUSTMENT, DELTA);
        assertEquals(TARGET_SPEED, westDownhill.getX() + SLOPE_ADJUSTMENT, DELTA);
        assertEquals(TARGET_SPEED, southUphill.getY() - SLOPE_ADJUSTMENT, DELTA);
        assertEquals(-TARGET_SPEED, southDownhill.getY() - SLOPE_ADJUSTMENT, DELTA);
    }

    @Test
    void vanillaRiderSlowdownIsPreCompensated() {
        Vec2D prepared = ReinforcedSlopeSpeedHandler.prepareMotion(
                new Vec2D(0.3, 0.0), BlockRailBase.EnumRailDirection.ASCENDING_EAST,
                TARGET_SPEED, SLOPE_ADJUSTMENT, 0.75);

        assertEquals(TARGET_SPEED, (prepared.getX() - SLOPE_ADJUSTMENT) * 0.75, DELTA);
    }

    @Test
    void stoppedCartIsNotStartedWithoutATravelDirection() {
        Vec2D prepared = ReinforcedSlopeSpeedHandler.prepareMotion(
                new Vec2D(), BlockRailBase.EnumRailDirection.ASCENDING_SOUTH,
                TARGET_SPEED, SLOPE_ADJUSTMENT, 1.0);

        assertEquals(0.0, prepared.magnitude(), DELTA);
    }
}
