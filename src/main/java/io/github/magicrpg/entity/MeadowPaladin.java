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
    public static final int SIGIL = 7;
    public static final int SWEEP_WINDUP = 17;
    public static final int OVERHEAD_WINDUP = 25;
    public static final int STUCK_DURATION = 24;
    public static final int RECOVERY_DURATION = 9;
    public static final int RESONANCE_WINDUP = 37;
    public static final int SIGIL_WINDUP = 30;
    private static final int REGENERATION_INTERVAL = 40;
    private static final double SWEEP_REACH = 6.2;
    private static final double SWEEP_HALF_ANGLE = Math.toRadians(109);
    private static final double CLEAVE_REACH = 8.5;
    private static final double CLEAVE_HALF_WIDTH = 1.85;
    private static final double SIGIL_RADIUS = 3.4;
    private static final double RING_OUTER = 8.0;
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(
        MeadowPaladin.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE_TICK = SynchedEntityData.defineId(
        MeadowPaladin.class, EntityDataSerializers.INT);

    private @Nullable BlockPos post;
    private final ServerBossEvent bossBar = new ServerBossEvent(UUID.randomUUID(), getType().getDescription(),
        BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
    private int lastAttack;
    private int resonanceCooldown;
    private int sigilCooldown;
    private float attackYaw;
    private Vec3 sigilCenter = Vec3.ZERO;

    public MeadowPaladin(EntityType<? extends MeadowPaladin> type, Level level) {
        super(type, level);
        bossBar.setVisible(false);
        xpReward = 32;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 250).add(Attributes.MOVEMENT_SPEED, .33)
            .add(Attributes.ARMOR, 12).add(Attributes.KNOCKBACK_RESISTANCE, 1)
            .add(Attributes.FOLLOW_RANGE, 36);
    }

    public void guard(BlockPos pos) {
        post = pos;
        setHomeTo(pos, 24);
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
        resonanceCooldown = Math.max(0, resonanceCooldown - 1);
        sigilCooldown = Math.max(0, sigilCooldown - 1);
        if (tickCount % REGENERATION_INTERVAL == 0 && isAlive() && getHealth() < getMaxHealth()) {
            heal(1.0F);
        }
        bossBar.setProgress(getHealth() / getMaxHealth());
        bossBar.setVisible(phase != GUARD && (getTarget() != null || phase >= SWEEP));
        if (phase >= SWEEP) {
            tickAttack(level, phase);
            return;
        }
        var target = getTarget();
        if (target != null && (!(target instanceof ServerPlayer player) || !inGuardZone(player, false))) {
            setTarget(null);
            target = null;
        }
        if (target == null && level.getDifficulty() != Difficulty.PEACEFUL) {
            var nearby = level.getEntitiesOfClass(ServerPlayer.class, new AABB(post).inflate(24),
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
        double distance = target.position().distanceTo(position());
        if (target instanceof ServerPlayer player && distance <= 18 && hasLineOfSight(player)
            && startAttack(player, distance)) {
            getNavigation().stop();
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
        return Math.abs(x) <= (starting ? 17 : 20) && y >= -2 && y <= 5
            && z >= -9 && z <= (starting ? 16 : 19);
    }

    private boolean startAttack(ServerPlayer target, double distance) {
        int attack;
        if (distance <= 7 && resonanceCooldown == 0 && getHealth() <= getMaxHealth() / 2
            && (lastAttack != RESONANCE && getRandom().nextInt(3) == 0)) {
            attack = RESONANCE;
            resonanceCooldown = 100;
        } else if (sigilCooldown == 0 && (distance > 7 || distance > 4 && getRandom().nextInt(4) == 0)) {
            attack = SIGIL;
            sigilCenter = target.position();
            sigilCooldown = 70;
        } else if (distance <= 7) {
            attack = distance < 2.8 ? SWEEP : distance > 5.5 ? OVERHEAD
                : getRandom().nextBoolean() ? SWEEP : OVERHEAD;
        } else {
            return false;
        }
        aimAt(target);
        face(attackYaw);
        lastAttack = attack;
        enter(attack);
        playSound(attack == RESONANCE || attack == SIGIL ? SoundEvents.WARDEN_SONIC_CHARGE
            : SoundEvents.IRON_GOLEM_REPAIR, 1, attack == SIGIL ? 1.45F : .75F);
        return true;
    }

    private void aimAt(ServerPlayer target) {
        attackYaw = (float) (Mth.atan2(target.getZ() - getZ(), target.getX() - getX()) * Mth.RAD_TO_DEG) - 90;
    }

    private void face(float yaw) {
        setYRot(yaw);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
    }

    private void tickAttack(ServerLevel level, int phase) {
        getNavigation().stop();
        int elapsed = phaseTick() + 1;
        if (getTarget() instanceof ServerPlayer player && inGuardZone(player, false) && hasLineOfSight(player)) {
            if (phase == SWEEP && elapsed <= SWEEP_WINDUP - 6
                || phase == OVERHEAD && elapsed <= OVERHEAD_WINDUP - 8) {
                aimAt(player);
            } else if (phase == SIGIL && elapsed <= SIGIL_WINDUP - 16) {
                sigilCenter = player.position();
                aimAt(player);
            }
        }
        face(attackYaw);
        entityData.set(PHASE_TICK, elapsed);
        boolean lockedOnThisTick = (phase == SWEEP && elapsed == SWEEP_WINDUP - 6)
            || (phase == OVERHEAD && elapsed == OVERHEAD_WINDUP - 8)
            || (phase == SIGIL && elapsed == SIGIL_WINDUP - 16);
        if (elapsed == 1 || elapsed % 4 == 0 || lockedOnThisTick) {
            if (phase == RESONANCE) {
                ring(level, position(), RING_OUTER, false);
            } else if (phase == SIGIL) {
                ring(level, sigilCenter, SIGIL_RADIUS, false);
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
            releaseCircle(level, position(), RING_OUTER, 14);
            enter(STUCK);
        } else if (phase == SIGIL && elapsed == SIGIL_WINDUP) {
            releaseCircle(level, sigilCenter, SIGIL_RADIUS, 13);
            enter(RECOVER);
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
                Vec3 point = position().add(forward.scale(CLEAVE_REACH * i / 10));
                mark(level, point.add(side.scale(CLEAVE_HALF_WIDTH)));
                mark(level, point.add(side.scale(-CLEAVE_HALF_WIDTH)));
            } else {
                double angle = (i - 5) * SWEEP_HALF_ANGLE / 5;
                mark(level, position().add(forward.scale(SWEEP_REACH * Math.cos(angle)))
                    .add(side.scale(SWEEP_REACH * Math.sin(angle))));
            }
        }
    }

    private void mark(ServerLevel level, Vec3 point) {
        level.sendParticles(ParticleTypes.WITCH, point.x, point.y + .22, point.z, 1, 0, 0, 0, 0);
    }

    private void ring(ServerLevel level, Vec3 center, double radius, boolean impact) {
        int points = Math.max(12, (int) (4 * radius));
        for (int i = 0; i < points; i++) {
            double angle = i * Math.TAU / points;
            level.sendParticles(impact ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.WITCH,
                center.x + radius * Math.cos(angle), center.y + .24, center.z + radius * Math.sin(angle),
                impact ? 3 : 1, .07, .02, .07, impact ? .08 : 0);
        }
    }

    private void releaseCircle(ServerLevel level, Vec3 center, double radius, float damage) {
        playSound(SoundEvents.WARDEN_SONIC_BOOM, 1.2F, .85F);
        for (int band = 1; band <= 3; band++) {
            ring(level, center, radius * band / 3, true);
        }
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        var source = damageSources().indirectMagic(this, this);
        var bounds = new AABB(center.x - radius, center.y - 3, center.z - radius,
            center.x + radius, center.y + 3, center.z + radius);
        for (var player : level.getEntitiesOfClass(ServerPlayer.class, bounds,
            player -> inGuardZone(player, false) && hasLineOfSight(player))) {
            Vec3 offset = player.position().subtract(center);
            if (offset.horizontalDistanceSqr() <= radius * radius && player.hurtServer(level, source, damage)) {
                player.knockback(.7, -offset.x, -offset.z, source, damage);
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
        for (var player : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(CLEAVE_REACH, 3, CLEAVE_REACH),
            player -> inGuardZone(player, false) && Math.abs(player.getY() - getY()) <= 3 && hasLineOfSight(player))) {
            Vec3 offset = player.position().subtract(position()).multiply(1, 0, 1);
            double reach = offset.dot(forward);
            double side = Math.abs(offset.x * forward.z - offset.z * forward.x);
            boolean hit = overhead ? reach >= 0 && reach <= CLEAVE_REACH && side <= CLEAVE_HALF_WIDTH
                : offset.lengthSqr() <= SWEEP_REACH * SWEEP_REACH
                    && offset.normalize().dot(forward) >= Math.cos(SWEEP_HALF_ANGLE);
            float damage = overhead ? 19 : 14;
            if (hit && player.hurtServer(level, source, damage)) {
                player.knockback(overhead ? .45 : .9, -offset.x, -offset.z, source, damage);
            }
        }
    }
}
