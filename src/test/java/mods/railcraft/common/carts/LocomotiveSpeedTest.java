package mods.railcraft.common.carts;

import mods.railcraft.common.carts.EntityLocomotive.LocoSpeed;
import mods.railcraft.common.util.misc.Vec2D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class LocomotiveSpeedTest {
    private static final double DELTA = 1.0E-9;

    @Test
    void lowerGearsHaveFixedLimitsAndMaxUsesTheTrackLimit() {
        assertEquals(0.1, LocoSpeed.SLOWEST.getSpeedLimit(), DELTA);
        assertEquals(0.2, LocoSpeed.SLOWER.getSpeedLimit(), DELTA);
        assertEquals(0.3, LocoSpeed.NORMAL.getSpeedLimit(), DELTA);
        assertEquals(Double.POSITIVE_INFINITY, LocoSpeed.MAX.getSpeedLimit());
    }

    @Test
    void lowerGearsLimitVectorSpeedEquallyInEveryDirection() {
        for (LocoSpeed gear : new LocoSpeed[]{LocoSpeed.SLOWEST, LocoSpeed.SLOWER, LocoSpeed.NORMAL}) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0)
                        continue;

                    Vec2D limited = gear.limitMotion(new Vec2D(x, z));
                    assertEquals(gear.getSpeedLimit(), limited.magnitude(), DELTA);
                    assertEquals(Math.signum(x), Math.signum(limited.getX()), DELTA);
                    assertEquals(Math.signum(z), Math.signum(limited.getY()), DELTA);
                    assertEquals(0.0, limited.getX() * z - limited.getY() * x, DELTA);
                }
            }
        }
    }

    @Test
    void gearLimitDoesNotAccelerateASlowOrStoppedLocomotive() {
        for (LocoSpeed gear : LocoSpeed.VALUES) {
            Vec2D slow = new Vec2D(0.02, -0.03);
            Vec2D stopped = new Vec2D();
            assertSame(slow, gear.limitMotion(slow));
            assertSame(stopped, gear.limitMotion(stopped));
        }
    }

    @Test
    void maxGearDoesNotImposeAFixedFourthSpeed() {
        Vec2D motion = new Vec2D(0.8, 0.8);
        assertSame(motion, LocoSpeed.MAX.limitMotion(motion));
    }
}
