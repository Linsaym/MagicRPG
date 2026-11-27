package io.github.magicrpg.client;

import static io.github.magicrpg.client.StormPilotModel.Bone.BODY;
import static io.github.magicrpg.client.StormPilotModel.Bone.LEFT_FIN;
import static io.github.magicrpg.client.StormPilotModel.Bone.LENS;
import static io.github.magicrpg.client.StormPilotModel.Bone.RIGHT_FIN;
import static io.github.magicrpg.entity.StormPilotFight.EYE_CLIP;
import static io.github.magicrpg.entity.StormPilotFight.EYE_CONTACT;
import static io.github.magicrpg.entity.StormPilotFight.EYE_DURATION;
import static io.github.magicrpg.entity.StormPilotFight.LINE_CLIP;
import static io.github.magicrpg.entity.StormPilotFight.LINE_CONTACT;
import static io.github.magicrpg.entity.StormPilotFight.LINE_DURATION;
import static io.github.magicrpg.entity.StormPilotFight.SURF_CLIP;
import static io.github.magicrpg.entity.StormPilotFight.SURF_CONTACT;
import static io.github.magicrpg.entity.StormPilotFight.SURF_DURATION;
import static io.github.magicrpg.entity.StormPilotFight.SURF_END;

import io.github.magicrpg.boss.animation.AnimationClip;
import io.github.magicrpg.boss.animation.BoneTransform;
import io.github.magicrpg.boss.animation.Curve;
import io.github.magicrpg.boss.animation.KeyframeTrack;
import java.util.List;
import java.util.Map;

final class StormPilotAnimations {
    static final Map<Integer, AnimationClip<StormPilotModel.Bone>> CLIPS = Map.of(
        LINE_CLIP, line(), SURF_CLIP, surf(), EYE_CLIP, eye());

    private StormPilotAnimations() {}

    private static AnimationClip<StormPilotModel.Bone> line() {
        return new AnimationClip<>(LINE_DURATION, Map.of(
            BODY, track(key(0, BoneTransform.IDENTITY), key(LINE_CONTACT - 4, body(-1, .23F, -.10F)),
                hit(LINE_CONTACT, body(-3, -.16F, .06F)), key(LINE_DURATION, BoneTransform.IDENTITY)),
            LEFT_FIN, fin(1, LINE_CONTACT, LINE_DURATION, .41F, -.25F),
            RIGHT_FIN, fin(-1, LINE_CONTACT, LINE_DURATION, .41F, -.25F),
            LENS, lens(LINE_CONTACT, LINE_DURATION, .85F, 1.29F)));
    }

    private static AnimationClip<StormPilotModel.Bone> surf() {
        return new AnimationClip<>(SURF_DURATION, Map.of(
            BODY, track(key(0, BoneTransform.IDENTITY), key(SURF_CONTACT - 4, body(-7, -.11F, 0)),
                hit(SURF_CONTACT, body(4, .18F, 0)), key(SURF_END - 6, body(1, .08F, 0)),
                key(SURF_DURATION, BoneTransform.IDENTITY)),
            LEFT_FIN, fin(1, SURF_CONTACT, SURF_DURATION, -.48F, .56F),
            RIGHT_FIN, fin(-1, SURF_CONTACT, SURF_DURATION, -.48F, .56F),
            LENS, lens(SURF_CONTACT, SURF_DURATION, 1.06F, 1.17F)));
    }

    private static AnimationClip<StormPilotModel.Bone> eye() {
        return new AnimationClip<>(EYE_DURATION, Map.of(
            BODY, track(key(0, BoneTransform.IDENTITY), key(12, body(-3, .02F, -.11F)),
                key(EYE_CONTACT - 4, body(-5, -.10F, .12F)), hit(EYE_CONTACT, body(-7, .07F, 0)),
                key(EYE_DURATION - 10, body(1, .13F, 0)), key(EYE_DURATION, BoneTransform.IDENTITY)),
            LEFT_FIN, fin(1, EYE_CONTACT, EYE_DURATION, .90F, -.67F),
            RIGHT_FIN, fin(-1, EYE_CONTACT, EYE_DURATION, .90F, -.67F),
            LENS, lens(EYE_CONTACT, EYE_DURATION, .72F, 1.45F)));
    }

    private static KeyframeTrack<BoneTransform> fin(int side, int contact, int end, float closed, float open) {
        var folded = new BoneTransform(-side * closed * 2, 0, 0, 0, -side * closed * .45F, side * closed);
        var spread = new BoneTransform(-side * open * 3, 0, -1, 0, -side * open * .35F, side * open);
        return track(key(0, BoneTransform.IDENTITY), key(contact - 4, folded), hit(contact, spread),
            key(contact + 5, spread), key(end, BoneTransform.IDENTITY));
    }

    private static KeyframeTrack<BoneTransform> lens(int contact, int end, float charge, float flash) {
        return track(key(0, BoneTransform.IDENTITY), key(contact - 4, lensScale(charge)),
            hit(contact, lensScale(flash)), key(end - 8, lensScale(1.08F)), key(end, BoneTransform.IDENTITY));
    }

    private static BoneTransform body(float y, float pitch, float roll) {
        return new BoneTransform(0, y, 0, pitch, 0, roll);
    }

    private static BoneTransform lensScale(float scale) {
        return new BoneTransform(0, 0, 1 - scale, 0, 0, 0, scale, scale, scale);
    }

    private static KeyframeTrack.Key<BoneTransform> key(int tick, BoneTransform pose) {
        return new KeyframeTrack.Key<>(tick, pose, Curve.SMOOTHSTEP);
    }

    private static KeyframeTrack.Key<BoneTransform> hit(int tick, BoneTransform pose) {
        return new KeyframeTrack.Key<>(tick, pose, Curve.EASE_IN);
    }

    @SafeVarargs
    private static KeyframeTrack<BoneTransform> track(KeyframeTrack.Key<BoneTransform>... keys) {
        return new KeyframeTrack<>(List.of(keys), BoneTransform::blend);
    }
}
