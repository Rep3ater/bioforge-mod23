package com.bioforge.mod.items;
import com.bioforge.mod.blocks.CloningVatBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
public class CloningCatalystItem extends Item {
    public CloningCatalystItem() { super(new Properties().stacksTo(16)); }
    @Override public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel(); if (level.isClientSide) return InteractionResult.SUCCESS;
        var be = level.getBlockEntity(ctx.getClickedPos());
        if (!(be instanceof CloningVatBlockEntity vat)) return InteractionResult.PASS;
        var inv = vat.getInventory();
        if (inv.getStackInSlot(1).isEmpty()) { inv.setStackInSlot(1, new ItemStack(this, 1)); ctx.getItemInHand().shrink(1); if (vat.tryStartGrowth()) ctx.getPlayer().sendSystemMessage(Component.literal("[BioForge] §aGrowth started!").withStyle(ChatFormatting.GREEN)); else ctx.getPlayer().sendSystemMessage(Component.literal("[BioForge] Catalyst inserted. Add DNA sample.").withStyle(ChatFormatting.YELLOW)); return InteractionResult.SUCCESS; }
        ctx.getPlayer().sendSystemMessage(Component.literal("[BioForge] Vat already has catalyst.").withStyle(ChatFormatting.RED)); return InteractionResult.FAIL;
    }
}
