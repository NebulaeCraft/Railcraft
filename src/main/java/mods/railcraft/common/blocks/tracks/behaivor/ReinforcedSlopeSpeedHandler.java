/*------------------------------------------------------------------------------
 Copyright (c) CovertJaguar, 2011-2020
 http://railcraft.info

 This code is the property of CovertJaguar
 and may only be used with explicit written
 permission unless otherwise specified on the
 license page at http://railcraft.info/wiki/info:license.
 -----------------------------------------------------------------------------*/
package mods.railcraft.common.blocks.tracks.behaivor;

import com.google.common.collect.MapMaker;
import mods.railcraft.common.blocks.tracks.TrackTools;
import mods.railcraft.common.carts.EntityCartBasic;
import mods.railcraft.common.util.misc.Vec2D;
import net.minecraft.block.BlockRailBase;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.minecart.MinecartUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Keeps minecarts moving at a constant horizontal speed on reinforced slopes.
 *
 * <p>The correction runs after the normal minecart update and at the lowest
 * world-tick priority, so locomotive force, drag, linkage physics, and the
 * vanilla elevation adjustment cannot accumulate into a different uphill or
 * downhill speed. The stored motion also pre-compensates the slope adjustment
 * that vanilla applies before movement on the next tick.</p>
 */
public final class ReinforcedSlopeSpeedHandler {
    public static final ReinforcedSlopeSpeedHandler INSTANCE = new ReinforcedSlopeSpeedHandler();
    private static final double MIN_MOTION = 1.0E-6;
    private static final double VANILLA_RIDER_MOVEMENT_SCALE = 0.75;

    private final Map<World, Map<EntityMinecart, BlockRailBase.EnumRailDirection>> pendingCarts =
            new MapMaker().weakKeys().makeMap();
    private final Map<EntityMinecart, Float> overriddenSpeedCaps = new MapMaker().weakKeys().makeMap();

    private ReinforcedSlopeSpeedHandler() {
    }

    @SubscribeEvent
    public void onMinecartUpdate(MinecartUpdateEvent event) {
        EntityMinecart cart = event.getMinecart();
        Float originalSpeedCap = overriddenSpeedCaps.remove(cart);
        if (originalSpeedCap != null)
            cart.setCurrentCartSpeedCapOnRail(originalSpeedCap);

        BlockPos pos = event.getPos();
        if (!TrackTools.isRailBlockAt(cart.world, pos)
                || TrackTools.getTrackTypeAt(cart.world, pos) != TrackTypes.REINFORCED.getTrackType())
            return;

        BlockRailBase.EnumRailDirection direction = TrackTools.getTrackDirection(cart.world, pos, cart);
        if (direction.isAscending())
            pendingCarts.computeIfAbsent(cart.world, world -> new IdentityHashMap<>()).put(cart, direction);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;

        Map<EntityMinecart, BlockRailBase.EnumRailDirection> carts = pendingCarts.remove(event.world);
        if (carts == null || carts.isEmpty())
            return;

        carts.forEach((cart, direction) -> {
            if (cart.isDead)
                return;

            double movementScale = cart.isBeingRidden() && !(cart instanceof EntityCartBasic)
                    ? VANILLA_RIDER_MOVEMENT_SCALE : 1.0;
            Vec2D preparedMotion = prepareMotion(
                    new Vec2D(cart.motionX, cart.motionZ),
                    direction,
                    SpeedController.REINFORCED_MAX_SPEED,
                    cart.getSlopeAdjustment(),
                    movementScale);
            cart.motionX = preparedMotion.getX();
            cart.motionZ = preparedMotion.getY();

            overriddenSpeedCaps.put(cart, cart.getCurrentCartSpeedCapOnRail());
            cart.setCurrentCartSpeedCapOnRail(SpeedController.REINFORCED_MAX_SPEED);
        });
    }

    /**
     * Produces the motion that vanilla must receive at the start of the next
     * tick so that movement after its slope and rider adjustments is fixed at
     * {@code targetSpeed} in either direction.
     */
    static Vec2D prepareMotion(Vec2D motion, BlockRailBase.EnumRailDirection slope,
                               double targetSpeed, double slopeAdjustment, double movementScale) {
        double axisMotion;
        double slopeDelta;
        boolean xAxis;

        switch (slope) {
            case ASCENDING_EAST:
                axisMotion = motion.getX();
                slopeDelta = -slopeAdjustment;
                xAxis = true;
                break;
            case ASCENDING_WEST:
                axisMotion = motion.getX();
                slopeDelta = slopeAdjustment;
                xAxis = true;
                break;
            case ASCENDING_NORTH:
                axisMotion = motion.getY();
                slopeDelta = slopeAdjustment;
                xAxis = false;
                break;
            case ASCENDING_SOUTH:
                axisMotion = motion.getY();
                slopeDelta = -slopeAdjustment;
                xAxis = false;
                break;
            default:
                return motion;
        }

        if (Math.abs(axisMotion) < MIN_MOTION)
            return motion;

        double speedBeforeRiderAdjustment = targetSpeed / movementScale;
        double preparedAxisMotion = Math.copySign(speedBeforeRiderAdjustment, axisMotion) - slopeDelta;
        return xAxis ? new Vec2D(preparedAxisMotion, 0.0) : new Vec2D(0.0, preparedAxisMotion);
    }
}
