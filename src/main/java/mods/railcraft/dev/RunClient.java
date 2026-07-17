/*------------------------------------------------------------------------------
 Copyright (c) CovertJaguar, 2011-2020
 http://railcraft.info

 This code is the property of CovertJaguar
 and may only be used with explicit written
 permission unless otherwise specified on the
 license page at http://railcraft.info/wiki/info:license.
 -----------------------------------------------------------------------------*/
package mods.railcraft.dev;

import java.lang.reflect.InvocationTargetException;

/** IntelliJ-visible bridge to ForgeGradle's generated client launcher. */
public final class RunClient {
    private RunClient() {
    }

    public static void main(String[] args) throws Throwable {
        try {
            Class.forName("GradleStart").getMethod("main", String[].class).invoke(null, (Object) args);
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }
}
