package com.bioforge.mod.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class BloodStorageBlock extends Block {
    public BloodStorageBlock() { super(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3f).requiresCorrectToolForDrops()); }
}
