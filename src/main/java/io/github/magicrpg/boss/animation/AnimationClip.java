package io.github.magicrpg.boss.animation;

import java.util.HashMap;
import java.util.Map;

public record AnimationClip<K>(int duration, Map<K, KeyframeTrack<BoneTransform>> tracks) {
    public AnimationClip {
        tracks = Map.copyOf(tracks);
        if (duration < 1 || tracks.values().stream().anyMatch(track -> track.endTick() > duration)) {
            throw new IllegalArgumentException("Tracks must fit inside a positive clip duration");
        }
    }

    public Map<K, BoneTransform> sample(float tick) {
        var pose = new HashMap<K, BoneTransform>();
        tracks.forEach((bone, track) -> pose.put(bone, track.sample(Math.clamp(tick, 0, duration))));
        return Map.copyOf(pose);
    }
}
