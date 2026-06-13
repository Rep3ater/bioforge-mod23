package com.bioforge.mod.client;

import com.bioforge.mod.BioForgeMod;
import com.bioforge.mod.bodyexit.BodyShellEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * BodyShellRenderer — renders an abandoned player body using the humanoid
 * biped model. Defaults to Steve skin; the owner's actual skin is fetched
 * via Minecraft's skin cache when available.
 *
 * The body is rendered slightly desaturated (via pose-stack alpha) to
 * visually distinguish it from a living entity.
 *
 * ModelLayers.ZOMBIE gives us a standard HumanoidModel with correct
 * proportions (same as a player) without needing a PlayerModel,
 * which is harder to instantiate outside of the player renderer.
 */
public class BodyShellRenderer extends HumanoidMobRenderer<BodyShellEntity, HumanoidModel<BodyShellEntity>> {

    /** Fallback — classic Steve skin used when the player skin isn't cached yet. */
    private static final ResourceLocation STEVE_SKIN =
            ResourceLocation.parse("minecraft:textures/entity/player/wide/steve.png");
    private static final ResourceLocation ALEX_SKIN =
            ResourceLocation.parse("minecraft:textures/entity/player/slim/alex.png");

    public BodyShellRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(BodyShellEntity entity) {
        // Try to use the actual player skin from Minecraft's skin manager
        java.util.UUID ownerUuid = entity.getOwnerUuid();
        if (ownerUuid != null) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            // Check if this is the local player
            if (mc.player != null && mc.player.getUUID().equals(ownerUuid)) {
                // Use local player skin
                com.mojang.authlib.GameProfile profile = mc.player.getGameProfile();
                ResourceLocation skin = mc.getSkinManager().getInsecureSkinLocation(profile);
                if (skin != null) return skin;
            }
            // For other players: look up in level
            if (mc.level != null) {
                net.minecraft.world.entity.player.Player found =
                        mc.level.getPlayerByUUID(ownerUuid);
                if (found != null) {
                    com.mojang.authlib.GameProfile profile = found.getGameProfile();
                    ResourceLocation skin = mc.getSkinManager().getInsecureSkinLocation(profile);
                    if (skin != null) return skin;
                }
            }
        }
        // Fallback to steve/alex based on slim model flag
        return entity.isSlimModel() ? ALEX_SKIN : STEVE_SKIN;
    }

    @Override
    protected void scale(BodyShellEntity entity, PoseStack poseStack, float partialTick) {
        // Scale to exactly player proportions
        poseStack.scale(1.0f, 1.0f, 1.0f);
    }

    @Override
    protected float getBob(BodyShellEntity entity, float partialTick) {
        // No arm-swing — body is inert
        return 0f;
    }
}
