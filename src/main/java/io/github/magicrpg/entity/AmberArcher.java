package io.github.magicrpg.entity;

import io.github.magicrpg.boss.BossFight;
import io.github.magicrpg.boss.BossMob;
import java.util.Comparator;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import org.jspecify.annotations.Nullable;

public final class AmberArcher extends BossMob {
    public static final float HEIGHT = 2F;
    private static final int LEGACY_MAX_HEALTH = 110;
    private static final int MAX_HEALTH = 220;
    private static final double ENGAGE_RANGE = 14;
    private final BossFight<AmberArcher> fight = new BossFight<>(this, AmberArcherFight.ENCOUNTER);
    private final ServerBossEvent bossBar = new ServerBossEvent(UUID.randomUUID(), getType().getDescription(),
        BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
    private @Nullable BlockPos post;

    public AmberArcher(EntityType<? extends AmberArcher> type, Level level) {
        super(type, level);
        xpReward = 20;
        setPersistenceRequired();
        bossBar.setVisible(false);
    }

    public static AttributeSupplier.Builder attributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, MAX_HEALTH).add(Attributes.ARMOR, 4)
            .add(Attributes.MOVEMENT_SPEED, .28).add(Attributes.KNOCKBACK_RESISTANCE, .8)
            .add(Attributes.FOLLOW_RANGE, ENGAGE_RANGE);
    }

    public static void damageShield(LivingShieldBlockEvent event) {
        var source = event.getDamageSource();
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isCreative()
            || !(source.getEntity() instanceof AmberArcher) || !(source.getDirectEntity() instanceof Arrow)
            || !event.getOriginalBlock() || !event.getBlocked() || event.getBlockedDamage() <= 0) {
            return;
        }
        var shield = player.getUseItem();
        if (!shield.is(Items.SHIELD) || !shield.isDamageableItem()) {
            return;
        }
        int maximum = shield.getMaxDamage();
        int wear = maximum / 2 + maximum % 2;
        // Apply a fixed amount of wear even when the shield has Unbreaking.
        event.setShieldDamage(0);
        if (maximum - shield.getDamageValue() > wear) {
            shield.setDamageValue(shield.getDamageValue() + wear);
            return;
        }
        var hand = player.getUsedItemHand();
        var broken = shield.copy();
        shield.shrink(1);
        player.onEquippedItemBroken(broken, hand.asEquipmentSlot());
        EventHooks.onPlayerDestroyItem(player, broken, hand);
        player.stopUsingItem();
    }

    @Override
    protected BossFight<AmberArcher> bossFight() {
        return fight;
    }

    public void guard(BlockPos pos) {
        post = pos.immutable();
        setHomeTo(post, 15);
    }

    boolean inCourtyard(Vec3 at) {
        if (post == null) {
            return false;
        }
        Vec3 relative = at.subtract(Vec3.atBottomCenterOf(post));
        return Math.abs(relative.x) <= 12 && relative.y >= -1 && relative.y <= 4
            && relative.z >= -8 && relative.z <= 11;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        if (post == null) {
            guard(blockPosition());
        }
        ServerPlayer target = level.getDifficulty() == Difficulty.PEACEFUL ? null : selectTarget(level);
        if (target != getTarget()) {
            setTarget(target);
        }
        bossBar.setProgress(getHealth() / getMaxHealth());
        bossBar.setVisible(target != null);

        var ability = fight.activeAbility();
        if (ability.isPresent()) {
            getNavigation().stop();
        } else if (target != null && distanceToSqr(target) > 25 && tickCount % 5 == 0) {
            getNavigation().moveTo(target, .85);
        } else if (target == null && tickCount % 10 == 0) {
            Vec3 home = Vec3.atBottomCenterOf(post);
            if (position().distanceToSqr(home) > .4) {
                getNavigation().moveTo(home.x, home.y, home.z, .9);
            }
        } else if (target != null && distanceToSqr(target) <= 25) {
            getNavigation().stop();
        }
        if (target != null && ability.filter(cast -> cast.id().equals(AmberArcherFight.FAN_ID)).isEmpty()) {
            Vec3 delta = target.position().subtract(position());
            face((float) (Mth.atan2(delta.z, delta.x) * Mth.RAD_TO_DEG) - 90);
        }
    }

    private @Nullable ServerPlayer selectTarget(ServerLevel level) {
        if (getTarget() instanceof ServerPlayer player) {
            return canFight(player) ? player : null;
        }
        return level.getEntitiesOfClass(ServerPlayer.class, new AABB(post).inflate(12, 5, 12),
                player -> canFight(player) && distanceToSqr(player) <= ENGAGE_RANGE * ENGAGE_RANGE
                    && hasLineOfSight(player))
            .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
    }

    boolean canFight(ServerPlayer player) {
        return player.isAlive() && player.level() == level() && !player.isCreative() && !player.isSpectator()
            && inCourtyard(player.position()) && distanceToSqr(player) <= 18 * 18;
    }

    private void face(float yaw) {
        setYRot(yaw);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (post != null && source.getEntity() instanceof ServerPlayer player && !player.isCreative()
            && !canFight(player)) {
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BAMBOO_WOOD_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BAMBOO_WOOD_BREAK;
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
        output.putBoolean("HealthRebalanced", true);
        if (post != null) {
            output.putLong("GuardPost", post.asLong());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        if (!input.getBooleanOr("HealthRebalanced", false)) {
            var maxHealth = getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null && maxHealth.getBaseValue() == LEGACY_MAX_HEALTH) {
                float healthFraction = getHealth() / getMaxHealth();
                maxHealth.setBaseValue(MAX_HEALTH);
                setHealth(healthFraction * getMaxHealth());
            } else if (maxHealth != null && maxHealth.getBaseValue() == MAX_HEALTH) {
                // Old saves can omit an unmodified maximum-health attribute.
                setHealth(getHealth() * MAX_HEALTH / LEGACY_MAX_HEALTH);
            }
        }
        input.getLong("GuardPost").map(BlockPos::of).ifPresent(this::guard);
    }
}
