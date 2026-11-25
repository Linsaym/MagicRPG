package io.github.magicrpg.compat.reforged;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.Block;
import thaumcraft.block.InfusionStabiliser;
import thaumcraft.infusion.InfusionMatrixBlockEntity;
import thaumcraft.infusion.PedestalBlockEntity;

final class MatrixAsymmetry {
    private MatrixAsymmetry() {}

    static void report(ServerPlayer player, InfusionMatrixBlockEntity matrix) {
        Level level = player.level();
        BlockPos center = matrix.getBlockPos();
        List<BlockPos> pedestals = matrix.getPedestals();
        reportPedestals(player, level, center, pedestals);
        reportStabilisers(player, level, center, pedestals);
    }

    private static void reportPedestals(ServerPlayer player, Level level, BlockPos center, List<BlockPos> pedestals) {
        for (BlockPos pos : pedestals) {
            if (!(level.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal)) {
                continue;
            }
            BlockPos opposite = opposite(center, pos);
            if (!level.hasChunkAt(opposite) || !(level.getBlockEntity(opposite) instanceof PedestalBlockEntity partner)) {
                reportBlock(player, pos, pedestal.getBlockState().getBlock());
            } else if (!pedestal.getStack().isEmpty() && partner.getStack().isEmpty()) {
                player.sendSystemMessage(Component.translatable("message.magicrpg.matrix_unpaired_item",
                    pos.getX(), pos.getY(), pos.getZ()));
            }
        }
    }

    private static void reportStabilisers(ServerPlayer player, Level level, BlockPos center, List<BlockPos> pedestals) {
        // Match Reforged 1.1.3's scan: twelve blocks horizontally, ten below and five above.
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-12, -10, -12), center.offset(12, 5, 12))) {
            if ((pos.getX() == center.getX() && pos.getZ() == center.getZ()) || !level.hasChunkAt(pos)) {
                continue;
            }
            Block block = level.getBlockState(pos).getBlock();
            if (!canStabilise(block, level, pos) || pedestals.contains(pos)) {
                continue;
            }
            BlockPos opposite = opposite(center, pos);
            // Reforged checks the opposite block at the original stabiliser's position.
            if (!level.hasChunkAt(opposite) || !canStabilise(level.getBlockState(opposite).getBlock(), level, pos)) {
                reportBlock(player, pos, block);
            }
        }
    }

    private static boolean canStabilise(Block block, Level level, BlockPos pos) {
        return block instanceof AbstractSkullBlock
            || block instanceof InfusionStabiliser stabiliser && stabiliser.canStabiliseInfusion(level, pos);
    }

    private static BlockPos opposite(BlockPos center, BlockPos pos) {
        return new BlockPos(2 * center.getX() - pos.getX(), pos.getY(), 2 * center.getZ() - pos.getZ());
    }

    private static void reportBlock(ServerPlayer player, BlockPos pos, Block block) {
        player.sendSystemMessage(Component.translatable("message.magicrpg.matrix_unpaired_block",
            block.getName(), pos.getX(), pos.getY(), pos.getZ()));
    }
}
