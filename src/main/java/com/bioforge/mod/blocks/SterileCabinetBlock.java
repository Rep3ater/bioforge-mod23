package com.bioforge.mod.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class SterileCabinetBlock extends Block {
    public SterileCabinetBlock() { super(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(2.5f)); }
}
