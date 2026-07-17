package mods.railcraft.common.util.network;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RailcraftInputStreamTest {

    private enum Value {
        FIRST, SECOND
    }

    @Test
    void readsValidEnum() throws IOException {
        try (RailcraftInputStream input = stream(1)) {
            assertEquals(Value.SECOND, input.readEnum(Value.values()));
        }
    }

    @Test
    void rejectsInvalidEnumAsIoError() throws IOException {
        try (RailcraftInputStream input = stream(255)) {
            assertThrows(IOException.class, () -> input.readEnum(Value.values()));
        }
    }

    private static RailcraftInputStream stream(int value) {
        return new RailcraftInputStream(new ByteArrayInputStream(new byte[]{(byte) value}));
    }
}
