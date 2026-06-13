package com.bioforge.mod.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class SurgicalLightBlock extends Block {
    public SurgicalLightBlock() { super(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(1f).lightLevel(s->15)); }
}
