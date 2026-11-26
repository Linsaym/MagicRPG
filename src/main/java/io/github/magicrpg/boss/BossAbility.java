package io.github.magicrpg.boss;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageSource;

public final class BossAbility<B extends BossMob> {
    private final Identifier id;
    private final int duration;
    private final int cooldown;
    private final int priority;
    private final int clip;
    private final Predicate<BossContext<B>> condition;
    private final InterruptRule<B> interruptRule;
    private final List<Step<B>> steps;

    private BossAbility(Builder<B> builder) {
        id = builder.id;
        duration = builder.duration;
        cooldown = builder.cooldown;
        priority = builder.priority;
        clip = builder.clip;
        condition = builder.condition;
        interruptRule = builder.interruptRule;
        steps = List.copyOf(builder.steps);
    }

    public static <B extends BossMob> Builder<B> builder(Identifier id, int duration) {
        return new Builder<>(id, duration);
    }

    public Identifier id() { return id; }
    public int duration() { return duration; }
    public int cooldown() { return cooldown; }
    public int priority() { return priority; }
    public int clip() { return clip; }

    boolean canStart(BossContext<B> context) {
        return condition.test(context);
    }

    boolean canInterrupt(BossContext<B> context, DamageSource source, float damage) {
        return interruptRule.test(context, source, damage);
    }

    void run(BossContext<B> context, int tick) {
        for (var step : steps) {
            if (!context.isCasting()) {
                break;
            }
            if (tick >= step.first && tick <= step.last && (tick - step.first) % step.interval == 0) {
                step.action.run(context);
            }
        }
    }

    @FunctionalInterface
    public interface InterruptRule<B extends BossMob> {
        boolean test(BossContext<B> context, DamageSource source, float damage);
    }

    private record Step<B extends BossMob>(int first, int last, int interval, BossAction<B> action) {}

    public static final class Builder<B extends BossMob> {
        private final Identifier id;
        private final int duration;
        private final List<Step<B>> steps = new ArrayList<>();
        private int cooldown;
        private int priority;
        private int clip;
        private Predicate<BossContext<B>> condition = context -> true;
        private InterruptRule<B> interruptRule = (context, source, damage) -> false;

        private Builder(Identifier id, int duration) {
            if (duration < 1) {
                throw new IllegalArgumentException("Ability duration must be positive");
            }
            this.id = Objects.requireNonNull(id);
            this.duration = duration;
        }

        public Builder<B> cooldown(int ticks) {
            if (ticks < 0) {
                throw new IllegalArgumentException("Cooldown cannot be negative");
            }
            cooldown = ticks;
            return this;
        }

        public Builder<B> priority(int value) {
            priority = value;
            return this;
        }

        public Builder<B> animation(int clipId) {
            if (clipId <= 0) {
                throw new IllegalArgumentException("Clip IDs must be positive");
            }
            clip = clipId;
            return this;
        }

        public Builder<B> requires(Predicate<BossContext<B>> predicate) {
            condition = condition.and(predicate);
            return this;
        }

        public Builder<B> interruptWhen(InterruptRule<B> rule) {
            interruptRule = Objects.requireNonNull(rule);
            return this;
        }

        public Builder<B> at(int tick, BossAction<B> action) {
            return during(tick, tick, 1, action);
        }

        public Builder<B> during(int first, int last, int interval, BossAction<B> action) {
            if (first < 0 || last < first || last > duration || interval < 1 || steps.size() >= 128) {
                throw new IllegalArgumentException("Invalid timeline interval or too many steps");
            }
            steps.add(new Step<>(first, last, interval, Objects.requireNonNull(action)));
            return this;
        }

        public BossAbility<B> build() {
            return new BossAbility<>(this);
        }
    }
}
