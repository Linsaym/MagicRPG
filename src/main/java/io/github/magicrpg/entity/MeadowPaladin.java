package io.github.magicrpg.entity;

import io.github.magicrpg.boss.BossFight;
import io.github.magicrpg.boss.BossMob;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class MeadowPaladin extends BossMob {
    private static final EntityDataAccessor<Boolean> SWORD_PLANTED =
        SynchedEntityData.defineId(MeadowPaladin.class, EntityDataSerializers.BOOLEAN);
    private static final int LEGACY_MAX_HEALTH = 250;
    private static final int MAX_HEALTH = 500;

    private final BossFight<MeadowPaladin> fight = new BossFight<>(this, MeadowPaladinFight.ENCOUNTER);
    private final ServerBossEvent bossBar = new ServerBossEvent(UUID.randomUUID(), getType().getDescription(),
        BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
    private @Nullable BlockPos post;

    public MeadowPaladin(EntityType<? extends MeadowPaladin> type, Level level) {
        super(type, level);
        bossBar.setVisible(false);
        xpReward = 32;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, MAX_HEALTH).add(Attributes.MOVEMENT_SPEED, .31)
            .add(Attributes.ARMOR, 12).add(Attributes.KNOCKBACK_RESISTANCE, 1)
            .add(Attributes.FOLLOW_RANGE, 36);
    }

    @Override
    protected BossFight<MeadowPaladin> bossFight() {
        return fight;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SWORD_PLANTED, false);
    }

    public boolean hasPlantedSword() {
        return entityData.get(SWORD_PLANTED);
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel) {
            entityData.set(SWORD_PLANTED, isAlive() && onGround()
                && (isNoAi() || getTarget() == null && isAtPost()));
        }
    }

    public void guard(BlockPos pos) {
        post = pos;
        setHomeTo(pos, 24);
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

    @Override
    public void die(DamageSource source) {
        super.die(source);
        bossBar.setVisible(false);
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
        var maxHealth = getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null && maxHealth.getBaseValue() == LEGACY_MAX_HEALTH) {
            float healthFraction = getHealth() / getMaxHealth();
            maxHealth.setBaseValue(MAX_HEALTH);
            setHealth(healthFraction * getMaxHealth());
        }
        input.getLong("GuardPost").map(BlockPos::of).ifPresent(this::guard);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (post != null && source.getEntity() instanceof ServerPlayer player && !player.isCreative()
            && !inGuardZone(player, getTarget() == null)) {
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        if (post == null) {
            guard(blockPosition());
        }

        var previousTarget = getTarget();
        var target = findTarget(level);
        if (target != previousTarget) {
            setTarget(target);
            if (target == null) {
                getMoveControl().setWait();
                brace();
            }
        }
        bossBar.setProgress(getHealth() / getMaxHealth());
        bossBar.setVisible(target != null);

        if (fight.activeAbility().isPresent()) {
            brace();
        } else if (target != null) {
            if (distanceToSqr(target) > 3 * 3 || !hasLineOfSight(target)) {
                if (tickCount % 5 == 0) {
                    getNavigation().moveTo(target, 1);
                }
            } else {
                getNavigation().stop();
            }
            face(yawTo(target));
        } else if (!isAtPost()) {
            returnToPost(level, Vec3.atBottomCenterOf(post));
        } else {
            getMoveControl().setWait();
            brace();
            face(0);
        }
    }

    private void returnToPost(ServerLevel level, Vec3 home) {
        Vec3 offset = home.subtract(position());
        // Ground navigation can finish outside the stance radius for a mob this wide.
        if (onGround() && offset.lengthSqr() <= 9
            && level.noBlockCollision(this, getBoundingBox().expandTowards(offset))) {
            getNavigation().stop();
            getMoveControl().setWantedPosition(home.x, home.y, home.z, .9);
        } else if (tickCount % 10 == 0) {
            getNavigation().moveTo(home.x, home.y, home.z, 0, .9);
        }
    }

    private boolean isAtPost() {
        return post != null && post.distToCenterSqr(getX(), getY() + .5, getZ()) <= .36;
    }

    private @Nullable ServerPlayer findTarget(ServerLevel level) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return null;
        }
        if (getTarget() instanceof ServerPlayer player && inGuardZone(player, false)) {
            return player;
        }
        ServerPlayer nearest = null;
        for (var player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(post).inflate(24),
            candidate -> inGuardZone(candidate, true) && hasLineOfSight(candidate))) {
            if (nearest == null || distanceToSqr(player) < distanceToSqr(nearest)) {
                nearest = player;
            }
        }
        return nearest;
    }

    boolean inGuardZone(ServerPlayer player, boolean starting) {
        if (!player.isAlive() || player.level() != level() || player.isSpectator() || player.isCreative() || post == null) {
            return false;
        }
        double x = player.getX() - post.getX() - .5;
        double y = player.getY() - post.getY();
        double z = player.getZ() - post.getZ() - .5;
        return Math.abs(x) <= (starting ? 17 : 20) && y >= -2 && y <= 5
            && z >= -9 && z <= (starting ? 16 : 19);
    }

    @Override
    public boolean isPushable() {
        return animationCue().clip() == 0 && super.isPushable();
    }

    void brace() {
        getNavigation().stop();
        setSpeed(0);
        setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
    }

    float yawTo(LivingEntity target) {
        return (float) (Mth.atan2(target.getZ() - getZ(), target.getX() - getX()) * Mth.RAD_TO_DEG) - 90;
    }

    void face(float yaw) {
        setYRot(yaw);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
    }
}
