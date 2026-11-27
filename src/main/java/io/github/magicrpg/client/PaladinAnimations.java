package io.github.magicrpg.client;

import static io.github.magicrpg.boss.animation.Curve.*;
import static io.github.magicrpg.client.PaladinGeometry.*;
import static io.github.magicrpg.client.PaladinModel.Bone.*;
import static io.github.magicrpg.client.PaladinModel.WALK_BLADE_ROLL;
import static io.github.magicrpg.client.PaladinModel.WALK_BLADE_SHIFT;
import static io.github.magicrpg.entity.MeadowPaladinFight.*;

import io.github.magicrpg.boss.animation.AnimationClip;
import io.github.magicrpg.boss.animation.AnimationCue;
import io.github.magicrpg.boss.animation.BoneTransform;
import io.github.magicrpg.boss.animation.Curve;
import io.github.magicrpg.boss.animation.KeyframeTrack;
import io.github.magicrpg.client.PaladinModel.Bone;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class PaladinAnimations {
    static final AnimationCue PLANTED_SWORD = new AnimationCue(4, 0);
    static final Map<Integer, AnimationClip<Bone>> CLIPS = Map.of(
        SWEEP, sweep(), HEW, hew(), RESONANCE, resonance(), PLANTED_SWORD.clip(), guardStance());

    private PaladinAnimations() {}

    private static AnimationClip<Bone> guardStance() {
        float hipRise = 1.5F;
        float tipBelowSoles = 1;
        float gripHeight = SOLES_Y + tipBelowSoles - SWORD_POINT_Y - (HIPS_Y - hipRise + CHEST_Y);
        return new AnimationClip<>(1, Map.of(
            HIPS, held(frame(0, LINEAR, 0, -hipRise, 0, 0, 0, 0)),
            BLADE, held(frame(0, LINEAR, -WALK_BLADE_SHIFT, gripHeight - GRIP_Y, -4.5F,
                -GRIP_PITCH, .055F, -WALK_BLADE_ROLL)),
            HELM, held(frame(0, LINEAR, 0, 0, 0, .10F, 0, 0)),
            CORE, held(core(0, .88F)),
            LEFT_BOOT, held(frame(0, LINEAR, 0, 0, -BOOT_STAGGER, 0, 0, 0)),
            RIGHT_BOOT, held(frame(0, LINEAR, 0, 0, BOOT_STAGGER, 0, 0, 0))));
    }

    private static AnimationClip<Bone> sweep() {
        return new AnimationClip<>(SWEEP_END, Map.of(
            HIPS, track(SWEEP_END,
                frame(13, SMOOTHSTEP, -2, 2, 2, 0, 0, 0),
                frame(17, EASE_OUT, -2.3F, 3, 2, 0, 0, 0),
                frame(SWEEP_CONTACT, EASE_IN, 1, 2, -1, 0, 0, 0),
                frame(26, EASE_OUT, 3, 3, -2, 0, 0, 0),
                frame(32, SMOOTHSTEP, 2, 3, -1, 0, 0, 0)),
            CHEST, track(SWEEP_END,
                frame(11, SMOOTHSTEP, 0, 0, 0, .05F, .64F, -.14F),
                frame(16, EASE_OUT, 0, 0, 0, .11F, .72F, -.17F),
                frame(SWEEP_CONTACT, EASE_IN, 0, 0, 0, .10F, 0, .03F),
                frame(25, EASE_OUT, 0, 0, 0, .26F, -.68F, .20F),
                frame(33, SMOOTHSTEP, 0, 0, 0, .27F, -.60F, .16F)),
            BLADE, track(SWEEP_END,
                frame(15, SMOOTHSTEP, -4, 4, 3, .70F, .68F, .15F),
                frame(18, EASE_OUT, -4, 4, 3, .69F, .85F, .14F),
                frame(SWEEP_CONTACT, EASE_IN, 0, 6, -3, .70F, 0, 0),
                frame(26, EASE_OUT, 4, 8, 3, .98F, -.65F, -.07F),
                frame(33, SMOOTHSTEP, 4, 8, 2, 1.00F, -.55F, -.07F)),
            HELM, track(SWEEP_END,
                frame(16, SMOOTHSTEP, 0, 0, 0, .04F, -.59F, .10F),
                frame(25, EASE_OUT, 0, 0, 0, .08F, .44F, -.08F),
                frame(33, SMOOTHSTEP, 0, 0, 0, .11F, .39F, -.08F)),
            LEFT_BOOT, track(SWEEP_END,
                frame(14, SMOOTHSTEP, 0, 0, 0, 0, 0, 0),
                frame(17, EASE_IN, 2, -2, -3, .12F, 0, 0),
                frame(20, EASE_OUT, 3, 0, -5, 0, .32F, 0),
                frame(33, LINEAR, 3, 0, -5, 0, .32F, 0),
                frame(37, SMOOTHSTEP, 2, -2, -3, -.12F, .17F, 0)),
            CAPE, track(SWEEP_END,
                frame(16, SMOOTHSTEP, 0, 0, 0, .03F, -.30F, 0),
                frame(24, EASE_IN, 0, 0, 0, .29F, .63F, .12F),
                frame(32, EASE_OUT, 0, 0, 0, .09F, .20F, 0)),
            HEM, track(SWEEP_END,
                frame(26, EASE_OUT, 0, 0, 0, .20F, .38F, .08F),
                frame(34, SMOOTHSTEP, 0, 0, 0, -.08F, -.10F, 0))));
    }

    private static AnimationClip<Bone> hew() {
        return new AnimationClip<>(HEW_END, Map.of(
            HIPS, track(HEW_END,
                frame(15, SMOOTHSTEP, 0, 2, 3, 0, 0, 0),
                frame(22, EASE_OUT, 0, 3, 2, 0, 0, 0),
                frame(HEW_CONTACT, EASE_IN, -.8F, 4, -3, 0, 0, 0),
                frame(34, EASE_OUT, -.8F, 5, -3, 0, 0, 0),
                frame(45, SMOOTHSTEP, -.8F, 4, -2.5F, 0, 0, 0),
                frame(50, EASE_IN, -.3F, 2, .6F, 0, 0, 0)),
            CHEST, track(HEW_END,
                frame(17, SMOOTHSTEP, 0, 0, 0, -.24F, .08F, .10F),
                frame(23, EASE_OUT, 0, 0, 0, -.22F, .06F, .07F),
                frame(HEW_CONTACT, EASE_IN, 0, 0, 0, .32F, 0, 0),
                frame(34, EASE_OUT, 0, 0, 0, .41F, 0, -.04F),
                frame(45, SMOOTHSTEP, 0, 0, 0, .37F, 0, -.05F),
                frame(50, EASE_OUT, 0, 0, 0, .13F, 0, .06F)),
            BLADE, track(HEW_END,
                frame(17, SMOOTHSTEP, 1.5F, -23, 4, -1.61F, 0, -.19F),
                frame(23, EASE_OUT, 1.2F, -23, 2, -1.51F, 0, -.11F),
                frame(HEW_CONTACT, EASE_IN, .8F, 3.65F, -3.96F, 1.26F, 0, 0),
                // The grip drops with the shoulders while the point stays against the floor.
                frame(34, EASE_OUT, .77F, .74F, -2.31F, 1.19F, 0, .01F),
                frame(45, SMOOTHSTEP, .73F, 2.22F, -2.91F, 1.22F, 0, .02F),
                frame(49, EASE_IN, .5F, -6, 4, .92F, 0, -.14F)),
            HELM, track(HEW_END,
                frame(19, SMOOTHSTEP, 0, 0, 0, .16F, -.08F, -.08F),
                frame(HEW_CONTACT, EASE_IN, 0, 0, 0, .19F, 0, 0),
                frame(37, EASE_OUT, 0, 0, 0, .30F, 0, 0),
                frame(48, SMOOTHSTEP, 0, 0, 0, .12F, 0, 0)),
            RIGHT_BOOT, track(HEW_END,
                frame(12, LINEAR, 0, 0, 0, 0, 0, 0),
                frame(17, EASE_OUT, -1, -3, -4, -.16F, 0, 0),
                frame(21, EASE_IN, -1, 0, -6, 0, -.06F, 0),
                frame(47, LINEAR, -1, 0, -6, 0, -.06F, 0),
                frame(52, SMOOTHSTEP, -.5F, -2, -3, -.12F, 0, 0)),
            CAPE, track(HEW_END,
                frame(21, SMOOTHSTEP, 0, 0, 0, -.18F, 0, -.06F),
                frame(32, EASE_OUT, 0, 0, 0, .36F, 0, .06F),
                frame(41, SMOOTHSTEP, 0, 0, 0, .02F, 0, 0)),
            HEM, track(HEW_END,
                frame(35, EASE_OUT, 0, 0, 0, .24F, 0, 0),
                frame(43, SMOOTHSTEP, 0, 0, 0, -.09F, 0, 0))));
    }

    private static AnimationClip<Bone> resonance() {
        return new AnimationClip<>(RESONANCE_END, Map.of(
            HIPS, track(RESONANCE_END,
                frame(12, SMOOTHSTEP, 0, 2, 1, 0, 0, 0),
                frame(28, SMOOTHSTEP, 0, 0, 0, 0, 0, 0),
                frame(RESONANCE_CONTACT, EASE_IN, 0, 3, -1, 0, 0, 0),
                frame(38, EASE_OUT, 0, 5, -1, 0, 0, 0),
                frame(57, SMOOTHSTEP, 0, 5, -1, 0, 0, 0)),
            CHEST, track(RESONANCE_END,
                frame(12, SMOOTHSTEP, 0, 0, 0, .18F, 0, 0),
                frame(28, SMOOTHSTEP, 0, 0, 0, -.11F, 0, 0),
                frame(RESONANCE_CONTACT, EASE_IN, 0, 0, 0, .16F, 0, 0),
                frame(38, EASE_OUT, 0, 0, 0, .27F, 0, 0),
                frame(54, SMOOTHSTEP, 0, 0, 0, .28F, 0, .025F),
                frame(62, EASE_OUT, 0, 0, 0, .11F, 0, -.045F)),
            BLADE, track(RESONANCE_END,
                frame(9, SMOOTHSTEP, 2, -14, -1, .85F, -.20F, -.72F),
                frame(18, SMOOTHSTEP, 0, -19, 0, 2.17F, 0, -.07F),
                frame(28, SMOOTHSTEP, 0, -25, 0, 2.39F, 0, 0),
                frame(RESONANCE_CONTACT, EASE_IN, 0, -4.09F, -1.12F, 2.12F, 0, 0),
                frame(38, EASE_OUT, 0, -7.75F, -2.17F, 2.01F, 0, 0),
                frame(54, SMOOTHSTEP, -.28F, -7.88F, -2.33F, 2.0F, 0, -.025F),
                frame(61, EASE_IN, 1, -18, 0, 1.93F, -.13F, -.18F)),
            HELM, track(RESONANCE_END,
                frame(24, SMOOTHSTEP, 0, 0, 0, -.11F, 0, 0),
                frame(38, EASE_OUT, 0, 0, 0, .30F, 0, 0),
                frame(57, SMOOTHSTEP, 0, 0, 0, .21F, 0, .04F)),
            CORE, track(RESONANCE_END,
                core(12, 1.16F), core(27, 1.42F), core(RESONANCE_CONTACT, 1.65F), core(44, .78F), core(61, .93F)),
            CAPE, track(RESONANCE_END,
                frame(26, SMOOTHSTEP, 0, 0, 0, .14F, 0, 0),
                frame(36, EASE_OUT, 0, 0, 0, .69F, 0, 0),
                frame(43, SMOOTHSTEP, 0, 0, 0, .21F, 0, 0),
                frame(51, SMOOTHSTEP, 0, 0, 0, -.06F, 0, 0)),
            HEM, track(RESONANCE_END,
                frame(39, EASE_OUT, 0, 0, 0, .36F, 0, 0),
                frame(48, SMOOTHSTEP, 0, 0, 0, -.18F, 0, 0))));
    }

    private static KeyframeTrack<BoneTransform> held(KeyframeTrack.Key<BoneTransform> pose) {
        return new KeyframeTrack<>(List.of(pose), BoneTransform::blend);
    }

    @SafeVarargs
    private static KeyframeTrack<BoneTransform> track(int end, KeyframeTrack.Key<BoneTransform>... frames) {
        var keys = new ArrayList<KeyframeTrack.Key<BoneTransform>>(frames.length + 2);
        keys.add(new KeyframeTrack.Key<>(0, BoneTransform.IDENTITY, LINEAR));
        keys.addAll(List.of(frames));
        keys.add(new KeyframeTrack.Key<>(end, BoneTransform.IDENTITY, SMOOTHSTEP));
        return new KeyframeTrack<>(keys, BoneTransform::blend);
    }

    private static KeyframeTrack.Key<BoneTransform> frame(int tick, Curve in, float x, float y, float z,
                                                         float pitch, float yaw, float roll) {
        return new KeyframeTrack.Key<>(tick, new BoneTransform(x, y, z, pitch, yaw, roll), in);
    }

    private static KeyframeTrack.Key<BoneTransform> core(int tick, float scale) {
        return new KeyframeTrack.Key<>(tick,
            new BoneTransform(0, 0, 0, 0, 0, 0, scale, scale, 1), SMOOTHSTEP);
    }
}
