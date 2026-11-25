package io.github.magicrpg.entity;

import io.github.magicrpg.IronWastelands;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public final class WastelandGiant extends Monster {
    public static final int SWEEP_IMPACT_TICK = 12;
    public static final int SWEEP_DURATION = 23;
    private static final long NO_SWEEP = -1;
    private static final EntityDataAccessor<Long> SWEEP_START =
        SynchedEntityData.defineId(WastelandGiant.class, EntityDataSerializers.LONG);

    public WastelandGiant(EntityType<? extends WastelandGiant> type, Level level) {
        super(type, level);
        xpReward = isColossus() ? 18 : 12;
    }

    public static AttributeSupplier.Builder attributes(double health, double speed, double damage, double armor,
                                                        double knockbackResistance) {
        return createMonsterAttributes().add(Attributes.MAX_HEALTH, health).add(Attributes.MOVEMENT_SPEED, speed)
            .add(Attributes.ATTACK_DAMAGE, damage).add(Attributes.ARMOR, armor)
            .add(Attributes.KNOCKBACK_RESISTANCE, knockbackResistance).add(Attributes.ATTACK_KNOCKBACK, 1.3)
            .add(Attributes.FOLLOW_RANGE, 36).add(Attributes.STEP_HEIGHT, 1);
    }

    public boolean isColossus() {
        return getType() == IronWastelands.IRON_COLOSSUS.get();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SWEEP_START, NO_SWEEP);
    }

    public float sweepTime(float partialTick) {
        long start = entityData.get(SWEEP_START);
        return start == NO_SWEEP ? 0 : Mth.clamp(level().getGameTime() - start + partialTick, 0, SWEEP_DURATION);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, isColossus() ? new ColossusSweepGoal() : new MeleeAttackGoal(this, 1, true));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, .85));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, .72));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, true,
            (candidate, level) -> isColossus() ? IronWastelands.isInfected(candidate)
                : IronWastelands.isConstruct(candidate)));
    }

    @Override
    protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
        if (!(source.getEntity() instanceof Mob)) {
            super.dropAllDeathLoot(level, source);
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return isColossus() ? SoundEvents.IRON_GOLEM_HURT : SoundEvents.ZOMBIE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return isColossus() ? SoundEvents.IRON_GOLEM_DEATH : SoundEvents.ZOMBIE_DEATH;
    }

    private final class ColossusSweepGoal extends MeleeAttackGoal {
        private @Nullable LivingEntity sweepTarget;

        private ColossusSweepGoal() {
            super(WastelandGiant.this, 1, true);
        }

        @Override
        public boolean canContinueToUse() {
            return sweepTarget != null || super.canContinueToUse();
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target) {
            if (canPerformAttack(target)) {
                sweepTarget = target;
                entityData.set(SWEEP_START, level().getGameTime());
                getNavigation().stop();
                faceSweepTarget(target);
                playSound(SoundEvents.IRON_GOLEM_REPAIR, 1, .65F);
            }
        }

        @Override
        public void tick() {
            var target = sweepTarget;
            if (target == null) {
                super.tick();
                return;
            }

            int elapsed = (int) (level().getGameTime() - entityData.get(SWEEP_START));
            if (elapsed < SWEEP_IMPACT_TICK - 3 && target.isAlive()) {
                faceSweepTarget(target);
            }
            if (elapsed == SWEEP_IMPACT_TICK && canAttack(target) && isWithinMeleeAttackRange(target)
                && getSensing().hasLineOfSight(target) && isInFront(target)) {
                doHurtTarget((ServerLevel) level(), target);
            }
            if (elapsed >= SWEEP_DURATION) {
                finishSweep();
                // Reacquire a path immediately if the target moved away during the swing.
                if (getTarget() != null) {
                    getNavigation().moveTo(getTarget(), 1);
                }
            }
        }

        @Override
        public void stop() {
            finishSweep();
            super.stop();
        }

        private void finishSweep() {
            sweepTarget = null;
            entityData.set(SWEEP_START, NO_SWEEP);
        }

        private void faceSweepTarget(LivingEntity target) {
            float yaw = targetYaw(target);
            setYRot(yaw);
            setYBodyRot(yaw);
            getLookControl().setLookAt(target, 30, 30);
        }

        private boolean isInFront(LivingEntity target) {
            return Math.abs(Mth.wrapDegrees(targetYaw(target) - getYRot())) <= 70;
        }

        private float targetYaw(LivingEntity target) {
            return (float) (Mth.atan2(target.getZ() - getZ(), target.getX() - getX()) * Mth.RAD_TO_DEG) - 90;
        }
    }
}
