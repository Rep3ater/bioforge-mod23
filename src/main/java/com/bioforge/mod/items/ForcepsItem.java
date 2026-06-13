package com.bioforge.mod.items;

import com.bioforge.mod.capabilities.BioForgeCapabilities;
import com.bioforge.mod.network.BioForgeNetwork;
import com.bioforge.mod.network.PlayProcedureEffectPacket;
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
import net.minecraftforge.network.PacketDistributor;

/** Extracts an organ from a sedated target with an open incision. Sneak+click to choose organ slot. */
public class ForcepsItem extends Item {
    private static final String[] ORGAN_SLOTS = {"heart","liver","kidney","lung"};

    public ForcepsItem() { super(new Properties().stacksTo(1).durability(64)); }


    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        if (!level.isClientSide) {
            interactLivingEntity(user.getItemInHand(hand), user, user, hand);
        }
        return InteractionResultHolder.success(user.getItemInHand(hand));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand hand) {
        Level level = user.level();
        if (level.isClientSide) return InteractionResult.SUCCESS;

        if (user.isShiftKeyDown()) {
            int cur = stack.getOrCreateTag().getInt("organ_index");
            cur = (cur + 1) % ORGAN_SLOTS.length;
            stack.getOrCreateTag().putInt("organ_index", cur);
            user.sendSystemMessage(Component.literal("[BioForge] Target organ: " + ORGAN_SLOTS[cur]).withStyle(ChatFormatting.YELLOW));
            return InteractionResult.SUCCESS;
        }

        var bio = target instanceof Player p ? BioForgeCapabilities.get(p)
                : target instanceof Villager v ? BioForgeCapabilities.get(v) : null;
        if (bio == null) return InteractionResult.FAIL;

        if (!bio.isSedated()) {
            user.sendSystemMessage(Component.literal("[BioForge] Target must be sedated.").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        if (!bio.hasCondition("incision_open")) {
            user.sendSystemMessage(Component.literal("[BioForge] Make an incision with a scalpel first.").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        String slot = ORGAN_SLOTS[stack.getOrCreateTag().getInt("organ_index")];
        bio.removeBlood(0.4f);
        bio.addPain(5f);
        bio.addMedicalHistoryEntry("[T] Organ extracted: " + slot + " by " + user.getName().getString());

        // Drop organ item
        Item organItem = switch (slot) {
            case "liver"  -> ModItems.LIVER.get();
            case "kidney" -> ModItems.KIDNEY.get();
            case "lung"   -> ModItems.LUNG.get();
            default       -> ModItems.HEART.get();
        };
        ItemStack drop = new ItemStack(organItem);
        drop.getOrCreateTag().putString("donor_dna", bio.getDnaHash());
        level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(
            level, target.getX(), target.getY(), target.getZ(), drop));

        BioForgeNetwork.CHANNEL.send(
            PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                target.getX(), target.getY(), target.getZ(), 24, level.dimension())),
            new PlayProcedureEffectPacket(PlayProcedureEffectPacket.EffectType.ORGAN_REMOVE, target.position(), target.getId()));

        stack.hurtAndBreak(2, user, p -> p.broadcastBreakEvent(hand));
        user.sendSystemMessage(Component.literal("[BioForge] " + slot + " extracted.").withStyle(ChatFormatting.GREEN));
        return InteractionResult.SUCCESS;
    }
}
