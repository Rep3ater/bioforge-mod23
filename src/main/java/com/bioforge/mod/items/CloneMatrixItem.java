package com.bioforge.mod.items;
import com.bioforge.mod.blocks.CloningVatBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
public class CloneMatrixItem extends Item {
    public CloneMatrixItem() { super(new Properties().stacksTo(1)); }
    @Override public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel(); if (level.isClientSide) return InteractionResult.SUCCESS;
        var be = level.getBlockEntity(ctx.getClickedPos());
        if (!(be instanceof CloningVatBlockEntity vat)) return InteractionResult.PASS;
        var inv = vat.getInventory();
        if (inv.getStackInSlot(3).isEmpty()) { inv.setStackInSlot(3, ctx.getItemInHand().copy()); ctx.getItemInHand().shrink(1); ctx.getPlayer().sendSystemMessage(Component.literal("[BioForge] §aClone Matrix inserted — stability improved.").withStyle(ChatFormatting.AQUA)); return InteractionResult.SUCCESS; }
        ctx.getPlayer().sendSystemMessage(Component.literal("[BioForge] Matrix slot full.").withStyle(ChatFormatting.RED)); return InteractionResult.FAIL;
    }
}
