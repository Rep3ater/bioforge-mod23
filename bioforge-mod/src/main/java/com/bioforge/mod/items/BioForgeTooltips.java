package com.bioforge.mod.items;
import com.bioforge.mod.BioForgeMod;
import com.bioforge.mod.capabilities.BioForgeCapabilities;
import com.bioforge.mod.capabilities.PlayerBioData;
import com.bioforge.mod.core.BloodType;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Injects dynamic NBT-based tooltips onto BioForge items.
 * Avoids subclassing every item just for tooltip logic.
 *
 * Tooltips shown:
 *  - Blood Syringe (Filled): blood type, volume
 *  - DNA Sample: source player name, DNA hash (first 8 chars), clone generation, memory present
 *  - Blood Bag: blood type, volume
 *  - Medical Record: patient ID, last entry
 *  - Patient Wristband: patient ID, blood type
 *  - ID Chip: player UUID snippet
 *  - Organ items: donor DNA hash, rejection risk hint
 *  - Limb items: limb type, model override if set
 *  - Memory Crystal: owner name, charged status
 *  - Anesthetic/Morphine Syringe: dosage
 */
@Mod.EventBusSubscriber(modid = BioForgeMod.MODID)
public class BioForgeTooltips {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();
        CompoundTag tag = stack.getTag();

        var reg = com.bioforge.mod.registry.ModItems.class;

        // ── Blood Syringe (Filled) ────────────────────────────────────────────
        if (stack.is(com.bioforge.mod.registry.ModItems.BLOOD_SYRINGE_FILLED.get())) {
            if (tag != null && tag.contains("blood_type")) {
                BloodType bt = BloodType.fromString(tag.getString("blood_type"));
                float vol = tag.getFloat("volume");
                tooltip.add(Component.literal("Blood Type: ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(bt.getDisplay()).withStyle(ChatFormatting.RED)));
                tooltip.add(Component.literal(String.format("Volume: %.2fL", vol)).withStyle(ChatFormatting.DARK_RED));
            } else {
                tooltip.add(Component.literal("(empty)").withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        // ── DNA Sample ────────────────────────────────────────────────────────
        if (stack.is(com.bioforge.mod.registry.ModItems.DNA_SAMPLE.get())) {
            if (tag != null) {
                String src = tag.getString("source_name");
                String hash = tag.getString("dna_hash");
                int gen = tag.getInt("clone_generation");
                boolean mem = tag.getBoolean("has_memory");
                tooltip.add(Component.literal("Source: ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(src).withStyle(ChatFormatting.AQUA)));
                if (!hash.isEmpty()) {
                    tooltip.add(Component.literal("DNA: ").withStyle(ChatFormatting.GRAY)
                            .append(Component.literal(hash.substring(0, Math.min(8, hash.length())) + "...").withStyle(ChatFormatting.DARK_AQUA)));
                }
                tooltip.add(Component.literal("Generation: ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(String.valueOf(gen)).withStyle(ChatFormatting.YELLOW)));
                tooltip.add(Component.literal("Memory: ").withStyle(ChatFormatting.GRAY)
                        .append(mem
                                ? Component.literal("Encoded").withStyle(ChatFormatting.GREEN)
                                : Component.literal("None").withStyle(ChatFormatting.DARK_GRAY)));
            }
        }

        // ── Blood Bag ─────────────────────────────────────────────────────────
        if (stack.is(com.bioforge.mod.registry.ModItems.BLOOD_BAG.get())) {
            if (tag != null && tag.contains("blood_type")) {
                BloodType bt = BloodType.fromString(tag.getString("blood_type"));
                float vol = tag.contains("volume") ? tag.getFloat("volume") : 0.45f;
                tooltip.add(Component.literal("Type: ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(bt.getDisplay()).withStyle(ChatFormatting.RED)));
                tooltip.add(Component.literal(String.format("%.2fL stored", vol)).withStyle(ChatFormatting.DARK_RED));
            } else {
                tooltip.add(Component.literal("Empty").withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        if (stack.is(com.bioforge.mod.registry.ModItems.MORPHINE.get())) {
            if (tag != null && tag.contains("dose")) {
                tooltip.add(Component.literal("Dose: ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(tag.getInt("dose") + "mg").withStyle(ChatFormatting.YELLOW)));
            }
        }

        // ── Limb items: show model override if set ────────────────────────────
        boolean isLimb = stack.is(com.bioforge.mod.registry.ModItems.PROSTHETIC_ARM.get())
                || stack.is(com.bioforge.mod.registry.ModItems.PROSTHETIC_LEG.get())
                || stack.is(com.bioforge.mod.registry.ModItems.CYBERNETIC_ARM.get())
                || stack.is(com.bioforge.mod.registry.ModItems.CYBERNETIC_LEG.get())
                || stack.is(com.bioforge.mod.registry.ModItems.BIOLOGICAL_ARM.get())
                || stack.is(com.bioforge.mod.registry.ModItems.BIOLOGICAL_LEG.get());
        if (isLimb && tag != null && tag.contains("model_override")) {
            String model = tag.getString("model_override");
            if (!model.isEmpty()) {
                tooltip.add(Component.literal("Model: ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(model).withStyle(ChatFormatting.BLUE)));
            }
        }
    }
}
