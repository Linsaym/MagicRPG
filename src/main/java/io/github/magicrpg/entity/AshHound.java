package io.github.magicrpg.entity;

import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class AshHound extends Monster {
    public enum AttackPhase { IDLE, WINDUP, DASH, RECOVERY }

    private static final EntityDataAccessor<Byte> PHASE = SynchedEntityData.defineId(AshHound.class, EntityDataSerializers.BYTE);
    private static final AttackPhase[] PHASES = AttackPhase.values();

    public AshHound(EntityType<? extends AshHound> type, Level level) {
        super(type, level);
        xpReward = 5;
    }

    public static AttributeSupplier.Builder attributes() {
        return createMonsterAttributes().add(Attributes.MAX_HEALTH, 24).add(Attributes.MOVEMENT_SPEED, 0.29)
            .add(Attributes.ATTACK_DAMAGE, 5).add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, (byte) AttackPhase.IDLE.ordinal());
    }

    public AttackPhase attackPhase() {
        return PHASES[entityData.get(PHASE)];
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PounceGoal());
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.85));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }

    private final class PounceGoal extends Goal {
        private static final int WINDUP_TICKS = 16;
        private static final int DASH_TICKS = 7;
        private static final int RECOVERY_TICKS = 22;
        private static final double ATTACK_RANGE = 4.5;

        private Vec3 heading = Vec3.ZERO;
        private int ticksLeft;
        private int repathDelay;

        PounceGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return getTarget() != null && getTarget().isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return attackPhase() != AttackPhase.IDLE || canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void stop() {
            if (attackPhase() == AttackPhase.DASH) {
                setDeltaMovement(0, getDeltaMovement().y, 0);
            }
            getNavigation().stop();
            setPhase(AttackPhase.IDLE, 0);
            repathDelay = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            switch (attackPhase()) {
                case IDLE -> {
                    if (target != null && target.isAlive()) {
                        chase(target);
                    }
                }
                case WINDUP -> windUp(target);
                case DASH -> dash(target);
                case RECOVERY -> {
                    if (--ticksLeft <= 0) {
                        setPhase(AttackPhase.IDLE, 0);
                    }
                }
            }
        }

        private void chase(LivingEntity target) {
            getLookControl().setLookAt(target, 30, 30);
            if (onGround() && distanceToSqr(target) <= ATTACK_RANGE * ATTACK_RANGE && getSensing().hasLineOfSight(target)) {
                getNavigation().stop();
                setDeltaMovement(0, getDeltaMovement().y, 0);
                setPhase(AttackPhase.WINDUP, WINDUP_TICKS);
                playSound(SoundEvents.RAVAGER_ROAR, 0.85F, 1.25F);
            } else if (--repathDelay <= 0) {
                getNavigation().moveTo(target, 1.1);
                repathDelay = 8;
            }
        }

        private void windUp(LivingEntity target) {
            if (target == null || !target.isAlive()) {
                recover();
                return;
            }
            getLookControl().setLookAt(target, 30, 30);
            if (ticksLeft % 4 == 0 && level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.7, getZ(), 5, 0.25, 0.15, 0.25, 0.015);
            }
            if (--ticksLeft <= 0) {
                Vec3 aim = target.position().subtract(position()).multiply(1, 0, 1);
                heading = aim.lengthSqr() < 0.01 ? getLookAngle().multiply(1, 0, 1).normalize() : aim.normalize();
                setPhase(AttackPhase.DASH, DASH_TICKS);
                playSound(SoundEvents.FIRECHARGE_USE, 0.8F, 1.2F);
                dash(target);
            }
        }

        private void dash(LivingEntity target) {
            if (target != null && target.isAlive() && getBoundingBox().inflate(0.28, 0.1, 0.28).intersects(target.getBoundingBox())
                && getSensing().hasLineOfSight(target) && level() instanceof ServerLevel server) {
                doHurtTarget(server, target);
                recover();
                return;
            }
            if (ticksLeft-- <= 0 || (ticksLeft < DASH_TICKS - 1 && horizontalCollision)) {
                recover();
                return;
            }
            setDeltaMovement(heading.x * 0.82, getDeltaMovement().y, heading.z * 0.82);
        }

        private void recover() {
            setDeltaMovement(0, getDeltaMovement().y, 0);
            setPhase(AttackPhase.RECOVERY, RECOVERY_TICKS);
            repathDelay = 0;
        }

        private void setPhase(AttackPhase phase, int duration) {
            entityData.set(PHASE, (byte) phase.ordinal());
            ticksLeft = duration;
        }
    }
}
