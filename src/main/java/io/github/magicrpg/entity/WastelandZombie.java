package io.github.magicrpg.entity;

import io.github.magicrpg.IronWastelands;
import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public final class WastelandZombie extends Zombie {
    public WastelandZombie(EntityType<? extends WastelandZombie> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, true,
            (candidate, level) -> IronWastelands.isConstruct(candidate)));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1));
        if (getType() == IronWastelands.HOLLOWED.get()) {
            goalSelector.addGoal(6, new FollowHordeGoal());
        }
    }

    @Override
    protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
        if (!(source.getEntity() instanceof Mob)) {
            super.dropAllDeathLoot(level, source);
        }
    }

    private final class FollowHordeGoal extends Goal {
        private @Nullable WastelandZombie leader;

        private FollowHordeGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            var follower = WastelandZombie.this;
            if (follower.getTarget() != null || follower.getRandom().nextInt(12) != 0) {
                return false;
            }
            // Following older neighbors prevents loops within a pack.
            leader = level().getEntitiesOfClass(WastelandZombie.class, getBoundingBox().inflate(12, 3, 12),
                    candidate -> candidate.getType() == getType() && candidate.getId() < getId()
                        && candidate.isAlive() && follower.isWithinHome(candidate.position()))
                .stream().min(Comparator.comparingDouble(follower::distanceToSqr)).orElse(null);
            return leader != null && distanceToSqr(leader) > 9;
        }

        @Override
        public boolean canContinueToUse() {
            return leader != null && leader.isAlive() && getTarget() == null && isWithinHome(leader.position())
                && distanceToSqr(leader) > 9 && distanceToSqr(leader) < 256;
        }

        @Override
        public void start() {
            tick();
        }

        @Override
        public void tick() {
            if (leader != null && getNavigation().isDone()) {
                getNavigation().moveTo(leader, 1.1);
            }
        }

        @Override
        public void stop() {
            leader = null;
            getNavigation().stop();
        }
    }
}
