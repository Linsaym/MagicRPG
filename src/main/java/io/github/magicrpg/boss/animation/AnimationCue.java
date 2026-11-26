package io.github.magicrpg.boss.animation;

/** A clip and its start tick travel together in a single synchronized entity field. */
public record AnimationCue(int clip, int startTick) {
    public long encode() {
        return ((long) clip << Integer.SIZE) | Integer.toUnsignedLong(startTick);
    }

    public static AnimationCue decode(long packed) {
        return new AnimationCue((int) (packed >>> Integer.SIZE), (int) packed);
    }

    public float elapsed(long gameTime, float partialTick) {
        return Math.max(0, ((int) gameTime - startTick) + partialTick);
    }
}
