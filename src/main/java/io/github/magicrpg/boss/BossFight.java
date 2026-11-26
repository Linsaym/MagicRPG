package io.github.magicrpg.boss;

import io.github.magicrpg.boss.BossContext.Scope;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class BossFight<B extends BossMob> {
    private static final int MAX_SUMMONS = 12;
    private static final int MAX_BLOCKS = 64;
    private static final int MAX_MODIFIERS = 16;

    private final B boss;
    private final BossEncounter<B> encounter;
    private final HashMap<Identifier, Long> cooldowns = new HashMap<>();
    private final HashMap<Identifier, Modifier> modifiers = new HashMap<>();
    private final ArrayList<Summon> summons = new ArrayList<>();
    private final ArrayList<Placement> placements = new ArrayList<>();
    private BossEncounter.Phase<B> phase;
    private long phaseStarted;
    private BossContext<B> cast;
    private Identifier lastCompleted;
    private int particlesLeft;
    private int soundsLeft;

    public BossFight(B boss, BossEncounter<B> encounter) {
        this.boss = Objects.requireNonNull(boss);
        this.encounter = Objects.requireNonNull(encounter);
    }

    public B boss() { return boss; }
    public Optional<Identifier> phase() { return Optional.ofNullable(phase).map(BossEncounter.Phase::id); }
    public Optional<Identifier> lastCompleted() { return Optional.ofNullable(lastCompleted); }
    public Optional<BossAbility<B>> activeAbility() { return cast == null ? Optional.empty() : cast.ability(); }
    public long phaseTicks(long now) { return phase == null ? 0 : Math.max(0, now - phaseStarted); }

    void tick(ServerLevel level) {
        long now = level.getGameTime();
        if (boss.isNoAi() || level.getDifficulty() == Difficulty.PEACEFUL) {
            reset(level);
            return;
        }

        var selection = new BossContext<>(this, level, null, boss.getTarget(), now);
        if (!encounter.activeWhen().test(selection)) {
            reset(level);
            return;
        }

        particlesLeft = 96;
        soundsLeft = 8;
        expire(level, now);
        if (!boss.isAlive()) {
            return;
        }
        BossEncounter.Phase<B> next = null;
        for (var candidate : encounter.phases()) {
            if (candidate.condition().test(selection)) {
                next = candidate;
                break;
            }
        }
        if (next != phase) {
            interrupt();
            phase = next;
            phaseStarted = now;
            selection = new BossContext<>(this, level, null, boss.getTarget(), now);
        }
        if (phase == null) {
            return;
        }
        if (cast != null) {
            var current = cast;
            var ability = current.ability().orElseThrow();
            int elapsed = current.elapsed();
            ability.run(current, elapsed);
            if (cast == current && elapsed >= ability.duration()) {
                lastCompleted = ability.id();
                interrupt();
            }
            return;
        }
        for (var ability : phase.abilities()) {
            if (now >= cooldowns.getOrDefault(ability.id(), Long.MIN_VALUE) && ability.canStart(selection)) {
                cooldowns.put(ability.id(), now + ability.cooldown());
                cast = new BossContext<>(this, level, ability, selection.target().orElse(null), now);
                boss.playAnimation(ability.clip(), now);
                ability.run(cast, 0);
                return;
            }
        }
    }

    public boolean interrupt() {
        if (cast == null) {
            return false;
        }
        var level = cast.level();
        cast = null;
        boss.playAnimation(0, level.getGameTime());
        clear(level, Scope.CAST);
        return true;
    }

    void reset(ServerLevel level) {
        interrupt();
        clear(level, Scope.ENCOUNTER);
        phase = null;
        lastCompleted = null;
        cooldowns.clear();
    }

    void onHurt(DamageSource source, float amount) {
        if (cast != null && cast.ability().orElseThrow().canInterrupt(cast, source, amount)) {
            interrupt();
        }
    }

    boolean isCurrent(BossContext<B> context) {
        return cast == context;
    }

    boolean isHostile(LivingEntity entity) {
        return entity != boss && entity.isAlive() && entity.level() == boss.level() && !entity.isSpectator()
            && (!(entity instanceof Player player) || !player.isCreative()) && !boss.isAlliedTo(entity)
            && !BossResources.isSummonOf(entity, boss) && encounter.canHit().test(boss, entity);
    }

    int takeParticles(int requested) {
        int granted = Math.min(requested, particlesLeft);
        particlesLeft -= granted;
        return granted;
    }

    boolean takeSound() {
        if (soundsLeft == 0) {
            return false;
        }
        soundsLeft--;
        return true;
    }

    void damageMultiplier(Identifier key, float factor, int ticks, Scope scope, long now) {
        if (!Float.isFinite(factor) || factor < 0 || factor > 8 || ticks < 1) {
            throw new IllegalArgumentException("Invalid damage multiplier or duration");
        }
        Objects.requireNonNull(key);
        Objects.requireNonNull(scope);
        if (modifiers.containsKey(key) || modifiers.size() < MAX_MODIFIERS) {
            modifiers.put(key, new Modifier(factor, now + ticks, scope));
        }
    }

    float damageMultiplier(long now) {
        double factor = 1;
        for (var modifier : modifiers.values()) {
            if (now < modifier.expires()) {
                factor *= modifier.factor();
            }
        }
        return (float) Math.min(factor, 8);
    }

    <M extends Mob> Optional<M> summon(BossContext<B> context, EntityType<M> type, Vec3 at, int ticks, Scope scope) {
        Objects.requireNonNull(scope);
        if (ticks < 1) {
            throw new IllegalArgumentException("Summon duration must be positive");
        }
        if (summons.size() >= MAX_SUMMONS || !BossActions.loadedAndInside(context.level(), at, boss.position())) {
            return Optional.empty();
        }

        M mob = type.create(context.level(), EntitySpawnReason.MOB_SUMMONED);
        if (mob == null || mob instanceof BossMob) {
            return Optional.empty();
        }
        mob.setPos(at);
        if (!BossActions.canOccupy(context.level(), mob, mob.getBoundingBox())) {
            return Optional.empty();
        }
        BossResources.markSummon(mob, boss);
        if (!context.level().addFreshEntity(mob)) {
            return Optional.empty();
        }
        if (!context.isCasting()) {
            mob.discard();
            return Optional.empty();
        }
        summons.add(new Summon(mob, context.level().getGameTime() + ticks, scope));
        mob.setTarget(context.target().orElse(null));
        return mob.isRemoved() ? Optional.empty() : Optional.of(mob);
    }

    void retargetSummons(LivingEntity target) {
        for (var summon : List.copyOf(summons)) {
            if (summon.mob().isAlive()) {
                summon.mob().setTarget(target);
            }
        }
    }

    boolean temporaryBlock(ServerLevel level, BlockPos pos, BlockState state, int ticks, Scope scope) {
        Objects.requireNonNull(scope);
        if (placements.size() >= MAX_BLOCKS || !BossActions.loadedAndInside(level, Vec3.atCenterOf(pos), boss.position())) {
            return false;
        }
        var originalCast = cast;
        var entry = BossArena.get(level).place(level, pos, state, ticks);
        if (entry == null) {
            return false;
        }
        if (cast != originalCast) {
            BossArena.get(level).release(level, entry);
            return false;
        }
        placements.add(new Placement(entry, scope));
        return true;
    }

    private void expire(ServerLevel level, long now) {
        modifiers.values().removeIf(modifier -> now >= modifier.expires());
        if (!summons.isEmpty()) {
            var expired = summons.stream().filter(summon -> now >= summon.expires() || summon.mob().isRemoved()).toList();
            summons.removeAll(expired);
            for (var summon : expired) {
                if (!summon.mob().isRemoved()) {
                    summon.mob().discard();
                }
            }
        }
        if (!placements.isEmpty()) {
            var arena = BossArena.get(level);
            placements.removeIf(placement -> !arena.contains(placement.entry()));
        }
    }

    private void clear(ServerLevel level, Scope scope) {
        modifiers.values().removeIf(modifier -> modifier.scope() == scope);
        List<Summon> expiredSummons = summons.isEmpty() ? List.of()
            : summons.stream().filter(summon -> summon.scope() == scope).toList();
        List<Placement> expiredBlocks = placements.isEmpty() ? List.of()
            : placements.stream().filter(placement -> placement.scope() == scope).toList();
        summons.removeAll(expiredSummons);
        placements.removeAll(expiredBlocks);
        for (var summon : expiredSummons) {
            if (!summon.mob().isRemoved()) {
                summon.mob().discard();
            }
        }
        for (var placement : expiredBlocks) {
            BossArena.get(level).release(level, placement.entry());
        }
    }

    private record Modifier(float factor, long expires, Scope scope) {}
    private record Summon(Mob mob, long expires, Scope scope) {}
    private record Placement(BossArena.Entry entry, Scope scope) {}
}
