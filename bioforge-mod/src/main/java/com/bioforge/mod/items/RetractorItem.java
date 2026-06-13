package com.bioforge.mod.items;

import com.bioforge.mod.capabilities.BioForgeCapabilities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;

/** Holds an incision open — extends the 'incision_open' window by reducing pain. */
public class RetractorItem extends Item {
    public RetractorItem() { super(new Properties().stacksTo(1)); }


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
        var bio = target instanceof Player p ? BioForgeCapabilities.get(p)
                : target instanceof Villager v ? BioForgeCapabilities.get(v) : null;
        if (bio == null) return InteractionResult.FAIL;
        if (!bio.hasCondition("incision_open")) {
            user.sendSystemMessage(Component.literal("[BioForge] No open incision to retract.").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        bio.reducePain(1.5f); // retraction is uncomfortable but controlled
        bio.addCondition("retracted"); // flag that incision is held open
        user.sendSystemMessage(Component.literal("[BioForge] Incision held open.").withStyle(ChatFormatting.YELLOW));
        return InteractionResult.SUCCESS;
    }
}
