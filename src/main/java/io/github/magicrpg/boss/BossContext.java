package io.github.magicrpg.boss;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class BossContext<B extends BossMob> {
    private final BossFight<B> fight;
    private final ServerLevel level;
    private final BossAbility<B> ability;
    private final LivingEntity target;
    private final long started;
    private Map<Key<?>, Object> memory;

    BossContext(BossFight<B> fight, ServerLevel level, BossAbility<B> ability, LivingEntity target, long started) {
        this.fight = fight;
        this.level = level;
        this.ability = ability;
        this.target = target;
        this.started = started;
    }

    public B boss() { return fight.boss(); }
    public ServerLevel level() { return level; }
    public Optional<Identifier> phase() { return fight.phase(); }
    public long phaseTicks() { return fight.phaseTicks(level.getGameTime()); }
    public Optional<Identifier> lastCompleted() { return fight.lastCompleted(); }
    public Optional<BossAbility<B>> ability() { return Optional.ofNullable(ability); }
    public boolean isCasting() { return fight.isCurrent(this); }

    public int elapsed() {
        return ability == null ? 0 : (int) Math.min(ability.duration(), level.getGameTime() - started);
    }

    public Optional<LivingEntity> target() {
        return Optional.ofNullable(target).filter(this::isHostile);
    }

    public boolean isHostile(LivingEntity entity) {
        return fight.isHostile(entity);
    }

    int takeParticles(int count) {
        return fight.takeParticles(count);
    }

    boolean takeSound() {
        return fight.takeSound();
    }

    public void interrupt() {
        if (isCasting()) {
            fight.interrupt();
        }
    }

    public <T> void remember(Key<T> key, T value) {
        requireCast();
        if (memory == null) {
            memory = new HashMap<>();
        }
        memory.put(key, key.type.cast(Objects.requireNonNull(value)));
    }

    public <T> Optional<T> recall(Key<T> key) {
        return memory == null ? Optional.empty() : Optional.ofNullable(key.type.cast(memory.get(key)));
    }

    public void damageMultiplier(Identifier key, float factor, int ticks, Scope scope) {
        requireCast();
        fight.damageMultiplier(key, factor, ticks, scope, level.getGameTime());
    }

    public <M extends Mob> Optional<M> summon(EntityType<M> type, Vec3 at, int ticks, Scope scope) {
        requireCast();
        return fight.summon(this, type, at, ticks, scope);
    }

    public void retargetSummons() {
        requireCast();
        var target = boss().getTarget();
        fight.retargetSummons(target != null && isHostile(target) ? target : null);
    }

    public boolean temporaryBlock(BlockPos pos, BlockState state, int ticks, Scope scope) {
        requireCast();
        return fight.temporaryBlock(level, pos, state, ticks, scope);
    }

    private void requireCast() {
        if (!isCasting()) {
            throw new IllegalStateException("This operation needs the current ability");
        }
    }

    public enum Scope { CAST, ENCOUNTER }

    /** Instance identity isolates keys owned by different actions, even for the same value type. */
    public static final class Key<T> {
        private final Class<T> type;

        public Key(Class<T> type) {
            this.type = Objects.requireNonNull(type);
        }
    }
}
