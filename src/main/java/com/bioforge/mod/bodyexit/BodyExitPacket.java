package com.bioforge.mod.bodyexit;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server → Client packet: notifies the client whether the local player is
 * currently projecting, and which entity ID is their body shell.
 *
 * On enter: shellEntityId ≥ 0, projecting = true
 * On exit:  shellEntityId = -1, projecting = false
 */
public class BodyExitPacket {

    private final int shellEntityId;
    private final boolean projecting;

    public BodyExitPacket(int shellEntityId, boolean projecting) {
        this.shellEntityId = shellEntityId;
        this.projecting = projecting;
    }

    public static void encode(BodyExitPacket p, FriendlyByteBuf buf) {
        buf.writeInt(p.shellEntityId);
        buf.writeBoolean(p.projecting);
    }

    public static BodyExitPacket decode(FriendlyByteBuf buf) {
        return new BodyExitPacket(buf.readInt(), buf.readBoolean());
    }

    public static void handle(BodyExitPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            BodyExitClientState.setProjecting(p.projecting, p.shellEntityId);
        });
        ctx.get().setPacketHandled(true);
    }
}
