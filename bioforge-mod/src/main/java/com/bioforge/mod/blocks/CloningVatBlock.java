package com.bioforge.mod.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class CloningVatBlock extends Block {
    public CloningVatBlock() { super(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_CYAN).strength(4f).lightLevel(s->4).noOcclusion()); }
}
