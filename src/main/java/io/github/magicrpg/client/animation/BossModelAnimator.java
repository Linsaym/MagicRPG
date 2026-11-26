package io.github.magicrpg.client.animation;

import io.github.magicrpg.boss.BossMob;
import io.github.magicrpg.boss.animation.AnimationClip;
import io.github.magicrpg.boss.animation.AnimationPlayer;
import io.github.magicrpg.boss.animation.BoneTransform;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.model.geom.ModelPart;

public final class BossModelAnimator<K> {
    private final Map<Integer, AnimationClip<K>> clips;
    private final float blendTicks;
    private final WeakHashMap<BossMob, AnimationPlayer<K>> players = new WeakHashMap<>();

    public BossModelAnimator(Map<Integer, AnimationClip<K>> clips, float blendTicks) {
        this.clips = Map.copyOf(clips);
        this.blendTicks = blendTicks;
    }

    public Map<K, BoneTransform> sample(BossMob entity, float partialTick) {
        return players.computeIfAbsent(entity, ignored -> new AnimationPlayer<>(clips, blendTicks))
            .sample(entity.animationCue(), entity.level().getGameTime(), partialTick);
    }

    public static <K> void apply(Map<K, BoneTransform> pose, Map<K, ModelPart> parts) {
        pose.forEach((bone, transform) -> {
            var part = parts.get(bone);
            if (part != null) {
                part.x += transform.x();
                part.y += transform.y();
                part.z += transform.z();
                part.xRot += transform.pitch();
                part.yRot += transform.yaw();
                part.zRot += transform.roll();
                part.xScale *= transform.scaleX();
                part.yScale *= transform.scaleY();
                part.zScale *= transform.scaleZ();
            }
        });
    }
}
