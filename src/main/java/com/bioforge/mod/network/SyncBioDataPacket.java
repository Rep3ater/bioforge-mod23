package com.bioforge.mod.network;

import com.bioforge.mod.capabilities.BioForgeCapabilities;
import com.bioforge.mod.capabilities.PlayerBioData;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent by the server to sync a player's full BioData to their client.
 * Triggered after any surgical procedure, login, or respawn.
 */
public class SyncBioDataPacket {

    private final CompoundTag nbt;

    public SyncBioDataPacket(PlayerBioData data) {
        this.nbt = data.serializeNBT();
    }

    public SyncBioDataPacket(CompoundTag nbt) {
        this.nbt = nbt;
    }

    public static void encode(SyncBioDataPacket packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.nbt);
    }

    public static SyncBioDataPacket decode(FriendlyByteBuf buf) {
        return new SyncBioDataPacket(buf.readNbt());
    }

    public static void handle(SyncBioDataPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(packet))
        );
        ctx.get().setPacketHandled(true);
    }

    private static void handleClient(SyncBioDataPacket packet) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.getCapability(BioForgeCapabilities.PLAYER_BIO_DATA).ifPresent(data -> {
                data.deserializeNBT(packet.nbt);
            });
        }
    }
}
