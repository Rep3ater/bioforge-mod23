package com.bioforge.mod.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class BloodCentrifugeBlock extends Block {
    public BloodCentrifugeBlock() { super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3f).requiresCorrectToolForDrops()); }
}
