package io.github.magicrpg.entity;

import io.github.magicrpg.MagicRpg;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class MeadowPaladin extends PathfinderMob {
    public static final int GUARD = 0;
    public static final int ADVANCE = 1;
    public static final int SWEEP = 2;
    public static final int OVERHEAD = 3;
    public static final int STUCK = 4;
    public static final int RECOVER = 5;
    public static final int RESONANCE = 6;
    public static final int SWEEP_WINDUP = 25;
    public static final int OVERHEAD_WINDUP = 33;
    public static final int STUCK_DURATION = 42;
    public static final int RECOVERY_DURATION = 15;
    public static final int RESONANCE_WINDUP = 40;
    private static final double RING_INNER = 3.2;
    private static final double RING_OUTER = 8.0;
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(
        MeadowPaladin.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE_TICK = SynchedEntityData.defineId(
        MeadowPaladin.class, EntityDataSerializers.INT);

    private @Nullable BlockPos post;
    private final ServerBossEvent bossBar = new ServerBossEvent(UUID.randomUUID(), getType().getDescription(),
        BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
    private boolean overheadNext;
    private int strikesUntilResonance;
    private float attackYaw;

    public MeadowPaladin(EntityType<? extends MeadowPaladin> type, Level level) {
        super(type, level);
        bossBar.setVisible(false);
        xpReward = 32;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 150).add(Attributes.MOVEMENT_SPEED, 0.27)
            .add(Attributes.ARMOR, 8).add(Attributes.KNOCKBACK_RESISTANCE, 1)
            .add(Attributes.FOLLOW_RANGE, 36);
    }

    public void guard(BlockPos pos) {
        post = pos;
        setHomeTo(pos, 16);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, GUARD).define(PHASE_TICK, 0);
    }

    public int phase() {
        return entityData.get(PHASE);
    }

    public int phaseTick() {
        return entityData.get(PHASE_TICK);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    private void enter(int phase) {
        entityData.set(PHASE, phase);
        entityData.set(PHASE_TICK, 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (post != null) {
            output.putLong("GuardPost", post.asLong());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.getLong("GuardPost").map(BlockPos::of).ifPresent(this::guard);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (post != null && source.getEntity() instanceof ServerPlayer player && !player.isCreative()
            && !inGuardZone(player, phase() == GUARD)) {
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        if (post == null) {
            guard(blockPosition());
        }
        int phase = phase();
        bossBar.setProgress(getHealth() / getMaxHealth());
        bossBar.setVisible(phase != GUARD && (getTarget() != null || phase >= SWEEP));
        if (phase >= SWEEP) {
            tickAttack(level, phase);
            return;
        }
        var target = getTarget();
        if (target instanceof ServerPlayer player && !inGuardZone(player, false)) {
            setTarget(null);
            target = null;
        }
        if (target == null && level.getDifficulty() != Difficulty.PEACEFUL) {
            var nearby = level.getEntitiesOfClass(ServerPlayer.class, new AABB(post).inflate(18),
                player -> inGuardZone(player, true) && hasLineOfSight(player));
            for (var player : nearby) {
                if (target == null || distanceToSqr(player) < distanceToSqr(target)) {
                    target = player;
                }
            }
            setTarget(target);
        }
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            setTarget(null);
            target = null;
        }
        if (target == null) {
            if (post.distToCenterSqr(getX(), getY() + .5, getZ()) > .36) {
                if (phase != ADVANCE) {
                    enter(ADVANCE);
                }
                if (tickCount % 10 == 0) {
                    getNavigation().moveTo(post.getX() + .5, post.getY(), post.getZ() + .5, .9);
                }
            } else {
                getNavigation().stop();
                if (phase != GUARD) {
                    enter(GUARD);
                }
                face(0);
            }
            return;
        }
        lookAt(target, 30, 10);
        if (distanceToSqr(target) <= 24 && hasLineOfSight(target)) {
            getNavigation().stop();
            attackYaw = (float) (Mth.atan2(target.getZ() - getZ(), target.getX() - getX()) * Mth.RAD_TO_DEG) - 90;
            face(attackYaw);
            if (getHealth() <= getMaxHealth() / 2 && strikesUntilResonance == 0) {
                enter(RESONANCE);
                strikesUntilResonance = 2;
                playSound(SoundEvents.WARDEN_SONIC_CHARGE, 1, 1.2F);
            } else {
                enter(overheadNext ? OVERHEAD : SWEEP);
                overheadNext = !overheadNext;
                strikesUntilResonance = Math.max(0, strikesUntilResonance - 1);
                playSound(SoundEvents.IRON_GOLEM_REPAIR, 1, .65F);
            }
        } else {
            if (phase != ADVANCE) {
                enter(ADVANCE);
            }
            if (tickCount % 5 == 0) {
                getNavigation().moveTo(target, 1);
            }
        }
    }

    private boolean inGuardZone(ServerPlayer player, boolean starting) {
        if (!player.isAlive() || player.isSpectator() || player.isCreative() || post == null) {
            return false;
        }
        double x = player.getX() - post.getX() - .5;
        double y = player.getY() - post.getY();
        double z = player.getZ() - post.getZ() - .5;
        return Math.abs(x) <= (starting ? 13 : 16) && y >= -2 && y <= (starting ? 3 : 5)
            && z >= -7 && z <= (starting ? 12 : 15);
    }

    private void face(float yaw) {
        setYRot(yaw);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
    }

    private void tickAttack(ServerLevel level, int phase) {
        getNavigation().stop();
        face(attackYaw);
        int elapsed = phaseTick() + 1;
        entityData.set(PHASE_TICK, elapsed);
        if (elapsed % 5 == 0) {
            if (phase == RESONANCE) {
                ring(level, RING_INNER, false);
                ring(level, RING_OUTER, false);
            } else if (phase == OVERHEAD || phase == SWEEP) {
                markStrike(level, phase == OVERHEAD);
            }
        }
        if (phase == SWEEP && elapsed == SWEEP_WINDUP) {
            strike(level, false);
            enter(RECOVER);
        } else if (phase == OVERHEAD && elapsed == OVERHEAD_WINDUP) {
            strike(level, true);
            enter(STUCK);
        } else if (phase == RESONANCE && elapsed == RESONANCE_WINDUP) {
            releaseResonance(level);
            enter(STUCK);
        } else if ((phase == STUCK && elapsed >= STUCK_DURATION)
            || (phase == RECOVER && elapsed >= RECOVERY_DURATION)) {
            enter(ADVANCE);
        }
    }

    private void markStrike(ServerLevel level, boolean overhead) {
        Vec3 forward = Vec3.directionFromRotation(0, attackYaw);
        Vec3 side = new Vec3(-forward.z, 0, forward.x);
        for (int i = 0; i <= 10; i++) {
            if (overhead) {
                Vec3 point = position().add(forward.scale(.6 * i));
                mark(level, point.add(side.scale(1.25)));
                mark(level, point.add(side.scale(-1.25)));
            } else {
                double angle = (i - 5) * Math.acos(.13) / 5;
                mark(level, position().add(forward.scale(5.5 * Math.cos(angle)))
                    .add(side.scale(5.5 * Math.sin(angle))));
            }
        }
    }

    private void mark(ServerLevel level, Vec3 point) {
        level.sendParticles(ParticleTypes.WITCH, point.x, point.y + .22, point.z, 1, 0, 0, 0, 0);
    }

    private void ring(ServerLevel level, double radius, boolean impact) {
        for (int i = 0; i < 32; i++) {
            double angle = i * Math.TAU / 32;
            level.sendParticles(impact ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.WITCH,
                getX() + radius * Math.cos(angle), getY() + .24, getZ() + radius * Math.sin(angle),
                impact ? 3 : 1, .07, .02, .07, impact ? .08 : 0);
        }
    }

    private void releaseResonance(ServerLevel level) {
        playSound(SoundEvents.WARDEN_SONIC_BOOM, 1.2F, .85F);
        for (double radius = RING_INNER; radius <= RING_OUTER; radius += 1.2) {
            ring(level, radius, true);
        }
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        var source = damageSources().indirectMagic(this, this);
        for (var player : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(RING_OUTER, 1, RING_OUTER),
            player -> canAttack(player) && !player.isCreative() && hasLineOfSight(player)
                && Math.abs(player.getY() - getY()) <= 2)) {
            Vec3 offset = player.position().subtract(position()).multiply(1, 0, 1);
            double distance = offset.horizontalDistance();
            if (distance >= RING_INNER && distance <= RING_OUTER && player.hurtServer(level, source, 10)) {
                player.knockback(.7, -offset.x, -offset.z, source, 10);
            }
        }
    }

    private void strike(ServerLevel level, boolean overhead) {
        Vec3 forward = Vec3.directionFromRotation(0, attackYaw);
        Vec3 impact = position().add(forward.scale(3.6));
        playSound(overhead ? SoundEvents.DEEPSLATE_BREAK : SoundEvents.PLAYER_ATTACK_SWEEP, 1.4F, .68F);
        if (overhead) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,
                MagicRpg.WHITE_STONE.get().defaultBlockState()), impact.x, impact.y + .15, impact.z,
                34, .65, .25, .65, .13);
        }
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        var source = damageSources().mobAttack(this);
        for (var player : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(6, 2.5, 6),
            player -> canAttack(player) && !player.isCreative() && Math.abs(player.getY() - getY()) <= 2.5
                && hasLineOfSight(player))) {
            Vec3 offset = player.position().subtract(position()).multiply(1, 0, 1);
            double reach = offset.dot(forward);
            double side = Math.abs(offset.x * forward.z - offset.z * forward.x);
            boolean hit = overhead ? reach > 0 && reach <= 6 && side < 1.25
                : offset.lengthSqr() <= 30.25 && offset.normalize().dot(forward) >= .13;
            if (hit && player.hurtServer(level, source, overhead ? 15 : 10)) {
                player.knockback(overhead ? .35 : .8, -offset.x, -offset.z, source,
                    overhead ? 15 : 10);
            }
        }
    }
}
