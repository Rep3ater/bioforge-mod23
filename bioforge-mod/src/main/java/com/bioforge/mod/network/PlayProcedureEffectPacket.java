package com.bioforge.mod.network;

import com.bioforge.mod.client.fx.ProcedureFX;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Tells clients to spawn the visual + sound FX for a given surgical procedure.
 * Separate from sync so effects play even for nearby players watching.
 */
public class PlayProcedureEffectPacket {

    public enum EffectType {
        BLOOD_DRAW,
        INCISION,
        BONE_SAW,
        CAUTERIZE,
        ORGAN_REMOVE,
        ORGAN_IMPLANT,
        LIMB_REMOVE,
        LIMB_ATTACH,
        CLONE_SPAWN,
        SEDATION,
        TRANSFUSION,
        STITCH
    }

    private final EffectType effectType;
    private final double x, y, z;
    private final int entityId;

    public PlayProcedureEffectPacket(EffectType effectType, Vec3 pos, int entityId) {
        this.effectType = effectType;
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
        this.entityId = entityId;
    }

    public static void encode(PlayProcedureEffectPacket p, FriendlyByteBuf buf) {
        buf.writeEnum(p.effectType);
        buf.writeDouble(p.x);
        buf.writeDouble(p.y);
        buf.writeDouble(p.z);
        buf.writeInt(p.entityId);
    }

    public static PlayProcedureEffectPacket decode(FriendlyByteBuf buf) {
        EffectType type = buf.readEnum(EffectType.class);
        double x = buf.readDouble(), y = buf.readDouble(), z = buf.readDouble();
        int eid = buf.readInt();
        return new PlayProcedureEffectPacket(type, new Vec3(x, y, z), eid);
    }

    public static void handle(PlayProcedureEffectPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ProcedureFX.play(packet.effectType, new Vec3(packet.x, packet.y, packet.z), packet.entityId))
        );
        ctx.get().setPacketHandled(true);
    }
}
