package com.bioforge.mod.blocks;

import com.bioforge.mod.BioForgeMod;
import com.bioforge.mod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import com.bioforge.mod.client.SurgicalTableMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class SurgicalTableBlock extends BaseEntityBlock {

    public SurgicalTableBlock() {
        super(BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.METAL).strength(3.5f).requiresCorrectToolForDrops());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SurgicalTableBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return BaseEntityBlock.createTickerHelper(type, ModBlockEntities.SURGICAL_TABLE.get(), (lvl, pos, st, be) -> {
            if (lvl.isClientSide) return;
            be.tickProcedure();
        });
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.CONSUME;
        NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("Surgical Table");
            }
            @Override
            public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
                return new SurgicalTableMenu(id, playerInv, pos);
            }
        }, pos);
        return InteractionResult.CONSUME;
    }
}
