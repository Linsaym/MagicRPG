package io.github.magicrpg.boss.animation;

import java.util.List;
import java.util.Objects;

public final class KeyframeTrack<T> {
    private final List<Key<T>> keys;
    private final Interpolator<T> interpolator;

    public KeyframeTrack(List<Key<T>> keys, Interpolator<T> interpolator) {
        this.keys = List.copyOf(keys);
        this.interpolator = Objects.requireNonNull(interpolator);
        if (keys.isEmpty() || keys.getFirst().tick() != 0) {
            throw new IllegalArgumentException("The first keyframe must be at tick 0");
        }
        for (int i = 1; i < keys.size(); i++) {
            if (keys.get(i).tick() <= keys.get(i - 1).tick()) {
                throw new IllegalArgumentException("Keyframe times must increase");
            }
        }
    }

    public float endTick() {
        return keys.getLast().tick();
    }

    public T sample(float tick) {
        if (tick <= 0) {
            return keys.getFirst().value();
        }
        if (tick >= endTick()) {
            return keys.getLast().value();
        }
        int start = 0;
        int end = keys.size() - 1;
        while (end - start > 1) {
            int middle = (start + end) / 2;
            if (keys.get(middle).tick() <= tick) {
                start = middle;
            } else {
                end = middle;
            }
        }
        var from = keys.get(start);
        var to = keys.get(end);
        float position = (tick - from.tick()) / (to.tick() - from.tick());
        return interpolator.blend(from.value(), to.value(), to.incoming().map(position));
    }

    @FunctionalInterface
    public interface Interpolator<T> {
        T blend(T from, T to, float weight);
    }

    public record Key<T>(float tick, T value, Curve incoming) {
        public Key {
            if (!Float.isFinite(tick) || tick < 0) {
                throw new IllegalArgumentException("Invalid keyframe time");
            }
            Objects.requireNonNull(value);
            Objects.requireNonNull(incoming);
        }
    }
}
