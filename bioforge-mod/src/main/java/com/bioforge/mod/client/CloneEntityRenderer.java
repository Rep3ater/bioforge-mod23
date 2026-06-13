package com.bioforge.mod.client;

import com.bioforge.mod.BioForgeMod;
import com.bioforge.mod.cloning.CloneEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * CloneEntity renderer.
 *
 * Uses HumanoidMobRenderer with a standard humanoid model (zombie geometry —
 * same proportions as a player). Textured with the developer-provided template
 * skin at textures/entity/clone/template_skin.png.
 *
 * Higher clone generations wobble slightly due to instability.
 *
 * ModelLayers.ZOMBIE is used rather than PLAYER because HumanoidMobRenderer
 * expects a HumanoidModel, not PlayerModel, and ZOMBIE is the canonical
 * HumanoidModel layer present in all 1.20.1 versions.
 */
public class CloneEntityRenderer extends HumanoidMobRenderer<CloneEntity, HumanoidModel<CloneEntity>> {

    private static final ResourceLocation TEMPLATE_SKIN =
            ResourceLocation.fromNamespaceAndPath(BioForgeMod.MODID, "textures/entity/clone/template_skin.png");

    public CloneEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(CloneEntity entity) {
        return TEMPLATE_SKIN;
    }

    @Override
    protected void scale(CloneEntity entity, PoseStack poseStack, float partialTick) {
        int gen = entity.getCloneGeneration();
        if (gen >= 4) {
            float t = entity.tickCount * 0.2f + partialTick;
            float wobble = 1.0f + (float) Math.sin(t) * 0.015f * (gen - 3);
            poseStack.scale(wobble, wobble, wobble);
        }
    }
}
