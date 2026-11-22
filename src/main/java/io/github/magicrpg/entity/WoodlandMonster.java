package io.github.magicrpg.entity;

import io.github.magicrpg.Goldenwood;
import io.github.magicrpg.world.PortalRoute;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public final class WoodlandMonster extends Monster {
    private static final DustParticleOptions CHARGED_SPARK = new DustParticleOptions(0xBE92F2, 0.95F);
    private static final EntityDataAccessor<Integer> ATTACK_POSE =
        SynchedEntityData.defineId(WoodlandMonster.class, EntityDataSerializers.INT);
    private final Kind kind;
    private int castTimer;
    private int castCooldown;

    public WoodlandMonster(EntityType<? extends WoodlandMonster> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
        xpReward = kind == Kind.GUARDIAN ? 9 : 4;

        if (kind == Kind.SPARK) {
            setNoGravity(true);
        } else {
            goalSelector.addGoal(0, new FloatGoal(this));
            goalSelector.addGoal(1, kind == Kind.BEETLE ? new MeleeAttackGoal(this, 1.15, true) : new EntAttack());
            if (kind != Kind.SPROUT) {
                goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.75));
            }
            goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 7));
        }
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public Kind kind() {
        return kind;
    }

    public int attackPose() {
        return entityData.get(ATTACK_POSE);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return kind == Kind.BEETLE ? 6 : super.getMaxSpawnClusterSize();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ATTACK_POSE, 0);
    }

    public static AttributeSupplier.Builder attributes(Kind kind) {
        return createMonsterAttributes().add(Attributes.MAX_HEALTH, kind.health)
            .add(Attributes.MOVEMENT_SPEED, kind.speed).add(Attributes.ATTACK_DAMAGE, kind.damage)
            .add(Attributes.ARMOR, kind == Kind.GUARDIAN ? 5 : 0)
            .add(Attributes.KNOCKBACK_RESISTANCE, kind == Kind.GUARDIAN ? 0.75 : 0)
            .add(Attributes.FOLLOW_RANGE, kind == Kind.SPROUT ? 11 : 22);
    }

    public static boolean canSpawn(EntityType<WoodlandMonster> type, ServerLevelAccessor level,
                                   EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        if (EntitySpawnReason.isSpawner(reason)) {
            return true;
        }
        if (!level.getLevel().dimension().equals(PortalRoute.GOLDENWOOD.dimension())) {
            return false;
        }
        var floor = level.getBlockState(pos.below());
        if (type == Goldenwood.GUARDIAN.get() || type == Goldenwood.SPARK.get()) {
            return floor.is(Goldenwood.HONEY_BRICKS.get()) || floor.is(Goldenwood.GOLDEN_PLANKS.get())
                || floor.is(Blocks.SMOOTH_SANDSTONE);
        }
        if (!level.canSeeSky(pos.above(12))) {
            return false;
        }
        if (type == Goldenwood.BEETLE.get()) {
            return (floor.is(Goldenwood.AMBER_GRASS.get()) || floor.is(Goldenwood.ROTTEN_LOG.get())
                || floor.is(Goldenwood.HONEYSTONE.get()) || floor.is(Blocks.PACKED_MUD))
                && Habitat.find(pos, 6, 2, p -> level.getBlockState(p).is(Goldenwood.ROTTEN_LOG.get()))
                    .isPresent();
        }
        return type == Goldenwood.SPROUT.get()
            && (floor.is(Goldenwood.AMBER_GRASS.get()) || floor.is(Blocks.PACKED_MUD))
            && Habitat.find(pos, 6, 3, p -> {
                var cover = level.getBlockState(p);
                return cover.is(Blocks.BUSH) || cover.is(Goldenwood.GOLDEN_LOG.get())
                    || cover.is(Goldenwood.HONEY_BRICKS.get()) || cover.is(Blocks.MOSSY_STONE_BRICKS)
                    || cover.is(Blocks.CRACKED_STONE_BRICKS);
            }).isPresent();
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (kind != Kind.SPARK) {
            return;
        }
        var target = getTarget();
        if (castCooldown > 0) {
            castCooldown--;
        }
        boolean canCast = target != null && target.isAlive() && distanceToSqr(target) < 256
            && getSensing().hasLineOfSight(target);
        if (canCast && castTimer == 0 && castCooldown == 0) {
            castTimer = 26;
            entityData.set(ATTACK_POSE, 1);
            playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1, 1.35F);
        }
        if (target != null) {
            lookAt(target, 25, 25);
            yBodyRot = getYHeadRot();
        }
        if (castTimer > 0) {
            setDeltaMovement(getDeltaMovement().scale(0.55));
            if (castTimer % 3 == 0) {
                var particles = castTimer <= 12 ? CHARGED_SPARK : ParticleTypes.WAX_ON;
                level.sendParticles(particles, getX(), getY(0.6), getZ(), 4, 0.26, 0.2, 0.26, 0.02);
            }
            if (--castTimer == 0) {
                if (canCast) {
                    AmberBolt.fire(level, this, target);
                    playSound(SoundEvents.BLAZE_SHOOT, 0.6F, 1.7F);
                }
                entityData.set(ATTACK_POSE, 0);
                castCooldown = 38;
            }
            return;
        }
        double bob = Math.sin(tickCount * 0.11) * 0.033;
        boolean nearFloor = !level.getBlockState(blockPosition().below()).getCollisionShape(level,
            blockPosition().below()).isEmpty();
        double lift = nearFloor ? 0.115 : bob - 0.012;
        Vec3 drift = new Vec3(Math.cos(tickCount * 0.048) * 0.034, lift, Math.sin(tickCount * 0.048) * 0.034);
        if (target != null && distanceToSqr(target) > 64) {
            Vec3 approach = target.position().subtract(position()).horizontal().normalize().scale(0.09);
            drift = drift.add(approach);
        }
        setDeltaMovement(drift);
    }

    public enum Kind {
        SPROUT("sprout_ent", 16, 0.28, 4),
        GUARDIAN("guardian_ent", 54, 0.19, 11),
        BEETLE("amber_bark_beetle", 8, 0.30, 2),
        SPARK("ruin_spark", 20, 0.19, 5);

        private final String id;
        private final double health;
        private final double speed;
        private final double damage;

        Kind(String id, double health, double speed, double damage) {
            this.id = id;
            this.health = health;
            this.speed = speed;
            this.damage = damage;
        }

        public String id() {
            return id;
        }
    }

    private final class EntAttack extends Goal {
        private int windup;
        private int recovery;
        private int leap;

        EntAttack() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return getTarget() != null && getTarget().isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void stop() {
            windup = 0;
            leap = 0;
            entityData.set(ATTACK_POSE, 0);
            getNavigation().stop();
        }

        @Override
        public void tick() {
            var target = getTarget();
            if (target == null || !(level() instanceof ServerLevel server)) {
                return;
            }
            getLookControl().setLookAt(target, 35, 35);
            if (leap > 0) {
                if (tryHit(server, target) || --leap == 0) {
                    leap = 0;
                    recovery = 20;
                    entityData.set(ATTACK_POSE, 0);
                }
                return;
            }
            if (windup > 0) {
                if (--windup == 0) {
                    if (kind == Kind.SPROUT && onGround()) {
                        var direction = target.position().subtract(position()).horizontal().normalize();
                        setDeltaMovement(direction.scale(0.65).add(0, 0.33, 0));
                        entityData.set(ATTACK_POSE, 2);
                        playSound(SoundEvents.ROOTED_DIRT_BREAK, 1, 0.85F);
                        leap = 11;
                    } else {
                        tryHit(server, target);
                        entityData.set(ATTACK_POSE, 2);
                        recovery = 30;
                    }
                }
                return;
            }
            if (recovery > 0) {
                if (--recovery == 19) {
                    entityData.set(ATTACK_POSE, 0);
                }
                return;
            }
            double reach = kind == Kind.SPROUT ? 4.4 : 2.9;
            if (distanceToSqr(target) < reach * reach && getSensing().hasLineOfSight(target)) {
                getNavigation().stop();
                windup = kind == Kind.SPROUT ? 17 : 24;
                entityData.set(ATTACK_POSE, 1);
                playSound(SoundEvents.AZALEA_LEAVES_HIT, 1, kind == Kind.SPROUT ? 1.4F : 0.68F);
            } else if (tickCount % 6 == 0) {
                getNavigation().moveTo(target, 1);
            }
        }

        private boolean tryHit(ServerLevel server, LivingEntity target) {
            double reach = kind == Kind.GUARDIAN ? 3.05 : 1.5;
            return distanceToSqr(target) <= reach * reach && getSensing().hasLineOfSight(target)
                && doHurtTarget(server, target);
        }
    }
}
