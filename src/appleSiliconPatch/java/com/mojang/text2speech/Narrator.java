package com.mojang.text2speech;

/**
 * Apple Silicon development replacement for Minecraft 1.12.2's narrator.
 * The original macOS implementation embeds an Intel-only native library.
 */
public interface Narrator {
    void say(String text);

    void clear();

    boolean active();

    static Narrator getNarrator() {
        return new Narrator() {
            @Override
            public void say(String text) {
            }

            @Override
            public void clear() {
            }

            @Override
            public boolean active() {
                return false;
            }
        };
    }
}
