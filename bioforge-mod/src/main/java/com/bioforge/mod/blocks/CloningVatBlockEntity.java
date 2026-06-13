package com.bioforge.mod.blocks;

import com.bioforge.mod.cloning.CloningSystem;
import com.bioforge.mod.config.BioForgeConfig;
import com.bioforge.mod.registry.ModBlockEntities;
import com.bioforge.mod.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity for the Cloning Vat.
 * Slots: 0=DNA sample, 1=catalyst, 2=memory crystal, 3=clone matrix
 */
public class CloningVatBlockEntity extends BlockEntity {

    public enum GrowthState { EMPTY, GROWING, READY }

    private final ItemStackHandler inventory = new ItemStackHandler(4) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    private final LazyOptional<IItemHandler> inventoryOptional = LazyOptional.of(() -> inventory);

    private GrowthState state = GrowthState.EMPTY;
    private int growthTicks = 0;
    private int maxGrowthTicks = 12000;

    public CloningVatBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CLONING_VAT.get(), pos, blockState);
    }

    public boolean tick() {
        if (state != GrowthState.GROWING) return false;
        growthTicks++;
        if (growthTicks >= maxGrowthTicks) {
            state = GrowthState.READY;
            setChanged();
            return true;
        }
        return false;
    }

    public boolean tryStartGrowth() {
        if (state != GrowthState.EMPTY) return false;
        ItemStack dnaSample = inventory.getStackInSlot(0);
        ItemStack catalyst  = inventory.getStackInSlot(1);
        if (!dnaSample.is(ModItems.DNA_SAMPLE.get())) return false;
        if (!catalyst.is(ModItems.CLONING_CATALYST.get())) return false;

        ItemStack memoryCrystal = inventory.getStackInSlot(2);
        if (memoryCrystal.is(ModItems.MEMORY_CRYSTAL.get())) {
            CloningSystem.insertMemoryCrystal(dnaSample, memoryCrystal);
            inventory.setStackInSlot(2, ItemStack.EMPTY);
        }

        catalyst.shrink(1);
        inventory.setStackInSlot(1, catalyst);
        maxGrowthTicks = BioForgeConfig.CLONE_GROWTH_TICKS.get();
        growthTicks = 0;
        state = GrowthState.GROWING;
        setChanged();
        return true;
    }

    public boolean extractClone(net.minecraft.world.entity.player.Player operator) {
        if (state != GrowthState.READY) return false;
        if (!(level instanceof ServerLevel serverLevel)) return false;
        ItemStack dnaSample = inventory.getStackInSlot(0);
        if (!dnaSample.is(ModItems.DNA_SAMPLE.get())) return false;

        Vec3 spawnPos = Vec3.atCenterOf(worldPosition).add(0, 0, 1);
        CloningSystem.spawnClone(dnaSample, serverLevel, spawnPos, operator);

        inventory.setStackInSlot(0, ItemStack.EMPTY);
        inventory.setStackInSlot(3, ItemStack.EMPTY);
        state = GrowthState.EMPTY;
        growthTicks = 0;
        setChanged();
        return true;
    }

    public GrowthState getState() { return state; }
    public int getGrowthTicks() { return growthTicks; }
    public int getMaxGrowthTicks() { return maxGrowthTicks; }
    public float getGrowthProgress() {
        return maxGrowthTicks == 0 ? 1f : (float) growthTicks / maxGrowthTicks;
    }
    public ItemStackHandler getInventory() { return inventory; }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return inventoryOptional.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() { super.invalidateCaps(); inventoryOptional.invalidate(); }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", inventory.serializeNBT());
        tag.putString("state", state.name());
        tag.putInt("growth_ticks", growthTicks);
        tag.putInt("max_growth_ticks", maxGrowthTicks);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("inventory")) inventory.deserializeNBT(tag.getCompound("inventory"));
        try { state = GrowthState.valueOf(tag.getString("state")); }
        catch (Exception e) { state = GrowthState.EMPTY; }
        growthTicks = tag.getInt("growth_ticks");
        maxGrowthTicks = tag.getInt("max_growth_ticks");
    }
}
