package com.bioforge.mod.client;

import com.bioforge.mod.BioForgeMod;
import com.bioforge.mod.capabilities.BioForgeCapabilities;
import com.bioforge.mod.capabilities.PlayerBioData;
import com.bioforge.mod.core.LimbState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Render layer for prosthetic and cybernetic limb overlays.
 *
 * VISIBILITY FIX:
 * BioForgePlayerRenderHandler.Pre already hides absent limbs before the base
 * player model renders. By the time this layer runs, those parts are invisible.
 * To render an overlay on a prosthetic/cybernetic limb we must:
 *   1. Temporarily restore the model part to visible
 *   2. Render the tinted overlay with the replacement texture
 *   3. Re-hide it so nothing else accidentally shows it
 *
 * For ABSENT limbs: skip entirely — nothing should render there.
 * For BIOLOGICAL: skip — vanilla skin handles it.
 * For PROSTHETIC: grey tinted overlay on the existing model part geometry.
 * For CYBERNETIC: cyan tinted overlay.
 *
 * This means prosthetics/cybernetics always show — they replace the limb visually
 * by drawing on the same bone with a different texture and tint.
 */
@Mod.EventBusSubscriber(
        modid = BioForgeMod.MODID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public class LimbModelLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final ResourceLocation PROSTHETIC_OVERLAY =
            ResourceLocation.fromNamespaceAndPath(BioForgeMod.MODID, "textures/entity/limb/prosthetic_overlay.png");
    private static final ResourceLocation CYBERNETIC_OVERLAY =
            ResourceLocation.fromNamespaceAndPath(BioForgeMod.MODID, "textures/entity/limb/cybernetic_overlay.png");

    public LimbModelLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {

        PlayerBioData bio = BioForgeCapabilities.get(player);
        PlayerModel<AbstractClientPlayer> model = getParentModel();

        for (String slot : new String[]{"left_arm", "right_arm", "left_leg", "right_leg"}) {
            LimbState state = bio.getLimb(slot);

            // Skip absent limbs and natural biological limbs — nothing to overlay
            if (!state.isPresent()) continue;
            if (state.getType() == LimbState.LimbType.BIOLOGICAL) continue;

            ResourceLocation tex = switch (state.getType()) {
                case PROSTHETIC -> PROSTHETIC_OVERLAY;
                case CYBERNETIC -> CYBERNETIC_OVERLAY;
                default -> null;
            };
            if (tex == null) continue;

            float[] c = (state.getType() == LimbState.LimbType.CYBERNETIC)
                    ? new float[]{0.3f, 0.9f, 1.0f, 0.85f}
                    : new float[]{0.65f, 0.65f, 0.68f, 0.90f};

            var vc = bufferSource.getBuffer(RenderType.entityTranslucent(tex));

            // Temporarily force the model part visible so the render call works,
            // then restore its visibility (BioForgePlayerRenderHandler already hid it
            // correctly for the base pass; we only need it visible for our overlay draw).
            poseStack.pushPose();
            switch (slot) {
                case "left_arm" -> {
                    boolean was = model.leftArm.visible;
                    model.leftArm.visible = true;
                    model.leftArm.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, c[0], c[1], c[2], c[3]);
                    model.leftArm.visible = was;
                }
                case "right_arm" -> {
                    boolean was = model.rightArm.visible;
                    model.rightArm.visible = true;
                    model.rightArm.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, c[0], c[1], c[2], c[3]);
                    model.rightArm.visible = was;
                }
                case "left_leg" -> {
                    boolean was = model.leftLeg.visible;
                    model.leftLeg.visible = true;
                    model.leftLeg.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, c[0], c[1], c[2], c[3]);
                    model.leftLeg.visible = was;
                }
                case "right_leg" -> {
                    boolean was = model.rightLeg.visible;
                    model.rightLeg.visible = true;
                    model.rightLeg.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, c[0], c[1], c[2], c[3]);
                    model.rightLeg.visible = was;
                }
            }
            poseStack.popPose();
        }
    }

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : new String[]{"default", "slim"}) {
            PlayerRenderer r = event.getSkin(skin);
            if (r != null) {
                r.addLayer(new LimbModelLayer(
                    (RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>)
                    (RenderLayerParent<?, ?>) r
                ));
            }
        }
    }
}
