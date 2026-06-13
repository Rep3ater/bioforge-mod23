package com.bioforge.mod.client;

import com.bioforge.mod.BioForgeMod;
import com.mojang.logging.LogUtils;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Runtime registry for custom limb models.
 *
 * This allows other mods, server admins, or future BioForge expansions to
 * register custom models for prosthetics and cybernetic limbs by name.
 *
 * Registration (from mod init or addon):
 *   BioForgeModelRegistry.register("bioforge:mech_arm", MyArmModel::createBodyLayer);
 *
 * Usage (in /bioforge limb set command):
 *   /bioforge limb set Alice right_arm cybernetic bioforge:mech_arm
 *
 * The model override string is stored in LimbState.modelOverride and
 * read by LimbModelLayer at render time to substitute the vanilla model part.
 *
 * Built-in models registered here:
 *  - bioforge:prosthetic_arm_basic
 *  - bioforge:prosthetic_leg_basic
 *  - bioforge:cybernetic_arm_v1
 *  - bioforge:cybernetic_leg_v1
 *  - bioforge:hook_arm
 *  - bioforge:blade_arm
 */
@Mod.EventBusSubscriber(modid = BioForgeMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BioForgeModelRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();

    // Model layer locations for Forge's model bakery
    public static final Map<String, ModelLayerLocation> LAYER_LOCATIONS = new HashMap<>();

    // Baked model parts — populated during RegisterLayerDefinitions
    private static final Map<String, ModelPart> BAKED_MODELS = new HashMap<>();

    static {
        registerBuiltins();
    }

    private static void registerBuiltins() {
        register("bioforge:prosthetic_arm_basic");
        register("bioforge:prosthetic_leg_basic");
        register("bioforge:cybernetic_arm_v1");
        register("bioforge:cybernetic_leg_v1");
        register("bioforge:hook_arm");
        register("bioforge:blade_arm");
    }

    /**
     * Register a model override name. Call this early (pre-init) from addons.
     * The model geometry must be added during RegisterLayerDefinitions.
     */
    public static void register(String modelId) {
        ResourceLocation rl = new ResourceLocation(modelId.contains(":") ? modelId : BioForgeMod.MODID + ":" + modelId);
        ModelLayerLocation location = new ModelLayerLocation(rl, "main");
        LAYER_LOCATIONS.put(modelId, location);
    }

    /**
     * Retrieve a baked ModelPart for a given model ID.
     * Returns null if the model is not registered or not yet baked.
     */
    public static ModelPart getModel(String modelId) {
        return BAKED_MODELS.get(modelId);
    }

    // ─── Forge events ─────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        // Prosthetic arm — simple box geometry
        event.registerLayerDefinition(
                LAYER_LOCATIONS.getOrDefault("bioforge:prosthetic_arm_basic",
                        new ModelLayerLocation(new ResourceLocation(BioForgeMod.MODID, "prosthetic_arm_basic"), "main")),
                BioForgeModelRegistry::buildProstheticArmLayer);

        event.registerLayerDefinition(
                LAYER_LOCATIONS.getOrDefault("bioforge:prosthetic_leg_basic",
                        new ModelLayerLocation(new ResourceLocation(BioForgeMod.MODID, "prosthetic_leg_basic"), "main")),
                BioForgeModelRegistry::buildProstheticLegLayer);

        event.registerLayerDefinition(
                LAYER_LOCATIONS.getOrDefault("bioforge:cybernetic_arm_v1",
                        new ModelLayerLocation(new ResourceLocation(BioForgeMod.MODID, "cybernetic_arm_v1"), "main")),
                BioForgeModelRegistry::buildCyberneticArmLayer);

        event.registerLayerDefinition(
                LAYER_LOCATIONS.getOrDefault("bioforge:cybernetic_leg_v1",
                        new ModelLayerLocation(new ResourceLocation(BioForgeMod.MODID, "cybernetic_leg_v1"), "main")),
                BioForgeModelRegistry::buildCyberneticLegLayer);

        LOGGER.info("[BioForge] Custom limb model layers registered.");
    }

    // ─── Layer builders ───────────────────────────────────────────────────────

    private static LayerDefinition buildProstheticArmLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Basic prosthetic arm: thinner, slightly offset
        root.addOrReplaceChild("arm", CubeListBuilder.create()
                .texOffs(40, 16)
                .addBox(-1.5f, -2.0f, -2.0f, 3, 12, 4),
                PartPose.offset(5, 2, 0));
        return LayerDefinition.create(mesh, 64, 32);
    }

    private static LayerDefinition buildProstheticLegLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("leg", CubeListBuilder.create()
                .texOffs(0, 16)
                .addBox(-2.0f, 0.0f, -2.0f, 4, 12, 4),
                PartPose.offset(1.9f, 12, 0));
        return LayerDefinition.create(mesh, 64, 32);
    }

    private static LayerDefinition buildCyberneticArmLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Cybernetic arm: slightly larger with panel detail shapes
        root.addOrReplaceChild("upper_arm", CubeListBuilder.create()
                .texOffs(40, 16)
                .addBox(-2.0f, -2.0f, -2.0f, 4, 6, 4),
                PartPose.offset(5, 2, 0));
        root.addOrReplaceChild("lower_arm", CubeListBuilder.create()
                .texOffs(40, 26)
                .addBox(-1.5f, 4.0f, -1.5f, 3, 6, 3),
                PartPose.offset(5, 2, 0));
        root.addOrReplaceChild("hand", CubeListBuilder.create()
                .texOffs(40, 36)
                .addBox(-1.5f, 10.0f, -2.0f, 3, 4, 4),
                PartPose.offset(5, 2, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition buildCyberneticLegLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("upper_leg", CubeListBuilder.create()
                .texOffs(0, 16)
                .addBox(-2.0f, 0.0f, -2.0f, 4, 6, 4),
                PartPose.offset(1.9f, 12, 0));
        root.addOrReplaceChild("lower_leg", CubeListBuilder.create()
                .texOffs(0, 26)
                .addBox(-1.5f, 6.0f, -1.5f, 3, 6, 3),
                PartPose.offset(1.9f, 12, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }
}
