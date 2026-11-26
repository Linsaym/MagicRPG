package io.github.magicrpg.boss;

import java.util.List;
import java.util.function.Predicate;

@FunctionalInterface
public interface BossAction<B extends BossMob> {
    void run(BossContext<B> context);

    @SafeVarargs
    static <B extends BossMob> BossAction<B> all(BossAction<B>... actions) {
        var steps = List.of(actions);
        return context -> {
            for (var step : steps) {
                if (!context.isCasting()) {
                    break;
                }
                step.run(context);
            }
        };
    }

    static <B extends BossMob> BossAction<B> when(Predicate<BossContext<B>> condition, BossAction<B> action) {
        return context -> {
            if (condition.test(context)) {
                action.run(context);
            }
        };
    }
}
