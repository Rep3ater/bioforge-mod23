package com.bioforge.mod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

// ─── ShowProcedureGuiPacket ──────────────────────────────────────────────────
// Tells the client which surgical GUI to open (sent by server after
// right-clicking on a surgical block with a valid target player nearby).

class ShowProcedureGuiPacket {
    private final String guiType; // e.g. "limb_surgery", "organ_transplant"
    private final int targetEntityId;

    ShowProcedureGuiPacket(String guiType, int targetEntityId) {
        this.guiType = guiType;
        this.targetEntityId = targetEntityId;
    }

    static void encode(ShowProcedureGuiPacket p, FriendlyByteBuf buf) {
        buf.writeUtf(p.guiType);
        buf.writeInt(p.targetEntityId);
    }

    static ShowProcedureGuiPacket decode(FriendlyByteBuf buf) {
        return new ShowProcedureGuiPacket(buf.readUtf(), buf.readInt());
    }

    static void handle(ShowProcedureGuiPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // TODO: open specific GUI screen on client
        });
        ctx.get().setPacketHandled(true);
    }
}

// ─── UpdateLimbModelPacket ───────────────────────────────────────────────────
// Tells clients to refresh the limb model renderer for a given entity.

class UpdateLimbModelPacket {
    private final int entityId;
    private final String slotName;
    private final String modelOverride;

    UpdateLimbModelPacket(int entityId, String slotName, String modelOverride) {
        this.entityId = entityId;
        this.slotName = slotName;
        this.modelOverride = modelOverride;
    }

    static void encode(UpdateLimbModelPacket p, FriendlyByteBuf buf) {
        buf.writeInt(p.entityId);
        buf.writeUtf(p.slotName);
        buf.writeUtf(p.modelOverride);
    }

    static UpdateLimbModelPacket decode(FriendlyByteBuf buf) {
        return new UpdateLimbModelPacket(buf.readInt(), buf.readUtf(), buf.readUtf());
    }

    static void handle(UpdateLimbModelPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // TODO: invalidate/reload limb model cache for entity
        });
        ctx.get().setPacketHandled(true);
    }
}

// ─── BloodSplatterEffectPacket ───────────────────────────────────────────────
// Spawns blood particle effects at a location visible to all nearby clients.

class BloodSplatterEffectPacket {
    private final double x, y, z;
    private final int count;
    private final float intensity;

    BloodSplatterEffectPacket(double x, double y, double z, int count, float intensity) {
        this.x = x; this.y = y; this.z = z;
        this.count = count;
        this.intensity = intensity;
    }

    static void encode(BloodSplatterEffectPacket p, FriendlyByteBuf buf) {
        buf.writeDouble(p.x); buf.writeDouble(p.y); buf.writeDouble(p.z);
        buf.writeInt(p.count);
        buf.writeFloat(p.intensity);
    }

    static BloodSplatterEffectPacket decode(FriendlyByteBuf buf) {
        return new BloodSplatterEffectPacket(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readInt(), buf.readFloat());
    }

    static void handle(BloodSplatterEffectPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // TODO: spawn blood particle cluster on client
        });
        ctx.get().setPacketHandled(true);
    }
}

// ─── RequestProcedurePacket ──────────────────────────────────────────────────
// Client -> Server: player requested to begin a procedure on a target.

class RequestProcedurePacket {
    private final String procedureId;
    private final int targetEntityId;
    private final net.minecraft.nbt.CompoundTag extraData;

    RequestProcedurePacket(String procedureId, int targetEntityId, net.minecraft.nbt.CompoundTag extraData) {
        this.procedureId = procedureId;
        this.targetEntityId = targetEntityId;
        this.extraData = extraData;
    }

    static void encode(RequestProcedurePacket p, FriendlyByteBuf buf) {
        buf.writeUtf(p.procedureId);
        buf.writeInt(p.targetEntityId);
        buf.writeNbt(p.extraData);
    }

    static RequestProcedurePacket decode(FriendlyByteBuf buf) {
        return new RequestProcedurePacket(buf.readUtf(), buf.readInt(), buf.readNbt());
    }

    static void handle(RequestProcedurePacket p, Supplier<NetworkEvent.Context> ctx) {
        net.minecraft.world.entity.player.Player sender = ctx.get().getSender();
        ctx.get().enqueueWork(() -> {
            if (sender != null) {
                com.bioforge.mod.procedures.ProcedureRegistry.execute(p.procedureId, sender, p.targetEntityId, p.extraData);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

// ─── ConfirmProcedurePacket ──────────────────────────────────────────────────
// Client -> Server: the patient has confirmed/consented to a procedure.

class ConfirmProcedurePacket {
    private final String procedureId;
    private final boolean confirmed;

    ConfirmProcedurePacket(String procedureId, boolean confirmed) {
        this.procedureId = procedureId;
        this.confirmed = confirmed;
    }

    static void encode(ConfirmProcedurePacket p, FriendlyByteBuf buf) {
        buf.writeUtf(p.procedureId);
        buf.writeBoolean(p.confirmed);
    }

    static ConfirmProcedurePacket decode(FriendlyByteBuf buf) {
        return new ConfirmProcedurePacket(buf.readUtf(), buf.readBoolean());
    }

    static void handle(ConfirmProcedurePacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // TODO: process patient consent on server
        });
        ctx.get().setPacketHandled(true);
    }
}
