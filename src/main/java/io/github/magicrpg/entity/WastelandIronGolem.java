package io.github.magicrpg.entity;

import io.github.magicrpg.IronWastelands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class WastelandIronGolem extends IronGolem {
    public WastelandIronGolem(EntityType<? extends IronGolem> type, Level level) {
        super(type, level);
        refreshDimensions();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1, true));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, .9));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, .75));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, true,
            (candidate, level) -> IronWastelands.isInfected(candidate)));
    }

    @Override
    protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
        if (!(source.getEntity() instanceof Mob)) {
            super.dropAllDeathLoot(level, source);
        }
    }
}
