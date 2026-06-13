package com.bioforge.mod.items;

import com.bioforge.mod.capabilities.BioForgeCapabilities;
import com.bioforge.mod.capabilities.PlayerBioData;
import com.bioforge.mod.network.BioForgeNetwork;
import com.bioforge.mod.network.PlayProcedureEffectPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

/** Stitches open wounds and incisions. */
public class StitchKitItem extends Item {
    public StitchKitItem() { super(new Properties().stacksTo(4)); }


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
        PlayerBioData bio = null;
        if (target instanceof Player p) bio = BioForgeCapabilities.get(p);
        else if (target instanceof Villager v) bio = BioForgeCapabilities.get(v);
        if (bio != null) {
            bio.removeCondition("incision_open");
            bio.getActiveConditions().stream().filter(c -> c.startsWith("bleeding_")).findFirst()
                .ifPresent(bio::removeCondition);
            bio.addPain(1f);
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 300, 0));
            bio.addMedicalHistoryEntry("[T] Sutures by " + user.getName().getString());
        }
        BioForgeNetwork.CHANNEL.send(
            PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                target.getX(), target.getY(), target.getZ(), 24, level.dimension())),
            new PlayProcedureEffectPacket(PlayProcedureEffectPacket.EffectType.STITCH, target.position(), target.getId()));
        stack.hurtAndBreak(1, user, p -> p.broadcastBreakEvent(hand));
        user.sendSystemMessage(Component.literal("[BioForge] Wound sutured.").withStyle(ChatFormatting.GREEN));
        return InteractionResult.SUCCESS;
    }
}
