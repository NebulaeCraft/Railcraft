/*------------------------------------------------------------------------------
 Copyright (c) CovertJaguar, 2011-2022
 http://railcraft.info

 This code is the property of CovertJaguar
 and may only be used with explicit written
 permission unless otherwise specified on the
 license page at http://railcraft.info/wiki/info:license.
 -----------------------------------------------------------------------------*/
package mods.railcraft.client.gui;

import mods.railcraft.common.blocks.RailcraftBlocks;
import mods.railcraft.common.core.RailcraftConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiContainerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Draws a warning over multiblock items without altering their world textures. */
@SideOnly(Side.CLIENT)
public enum HighRiskItemOverlay {
    INSTANCE;

    private static final ResourceLocation WARNING_TEXTURE =
            new ResourceLocation(RailcraftConstants.RESOURCE_DOMAIN, "textures/gui/multiblock_high_risk.png");
    private static final int SOURCE_SIZE = 220;
    private static final int OVERLAY_SIZE = 8;

    @SubscribeEvent
    public void draw(GuiContainerEvent.DrawForeground event) {
        boolean hasWarning = event.getGuiContainer().inventorySlots.inventorySlots.stream()
                .anyMatch(this::isHighRiskSlot);
        if (!hasWarning)
            return;

        Minecraft.getMinecraft().getTextureManager().bindTexture(WARNING_TEXTURE);
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, 0.0F, 300.0F);
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        for (Slot slot : event.getGuiContainer().inventorySlots.inventorySlots) {
            if (isHighRiskSlot(slot)) {
                Gui.drawScaledCustomSizeModalRect(
                        slot.xPos + OVERLAY_SIZE,
                        slot.yPos + OVERLAY_SIZE,
                        0.0F,
                        0.0F,
                        SOURCE_SIZE,
                        SOURCE_SIZE,
                        OVERLAY_SIZE,
                        OVERLAY_SIZE,
                        SOURCE_SIZE,
                        SOURCE_SIZE
                );
            }
        }

        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    private boolean isHighRiskSlot(Slot slot) {
        return slot.isEnabled() && slot.getHasStack() && isHighRisk(slot.getStack());
    }

    private boolean isHighRisk(ItemStack stack) {
        return stack.getItem() instanceof ItemBlock
                && RailcraftBlocks.isHighRiskMultiblock(((ItemBlock) stack.getItem()).getBlock());
    }
}
