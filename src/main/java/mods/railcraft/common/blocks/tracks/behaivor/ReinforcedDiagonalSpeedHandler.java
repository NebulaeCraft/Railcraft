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
import mods.railcraft.common.blocks.tracks.TrackShapeHelper;
import mods.railcraft.common.blocks.tracks.TrackTools;
import mods.railcraft.common.carts.EntityCartBasic;
import mods.railcraft.common.carts.EntityLocomotive;
import mods.railcraft.common.carts.Train;
import mods.railcraft.common.util.misc.Vec2D;
import net.minecraft.block.BlockRailBase;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.minecart.MinecartUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Keeps the horizontal vector speed on reinforced diagonal track at the
 * reinforced-track target instead of applying that target to both axes.
 *
 * <p>A Minecraft corner rail contains a 45-degree diagonal path. This also
 * covers long diagonal lines assembled from alternating corner rails. Only a
 * train with a running locomotive has its speed maintained; unpowered carts
 * are capped but retain normal drag.</p>
 */
public final class ReinforcedDiagonalSpeedHandler {
    public static final ReinforcedDiagonalSpeedHandler INSTANCE = new ReinforcedDiagonalSpeedHandler();
    private static final double MIN_MOTION = 1.0E-6;
    private static final double VANILLA_RIDER_MOVEMENT_SCALE = 0.75;

    private final Map<World, Set<EntityMinecart>> pendingCarts = new MapMaker().weakKeys().makeMap();
    private final Map<EntityMinecart, Float> overriddenSpeedCaps = new MapMaker().weakKeys().makeMap();

    private ReinforcedDiagonalSpeedHandler() {
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
        if (isDiagonal(direction))
            pendingCarts.computeIfAbsent(cart.world,
                    world -> Collections.newSetFromMap(new IdentityHashMap<>())).add(cart);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;

        Set<EntityMinecart> carts = pendingCarts.remove(event.world);
        if (carts == null || carts.isEmpty())
            return;

        carts.forEach(cart -> {
            if (cart.isDead)
                return;

            boolean maintainSpeed = hasRunningLocomotive(cart);
            double movementScale = cart.isBeingRidden() && !(cart instanceof EntityCartBasic)
                    ? VANILLA_RIDER_MOVEMENT_SCALE : 1.0;
            Vec2D preparedMotion = prepareMotion(
                    new Vec2D(cart.motionX, cart.motionZ),
                    SpeedController.REINFORCED_MAX_SPEED,
                    movementScale,
                    maintainSpeed);
            cart.motionX = preparedMotion.getX();
            cart.motionZ = preparedMotion.getY();

            if (maintainSpeed) {
                overriddenSpeedCaps.put(cart, cart.getCurrentCartSpeedCapOnRail());
                cart.setCurrentCartSpeedCapOnRail(SpeedController.REINFORCED_MAX_SPEED);
            }
        });
    }

    /**
     * Checks the whole linked train so the correction is independent of
     * whether the locomotive is pulling from the front or pushing from behind.
     */
    private static boolean hasRunningLocomotive(EntityMinecart cart) {
        return Train.streamCarts(cart).anyMatch(trainCart ->
                trainCart instanceof EntityLocomotive && ((EntityLocomotive) trainCart).isRunning());
    }

    static boolean isDiagonal(BlockRailBase.EnumRailDirection direction) {
        return TrackShapeHelper.isTurn(direction);
    }

    /**
     * Produces a vector whose magnitude becomes {@code targetSpeed} after the
     * rider movement multiplier is applied by vanilla minecart movement. An
     * unpowered cart is only reduced when it exceeds the target; lower speeds
     * are left untouched so normal drag can bring the cart to a stop.
     */
    static Vec2D prepareMotion(Vec2D motion, double targetSpeed, double movementScale, boolean maintainSpeed) {
        double magnitude = motion.magnitude();
        if (magnitude < MIN_MOTION)
            return motion;

        double preparedMagnitude = targetSpeed / movementScale;
        if (!maintainSpeed && magnitude <= preparedMagnitude)
            return motion;

        double scale = preparedMagnitude / magnitude;
        return new Vec2D(motion.getX() * scale, motion.getY() * scale);
    }
}
