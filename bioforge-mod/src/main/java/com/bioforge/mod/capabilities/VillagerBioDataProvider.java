package com.bioforge.mod.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.Direction;

public class VillagerBioDataProvider implements ICapabilitySerializable<CompoundTag> {
    private final PlayerBioData data;
    private final LazyOptional<PlayerBioData> optional;

    public VillagerBioDataProvider() {
        this(new PlayerBioData());
    }

    public VillagerBioDataProvider(PlayerBioData initialData) {
        this.data = initialData;
        this.data.initDefaultLimbs();
        this.optional = LazyOptional.of(() -> data);
    }

    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == BioForgeCapabilities.PLAYER_BIO_DATA ? optional.cast() : LazyOptional.empty();
    }
    @Override public CompoundTag serializeNBT() { return data.serializeNBT(); }
    @Override public void deserializeNBT(CompoundTag nbt) { data.deserializeNBT(nbt); }
    public void invalidate() { optional.invalidate(); }
}
