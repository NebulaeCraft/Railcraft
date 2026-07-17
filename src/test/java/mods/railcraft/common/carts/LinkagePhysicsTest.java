package mods.railcraft.common.carts;

import mods.railcraft.common.util.misc.Vec2D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkagePhysicsTest {
    private static final double DELTA = 1.0E-9;

    @Test
    void compressedChordDoesNotCreateSpringForceOnCurve() {
        assertEquals(0.0, LinkagePhysics.springStretch(1.3, 1.56, true), DELTA);
        assertEquals(-0.26, LinkagePhysics.springStretch(1.3, 1.56, false), DELTA);
        assertEquals(0.24, LinkagePhysics.springStretch(1.8, 1.56, true), DELTA);
    }

    @Test
    void equalSpeedsOnDifferentTangentsAreNotDamped() {
        Vec2D east = new Vec2D(0.4, 0.0);
        Vec2D south = new Vec2D(0.0, 0.4);
        Vec2D chord = new Vec2D(Math.sqrt(0.5), Math.sqrt(0.5));

        Vec2D adjustment = LinkagePhysics.dampingAdjustment(east, south, chord, 0.4, true);

        assertEquals(0.0, adjustment.magnitude(), DELTA);
        assertTrue(LinkagePhysics.directionsDiffer(east, south));
    }

    @Test
    void oppositeMotionOnStraightTrackIsNotMistakenForACurve() {
        assertFalse(LinkagePhysics.directionsDiffer(new Vec2D(0.2, 0.0), new Vec2D(-0.2, 0.0)));
    }

    @Test
    void straightTrackKeepsOriginalChordDamping() {
        Vec2D slower = new Vec2D(0.2, 0.0);
        Vec2D faster = new Vec2D(0.4, 0.0);
        Vec2D chord = new Vec2D(1.0, 0.0);

        Vec2D adjustment = LinkagePhysics.dampingAdjustment(slower, faster, chord, 0.4, false);

        assertEquals(0.08, adjustment.getX(), DELTA);
        assertEquals(0.0, adjustment.getY(), DELTA);
        assertFalse(LinkagePhysics.directionsDiffer(slower, faster));
    }

    @Test
    void curveDampingChangesOnlyAlongEachCartsTangent() {
        Vec2D slower = new Vec2D(0.2, 0.0);
        Vec2D faster = new Vec2D(0.0, 0.4);

        Vec2D slowerAdjustment = LinkagePhysics.dampingAdjustment(slower, faster, new Vec2D(1.0, 0.0), 0.4, true);
        Vec2D fasterAdjustment = LinkagePhysics.dampingAdjustment(faster, slower, new Vec2D(0.0, -1.0), 0.4, true);

        assertEquals(0.08, slowerAdjustment.getX(), DELTA);
        assertEquals(0.0, slowerAdjustment.getY(), DELTA);
        assertEquals(0.0, fasterAdjustment.getX(), DELTA);
        assertEquals(-0.08, fasterAdjustment.getY(), DELTA);
    }

    @Test
    void curveDampingCanStartAStationaryCart() {
        Vec2D stoppedFrontCart = new Vec2D();
        Vec2D movingRearCart = new Vec2D(0.4, 0.0);
        Vec2D chordTowardRear = new Vec2D(-1.0, 0.0);

        Vec2D adjustment = LinkagePhysics.dampingAdjustment(
                stoppedFrontCart, movingRearCart, chordTowardRear, 0.4, true);

        assertEquals(0.16, adjustment.getX(), DELTA);
        assertEquals(0.0, adjustment.getY(), DELTA);
    }
}
