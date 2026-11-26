package io.github.magicrpg.boss.animation;

import java.util.HashMap;
import java.util.Map;

/** Keep one player per rendered entity, sharing only immutable clips. */
public final class AnimationPlayer<K> {
    private final Map<Integer, AnimationClip<K>> clips;
    private final float blendTicks;
    private AnimationCue current;
    private Map<K, BoneTransform> lastPose = Map.of();
    private Map<K, BoneTransform> outgoing = Map.of();
    private double blendStarted;

    public AnimationPlayer(Map<Integer, AnimationClip<K>> clips, float blendTicks) {
        if (!Float.isFinite(blendTicks) || blendTicks < 0 || clips.keySet().stream().anyMatch(id -> id <= 0)) {
            throw new IllegalArgumentException("Invalid transition time or clip ID");
        }
        this.clips = Map.copyOf(clips);
        this.blendTicks = blendTicks;
    }

    public Map<K, BoneTransform> sample(AnimationCue cue, long gameTime, float partialTick) {
        if (current != null && current.clip() == 0 && cue.clip() == 0 && outgoing.isEmpty()) {
            return Map.of();
        }
        double time = gameTime + (double) partialTick;
        if (!cue.equals(current)) {
            if (current != null) {
                outgoing = lastPose;
                blendStarted = time;
            } else {
                // Newly tracked bosses enter at the server's current pose.
                blendStarted = time - blendTicks;
            }
            current = cue;
        }
        float weight = blendTicks == 0 ? 1 : Curve.SMOOTHSTEP.map((float) ((time - blendStarted) / blendTicks));
        var clip = clips.get(cue.clip());
        var incoming = clip == null ? Map.<K, BoneTransform>of() : clip.sample(cue.elapsed(gameTime, partialTick));
        if (weight == 1) {
            lastPose = incoming;
            outgoing = Map.of();
        } else {
            var blended = new HashMap<K, BoneTransform>();
            outgoing.forEach((bone, pose) -> blended.put(bone,
                pose.blend(incoming.getOrDefault(bone, BoneTransform.IDENTITY), weight)));
            incoming.forEach((bone, pose) -> {
                if (!outgoing.containsKey(bone)) {
                    blended.put(bone, BoneTransform.IDENTITY.blend(pose, weight));
                }
            });
            lastPose = Map.copyOf(blended);
        }
        return lastPose;
    }
}
