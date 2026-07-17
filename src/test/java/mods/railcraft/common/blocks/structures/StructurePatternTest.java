package mods.railcraft.common.blocks.structures;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructurePatternTest {

    private final StructurePattern pattern = new StructurePattern(new char[][][]{
            {
                    {'A', 'B'},
                    {'C', 'D'}
            }
    }, 0, 0, 0);

    @Test
    void readsMarkersInsidePattern() {
        assertEquals('A', pattern.getPatternMarker(0, 0, 0));
        assertEquals('D', pattern.getPatternMarker(new BlockPos(1, 0, 1)));
        assertTrue(pattern.contains(BlockPos.ORIGIN));
    }

    @Test
    void treatsEveryOutsidePositionAsEmpty() {
        for (EnumFacing facing : EnumFacing.VALUES) {
            BlockPos outside = BlockPos.ORIGIN.offset(facing);
            if (pattern.contains(outside))
                continue;
            assertEquals(StructurePattern.EMPTY_MARKER, pattern.getPatternMarker(outside));
            assertFalse(pattern.contains(outside));
        }
        assertEquals(StructurePattern.EMPTY_MARKER, pattern.getPatternMarker(2, 0, 0));
        assertEquals(StructurePattern.EMPTY_MARKER, pattern.getPatternMarker(0, 1, 0));
        assertEquals(StructurePattern.EMPTY_MARKER, pattern.getPatternMarker(0, 0, 2));
    }
}
