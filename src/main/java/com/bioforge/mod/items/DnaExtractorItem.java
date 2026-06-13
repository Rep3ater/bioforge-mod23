package com.bioforge.mod.items;

import com.bioforge.mod.capabilities.BioForgeCapabilities;
import com.bioforge.mod.cloning.CloningSystem;
import com.bioforge.mod.core.BloodType;
import com.bioforge.mod.network.BioForgeNetwork;
import com.bioforge.mod.network.SyncBioDataPacket;
import com.bioforge.mod.registry.ModItems;
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

public class DnaExtractorItem extends Item {

    public DnaExtractorItem() { super(new Properties().stacksTo(1).durability(64)); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        if (!level.isClientSide) {
            ItemStack sample = CloningSystem.captureDna(user, user);
            if (!user.getInventory().add(sample)) user.drop(sample, false);
            user.getItemInHand(hand).hurtAndBreak(1, user, p -> p.broadcastBreakEvent(hand));
            user.sendSystemMessage(Component.literal("[BioForge] §aDNA captured (self).").withStyle(ChatFormatting.AQUA));
        }
        return InteractionResultHolder.success(user.getItemInHand(hand));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user,
                                                   LivingEntity target, InteractionHand hand) {
        if (!user.level().isClientSide) {
            ItemStack sample;
            if (target instanceof Player patient) {
                sample = CloningSystem.captureDna(patient, user);
                user.sendSystemMessage(Component.literal(
                    "[BioForge] §aDNA captured from " + patient.getName().getString()).withStyle(ChatFormatting.AQUA));
            } else if (target instanceof Villager) {
                sample = buildVillagerSample(target);
                user.sendSystemMessage(Component.literal("[BioForge] §aDNA captured from Villager.").withStyle(ChatFormatting.AQUA));
            } else {
                user.sendSystemMessage(Component.literal("[BioForge] Can't extract from this.").withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }
            if (!user.getInventory().add(sample)) user.drop(sample, false);
            stack.hurtAndBreak(1, user, p -> p.broadcastBreakEvent(hand));
        }
        return InteractionResult.SUCCESS;
    }

    private static ItemStack buildVillagerSample(LivingEntity villager) {
        ItemStack sample = new ItemStack(ModItems.DNA_SAMPLE.get());
        var tag = sample.getOrCreateTag();
        tag.putString("source_name", "Villager");
        tag.putString("source_uuid", villager.getUUID().toString());
        String hash = String.format("%016X%016X",
            villager.getUUID().getMostSignificantBits(),
            villager.getUUID().getLeastSignificantBits());
        tag.putString("dna_hash", hash);
        tag.putString("blood_type", BloodType.random(villager.level().random).name());
        tag.putInt("clone_generation", 0);
        tag.putBoolean("has_memory", false);
        sample.setHoverName(Component.literal("DNA Sample [Villager]").withStyle(ChatFormatting.AQUA));
        return sample;
    }
}
