package mods.railcraft.common.blocks.tracks.behaivor;

import mods.railcraft.common.util.misc.Vec2D;
import mods.railcraft.common.carts.EntityLocomotive.LocoSpeed;
import net.minecraft.block.BlockRailBase;
import net.minecraft.entity.item.EntityMinecartEmpty;
import net.minecraft.init.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReinforcedDiagonalSpeedHandlerTest {
    private static final double DELTA = 1.0E-9;
    private static final double TARGET_SPEED = 0.6;

    @BeforeAll
    static void setUp() {
        Bootstrap.register();
    }

    @Test
    void runningLocomotiveUsesItsSelectedGearForMotionAndMovementCap() {
        double[] expectedSpeeds = {0.1, 0.2, 0.3, SpeedController.REINFORCED_MAX_SPEED};
        for (LocoSpeed gear : LocoSpeed.VALUES) {
            SpeedTestLocomotive loco = new SpeedTestLocomotive(gear, true);
            loco.motionX = 0.05;
            loco.motionZ = -0.05;

            ReinforcedDiagonalSpeedHandler.INSTANCE.adjustSpeed(loco);

            assertEquals(expectedSpeeds[gear.ordinal()], Math.hypot(loco.motionX, loco.motionZ), DELTA);
            assertEquals(expectedSpeeds[gear.ordinal()], loco.getCurrentCartSpeedCapOnRail(), 1.0E-7);
        }
    }

    @Test
    void loweringTheGearOnADiagonalImmediatelyLowersTheTarget() {
        SpeedTestLocomotive loco = new SpeedTestLocomotive(LocoSpeed.MAX, true);
        loco.motionX = 0.4;
        loco.motionZ = 0.4;
        ReinforcedDiagonalSpeedHandler.INSTANCE.adjustSpeed(loco);

        loco.setSpeed(LocoSpeed.SLOWEST);
        ReinforcedDiagonalSpeedHandler.INSTANCE.adjustSpeed(loco);

        assertEquals(0.1, Math.hypot(loco.motionX, loco.motionZ), DELTA);
        assertEquals(0.1, loco.getCurrentCartSpeedCapOnRail(), 1.0E-7);
    }

    @Test
    void standaloneMinecartRetainsDragAndIsOnlyCappedAboveTheTrackLimit() {
        EntityMinecartEmpty cart = new EntityMinecartEmpty(null);
        cart.motionX = 0.2;
        cart.motionZ = 0.2;
        for (int tick = 0; tick < 20; tick++) {
            cart.motionX *= 0.96;
            cart.motionZ *= 0.96;
            ReinforcedDiagonalSpeedHandler.INSTANCE.adjustSpeed(cart);
        }
        assertEquals(Math.hypot(0.2, 0.2) * Math.pow(0.96, 20),
                Math.hypot(cart.motionX, cart.motionZ), DELTA);

        cart.motionX = 0.6;
        cart.motionZ = 0.6;
        ReinforcedDiagonalSpeedHandler.INSTANCE.adjustSpeed(cart);
        assertEquals(SpeedController.REINFORCED_MAX_SPEED, Math.hypot(cart.motionX, cart.motionZ), DELTA);
    }

    @Test
    void idleLocomotiveKeepsItsGearLimitWithoutBeingAccelerated() {
        SpeedTestLocomotive loco = new SpeedTestLocomotive(LocoSpeed.SLOWEST, false);
        loco.motionX = 0.4;
        loco.motionZ = 0.4;
        ReinforcedDiagonalSpeedHandler.INSTANCE.adjustSpeed(loco);
        assertEquals(0.1, Math.hypot(loco.motionX, loco.motionZ), DELTA);

        loco.motionX *= 0.9;
        loco.motionZ *= 0.9;
        ReinforcedDiagonalSpeedHandler.INSTANCE.adjustSpeed(loco);
        assertEquals(0.09, Math.hypot(loco.motionX, loco.motionZ), DELTA);
    }

    @Test
    void longDiagonalBlockTransitionDoesNotRaiseVectorMagnitude() {
        Vec2D prepared = ReinforcedDiagonalSpeedHandler.prepareMotion(
                new Vec2D(0.6, 0.6), TARGET_SPEED, 1.0, false);

        assertEquals(TARGET_SPEED, prepared.magnitude(), DELTA);
        assertEquals(TARGET_SPEED / Math.sqrt(2.0), prepared.getX(), DELTA);
        assertEquals(TARGET_SPEED / Math.sqrt(2.0), prepared.getY(), DELTA);
    }

    @Test
    void diagonalTravelDirectionIsPreserved() {
        Vec2D prepared = ReinforcedDiagonalSpeedHandler.prepareMotion(
                new Vec2D(-0.3, 0.3), TARGET_SPEED, 1.0, true);

        assertEquals(TARGET_SPEED, prepared.magnitude(), DELTA);
        assertEquals(-TARGET_SPEED / Math.sqrt(2.0), prepared.getX(), DELTA);
        assertEquals(TARGET_SPEED / Math.sqrt(2.0), prepared.getY(), DELTA);
    }

    @Test
    void vanillaRiderSlowdownIsPreCompensated() {
        Vec2D prepared = ReinforcedDiagonalSpeedHandler.prepareMotion(
                new Vec2D(0.2, 0.2), TARGET_SPEED, 0.75, true);

        assertEquals(TARGET_SPEED, prepared.magnitude() * 0.75, DELTA);
    }

    @Test
    void stoppedCartIsNotStartedWithoutATravelDirection() {
        Vec2D prepared = ReinforcedDiagonalSpeedHandler.prepareMotion(
                new Vec2D(), TARGET_SPEED, 1.0, true);

        assertEquals(0.0, prepared.magnitude(), DELTA);
    }

    @Test
    void unpoweredCartKeepsItsPostDragSpeedBelowTheLimit() {
        Vec2D postDragMotion = new Vec2D(0.3, 0.4);

        Vec2D prepared = ReinforcedDiagonalSpeedHandler.prepareMotion(
                postDragMotion, TARGET_SPEED, 1.0, false);

        assertEquals(0.3, prepared.getX(), DELTA);
        assertEquals(0.4, prepared.getY(), DELTA);
        assertEquals(0.5, prepared.magnitude(), DELTA);
    }

    @Test
    void poweredTrainRestoresTargetSpeedAfterLinkDrag() {
        Vec2D prepared = ReinforcedDiagonalSpeedHandler.prepareMotion(
                new Vec2D(0.3, 0.4), TARGET_SPEED, 1.0, true);

        assertEquals(TARGET_SPEED, prepared.magnitude(), DELTA);
        assertEquals(0.36, prepared.getX(), DELTA);
        assertEquals(0.48, prepared.getY(), DELTA);
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
