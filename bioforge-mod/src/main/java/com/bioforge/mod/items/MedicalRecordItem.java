package com.bioforge.mod.items;

import com.bioforge.mod.capabilities.BioForgeCapabilities;
import com.bioforge.mod.capabilities.PlayerBioData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MedicalRecordItem extends Item {
    public MedicalRecordItem() { super(new Properties().stacksTo(1)); }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand hand) {
        if (target.level().isClientSide) return InteractionResult.SUCCESS;
        PlayerBioData bio = null;
        String name = "";
        if (target instanceof Player p) { bio = BioForgeCapabilities.get(p); name = p.getName().getString(); }
        else if (target instanceof Villager v) { bio = BioForgeCapabilities.get(v); name = "Villager"; }
        if (bio == null) return InteractionResult.FAIL;

        var tag = stack.getOrCreateTag();
        tag.putString("patient_name", name);
        tag.putString("patient_id", bio.getPatientId().isEmpty() ? "UNASSIGNED" : bio.getPatientId());
        tag.putString("blood_type", bio.getBloodType().getDisplay());
        tag.putInt("entry_count", bio.getMedicalHistory().size());
        if (!bio.getMedicalHistory().isEmpty()) {
            tag.putString("last_entry", bio.getMedicalHistory().get(bio.getMedicalHistory().size() - 1));
        }

        var hist = new StringBuilder();
        bio.getMedicalHistory().forEach(e -> hist.append(e).append("\n"));
        tag.putString("history", hist.toString());

        stack.setHoverName(Component.literal("Medical Record [" + name + "]").withStyle(ChatFormatting.GOLD));
        user.sendSystemMessage(Component.literal("[BioForge] Record updated for " + name).withStyle(ChatFormatting.GOLD));
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        var tag = stack.getTag();
        if (tag == null || !tag.contains("patient_name")) {
            user.sendSystemMessage(Component.literal("[BioForge] Record is empty. Right-click a patient to fill it.").withStyle(ChatFormatting.GRAY));
            return InteractionResultHolder.success(stack);
        }
        user.sendSystemMessage(Component.literal("§6═══ Medical Record ═══"));
        user.sendSystemMessage(Component.literal("Patient: §f" + tag.getString("patient_name")));
        user.sendSystemMessage(Component.literal("ID: §f" + tag.getString("patient_id")));
        user.sendSystemMessage(Component.literal("Blood Type: §c" + tag.getString("blood_type")));
        user.sendSystemMessage(Component.literal("§6─── History ───"));
        String hist = tag.getString("history");
        for (String line : hist.split("\n")) {
            if (!line.isBlank()) user.sendSystemMessage(Component.literal("  §7" + line));
        }
        return InteractionResultHolder.success(stack);
    }
}
