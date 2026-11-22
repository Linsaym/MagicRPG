package io.github.magicrpg.world;

import io.github.magicrpg.WayfarerMirrors;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class WayfarerMirrorEntity extends BlockEntity {
    public WayfarerMirrorEntity(BlockPos pos, BlockState state) {
        super(WayfarerMirrors.BLOCK_ENTITY.get(), pos, state);
    }

    @Nullable UUID pair() {
        return components().get(WayfarerMirrors.PAIR.get());
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        var pair = pair();
        if (level instanceof ServerLevel serverLevel && pair != null) {
            WayfarerMirrorLinks.get(serverLevel).remove(pair, WayfarerMirrorLinks.Endpoint.at(serverLevel, pos));
        }
        super.preRemoveSideEffects(pos, state);
    }
}
