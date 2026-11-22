package io.github.magicrpg.entity;

import io.github.magicrpg.MagicRpg;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

public final class StarHare extends PathfinderMob {
    private int lastScare = -40;

    public StarHare(EntityType<? extends StarHare> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 8).add(Attributes.MOVEMENT_SPEED, 0.32);
    }

    public static boolean canSpawn(EntityType<StarHare> type, ServerLevelAccessor level,
                                   EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(MagicRpg.LILAC_GRASS.get());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 8, 1.15, 1.55) {
            @Override
            public void start() {
                super.start();
                if (tickCount - lastScare >= 40 && level() instanceof ServerLevel server) {
                    lastScare = tickCount;
                    server.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.45, getZ(),
                        7, 0.25, 0.22, 0.25, 0.025);
                }
            }
        });
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.75));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }
}
