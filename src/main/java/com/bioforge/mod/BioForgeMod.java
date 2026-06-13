package com.bioforge.mod;

import com.bioforge.mod.cloning.CloneEntity;
import com.bioforge.mod.bodyexit.BodyShellEntity;
import com.bioforge.mod.bodyexit.ModBodyEntities;
import com.bioforge.mod.registry.*;
import com.bioforge.mod.registry.ModBlockEntities;
import com.bioforge.mod.config.BioForgeConfig;
import com.bioforge.mod.network.BioForgeNetwork;
import com.bioforge.mod.capabilities.BioForgeCapabilities;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(BioForgeMod.MODID)
public class BioForgeMod {

    public static final String MODID = "bioforge";
    public static final String VERSION = "0.1.0-alpha";
    public static final String BUILD_DATE = "2026-06-11";

    private static final Logger LOGGER = LogUtils.getLogger();

    public BioForgeMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Deferred registries
        ModItems.ITEMS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);
        ModMenuTypes.MENUS.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        ModParticles.PARTICLES.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModBodyEntities.ENTITIES.register(modEventBus);   // body shell entity
        ModCreativeTabs.TABS.register(modEventBus);
        ModRecipes.RECIPE_TYPES.register(modEventBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);

        // Entity attributes — must be registered on the mod event bus
        modEventBus.addListener(this::onEntityAttributeCreation);

        modEventBus.addListener(this::commonSetup);

        // Capabilities
        BioForgeCapabilities.register(modEventBus);

        // Forge bus events (BioForgeEvents, BioForgeCommand, etc. via @EventBusSubscriber)
        MinecraftForge.EVENT_BUS.register(this);

        // Config
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, BioForgeConfig.COMMON_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, BioForgeConfig.CLIENT_SPEC);

        LOGGER.info("[BioForge] Loaded v{} ({})", VERSION, BUILD_DATE);
    }

    private void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.CLONE.get(), CloneEntity.createAttributes().build());
        event.put(ModBodyEntities.BODY_SHELL.get(), BodyShellEntity.createAttributes().build());
        LOGGER.info("[BioForge] Entity attributes registered.");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BioForgeNetwork.register();
            LOGGER.info("[BioForge] Common setup complete — v{}", VERSION);
        });
    }
}
