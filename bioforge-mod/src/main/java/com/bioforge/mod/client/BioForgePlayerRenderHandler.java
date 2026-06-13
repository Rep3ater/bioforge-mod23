package com.bioforge.mod.client;

import com.bioforge.mod.BioForgeMod;
import com.bioforge.mod.capabilities.BioForgeCapabilities;
import com.bioforge.mod.capabilities.PlayerBioData;
import com.bioforge.mod.core.LimbState;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Hides absent limbs BEFORE the base player model renders.
 * LimbModelLayer then draws the prosthetic/cybernetic overlay on top.
 *
 * Post event restores visibility so other mods / the next frame start clean.
 */
@Mod.EventBusSubscriber(modid = BioForgeMod.MODID, value = Dist.CLIENT)
public class BioForgePlayerRenderHandler {

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        PlayerBioData bio = BioForgeCapabilities.get(player);
        PlayerRenderer r = event.getRenderer();

        r.getModel().leftArm.visible    = bio.getLimb("left_arm").isPresent();
        r.getModel().leftSleeve.visible = bio.getLimb("left_arm").isPresent();
        r.getModel().rightArm.visible   = bio.getLimb("right_arm").isPresent();
        r.getModel().rightSleeve.visible= bio.getLimb("right_arm").isPresent();
        r.getModel().leftLeg.visible    = bio.getLimb("left_leg").isPresent();
        r.getModel().leftPants.visible  = bio.getLimb("left_leg").isPresent();
        r.getModel().rightLeg.visible   = bio.getLimb("right_leg").isPresent();
        r.getModel().rightPants.visible = bio.getLimb("right_leg").isPresent();
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        // Restore all limbs to visible so the model is clean for the next render pass
        PlayerRenderer r = event.getRenderer();
        r.getModel().leftArm.visible     = true;
        r.getModel().leftSleeve.visible  = true;
        r.getModel().rightArm.visible    = true;
        r.getModel().rightSleeve.visible = true;
        r.getModel().leftLeg.visible     = true;
        r.getModel().leftPants.visible   = true;
        r.getModel().rightLeg.visible    = true;
        r.getModel().rightPants.visible  = true;
    }
}
