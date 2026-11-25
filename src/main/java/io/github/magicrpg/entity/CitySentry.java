package io.github.magicrpg.entity;

import io.github.magicrpg.MagicRpg;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class CitySentry extends PathfinderMob {
    private @Nullable BlockPos post;

    public CitySentry(EntityType<? extends CitySentry> type, Level level) {
        super(type, level);
        xpReward = 2;
    }

    public static AttributeSupplier.Builder attributes(double health, double speed, double damage, double armor) {
        return createMobAttributes().add(Attributes.MAX_HEALTH, health).add(Attributes.MOVEMENT_SPEED, speed)
            .add(Attributes.ATTACK_DAMAGE, damage).add(Attributes.ARMOR, armor)
            .add(Attributes.FOLLOW_RANGE, 16);
    }

    public void station(BlockPos pos) {
        post = pos;
        setHomeTo(pos, getType() == MagicRpg.HOLLOW_ARMOR.get() ? 15 : 10);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, .8));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        if (getType() != MagicRpg.BURIED_WORKER.get()) {
            targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true,
                (target, level) -> canAttack(target)));
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (!super.doHurtTarget(level, target)) {
            return false;
        }
        if (getType() == MagicRpg.CITY_STONE_GUARD.get() && target instanceof LivingEntity living) {
            var source = damageSources().mobAttack(this);
            living.knockback(.85, getX() - target.getX(), getZ() - target.getZ(), source, 9);
            playSound(SoundEvents.CALCITE_BREAK, .8F, .65F);
            level.sendParticles(ParticleTypes.POOF, target.getX(), target.getY() + .5, target.getZ(),
                9, .25, .2, .25, .09);
        } else if (getType() == MagicRpg.HOLLOW_ARMOR.get()) {
            playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, .8F, 1.3F);
            level.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY() + .8, target.getZ(),
                14, .3, .25, .3, .03);
        }
        return true;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return level().getDifficulty() != Difficulty.PEACEFUL
            && (post == null || Math.abs(target.getY() - post.getY()) < 3 && isWithinHome(target.position()))
            && super.canAttack(target);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (post != null) {
            output.putLong("Station", post.asLong());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.getLong("Station").map(BlockPos::of).ifPresent(this::station);
    }
}
