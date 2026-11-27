package io.github.magicrpg.entity;

import io.github.magicrpg.boss.BossFight;
import io.github.magicrpg.boss.BossMob;
import java.util.UUID;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class StormPilot extends BossMob {
    static final double ARENA_RADIUS = 20;
    static final double ARENA_HEIGHT = 3.5;

    private final BossFight<StormPilot> fight = new BossFight<>(this, StormPilotFight.ENCOUNTER);
    private final ServerBossEvent bossBar = new ServerBossEvent(UUID.randomUUID(), getType().getDescription(),
        BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);
    private @Nullable BlockPos perch;

    public StormPilot(EntityType<? extends StormPilot> type, Level level) {
        super(type, level);
        bossBar.setVisible(false);
        xpReward = 42;
        setNoGravity(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 320).add(Attributes.ARMOR, 8)
            .add(Attributes.MOVEMENT_SPEED, 0).add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    @Override
    protected BossFight<StormPilot> bossFight() {
        return fight;
    }

    public void anchorAt(BlockPos pos) {
        perch = pos.immutable();
        snapTo(Vec3.atBottomCenterOf(perch), 0, 0);
    }

    @Override
    protected Vec3 legacyRespawnPosition() {
        return perch == null ? super.legacyRespawnPosition() : Vec3.atBottomCenterOf(perch);
    }

    @Override
    public void tick() {
        if (level() instanceof ServerLevel && isAlive()) {
            if (perch == null) {
                anchorAt(blockPosition());
            }
            Vec3 anchor = Vec3.atBottomCenterOf(perch);
            if (distanceToSqr(anchor) > 0.0001) {
                setPos(anchor.x, anchor.y, anchor.z);
            }
            setDeltaMovement(Vec3.ZERO);
        }
        super.tick();
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        var previous = getTarget();
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            setTarget(null);
        } else if (!isParticipant(previous) && (previous != null || tickCount % 5 == 0)) {
            setTarget(findTarget(level));
        }
        var target = getTarget();
        bossBar.setProgress(getHealth() / getMaxHealth());
        bossBar.setVisible(target != null);
        if (target != null && fight.activeAbility().isEmpty()) {
            aimAt(target);
        }
    }

    private @Nullable ServerPlayer findTarget(ServerLevel level) {
        var area = AABB.ofSize(position(), ARENA_RADIUS * 2, ARENA_HEIGHT * 2, ARENA_RADIUS * 2);
        ServerPlayer nearest = null;
        double distance = Double.POSITIVE_INFINITY;
        for (var player : level.getEntitiesOfClass(ServerPlayer.class, area, this::isParticipant)) {
            double candidateDistance = distanceToSqr(player);
            if (candidateDistance < distance) {
                nearest = player;
                distance = candidateDistance;
            }
        }
        return nearest;
    }

    boolean isParticipant(@Nullable LivingEntity entity) {
        return entity instanceof ServerPlayer player && player.isAlive() && !player.isSpectator() && !player.isCreative()
            && isInArena(player);
    }

    private boolean isInArena(ServerPlayer player) {
        double height = player.getY() - getY();
        return player.level() == level() && height >= -.3 && height <= ARENA_HEIGHT
            && player.position().subtract(position()).horizontalDistanceSqr() <= ARENA_RADIUS * ARENA_RADIUS;
    }

    float aimAt(LivingEntity target) {
        float yaw = (float) (Mth.atan2(target.getZ() - getZ(), target.getX() - getX()) * Mth.RAD_TO_DEG) - 90;
        setYRot(yaw);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
        return yaw;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof ServerPlayer player && !player.isCreative() && !isInArena(player)) {
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE_ENTITY;
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
        if (perch != null) {
            output.putLong("Perch", perch.asLong());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        perch = input.getLong("Perch").map(BlockPos::of).orElse(null);
    }
}
