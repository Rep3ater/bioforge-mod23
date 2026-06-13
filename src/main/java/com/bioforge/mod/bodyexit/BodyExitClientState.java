package com.bioforge.mod.bodyexit;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Client-side singleton tracking projection state.
 * Updated by BodyExitPacket when the server confirms entry/exit.
 */
@OnlyIn(Dist.CLIENT)
public class BodyExitClientState {

    private static boolean projecting = false;
    private static int shellEntityId = -1;

    public static boolean isProjecting() { return projecting; }
    public static int getShellEntityId() { return shellEntityId; }

    public static void setProjecting(boolean active, int entityId) {
        projecting = active;
        shellEntityId = entityId;
    }
}
