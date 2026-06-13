package com.bioforge.mod.items;
import com.bioforge.mod.blocks.CloningVatBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
public class DnaSampleItem extends Item {
    public DnaSampleItem() { super(new Properties().stacksTo(1)); }
    @Override public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel(); if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockPos pos = ctx.getClickedPos(); var be = level.getBlockEntity(pos);
        if (!(be instanceof CloningVatBlockEntity vat)) return InteractionResult.PASS;
        var inv = vat.getInventory();
        if (inv.getStackInSlot(0).isEmpty()) { inv.setStackInSlot(0, ctx.getItemInHand().copy()); ctx.getItemInHand().shrink(1); if (vat.tryStartGrowth()) ctx.getPlayer().sendSystemMessage(Component.literal("[BioForge] §aGrowth started!").withStyle(ChatFormatting.GREEN)); else ctx.getPlayer().sendSystemMessage(Component.literal("[BioForge] DNA inserted. Add catalyst.").withStyle(ChatFormatting.YELLOW)); return InteractionResult.SUCCESS; }
        ctx.getPlayer().sendSystemMessage(Component.literal("[BioForge] Vat already has DNA.").withStyle(ChatFormatting.RED)); return InteractionResult.FAIL;
    }
}
