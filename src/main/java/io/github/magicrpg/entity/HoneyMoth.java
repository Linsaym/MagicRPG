package io.github.magicrpg.entity;

import io.github.magicrpg.Goldenwood;
import io.github.magicrpg.world.HoneybellBlock;
import io.github.magicrpg.world.PortalRoute;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class HoneyMoth extends AmbientCreature {
    private @Nullable BlockPos flower;
    private int nextVisit;

    public HoneyMoth(EntityType<? extends HoneyMoth> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder attributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 5).add(Attributes.MOVEMENT_SPEED, 0.24);
    }

    public static boolean canSpawn(EntityType<HoneyMoth> type, ServerLevelAccessor level,
                                   EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return level.getLevel().dimension().equals(PortalRoute.GOLDENWOOD.dimension())
            && Habitat.find(pos, 7, 3, p -> level.getBlockState(p).is(Goldenwood.HONEYBELL.get()))
                .isPresent();
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (tickCount >= nextVisit && (flower == null || tickCount % 66 == 0)) {
            flower = Habitat.find(blockPosition(), 7, 4, p -> {
                var state = level.getBlockState(p);
                return state.is(Goldenwood.HONEYBELL.get()) && !state.getValue(BlockStateProperties.LIT);
            }).orElse(null);
            nextVisit = tickCount + 40;
        }

        double angle = tickCount * 0.145 + getId();
        Vec3 motion = new Vec3(Math.cos(angle) * 0.07, Math.sin(angle * 0.7) * 0.025, Math.sin(angle) * 0.07);
        var visited = flower;
        if (visited != null) {
            var direction = Vec3.atBottomCenterOf(visited).add(0, 0.75, 0).subtract(position());
            if (direction.lengthSqr() > 0.22) {
                motion = direction.normalize().scale(0.14).add(motion.scale(0.36));
            } else {
                var state = level.getBlockState(visited);
                if (state.is(Goldenwood.HONEYBELL.get()) && !state.getValue(BlockStateProperties.LIT)) {
                    level.setBlockAndUpdate(visited, state.setValue(BlockStateProperties.LIT, true));
                    level.sendParticles(HoneybellBlock.POLLEN_DUST, getX(), getY(), getZ(), 6, 0.2, 0.2, 0.2, 0.02);
                }
                flower = null;
                nextVisit = tickCount + 100;
            }
        }
        if (isInWater() || !level.getBlockState(blockPosition()).getFluidState().isEmpty()) {
            motion = motion.add(0, 0.12, 0);
        }
        setDeltaMovement(motion);
        setYRot((float) Math.toDegrees(Math.atan2(-motion.x, motion.z)));
        yBodyRot = getYRot();
    }
}
