package io.github.magicrpg.world;

import io.github.magicrpg.Goldenwood;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

public final class HoneybellBlock extends FlowerBlock {
    public static final DustParticleOptions POLLEN_DUST = new DustParticleOptions(0xDCEB93, 0.65F);

    public HoneybellBlock(Properties properties) {
        super(MobEffects.GLOWING, 5, properties);
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.LIT);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                BlockHitResult hit) {
        if (!state.getValue(BlockStateProperties.LIT)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.LIT, false));
            if (!player.addItem(Goldenwood.POLLEN.toStack())) {
                Block.popResource(level, pos, Goldenwood.POLLEN.toStack());
            }
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6F, 1.4F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(BlockStateProperties.LIT) && random.nextInt(9) == 0) {
            level.addParticle(POLLEN_DUST, pos.getX() + 0.5, pos.getY() + 0.55, pos.getZ() + 0.5,
                0, 0.035, 0);
        }
    }
}
