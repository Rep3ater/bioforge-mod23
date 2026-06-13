package com.bioforge.mod.blocks;

import com.bioforge.mod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity for the Surgical Table.
 * Slots: 0=tool, 1=material, 2=auxiliary, 3=output
 */
public class SurgicalTableBlockEntity extends BlockEntity {

    private final ItemStackHandler inventory = new ItemStackHandler(4) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    private final LazyOptional<IItemHandler> inventoryOptional = LazyOptional.of(() -> inventory);

    private String activeProcedureId = "";
    private String patientUuid = "";
    private int procedureProgressTicks = 0;
    private int procedureDurationTicks = 0;

    public SurgicalTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SURGICAL_TABLE.get(), pos, state);
    }

    public void startProcedure(String procedureId, String patientUuid, int durationTicks) {
        this.activeProcedureId = procedureId;
        this.patientUuid = patientUuid;
        this.procedureDurationTicks = durationTicks;
        this.procedureProgressTicks = 0;
        setChanged();
    }

    public void cancelProcedure() {
        activeProcedureId = "";
        patientUuid = "";
        procedureProgressTicks = 0;
        procedureDurationTicks = 0;
        setChanged();
    }

    public boolean hasProcedureInProgress() { return !activeProcedureId.isEmpty(); }

    public float getProcedureProgress() {
        return procedureDurationTicks == 0 ? 1f : (float) procedureProgressTicks / procedureDurationTicks;
    }

    public void tickProcedure() {
        if (!activeProcedureId.isEmpty() && procedureDurationTicks > 0) {
            procedureProgressTicks++;
            if (procedureProgressTicks >= procedureDurationTicks) completeProcedure();
        }
    }

    private void completeProcedure() {
        if (level == null || level.isClientSide || patientUuid.isEmpty()) return;
        try {
            java.util.UUID uuid = java.util.UUID.fromString(patientUuid);
            for (net.minecraft.world.entity.player.Player player : level.players()) {
                if (player.getUUID().equals(uuid)) {
                    com.bioforge.mod.procedures.ProcedureRegistry.execute(
                            activeProcedureId, player, player.getId(), new net.minecraft.nbt.CompoundTag());
                    break;
                }
            }
        } catch (IllegalArgumentException ignored) {}
        cancelProcedure();
    }

    public ItemStackHandler getInventory() { return inventory; }
    public String getActiveProcedureId() { return activeProcedureId; }
    public String getPatientUuid() { return patientUuid; }

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
        tag.putString("procedure_id", activeProcedureId);
        tag.putString("patient_uuid", patientUuid);
        tag.putInt("progress", procedureProgressTicks);
        tag.putInt("duration", procedureDurationTicks);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("inventory")) inventory.deserializeNBT(tag.getCompound("inventory"));
        activeProcedureId = tag.getString("procedure_id");
        patientUuid = tag.getString("patient_uuid");
        procedureProgressTicks = tag.getInt("progress");
        procedureDurationTicks = tag.getInt("duration");
    }
}
