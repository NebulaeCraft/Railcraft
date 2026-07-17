package mods.railcraft.common.plugins.forge;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NBTPluginTest {

    private enum Value {
        FIRST, SECOND
    }

    @Test
    void ignoresTruncatedBlockPosition() {
        NBTTagCompound data = new NBTTagCompound();
        data.setIntArray("pos", new int[]{1, 2});

        assertNull(NBTPlugin.readBlockPos(data, "pos"));
    }

    @Test
    void readsCompleteBlockPosition() {
        NBTTagCompound data = new NBTTagCompound();
        data.setIntArray("pos", new int[]{1, 2, 3});

        assertEquals(new BlockPos(1, 2, 3), NBTPlugin.readBlockPos(data, "pos"));
    }

    @Test
    void fallsBackForInvalidEnums() {
        NBTTagCompound data = new NBTTagCompound();
        data.setByte("ordinal", (byte) -1);
        data.setString("name", "NOT_A_VALUE");

        assertEquals(Value.FIRST, NBTPlugin.readEnumOrdinal(data, "ordinal", Value.values(), Value.FIRST));
        assertEquals(Value.SECOND, NBTPlugin.readEnumName(data, "name", Value.SECOND));
    }
}
