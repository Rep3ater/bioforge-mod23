package com.bioforge.mod.items;

import com.bioforge.mod.capabilities.BioForgeCapabilities;
import com.bioforge.mod.capabilities.PlayerBioData;
import com.bioforge.mod.core.LimbState;
import com.bioforge.mod.network.BioForgeNetwork;
import com.bioforge.mod.network.PlayProcedureEffectPacket;
import com.bioforge.mod.network.SyncBioDataPacket;
import com.bioforge.mod.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

/**
 * Bone Saw — amputates OR removes a limb (including prosthetics/cybernetics).
 *
 * Sneak + right-click (any target incl. air) = cycle target slot.
 * Right-click on entity OR right-click air (self) = operate on selected slot.
 *
 * If the slot has a prosthetic or cybernetic, it removes it cleanly and drops
 * it as an item (no blood, no pain — it's mechanical).
 * If the slot has a biological limb, it amputates with blood/pain.
 * If the slot is already absent, it tells you so.
 */
public class BoneSawItem extends Item {

    private static final String[] SLOTS = {"left_arm", "right_arm", "left_leg", "right_leg"};

    public BoneSawItem() {
        super(new Properties().stacksTo(1).durability(64));
    }

    // ── Sneak + right-click air = cycle slot ──────────────────────────────────
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (user.isShiftKeyDown()) {
            cycleSlot(stack, user);
            return InteractionResultHolder.success(stack);
        }
        // Right-click air = self
        if (!level.isClientSide) operate(stack, user, user, hand);
        return InteractionResultHolder.success(stack);
    }

    // ── Right-click on entity = operate on them ───────────────────────────────
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user,
                                                   LivingEntity target, InteractionHand hand) {
        if (user.isShiftKeyDown()) {
            if (!user.level().isClientSide) cycleSlot(stack, user);
            return InteractionResult.SUCCESS;
        }
        if (!user.level().isClientSide) operate(stack, user, target, hand);
        return InteractionResult.SUCCESS;
    }

    private void cycleSlot(ItemStack stack, Player user) {
        int cur = (stack.getOrCreateTag().getInt("slot_index") + 1) % SLOTS.length;
        stack.getOrCreateTag().putInt("slot_index", cur);
        user.sendSystemMessage(Component.literal(
            "[BioForge] Bone Saw target: §e" + SLOTS[cur]).withStyle(ChatFormatting.YELLOW));
    }

    private void operate(ItemStack stack, Player surgeon, LivingEntity target, InteractionHand hand) {
        String slot = SLOTS[stack.getOrCreateTag().getInt("slot_index")];
        boolean isSelf = target == surgeon;

        PlayerBioData bio = null;
        if (target instanceof Player p) bio = BioForgeCapabilities.get(p);
        else if (target instanceof Villager v) bio = BioForgeCapabilities.get(v);
        if (bio == null) return;

        LimbState current = bio.getLimb(slot);

        if (!current.isPresent()) {
            surgeon.sendSystemMessage(Component.literal(
                "[BioForge] §c" + slot + " is already absent.").withStyle(ChatFormatting.RED));
            return;
        }

        boolean mechanical = current.getType() == LimbState.LimbType.PROSTHETIC
                          || current.getType() == LimbState.LimbType.CYBERNETIC;

        if (mechanical) {
            // Clean removal of mechanical limb — drop it, no gore
            Item dropItem = switch (current.getType()) {
                case CYBERNETIC -> slot.contains("arm")
                        ? ModItems.CYBERNETIC_ARM.get() : ModItems.CYBERNETIC_LEG.get();
                default -> slot.contains("arm")
                        ? ModItems.PROSTHETIC_ARM.get() : ModItems.PROSTHETIC_LEG.get();
            };
            ItemStack drop = new ItemStack(dropItem);
            if (!current.getModelOverride().isEmpty())
                drop.getOrCreateTag().putString("model_override", current.getModelOverride());
            target.level().addFreshEntity(new ItemEntity(
                target.level(), target.getX(), target.getY(), target.getZ(), drop));

            bio.setLimb(slot, LimbState.absent());
            surgeon.sendSystemMessage(Component.literal(
                "[BioForge] §a" + slot + " removed cleanly.").withStyle(ChatFormatting.GREEN));

        } else {
            // Biological amputation
            if (!bio.isSedated() && !isSelf) {
                target.hurt(target.damageSources().generic(), 6.0f);
                bio.addPain(9f);
            } else if (isSelf) {
                target.hurt(target.damageSources().generic(), 4.0f);
                bio.addPain(8f);
            }

            // Drop the limb
            Item dropItem = slot.contains("arm")
                    ? ModItems.BIOLOGICAL_ARM.get() : ModItems.BIOLOGICAL_LEG.get();
            ItemStack drop = new ItemStack(dropItem);
            drop.getOrCreateTag().putString("donor_dna", bio.getDnaHash());
            target.level().addFreshEntity(new ItemEntity(
                target.level(), target.getX(), target.getY(), target.getZ(), drop));

            bio.setLimb(slot, LimbState.absent());
            bio.removeBlood(0.8f);
            bio.addCondition("bleeding_" + slot);
            bio.addMedicalHistoryEntry("[T] Amputation: " + slot
                + (isSelf ? " (self)" : " by " + surgeon.getName().getString()));

            BioForgeNetwork.CHANNEL.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                    target.getX(), target.getY(), target.getZ(), 24, target.level().dimension())),
                new PlayProcedureEffectPacket(
                    PlayProcedureEffectPacket.EffectType.LIMB_REMOVE, target.position(), target.getId()));

            surgeon.sendSystemMessage(Component.literal(
                "[BioForge] §c" + slot + " amputated.").withStyle(ChatFormatting.DARK_RED));
        }

        // Sync to target
        if (target instanceof ServerPlayer sp)
            BioForgeNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new SyncBioDataPacket(bio));

        stack.hurtAndBreak(mechanical ? 1 : 4, surgeon, p -> p.broadcastBreakEvent(hand));
    }
}
