package io.github.magicrpg.entity;

import io.github.magicrpg.MagicRpg;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

public final class ChalkGuardian extends Monster {
    public ChalkGuardian(EntityType<? extends ChalkGuardian> type, Level level) {
        super(type, level);
        xpReward = 3;
    }

    public static AttributeSupplier.Builder attributes() {
        return createMonsterAttributes().add(Attributes.MAX_HEALTH, 28).add(Attributes.MOVEMENT_SPEED, .25)
            .add(Attributes.ATTACK_DAMAGE, 6).add(Attributes.ARMOR, 4).add(Attributes.FOLLOW_RANGE, 18);
    }

    public static boolean canSpawn(EntityType<ChalkGuardian> type, ServerLevelAccessor level,
                                   EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL && random.nextInt(8) == 0
            && level.getBlockState(pos.below()).is(MagicRpg.WHITE_STONE.get());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1, true));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (!super.hurtServer(level, source, amount)) {
            return false;
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, MagicRpg.WHITE_STONE.get().defaultBlockState()),
            getX(), getY() + 0.55, getZ(), 14, 0.3, 0.32, 0.3, 0.09);
        return true;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.CALCITE_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CALCITE_BREAK;
    }
}
