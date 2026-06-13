package com.bioforge.mod.items;

import com.bioforge.mod.capabilities.BioForgeCapabilities;
import com.bioforge.mod.capabilities.PlayerBioData;
import com.bioforge.mod.core.LimbState;
import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SurgicalToolItem extends Item {

    private final ToolType type;

    public SurgicalToolItem(Properties properties, ToolType type) {
        super(properties);
        this.type = type;
    }

    public static boolean applySelfSurgery(Player patient, InteractionHand hand, ToolType type) {
        if (!patient.level().isClientSide) {
            PlayerBioData data = BioForgeCapabilities.get(patient);
            if (data == null) return false;

            ItemStack stack = patient.getItemInHand(hand);
            if (stack.isEmpty()) return false;

            if (!isValidPatient(data)) {
                patient.sendSystemMessage(net.minecraft.network.chat.Component.literal("Not stable enough for surgery.").withStyle(ChatFormatting.RED));
                return false;
            }

            stack.hurtAndBreak(1, (net.minecraft.world.entity.LivingEntity) patient, e -> e.broadcastBreakEvent(hand));

            boolean changed = doSurgery(patient, data, type);
            data.reducePain(2f);

            if (changed) {
                patient.level().playSound(null, patient.getX(), patient.getY(), patient.getZ(),
                        SoundEvents.PLAYER_LEVELUP, patient.getSoundSource(), 1.0f, 1.2f);
                patient.sendSystemMessage(net.minecraft.network.chat.Component.literal("Surgery complete.").withStyle(ChatFormatting.GRAY));
            }
            return changed;
        }
        return true;
    }

    public static boolean applyTool(Level level, Player surgeon, LivingEntity target, InteractionHand hand, ToolType type) {
        if (level.isClientSide) return true;
        if (target == surgeon) return applySelfSurgery(surgeon, hand, type);

        PlayerBioData bio = null;
        if (target instanceof Player p) bio = BioForgeCapabilities.get(p);
        else if (target instanceof net.minecraft.world.entity.npc.Villager v) bio = BioForgeCapabilities.get(v);
        if (bio == null) return false;

        ItemStack stack = surgeon.getItemInHand(hand);
        if (stack.isEmpty()) return false;

        if (!isValidPatient(bio)) {
            surgeon.sendSystemMessage(net.minecraft.network.chat.Component.literal("Not stable enough for surgery.").withStyle(ChatFormatting.RED));
            return false;
        }

        stack.hurtAndBreak(1, (net.minecraft.world.entity.LivingEntity) surgeon, e -> e.broadcastBreakEvent(hand));

        boolean changed = doSurgery(surgeon, bio, type);
        bio.reducePain(2f);

        if (changed) {
            surgeon.level().playSound(null, surgeon.getX(), surgeon.getY(), surgeon.getZ(),
                    SoundEvents.PLAYER_LEVELUP, surgeon.getSoundSource(), 1.0f, 1.2f);
            surgeon.sendSystemMessage(net.minecraft.network.chat.Component.literal("Surgery complete.").withStyle(ChatFormatting.GRAY));
        }
        return changed;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("[BioForge] SurgicalTool mode: " + type).withStyle(ChatFormatting.YELLOW));
            }
            return InteractionResultHolder.success(stack);
        }
        if (!level.isClientSide) applySelfSurgery(player, hand, type);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        boolean ok = applyTool(player.level(), player, target, hand, type);
        return ok ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    private static boolean isValidPatient(PlayerBioData data) {
        int limbs = 0;
        for (LimbState s : data.getAllLimbs().values()) if (s.isPresent()) limbs++;
        return limbs > 2;
    }

    private static boolean doSurgery(Player surgeon, PlayerBioData data, ToolType type) {
        java.util.List<String> removable = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, LimbState> e : data.getAllLimbs().entrySet()) {
            if (e.getValue().isPresent()) removable.add(e.getKey());
        }
        if (removable.isEmpty()) return false;

        switch (type) {
            case SCALPEL -> {
                String slot = removable.get(surgeon.getRandom().nextInt(removable.size()));
                data.setLimb(slot, LimbState.absent());
                surgeon.sendSystemMessage(net.minecraft.network.chat.Component.literal("Removed " + slot));
                return true;
            }
            case BONE_SAW -> {
                String slot = removable.get(surgeon.getRandom().nextInt(removable.size()));
                data.setLimb(slot, LimbState.absent());
                surgeon.sendSystemMessage(net.minecraft.network.chat.Component.literal("Sawed off " + slot));
                return true;
            }
            case CAUTERIZER -> {
                surgeon.sendSystemMessage(net.minecraft.network.chat.Component.literal("Cauterized."));
                return true;
            }
            default -> { return false; }
        }
    }

    public enum ToolType { SCALPEL, BONE_SAW, CAUTERIZER }
}
