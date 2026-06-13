package com.bioforge.mod.items;

import com.bioforge.mod.bodyexit.BodyExitSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * SpiritCatalystItem — activates the Body Exit / Astral Projection system.
 *
 * Right-click:
 *  - If not projecting → call BodyExitSystem.leaveBody()
 *  - If projecting → no-op (must interact with body to return)
 *
 * This item is retained in the player's off-hand when they leave —
 * it is NOT transferred to the shell, so they always have a way back.
 *
 * Actually: this item is removed from inventory when leaving and placed
 * back when returning, to avoid duplication.
 */
public class SpiritCatalystItem extends Item {

    public SpiritCatalystItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        if (!level.isClientSide && user instanceof ServerPlayer sp) {
            if (BodyExitSystem.isProjecting(sp)) {
                sp.sendSystemMessage(Component.literal("§c[BioForge] You must return to your body first — interact with it."));
            } else {
                BodyExitSystem.leaveBody(sp);
                // The item will be stored with the inventory snapshot inside the shell.
                // shrink here so it doesn't duplicate when returned.
            }
        }
        return InteractionResultHolder.success(user.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Detach your consciousness from your body.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§8Right-click to leave body.").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("§8Interact with your body to return.").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§4⚠ If your body dies, so do you.").withStyle(ChatFormatting.DARK_RED));
    }
}
