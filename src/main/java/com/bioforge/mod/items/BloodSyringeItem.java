package com.bioforge.mod.items;

import com.bioforge.mod.capabilities.BioForgeCapabilities;
import com.bioforge.mod.capabilities.PlayerBioData;
import com.bioforge.mod.core.BloodType;
import com.bioforge.mod.network.BioForgeNetwork;
import com.bioforge.mod.network.PlayProcedureEffectPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

/**
 * Empty blood syringe.
 * Right-click on another player → draws 0.45L of blood, creates a Filled Syringe.
 * Shift+right-click on self → self blood draw (no sedation required, minor pain).
 */
public class BloodSyringeItem extends Item {

    public static final float DRAW_VOLUME = 0.45f;

    public BloodSyringeItem() {
        super(new Properties().stacksTo(16));
    }


    /** Right-click air = draw from yourself */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        if (!level.isClientSide) {
            var bio = com.bioforge.mod.capabilities.BioForgeCapabilities.get(user);
            if (bio.getBloodVolume() < DRAW_VOLUME + 0.5f) {
                user.sendSystemMessage(net.minecraft.network.chat.Component.literal("[BioForge] Insufficient blood.").withStyle(net.minecraft.ChatFormatting.RED));
                return InteractionResultHolder.fail(user.getItemInHand(hand));
            }
            bio.removeBlood(DRAW_VOLUME);
            bio.addPain(0.5f);
            net.minecraft.world.item.ItemStack filled = new net.minecraft.world.item.ItemStack(com.bioforge.mod.registry.ModItems.BLOOD_SYRINGE_FILLED.get());
            net.minecraft.nbt.CompoundTag tag = filled.getOrCreateTag();
            tag.putString("blood_type", bio.getBloodType().name());
            tag.putFloat("volume", DRAW_VOLUME);
            tag.putString("donor_name", user.getName().getString());
            filled.setHoverName(net.minecraft.network.chat.Component.literal("Blood [" + bio.getBloodType().getDisplay() + "] — " + user.getName().getString()).withStyle(net.minecraft.ChatFormatting.RED));
            user.getItemInHand(hand).shrink(1);
            if (!user.getInventory().add(filled)) user.drop(filled, false);
            if (user instanceof net.minecraft.server.level.ServerPlayer sp)
                com.bioforge.mod.network.BioForgeNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp), new com.bioforge.mod.network.SyncBioDataPacket(bio));
            user.sendSystemMessage(net.minecraft.network.chat.Component.literal("[BioForge] §aBlood drawn (self). Type: " + bio.getBloodType().getDisplay()).withStyle(net.minecraft.ChatFormatting.GREEN));
        }
        return InteractionResultHolder.success(user.getItemInHand(hand));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player surgeon,
                                                   net.minecraft.world.entity.LivingEntity target,
                                                   InteractionHand hand) {
        Level level = surgeon.level();
        if (level.isClientSide) return InteractionResult.SUCCESS;

        if (!(target instanceof Player patient)) {
            surgeon.sendSystemMessage(Component.literal("[BioForge] Can only draw blood from players.").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        PlayerBioData bio = BioForgeCapabilities.get(patient);

        if (bio.getBloodVolume() < DRAW_VOLUME + 0.5f) {
            surgeon.sendSystemMessage(Component.literal("[BioForge] Patient has insufficient blood.").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        // Perform draw
        bio.removeBlood(DRAW_VOLUME);
        bio.addPain(0.5f);

        // Build filled syringe
        ItemStack filled = new ItemStack(com.bioforge.mod.registry.ModItems.BLOOD_SYRINGE_FILLED.get());
        CompoundTag tag = filled.getOrCreateTag();
        tag.putString("blood_type", bio.getBloodType().name());
        tag.putFloat("volume", DRAW_VOLUME);
        tag.putString("donor_uuid", patient.getUUID().toString());
        tag.putString("donor_name", patient.getName().getString());
        filled.setHoverName(Component.literal("Blood [" + bio.getBloodType().getDisplay() + "] — " + patient.getName().getString())
                .withStyle(ChatFormatting.RED));

        // Replace 1 empty syringe with filled, give remainder if surgeon is not patient
        stack.shrink(1);
        if (!surgeon.getInventory().add(filled)) {
            surgeon.drop(filled, false);
        }

        // FX
        BioForgeNetwork.CHANNEL.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                        patient.getX(), patient.getY(), patient.getZ(), 24, level.dimension())),
                new PlayProcedureEffectPacket(PlayProcedureEffectPacket.EffectType.BLOOD_DRAW, patient.position(), patient.getId())
        );

        bio.addMedicalHistoryEntry("[T] Blood drawn by " + surgeon.getName().getString()
                + ". Vol: " + DRAW_VOLUME + "L, Type: " + bio.getBloodType().getDisplay());

        surgeon.sendSystemMessage(Component.literal("[BioForge] Blood drawn. Type: " + bio.getBloodType().getDisplay())
                .withStyle(ChatFormatting.GREEN));
        return InteractionResult.SUCCESS;
    }
}
