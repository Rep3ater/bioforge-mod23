package com.bioforge.mod.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class ProstheticsWorkbenchBlock extends Block {
    public ProstheticsWorkbenchBlock() { super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3f).requiresCorrectToolForDrops()); }
}
