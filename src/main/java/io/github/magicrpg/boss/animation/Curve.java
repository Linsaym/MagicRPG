package io.github.magicrpg.boss.animation;

public enum Curve {
    LINEAR,
    STEP,
    SMOOTHSTEP,
    EASE_IN,
    EASE_OUT;

    public float map(float position) {
        float t = Math.clamp(position, 0, 1);
        return switch (this) {
            case LINEAR -> t;
            case STEP -> t < 1 ? 0 : 1;
            case SMOOTHSTEP -> t * t * (3 - 2 * t);
            case EASE_IN -> t * t * t;
            case EASE_OUT -> 1 - (1 - t) * (1 - t) * (1 - t);
        };
    }
}
