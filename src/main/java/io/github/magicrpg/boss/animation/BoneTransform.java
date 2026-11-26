package io.github.magicrpg.boss.animation;

public record BoneTransform(float x, float y, float z, float pitch, float yaw, float roll,
                            float scaleX, float scaleY, float scaleZ) {
    public static final BoneTransform IDENTITY = new BoneTransform(0, 0, 0, 0, 0, 0);

    public BoneTransform {
        if (!(Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z) && Float.isFinite(pitch)
            && Float.isFinite(yaw) && Float.isFinite(roll) && Float.isFinite(scaleX) && scaleX > 0
            && Float.isFinite(scaleY) && scaleY > 0 && Float.isFinite(scaleZ) && scaleZ > 0)) {
            throw new IllegalArgumentException("Invalid bone transform");
        }
    }

    public BoneTransform(float x, float y, float z, float pitch, float yaw, float roll) {
        this(x, y, z, pitch, yaw, roll, 1, 1, 1);
    }

    public BoneTransform blend(BoneTransform to, float weight) {
        return new BoneTransform(
            mix(x, to.x, weight), mix(y, to.y, weight), mix(z, to.z, weight),
            mix(pitch, to.pitch, weight), mix(yaw, to.yaw, weight), mix(roll, to.roll, weight),
            mix(scaleX, to.scaleX, weight), mix(scaleY, to.scaleY, weight), mix(scaleZ, to.scaleZ, weight));
    }

    private static float mix(float from, float to, float weight) {
        return (1 - weight) * from + weight * to;
    }
}
