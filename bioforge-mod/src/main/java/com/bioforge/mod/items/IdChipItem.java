package com.bioforge.mod.items;

import com.bioforge.mod.capabilities.BioForgeCapabilities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Implant-style chip: right-click to read any encoded data off an ID chip, or write to a target. */
public class IdChipItem extends Item {
    public IdChipItem() { super(new Properties().stacksTo(1)); }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand hand) {
        if (target.level().isClientSide) return InteractionResult.SUCCESS;
        var bio = target instanceof Player p ? BioForgeCapabilities.get(p)
                : target instanceof Villager v ? BioForgeCapabilities.get(v) : null;
        if (bio == null) return InteractionResult.FAIL;

        var tag = stack.getOrCreateTag();
        tag.putString("uuid", target.getUUID().toString());
        tag.putString("blood_type", bio.getBloodType().getDisplay());
        tag.putString("dna_hash", bio.getDnaHash().isEmpty() ? "UNSAMPLED" : bio.getDnaHash().substring(0, 8));
        tag.putString("patient_id", bio.getPatientId());
        stack.setHoverName(Component.literal("ID Chip [" + target.getUUID().toString().substring(0,8) + "]").withStyle(ChatFormatting.DARK_AQUA));
        user.sendSystemMessage(Component.literal("[BioForge] Chip encoded. UUID:" + target.getUUID().toString().substring(0,8) + "...").withStyle(ChatFormatting.DARK_AQUA));
        return InteractionResult.SUCCESS;
    }
}
