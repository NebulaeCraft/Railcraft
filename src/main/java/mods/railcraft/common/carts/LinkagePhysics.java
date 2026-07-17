/*------------------------------------------------------------------------------
 Copyright (c) CovertJaguar, 2011-2020
 http://railcraft.info

 This code is the property of CovertJaguar
 and may only be used with explicit written
 permission unless otherwise specified on the
 license page at http://railcraft.info/wiki/info:license.
 -----------------------------------------------------------------------------*/
package mods.railcraft.common.carts;

import mods.railcraft.common.util.misc.Vec2D;

/**
 * Pure calculations used by {@link LinkageHandler}.
 */
final class LinkagePhysics {
    private static final double MIN_SPEED_SQ = 1.0E-10;
    private static final double PARALLEL_DOT = 0.999;
    /**
     * Linked carts do not collide with each other, so curved-link spring
     * handling must retain a small hard-compression zone. This matches the
     * normal cart collision rest distance used by {@link MinecartHooks}.
     */
    static final double MINIMUM_CURVED_LINK_DISTANCE = 1.28;

    private LinkagePhysics() {
    }

    /**
     * A center-to-center chord is shorter than the distance along a bend.  A
     * compressed spring based on that chord therefore applies a force even
     * when two carts are correctly spaced along curved track.
     */
    static double springStretch(double distance, double optimalDistance, boolean curvedLink) {
        double stretch = distance - optimalDistance;
        if (!curvedLink || stretch >= 0.0)
            return stretch;

        // Ignore only the harmless loss of chord length through a bend. Once
        // the carts are closer than their collision rest distance, restore a
        // compression spring so directly linked carts cannot pass through one
        // another while their normal collision response is disabled.
        double minimumDistance = Math.min(optimalDistance, MINIMUM_CURVED_LINK_DISTANCE);
        return Math.min(0.0, distance - minimumDistance);
    }

    /**
     * Detects the entry/exit of a bend even when neither cart is currently on
     * the turn block itself.
     */
    static boolean directionsDiffer(Vec2D firstVelocity, Vec2D secondVelocity) {
        double firstSpeedSq = firstVelocity.magnitudeSq();
        double secondSpeedSq = secondVelocity.magnitudeSq();
        if (firstSpeedSq < MIN_SPEED_SQ || secondSpeedSq < MIN_SPEED_SQ)
            return false;

        double cosine = firstVelocity.dotProduct(secondVelocity) / Math.sqrt(firstSpeedSq * secondSpeedSq);
        return Math.abs(cosine) < PARALLEL_DOT;
    }

    /**
     * Calculates the damping adjustment for one cart.
     *
     * On straight track this is the original chord-based damping calculation.
     * On a bend it compares scalar along-track speeds and applies the result in
     * the cart's own direction of travel.  Equal-speed carts turning through
     * different headings consequently receive no artificial braking force.
     */
    static Vec2D dampingAdjustment(Vec2D velocity, Vec2D otherVelocity, Vec2D chordDirection,
                                   double damping, boolean curvedLink) {
        if (!curvedLink) {
            double relativeSpeed = Vec2D.subtract(otherVelocity, velocity).dotProduct(chordDirection);
            return new Vec2D(
                    damping * relativeSpeed * chordDirection.getX(),
                    damping * relativeSpeed * chordDirection.getY());
        }

        double speed = velocity.magnitude();
        if (speed * speed < MIN_SPEED_SQ) {
            double otherSpeed = otherVelocity.magnitude();
            if (otherSpeed * otherSpeed < MIN_SPEED_SQ)
                return new Vec2D();

            // A stopped cart has no velocity tangent yet.  Orient the chord in
            // the moving cart's direction; the rail will project this onto the
            // stopped cart's local tangent during its next update.
            double direction = otherVelocity.dotProduct(chordDirection) < 0.0 ? -1.0 : 1.0;
            return new Vec2D(
                    damping * otherSpeed * direction * chordDirection.getX(),
                    damping * otherSpeed * direction * chordDirection.getY());
        }

        double speedDifference = otherVelocity.magnitude() - speed;
        return new Vec2D(
                damping * speedDifference * velocity.getX() / speed,
                damping * speedDifference * velocity.getY() / speed);
    }
}
