package com.bioforge.mod.capabilities;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerBioDataProvider implements ICapabilitySerializable<CompoundTag> {

    private final PlayerBioData data = new PlayerBioData();
    private final LazyOptional<PlayerBioData> optional = LazyOptional.of(() -> data);

    public PlayerBioDataProvider() {
        data.initDefaultLimbs();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == BioForgeCapabilities.PLAYER_BIO_DATA) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return data.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        data.deserializeNBT(nbt);
    }

    public void invalidate() {
        optional.invalidate();
    }
}
