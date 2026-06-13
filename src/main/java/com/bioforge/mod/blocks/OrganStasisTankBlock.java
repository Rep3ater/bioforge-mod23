package com.bioforge.mod.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class OrganStasisTankBlock extends Block {
    public OrganStasisTankBlock() { super(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BLUE).strength(3f).noOcclusion().lightLevel(s->2)); }
}
