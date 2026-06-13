package com.bioforge.mod.client;

import com.bioforge.mod.blocks.SurgicalTableBlockEntity;
import com.bioforge.mod.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

public class SurgicalTableMenu extends AbstractContainerMenu {

    private final BlockPos pos;

    protected SurgicalTableMenu(MenuType<?> type, int id, Inventory inv, BlockPos pos, @Nullable IItemHandler input) {
        super(type, id);
        this.pos = pos;
    }

    public SurgicalTableMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(ModMenuTypes.SURGICAL_TABLE.get(), id, inv, buf.readBlockPos(), null);
    }

    public SurgicalTableMenu(int id, Inventory inv, BlockPos pos) {
        this(ModMenuTypes.SURGICAL_TABLE.get(), id, inv, pos, null);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
