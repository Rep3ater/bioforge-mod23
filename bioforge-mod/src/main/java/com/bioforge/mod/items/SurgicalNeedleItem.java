package com.bioforge.mod.items;

import com.bioforge.mod.capabilities.BioForgeCapabilities;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;

/** Used to start suturing; reduces pain and closes incisions. */
public class SurgicalNeedleItem extends Item {
    public SurgicalNeedleItem() { super(new Properties().stacksTo(8).durability(32)); }


    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        if (!level.isClientSide) {
            interactLivingEntity(user.getItemInHand(hand), user, user, hand);
        }
        return InteractionResultHolder.success(user.getItemInHand(hand));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand hand) {
        if (target.level().isClientSide) return InteractionResult.SUCCESS;
        if (target instanceof Player p) {
            var bio = BioForgeCapabilities.get(p);
            bio.removeCondition("incision_open");
            bio.reducePain(2f);
        } else if (target instanceof Villager v) {
            var bio = BioForgeCapabilities.get(v);
            bio.removeCondition("incision_open");
        }
        target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
        stack.hurtAndBreak(1, user, p -> p.broadcastBreakEvent(hand));
        user.sendSystemMessage(Component.literal("[BioForge] Sutures started.").withStyle(ChatFormatting.GREEN));
        return InteractionResult.SUCCESS;
    }
}
