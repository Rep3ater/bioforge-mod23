package com.bioforge.mod.capabilities;

import com.bioforge.mod.BioForgeMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;

public class BioForgeCapabilities {

    public static final Capability<PlayerBioData> PLAYER_BIO_DATA =
            CapabilityManager.get(new CapabilityToken<>() {});

    public static final ResourceLocation BIO_DATA_KEY =
            new ResourceLocation(BioForgeMod.MODID, "bio_data");

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(BioForgeCapabilities::onRegisterCapabilities);
        MinecraftForge.EVENT_BUS.register(BioForgeCapabilities.class);
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PlayerBioData.class);
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<net.minecraft.world.entity.Entity> event) {
        if (event.getObject() instanceof LivingEntity living && !(living instanceof net.minecraft.world.entity.animal.Animal) && !(living instanceof net.minecraft.world.entity.monster.Monster)) {
            if (!living.getCapability(PLAYER_BIO_DATA).isPresent()) {
                PlayerBioDataProvider provider = new PlayerBioDataProvider();
                event.addCapability(BIO_DATA_KEY, provider);
            }
        }
    }

    /**
     * Safe helper — gets BioData or returns a transient default for a LivingEntity.
     */
    public static PlayerBioData get(LivingEntity living) {
        return living.getCapability(PLAYER_BIO_DATA).orElseGet(() -> {
            PlayerBioData fallback = new PlayerBioData();
            fallback.initDefaultLimbs();
            return fallback;
        });
    }
}
