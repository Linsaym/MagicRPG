package io.github.magicrpg.client;

import static io.github.magicrpg.client.AmberArcherGeometry.BOW_LOWER_TIP;
import static io.github.magicrpg.client.AmberArcherGeometry.BOW_STRING_Z;
import static io.github.magicrpg.client.AmberArcherGeometry.BOW_UPPER_TIP;
import static io.github.magicrpg.client.AmberArcherGeometry.FOREARM_LENGTH;
import static io.github.magicrpg.client.AmberArcherGeometry.SHIN_LENGTH;
import static io.github.magicrpg.client.AmberArcherGeometry.THIGH_LENGTH;
import static io.github.magicrpg.client.AmberArcherGeometry.UPPER_ARM_LENGTH;

import io.github.magicrpg.boss.animation.BoneTransform;
import io.github.magicrpg.client.animation.BossModelAnimator;
import java.util.Map;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class AmberArcherModel extends EntityModel<AmberArcherModel.State> {
    enum Bone { BODY, HOOD, BOW, DRAW_HAND, NOCK, LEFT_FOOT, RIGHT_FOOT, MANTLE, TABARD, QUIVER }

    static final class State extends LivingEntityRenderState {
        Map<Bone, BoneTransform> pose = Map.of();
        float walkWeight;
    }

    private static final Vec3 BOW_SHOULDER = new Vec3(10.15, -13.15, .07);
    private static final Vec3 DRAW_SHOULDER = new Vec3(-10.15, -13.15, .07);
    private static final Vec3 LEFT_ELBOW = new Vec3(.05, .45, 1);
    private static final Vec3 RIGHT_ELBOW = new Vec3(-1, .35, .44);
    private static final Vec3 KNEE = new Vec3(0, 0, -1);

    private final Map<Bone, ModelPart> bones;
    private final ModelPart upperString;
    private final ModelPart lowerString;
    private final ModelPart bowGlove;
    private final ModelPart drawGlove;
    private final ModelPart nockedArrow;
    private final TwoBoneLimb bowArm;
    private final TwoBoneLimb drawArm;
    private final TwoBoneLimb leftLeg;
    private final TwoBoneLimb rightLeg;

    AmberArcherModel(ModelPart root) {
        super(root);
        var body = root.getChild("body");
        var bow = body.getChild("bow");
        var nock = bow.getChild("nock");
        var tabard = body.getChild("tabard");
        upperString = bow.getChild("upper_string");
        lowerString = bow.getChild("lower_string");
        bowGlove = bow.getChild("glove");
        drawGlove = body.getChild("right_glove");
        nockedArrow = nock.getChild("arrow");
        bowArm = new TwoBoneLimb(body, "left_upper", "left_forearm", UPPER_ARM_LENGTH, FOREARM_LENGTH);
        drawArm = new TwoBoneLimb(body, "right_upper", "right_forearm", UPPER_ARM_LENGTH, FOREARM_LENGTH);
        leftLeg = new TwoBoneLimb(root, "left_thigh", "left_shin", THIGH_LENGTH, SHIN_LENGTH);
        rightLeg = new TwoBoneLimb(root, "right_thigh", "right_shin", THIGH_LENGTH, SHIN_LENGTH);
        bones = Map.of(Bone.BODY, body, Bone.HOOD, body.getChild("hood"), Bone.BOW, bow,
            Bone.DRAW_HAND, body.getChild("draw_hand"), Bone.NOCK, nock,
            Bone.LEFT_FOOT, root.getChild("left_foot"), Bone.RIGHT_FOOT, root.getChild("right_foot"),
            Bone.MANTLE, body.getChild("mantle"), Bone.TABARD, tabard, Bone.QUIVER, body.getChild("quiver"));
    }

    @Override
    public void setupAnim(State state) {
        super.setupAnim(state);
        var body = bones.get(Bone.BODY);
        var hood = bones.get(Bone.HOOD);
        var bow = bones.get(Bone.BOW);
        var draw = bones.get(Bone.DRAW_HAND);
        var nock = bones.get(Bone.NOCK);
        var leftFoot = bones.get(Bone.LEFT_FOOT);
        var rightFoot = bones.get(Bone.RIGHT_FOOT);
        var cape = bones.get(Bone.MANTLE);
        var tabard = bones.get(Bone.TABARD);
        var quiver = bones.get(Bone.QUIVER);

        float walk = Mth.clamp(state.walkAnimationSpeed * 1.65F, 0, 1) * state.walkWeight;
        float step = state.walkAnimationPos * .7F;
        float sway = Mth.sin(step) * walk;
        float breath = Mth.sin(state.ageInTicks * .105F);
        body.y += Math.abs(Mth.cos(step)) * walk * .75F + breath * .11F * state.walkWeight;
        body.xRot += .09F * walk;
        body.yRot += sway * .13F - .05F * state.walkWeight;
        body.zRot += sway * .057F + .076F * state.walkWeight;
        hood.xRot += Mth.clamp(state.xRot * Mth.DEG_TO_RAD, -.39F, .43F) * state.walkWeight - body.xRot * .55F;
        hood.yRot += Mth.clamp(state.yRot * Mth.DEG_TO_RAD, -.48F, .48F) * state.walkWeight - sway * .10F;
        bow.z += Mth.cos(step) * 1.3F * walk;
        bow.zRot += sway * .07F;
        bow.y += breath * .25F * state.walkWeight;
        draw.z -= Mth.cos(step) * 2.6F * walk;
        cape.xRot += walk * .24F + breath * .029F;
        cape.zRot -= sway * .11F;
        tabard.xRot += Mth.cos(step - .6F) * .14F * walk;
        quiver.zRot += sway * .052F;
        stride(leftFoot, step, walk);
        stride(rightFoot, step + Mth.PI, walk);
        BossModelAnimator.apply(state.pose, bones);

        Vec3 pull = position(nock);
        aimString(upperString, BOW_UPPER_TIP, pull);
        aimString(lowerString, BOW_LOWER_TIP, pull);
        float contact = Mth.clamp((nock.z - BOW_STRING_Z) / 1.35F, 0, 1);
        nockedArrow.visible = contact > .55F;
        var bowRotation = rotation(bow);
        Vec3 bowHand = position(bow).add(transform(bowRotation, position(bowGlove)));
        Vec3 drawHand = position(draw).lerp(position(bow).add(transform(bowRotation, pull)), contact);
        bowArm.reach(BOW_SHOULDER, bowHand, LEFT_ELBOW);
        drawArm.reach(DRAW_SHOULDER, drawHand, RIGHT_ELBOW);
        drawGlove.setPos((float) drawHand.x, (float) drawHand.y, (float) drawHand.z);
        drawGlove.xRot = Mth.lerp(contact, drawArm.lower.xRot + draw.xRot, bow.xRot - .18F);
        drawGlove.yRot = Mth.lerp(contact, draw.yRot, bow.yRot);
        drawGlove.zRot = Mth.lerp(contact, drawArm.lower.zRot + draw.zRot, bow.zRot - .29F);

        var hips = rotation(body);
        leg(leftLeg, body, hips, leftFoot, 3.95);
        leg(rightLeg, body, hips, rightFoot, -3.95);
    }

    private static void stride(ModelPart foot, float cycle, float weight) {
        foot.z += Mth.cos(cycle) * weight * 5.5F;
        foot.y -= Math.max(0, Mth.sin(cycle)) * weight * 3.3F;
        foot.xRot += Math.max(0, Mth.sin(cycle)) * weight * .34F;
    }

    private static void leg(TwoBoneLimb leg, ModelPart body, Quaternionf hips, ModelPart foot, double side) {
        Vec3 hip = position(body).add(transform(hips, new Vec3(side, 3.8, 0)));
        Vec3 ankle = position(foot).add(transform(rotation(foot), new Vec3(0, -4, 0)));
        leg.reach(hip, ankle, KNEE);
    }

    private static Vec3 position(ModelPart part) {
        return new Vec3(part.x, part.y, part.z);
    }

    private static Quaternionf rotation(ModelPart part) {
        return new Quaternionf().rotationZYX(part.zRot, part.yRot, part.xRot);
    }

    private static Vec3 transform(Quaternionf rotation, Vec3 point) {
        var result = rotation.transform(new Vector3f((float) point.x, (float) point.y, (float) point.z));
        return new Vec3(result.x, result.y, result.z);
    }

    private static void aimString(ModelPart string, float tipY, Vec3 nock) {
        Vec3 tip = new Vec3(0, tipY, BOW_STRING_Z);
        aim(string, tip, nock, 1);
    }

    private record TwoBoneLimb(ModelPart upper, ModelPart lower, double upperLength, double lowerLength) {
        TwoBoneLimb(ModelPart parent, String upper, String lower, double upperLength, double lowerLength) {
            this(parent.getChild(upper), parent.getChild(lower), upperLength, lowerLength);
        }

        void reach(Vec3 start, Vec3 end, Vec3 bend) {
            Vec3 offset = end.subtract(start);
            double distance = offset.length();
            Vec3 axis = distance > 1.0E-5 ? offset.scale(1 / distance) : new Vec3(0, 1, 0);
            Vec3 outward = bend.subtract(axis.scale(bend.dot(axis))).normalize();
            double span = Mth.clamp(distance, Math.abs(upperLength - lowerLength) + .001, upperLength + lowerLength - .001);
            double along = (span * span + upperLength * upperLength - lowerLength * lowerLength) / (2 * span);
            double lift = Math.sqrt(Math.max(0, upperLength * upperLength - along * along));
            Vec3 elbow = start.add(axis.scale(along)).add(outward.scale(lift));
            aim(upper, start, elbow, upperLength);
            aim(lower, elbow, end, lowerLength);
        }
    }

    private static void aim(ModelPart part, Vec3 start, Vec3 end, double length) {
        Vec3 delta = end.subtract(start);
        part.setPos((float) start.x, (float) start.y, (float) start.z);
        part.xRot = (float) Mth.atan2(delta.z, Math.hypot(delta.x, delta.y));
        part.zRot = (float) Mth.atan2(-delta.x, delta.y);
        part.yScale = (float) (delta.length() / length);
    }
}
