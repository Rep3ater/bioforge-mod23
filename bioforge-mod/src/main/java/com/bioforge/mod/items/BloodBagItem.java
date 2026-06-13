package com.bioforge.mod.items;

import com.bioforge.mod.capabilities.BioForgeCapabilities;
import com.bioforge.mod.capabilities.PlayerBioData;
import com.bioforge.mod.core.BloodType;
import com.bioforge.mod.network.BioForgeNetwork;
import com.bioforge.mod.network.SyncBioDataPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

public class BloodBagItem extends Item {
    public static final float CAP = 0.9f;

    public BloodBagItem() { super(new Properties().stacksTo(8)); }

    // Right-click air = self-transfuse if filled, or inform if empty
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        if (!level.isClientSide) {
            ItemStack stack = user.getItemInHand(hand);
            var tag = stack.getTag();
            if (tag != null && tag.contains("blood_type")) {
                transfuse(stack, user, user);
            } else {
                user.sendSystemMessage(Component.literal(
                    "[BioForge] Empty bag. Right-click a patient to fill.").withStyle(ChatFormatting.GRAY));
            }
        }
        return InteractionResultHolder.success(user.getItemInHand(hand));
    }

    // Right-click entity = fill if empty, transfuse if full
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user,
                                                   LivingEntity target, InteractionHand hand) {
        if (!user.level().isClientSide) {
            var tag = stack.getOrCreateTag();
            if (!tag.contains("blood_type")) fill(stack, user, target);
            else transfuse(stack, user, target);
        }
        return InteractionResult.SUCCESS;
    }

    private void fill(ItemStack stack, Player user, LivingEntity target) {
        PlayerBioData bio = getBio(target);
        if (bio == null) { user.sendSystemMessage(Component.literal("[BioForge] Can't draw blood from this.").withStyle(ChatFormatting.RED)); return; }
        if (bio.getBloodVolume() < CAP + 0.5f) { user.sendSystemMessage(Component.literal("[BioForge] Insufficient blood.").withStyle(ChatFormatting.RED)); return; }
        bio.removeBlood(CAP);
        var tag = stack.getOrCreateTag();
        tag.putString("blood_type", bio.getBloodType().name());
        tag.putFloat("volume", CAP);
        stack.setHoverName(Component.literal("Blood Bag [" + bio.getBloodType().getDisplay() + "]").withStyle(ChatFormatting.RED));
        sync(target, bio);
        user.sendSystemMessage(Component.literal("[BioForge] §aBag filled: " + bio.getBloodType().getDisplay()).withStyle(ChatFormatting.GREEN));
    }

    private void transfuse(ItemStack stack, Player user, LivingEntity target) {
        var tag = stack.getTag();
        if (tag == null) return;
        PlayerBioData bio = getBio(target);
        if (bio == null) return;
        BloodType donor = BloodType.fromString(tag.getString("blood_type"));
        float vol = tag.getFloat("volume");
        if (!bio.getBloodType().canReceiveFrom(donor)) {
            target.hurt(target.damageSources().generic(), 4f);
            bio.addCondition("transfusion_reaction");
            user.sendSystemMessage(Component.literal("[BioForge] §4TRANSFUSION REACTION!").withStyle(ChatFormatting.DARK_RED));
        } else {
            bio.addBlood(vol);
            user.sendSystemMessage(Component.literal("[BioForge] §aTransfused " + vol + "L " + donor.getDisplay()).withStyle(ChatFormatting.GREEN));
        }
        tag.remove("blood_type");
        tag.remove("volume");
        stack.setHoverName(Component.literal("Blood Bag (Empty)").withStyle(ChatFormatting.GRAY));
        sync(target, bio);
    }

    private static PlayerBioData getBio(LivingEntity e) {
        if (e instanceof Player p) return BioForgeCapabilities.get(p);
        if (e instanceof Villager v) return BioForgeCapabilities.get(v);
        return null;
    }

    private static void sync(LivingEntity e, PlayerBioData bio) {
        if (e instanceof ServerPlayer sp)
            BioForgeNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new SyncBioDataPacket(bio));
    }
}
