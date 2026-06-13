package com.bioforge.mod.client;

import com.bioforge.mod.BioForgeMod;
import com.bioforge.mod.bodyexit.ModBodyEntities;
import com.bioforge.mod.client.fx.BioForgeSounds;
import com.bioforge.mod.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod.EventBusSubscriber(modid = BioForgeMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BioForgeClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(BioForgeSounds::init);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CLONE.get(), CloneEntityRenderer::new);
        event.registerEntityRenderer(ModBodyEntities.BODY_SHELL.get(), BodyShellRenderer::new);
    }
}
