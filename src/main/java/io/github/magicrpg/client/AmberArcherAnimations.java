package io.github.magicrpg.client;

import static io.github.magicrpg.boss.animation.BoneTransform.IDENTITY;
import static io.github.magicrpg.client.AmberArcherModel.Bone.*;
import static io.github.magicrpg.entity.AmberArcherFight.*;

import io.github.magicrpg.boss.animation.AnimationClip;
import io.github.magicrpg.boss.animation.BoneTransform;
import io.github.magicrpg.boss.animation.Curve;
import io.github.magicrpg.boss.animation.KeyframeTrack;
import io.github.magicrpg.client.AmberArcherModel.Bone;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class AmberArcherAnimations {
    static final Map<Integer, AnimationClip<Bone>> CLIPS = Map.of(
        BIND_CLIP, bind(), RAIN_CLIP, rain(), FAN_CLIP, fan());

    private AmberArcherAnimations() {}

    private static AnimationClip<Bone> bind() {
        BoneTransform aim = bowPose(5.9F, -10.95F, -17.15F, .15F, .11F, -.02F);
        BoneTransform recoil = bowPose(6.45F, -10.55F, -16.42F, .06F, .05F, .03F);
        BoneTransform spent = bowPose(11.75F, 3.1F, -9.05F, .18F, -1.01F, .58F);
        return new AnimationClip<>(BIND_END, Map.of(
            BODY, track(key(0, IDENTITY), key(4, pose(-.55F, .8F, .45F, .12F, -.17F, -.072F)),
                key(10, pose(-.45F, .67F, .1F, .045F, -.20F, -.062F)),
                key(LAST_SHOT, pose(-.25F, .89F, .3F, .075F, -.20F, -.058F)),
                key(RELEASE + 7, pose(.25F, 1.15F, .38F, .20F, -.04F, .06F)),
                key(BIND_END, IDENTITY)),
            HOOD, track(key(0, IDENTITY), key(9, rotate(.12F, .17F, .063F)),
                key(LAST_SHOT, rotate(.16F, .18F, .063F)), key(RELEASE + 7, rotate(.21F, .08F, -.09F)),
                key(BIND_END, IDENTITY)),
            BOW, repeatShots(aim, aim, recoil, bowPose(6.15F, -10.8F, -16.71F, .08F, .06F, 0), spent),
            NOCK, repeatShots(shift(0, 0, 1.55F), shift(0, 0, 4.10F),
                shift(0, 0, -.65F), IDENTITY, IDENTITY),
            DRAW_HAND, repeatShots(pose(5.4F, -14F, -5F, .2F, .65F, .3F),
                pose(5.6F, -15F, -4F, .05F, .7F, .32F),
                pose(2.55F, -16.2F, 1.2F, -.25F, .7F, .47F),
                pose(1.55F, -15.1F, 1.55F, -.15F, .55F, .32F), shift(.5F, .55F, -.65F)),
            LEFT_FOOT, track(key(0, IDENTITY), key(8, pose(1.0F, 0, -2.8F, 0, -.2F, 0)),
                key(RELEASE + 9, pose(1F, 0, -2.8F, 0, -.2F, 0)), key(BIND_END, IDENTITY)),
            RIGHT_FOOT, track(key(0, IDENTITY), key(9, pose(-.75F, 0, 2.15F, 0, .16F, 0)),
                key(RELEASE + 12, pose(-.75F, 0, 2.15F, 0, .16F, 0)), key(BIND_END, IDENTITY)),
            MANTLE, track(key(0, IDENTITY), key(11, rotate(.09F, .16F, .08F)),
                key(RELEASE + 7, rotate(-.12F, -.04F, -.11F)), key(BIND_END, IDENTITY))));
    }

    private static KeyframeTrack<BoneTransform> repeatShots(BoneTransform load, BoneTransform draw,
                                                            BoneTransform release, BoneTransform follow, BoneTransform rest) {
        var keys = new ArrayList<KeyframeTrack.Key<BoneTransform>>();
        keys.add(key(0, IDENTITY));
        for (int shot = FIRST_SHOT; shot <= LAST_SHOT; shot += SHOT_INTERVAL) {
            keys.add(key(shot - 8, load));
            keys.add(key(shot - 1, draw));
            keys.add(new KeyframeTrack.Key<>(shot, release, Curve.EASE_IN));
            keys.add(key(shot + 3, follow));
        }
        keys.add(key(RELEASE + 5, rest));
        keys.add(key(BIND_END - 7, rest));
        keys.add(key(BIND_END, IDENTITY));
        return new KeyframeTrack<>(keys, BoneTransform::blend);
    }

    private static AnimationClip<Bone> rain() {
        var up = bowPose(7.9F, -18, -10.9F, -1.44F, -.61F, .07F);
        return new AnimationClip<>(RAIN_END, Map.of(
            BODY, track(key(0, IDENTITY), key(4, pose(.45F, .6F, .15F, .09F, -.13F, .085F)),
                key(RAIN_LAUNCH - 1, pose(.07F, -.3F, .46F, -.21F, -.15F, .08F)),
                key(RAIN_LAUNCH + 3, rotate(-.30F, -.11F, .09F)), key(24, rotate(-.09F, -.1F, .055F)),
                key(RAIN_FALL + 10, IDENTITY)),
            HOOD, track(key(0, IDENTITY), key(5, rotate(.10F, .13F, 0)),
                key(RAIN_LAUNCH - 1, rotate(-.52F, .16F, -.04F)), key(21, rotate(-.5F, .13F, 0)),
                key(RAIN_FALL + 2, rotate(.15F, .11F, .035F)), key(RAIN_END, IDENTITY)),
            BOW, track(key(0, IDENTITY), key(4, bowPose(12.15F, -3.1F, -10.8F, -.35F, -.58F, .30F)),
                key(RAIN_LAUNCH - 1, up), key(RAIN_LAUNCH, bowPose(7.9F, -18.5F, -11.42F, -1.57F, -.61F, .07F)),
                key(RAIN_LAUNCH + 5, up), key(23, bowPose(11.3F, -7.8F, -10.7F, -.77F, -.42F, .27F)),
                key(RAIN_FALL, bowPose(13.05F, .05F, -9.2F, -.25F, -.77F, .47F)), key(RAIN_END, IDENTITY)),
            NOCK, track(key(0, IDENTITY), key(5, shift(0, 0, 1.45F)),
                key(RAIN_LAUNCH - 1, shift(0, 0, 4.85F)), key(RAIN_LAUNCH, shift(0, 0, -.74F)),
                key(RAIN_LAUNCH + 3, IDENTITY)),
            DRAW_HAND, track(key(0, IDENTITY), key(6, pose(6.7F, -13.2F, -5.4F, .17F, .32F, .3F)),
                key(RAIN_LAUNCH - 1, pose(7.7F, -13.2F, -2.8F, .20F, .42F, .42F)),
                key(RAIN_LAUNCH, pose(5.05F, -14.6F, 1.1F, -.30F, .5F, .66F)),
                key(26, pose(2.8F, -7.6F, .18F, -.12F, .27F, .28F)), key(RAIN_FALL + 7, IDENTITY)),
            MANTLE, track(key(0, IDENTITY), key(RAIN_LAUNCH + 4, rotate(.18F, .11F, -.07F)),
                key(RAIN_FALL, rotate(-.09F, -.07F, .05F)), key(RAIN_END, IDENTITY)),
            RIGHT_FOOT, track(key(0, IDENTITY), key(5, shift(-.15F, -.82F, 1.05F)),
                key(9, pose(-.38F, 0, 1.95F, 0, .10F, 0)), key(33, pose(-.38F, 0, 1.95F, 0, .10F, 0)),
                key(RAIN_END, IDENTITY))));
    }

    private static AnimationClip<Bone> fan() {
        var windup = bowPose(8.25F, -2.5F, -12.8F, -.06F, .10F, 1.39F);
        return new AnimationClip<>(FAN_END, Map.of(
            BODY, track(key(0, IDENTITY), key(6, pose(-.5F, 1.75F, -.95F, .21F, -.12F, -.12F)),
                key(FAN_SHOT - 1, pose(-.15F, 2.65F, -1.15F, .26F, -.16F, -.09F)),
                key(FAN_SHOT + 3, pose(.42F, -.45F, .43F, -.19F, -.14F, .11F)),
                key(25, pose(.23F, 1.95F, -.35F, .20F, -.07F, .055F)), key(FAN_END, IDENTITY)),
            HOOD, track(key(0, IDENTITY), key(FAN_SHOT - 1, rotate(-.15F, .14F, .11F)),
                key(FAN_SHOT + 4, rotate(.095F, .16F, -.10F)), key(26, rotate(-.04F, .09F, -.04F)),
                key(FAN_END, IDENTITY)),
            BOW, track(key(0, IDENTITY), key(6, windup),
                key(FAN_SHOT - 1, bowPose(7.65F, -3.9F, -13.6F, .17F, .08F, 1.58F)),
                key(FAN_SHOT, bowPose(7.15F, -4.4F, -13.9F, .23F, .10F, 1.51F)),
                key(FAN_SHOT + 4, bowPose(8.25F, -3.8F, -13.65F, .09F, .22F, 1.63F)),
                key(25, bowPose(12.2F, 2.3F, -10.55F, .14F, -.78F, .89F)), key(FAN_END, IDENTITY)),
            NOCK, track(key(0, IDENTITY), key(7, shift(0, 0, 1.45F)),
                key(FAN_SHOT - 1, shift(0, 0, 4.30F)), key(FAN_SHOT, shift(0, 0, -.72F)),
                key(FAN_SHOT + 4, IDENTITY)),
            DRAW_HAND, track(key(0, IDENTITY), key(6, shift(5.3F, -10F, -5.5F)),
                key(FAN_SHOT - 1, pose(5F, -11.1F, -2.6F, -.23F, .62F, -.41F)),
                key(FAN_SHOT, pose(1.8F, -10.8F, .95F, -.37F, .18F, -.32F)),
                key(FAN_SHOT + 4, pose(.7F, -9.05F, -1.3F, -.26F, .17F, -.28F)), key(FAN_END, IDENTITY)),
            MANTLE, track(key(0, IDENTITY), key(FAN_SHOT - 2, rotate(-.09F, .1F, .06F)),
                key(FAN_SHOT + 6, rotate(-.45F, .09F, -.21F)), key(27, rotate(.16F, -.08F, .10F)),
                key(FAN_END, IDENTITY)),
            QUIVER, track(key(0, IDENTITY), key(FAN_SHOT + 5, rotate(-.16F, .05F, -.16F)),
                key(26, rotate(.10F, -.04F, .065F)), key(FAN_END, IDENTITY)),
            LEFT_FOOT, track(key(0, IDENTITY), key(FAN_SHOT - 1, shift(.45F, 0, -.65F)),
                key(FAN_SHOT + 5, pose(.6F, -3.35F, -3.1F, -.30F, -.10F, -.09F)),
                key(23, pose(.72F, 0, -1.65F, -.05F, -.10F, 0)), key(FAN_END, IDENTITY)),
            RIGHT_FOOT, track(key(0, IDENTITY), key(FAN_SHOT - 1, shift(-.45F, 0, 1.1F)),
                key(FAN_SHOT + 4, pose(-.7F, -2.2F, 3.4F, .43F, .16F, .07F)),
                key(22, pose(-.85F, 0, 3.7F, .07F, .15F, 0)), key(FAN_END, IDENTITY))));
    }

    @SafeVarargs
    private static KeyframeTrack<BoneTransform> track(KeyframeTrack.Key<BoneTransform>... keys) {
        return new KeyframeTrack<>(List.of(keys), BoneTransform::blend);
    }

    private static KeyframeTrack.Key<BoneTransform> key(int tick, BoneTransform transform) {
        return new KeyframeTrack.Key<>(tick, transform, Curve.SMOOTHSTEP);
    }

    private static BoneTransform rotate(float pitch, float yaw, float roll) {
        return pose(0, 0, 0, pitch, yaw, roll);
    }

    private static BoneTransform shift(float x, float y, float z) {
        return pose(x, y, z, 0, 0, 0);
    }

    private static BoneTransform bowPose(float x, float y, float z, float pitch, float yaw, float roll) {
        var rest = AmberArcherGeometry.BOW_REST;
        return pose(x - rest.x(), y - rest.y(), z - rest.z(),
            pitch - rest.xRot(), yaw - rest.yRot(), roll - rest.zRot());
    }

    private static BoneTransform pose(float x, float y, float z, float pitch, float yaw, float roll) {
        return new BoneTransform(x, y, z, pitch, yaw, roll);
    }
}
