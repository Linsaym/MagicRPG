package io.github.magicrpg.boss;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public record BossEncounter<B extends BossMob>(
    Predicate<BossContext<B>> activeWhen,
    BiPredicate<B, LivingEntity> canHit,
    List<Phase<B>> phases
) {
    public BossEncounter {
        Objects.requireNonNull(activeWhen);
        Objects.requireNonNull(canHit);
        phases = List.copyOf(phases);
        if (phases.isEmpty()) {
            throw new IllegalArgumentException("An encounter needs at least one phase");
        }
        var phaseIds = new HashSet<Identifier>();
        var abilitiesById = new HashMap<Identifier, BossAbility<B>>();
        for (var phase : phases) {
            if (!phaseIds.add(phase.id())) {
                throw new IllegalArgumentException("Duplicate phase " + phase.id());
            }
            for (var ability : phase.abilities()) {
                var previous = abilitiesById.putIfAbsent(ability.id(), ability);
                if (previous != null && previous != ability) {
                    throw new IllegalArgumentException("Different definitions for ability " + ability.id());
                }
            }
        }
    }

    public BossEncounter(List<Phase<B>> phases) {
        this(context -> context.target().isPresent(),
            (boss, target) -> target instanceof Player || target == boss.getTarget(), phases);
    }

    public record Phase<B extends BossMob>(
        Identifier id, Predicate<BossContext<B>> condition, List<BossAbility<B>> abilities
    ) {
        public Phase {
            Objects.requireNonNull(id);
            Objects.requireNonNull(condition);
            abilities = abilities.stream().sorted(Comparator.comparingInt(BossAbility<B>::priority).reversed()).toList();
            if (abilities.stream().map(BossAbility::id).distinct().count() != abilities.size()) {
                throw new IllegalArgumentException("Duplicate ability in phase " + id);
            }
        }

        public Phase(Identifier id, List<BossAbility<B>> abilities) {
            this(id, context -> true, abilities);
        }
    }
}
