package com.bioforge.mod.network;

import com.bioforge.mod.BioForgeMod;
import com.bioforge.mod.bodyexit.BodyExitPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class BioForgeNetwork {

    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(BioForgeMod.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private static int id = 0;

    public static void register() {
        // Server -> Client
        CHANNEL.registerMessage(id++, SyncBioDataPacket.class,
                SyncBioDataPacket::encode, SyncBioDataPacket::decode, SyncBioDataPacket::handle);
        CHANNEL.registerMessage(id++, PlayProcedureEffectPacket.class,
                PlayProcedureEffectPacket::encode, PlayProcedureEffectPacket::decode, PlayProcedureEffectPacket::handle);
        CHANNEL.registerMessage(id++, ShowProcedureGuiPacket.class,
                ShowProcedureGuiPacket::encode, ShowProcedureGuiPacket::decode, ShowProcedureGuiPacket::handle);
        CHANNEL.registerMessage(id++, UpdateLimbModelPacket.class,
                UpdateLimbModelPacket::encode, UpdateLimbModelPacket::decode, UpdateLimbModelPacket::handle);
        CHANNEL.registerMessage(id++, BloodSplatterEffectPacket.class,
                BloodSplatterEffectPacket::encode, BloodSplatterEffectPacket::decode, BloodSplatterEffectPacket::handle);
        CHANNEL.registerMessage(id++, BodyExitPacket.class,
                BodyExitPacket::encode, BodyExitPacket::decode, BodyExitPacket::handle);

        // Client -> Server
        CHANNEL.registerMessage(id++, RequestProcedurePacket.class,
                RequestProcedurePacket::encode, RequestProcedurePacket::decode, RequestProcedurePacket::handle);
        CHANNEL.registerMessage(id++, ConfirmProcedurePacket.class,
                ConfirmProcedurePacket::encode, ConfirmProcedurePacket::decode, ConfirmProcedurePacket::handle);
    }
}
