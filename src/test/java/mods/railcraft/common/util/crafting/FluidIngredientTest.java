package mods.railcraft.common.util.crafting;

import mods.railcraft.common.fluids.FluidContainerHandler;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.crafting.Ingredient;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FluidIngredientTest {

    @Test
    void missingForgeFluidCapabilitySkipsFluidRecipesWithoutScanningItems() {
        Bootstrap.register();
        Capability<IFluidHandlerItem> original = CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY;
        try {
            CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY = null;
            FluidStack fluidStack = new FluidStack(FluidRegistry.WATER, Fluid.BUCKET_VOLUME);

            assertSame(Ingredient.EMPTY, Ingredients.from(fluidStack));
            assertTrue(FluidContainerHandler.INSTANCE.findCanDrain(fluidStack).isEmpty());
            assertTrue(new FluidIngredient(fluidStack).getMatchingStacks().length == 0);
        } finally {
            CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY = original;
        }
    }
}
