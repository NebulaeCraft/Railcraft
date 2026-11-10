package mods.railcraft.common.blocks.tracks.behaivor;

import mods.railcraft.common.carts.EntityLocomotiveCreative;
import mods.railcraft.common.plugins.forge.DataManagerPlugin;

class SpeedTestLocomotive extends EntityLocomotiveCreative {
    static {
        DataManagerPlugin.register();
    }

    private final boolean running;

    SpeedTestLocomotive(LocoSpeed speed, boolean running) {
        super(null);
        setSpeed(speed);
        this.running = running;
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
