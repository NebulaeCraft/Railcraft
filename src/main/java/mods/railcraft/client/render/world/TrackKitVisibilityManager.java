/*------------------------------------------------------------------------------
 Copyright (c) CovertJaguar, 2011-2026
 http://railcraft.info

 This code is the property of CovertJaguar
 and may only be used with explicit written
 permission unless otherwise specified on the
 license page at http://railcraft.info/wiki/info:license.
 -----------------------------------------------------------------------------*/
package mods.railcraft.client.render.world;

import mods.railcraft.common.items.ItemGoggles;
import mods.railcraft.common.items.ItemGoggles.GoggleAura;
import mods.railcraft.common.util.misc.Game;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Tracks whether the local player can inspect normally hidden track kits and
 * refreshes chunk models when that visibility changes.
 */
@SideOnly(Side.CLIENT)
public class TrackKitVisibilityManager {
    public static final TrackKitVisibilityManager INSTANCE = new TrackKitVisibilityManager();
    private static volatile boolean trackAuraActive;

    private TrackKitVisibilityManager() {
    }

    public static boolean isTrackAuraActive() {
        return trackAuraActive;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;

        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.player;
        boolean active = player != null
                && ItemGoggles.getCurrentAura(ItemGoggles.getGoggles(player)) == GoggleAura.TRACK;
        if (active != trackAuraActive) {
            trackAuraActive = active;
            if (minecraft.renderGlobal != null)
                minecraft.renderGlobal.loadRenderers();
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (Game.isClient(event.getWorld()))
            trackAuraActive = false;
    }
}
