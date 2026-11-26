package io.github.magicrpg.boss;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class BossActions {
    private BossActions() {}

    public static <B extends BossMob> BossAction<B> warning(Function<BossContext<B>, BossZone.Placed> zone,
                                                           ParticleOptions particle, int points) {
        if (points < 8 || points > 96) {
            throw new IllegalArgumentException("Warnings require 8–96 points");
        }
        return context -> outline(context, zone.apply(context), particle, points);
    }

    private static void outline(BossContext<?> context, BossZone.Placed zone, ParticleOptions particle, int points) {
        int granted = context.takeParticles(points);
        if (granted >= 8) {
            zone.outline(granted, at -> context.level().sendParticles(
                particle, at.x, at.y + .2, at.z, 1, 0, 0, 0, 0));
        }
    }

    public static <B extends BossMob> BossAction<B> particles(Function<BossContext<B>, Vec3> position,
                                                             ParticleOptions particle, int count,
                                                             Vec3 spread, double speed) {
        if (count < 1 || count > 96 || !finite(spread) || spread.x < 0 || spread.y < 0 || spread.z < 0
            || !Double.isFinite(speed) || speed < 0) {
            throw new IllegalArgumentException("Invalid particle effect");
        }
        return context -> {
            int granted = context.takeParticles(count);
            if (granted > 0) {
                Vec3 at = position.apply(context);
                context.level().sendParticles(particle, at.x, at.y, at.z, granted, spread.x, spread.y, spread.z, speed);
            }
        };
    }

    public static <B extends BossMob> BossAction<B> sound(SoundEvent sound, float volume, float pitch) {
        if (!(volume >= 0 && volume <= 4 && pitch > 0 && pitch <= 4)) {
            throw new IllegalArgumentException("Invalid sound volume or pitch");
        }
        return context -> {
            if (context.takeSound()) {
                var boss = context.boss();
                context.level().playSound(null, boss.getX(), boss.getY(), boss.getZ(), sound,
                    SoundSource.HOSTILE, volume, pitch);
            }
        };
    }

    public record Damage<B extends BossMob>(Function<B, DamageSource> source, float amount, double knockback,
                                            boolean requireSight) {
        public Damage {
            Objects.requireNonNull(source);
            if (!Float.isFinite(amount) || amount <= 0 || !(knockback >= 0 && knockback <= 4)) {
                throw new IllegalArgumentException("Invalid damage or knockback");
            }
        }
    }

    public static <B extends BossMob> BossAction<B> hit(Function<BossContext<B>, BossZone.Placed> zone,
                                                       Damage<B> damage) {
        return context -> hit(context, zone.apply(context), damage, null);
    }

    private static <B extends BossMob> void hit(BossContext<B> context, BossZone.Placed zone, Damage<B> damage,
                                               Set<UUID> touched) {
        var source = damage.source().apply(context.boss());
        for (var entity : context.level().getEntitiesOfClass(LivingEntity.class, zone.bounds().inflate(0.001))) {
            if (!context.isCasting()) {
                return;
            }
            if (!zone.contains(entity.position()) || !context.isHostile(entity)
                || damage.requireSight() && !context.boss().hasLineOfSight(entity)
                || touched != null && (touched.size() >= 512 || !touched.add(entity.getUUID()))) {
                continue;
            }
            if (entity.hurtServer(context.level(), source, damage.amount()) && damage.knockback() > 0) {
                Vec3 offset = entity.position().subtract(zone.origin());
                if (offset.horizontalDistanceSqr() > 1.0E-8) {
                    entity.knockback(damage.knockback(), -offset.x, -offset.z, source, damage.amount());
                }
            }
        }
    }

    public static <B extends BossMob> BossAction<B> wave(int first, int last, double radius, double thickness,
                                                        double halfHeight, Function<BossContext<B>, Vec3> origin,
                                                        Damage<B> damage, ParticleOptions particle) {
        if (first < 0 || last <= first || !(radius > 0 && radius <= BossZone.MAX_REACH)
            || !(thickness > 0 && thickness <= radius) || !(halfHeight > 0 && halfHeight <= BossZone.MAX_REACH)) {
            throw new IllegalArgumentException("Invalid wave distance or timing");
        }
        var key = new BossContext.Key<>(WaveState.class);
        return context -> {
            int elapsed = context.elapsed();
            if (elapsed < first || elapsed > last) {
                return;
            }
            var state = context.recall(key).orElseGet(() -> {
                var created = new WaveState(origin.apply(context));
                context.remember(key, created);
                return created;
            });
            double front = thickness + (radius - thickness) * (elapsed - first) / (last - first);
            var ring = new BossZone.Ring(Math.max(0, Math.min(state.front, front) - thickness), front)
                .at(state.origin, 0, halfHeight);
            state.front = front;
            outline(context, new BossZone.Circle(front).at(state.origin, 0, halfHeight), particle, 24);
            hit(context, ring, damage, state.touched);
        };
    }

    public static <B extends BossMob> BossAction<B> dash(Function<BossContext<B>, Vec3> direction, double speed) {
        if (!(speed > 0 && speed <= 3)) {
            throw new IllegalArgumentException("Dash speed must be in (0, 3]");
        }
        return context -> {
            var boss = context.boss();
            var horizontal = direction.apply(context).multiply(1, 0, 1);
            if (finite(horizontal) && horizontal.horizontalDistanceSqr() > 1.0E-8) {
                move(context, horizontal.normalize().scale(speed).add(0, boss.getDeltaMovement().y, 0));
            }
        };
    }

    public static <B extends BossMob> BossAction<B> leap(Function<BossContext<B>, Vec3> direction,
                                                        double speed, double rise) {
        if (!(speed >= 0 && speed <= 3 && rise > 0 && rise <= 2)) {
            throw new IllegalArgumentException("Invalid leap velocity");
        }
        return context -> {
            var boss = context.boss();
            if (boss.onGround()) {
                var horizontal = speed == 0 ? Vec3.ZERO : direction.apply(context).multiply(1, 0, 1);
                if (finite(horizontal)) {
                    move(context, horizontal.normalize().scale(speed).add(0, rise, 0));
                }
            }
        };
    }

    private static void move(BossContext<?> context, Vec3 motion) {
        var boss = context.boss();
        if (!boss.isPassenger() && areaLoaded(context.level(), boss.getBoundingBox().expandTowards(motion))) {
            boss.getNavigation().stop();
            boss.setDeltaMovement(motion);
            boss.syncVelocity = true;
        }
    }

    public static <B extends BossMob> BossAction<B> pinPlayer() {
        var anchor = new BossContext.Key<>(Vec3.class);
        return context -> {
            if (!(context.target().orElse(null) instanceof ServerPlayer player)) {
                context.interrupt();
                return;
            }
            player.stopRiding();
            Vec3 at = context.recall(anchor).orElseGet(() -> {
                Vec3 start = player.position();
                context.remember(anchor, start);
                return start;
            });
            Vec3 offset = at.subtract(player.position());
            if (!loadedAndInside(context.level(), at, context.boss().position())
                || context.level().getBlockCollisions(player, player.getBoundingBox().move(offset)).iterator().hasNext()) {
                context.interrupt();
                return;
            }
            Vec3 motion = player.getDeltaMovement();
            if (offset.lengthSqr() > 1.0E-6 || motion.horizontalDistanceSqr() > 1.0E-6 || motion.y > .001) {
                player.connection.teleport(new PositionMoveRotation(at, Vec3.ZERO, 0, 0), Relative.ROTATION);
            } else if (motion.y < 0) {
                player.setDeltaMovement(Vec3.ZERO);
            }
            player.resetFallDistance();
        };
    }

    public static <B extends BossMob> BossAction<B> teleport(Function<BossContext<B>, Vec3> destination) {
        return context -> tryTeleport(context, destination.apply(context));
    }

    public static boolean tryTeleport(BossContext<?> context, Vec3 at) {
        var boss = context.boss();
        var level = context.level();
        if (!context.isCasting() || boss.isPassenger() || boss.isVehicle() || !loadedAndInside(level, at, boss.position())
            || !canOccupy(level, boss, boss.getBoundingBox().move(at.subtract(boss.position())))) {
            return false;
        }

        boss.getNavigation().stop();
        boss.teleportTo(at.x, at.y, at.z);
        boss.setDeltaMovement(Vec3.ZERO);
        return true;
    }

    static boolean loadedAndInside(ServerLevel level, Vec3 position, Vec3 origin) {
        if (!finite(position) || position.distanceToSqr(origin) > BossZone.MAX_REACH * BossZone.MAX_REACH) {
            return false;
        }
        var pos = BlockPos.containing(position);
        return level.isInWorldBounds(pos) && level.getWorldBorder().isWithinBounds(pos) && level.isLoaded(pos);
    }

    static boolean canOccupy(ServerLevel level, Entity entity, AABB bounds) {
        return areaLoaded(level, bounds) && level.noCollision(entity, bounds)
            && level.getEntities(entity, bounds, other -> other instanceof LivingEntity
                && other.isAlive()).isEmpty();
    }

    private static boolean areaLoaded(ServerLevel level, AABB bounds) {
        var minimum = BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ);
        var maximum = BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ);
        return level.isInWorldBounds(minimum) && level.isInWorldBounds(maximum)
            && level.getWorldBorder().isWithinBounds(bounds) && level.hasChunksAt(minimum, maximum);
    }

    private static boolean finite(Vec3 vector) {
        return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
    }

    private static final class WaveState {
        private final Vec3 origin;
        private final Set<UUID> touched = new HashSet<>();
        private double front;

        private WaveState(Vec3 origin) {
            this.origin = origin;
        }
    }
}
