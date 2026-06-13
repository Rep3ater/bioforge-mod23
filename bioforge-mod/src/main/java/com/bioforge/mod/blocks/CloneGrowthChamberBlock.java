package com.bioforge.mod.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class CloneGrowthChamberBlock extends Block {
    public CloneGrowthChamberBlock() { super(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_CYAN).strength(5f).noOcclusion().lightLevel(s->8)); }
}
