package io.github.magicrpg.client;

import static io.github.magicrpg.client.PaladinGeometry.LEFT_HAND;
import static io.github.magicrpg.client.PaladinGeometry.RIGHT_HAND;

import io.github.magicrpg.MagicRpg;
import io.github.magicrpg.boss.animation.BoneTransform;
import io.github.magicrpg.client.animation.BossModelAnimator;
import java.util.Map;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class PaladinModel extends EntityModel<PaladinModel.State> {
    static final ModelLayerLocation LAYER = new ModelLayerLocation(MagicRpg.id("meadow_paladin"), "main");

    enum Bone { HIPS, CHEST, HELM, BLADE, CAPE, HEM, CORE, LEFT_BOOT, RIGHT_BOOT }

    static final class State extends LivingEntityRenderState {
        Map<Bone, BoneTransform> pose = Map.of();
        float walkWeight;
        float glow;
    }

    static final float WALK_BLADE_SHIFT = 2.5F, WALK_BLADE_ROLL = .23F;
    private static final Vec3 KNEE = new Vec3(0, 0, -1);

    private final Map<Bone, ModelPart> bones;
    private final ModelPart hips;
    private final ModelPart blade;
    private final Limb rightArm;
    private final Limb leftArm;
    private final Limb rightLeg;
    private final Limb leftLeg;

    PaladinModel(ModelPart root) {
        super(root);
        hips = root.getChild("hips");
        var chest = hips.getChild("chest");
        var cape = chest.getChild("cape");
        blade = chest.getChild("blade");
        var leftBoot = root.getChild("left_boot");
        var rightBoot = root.getChild("right_boot");
        bones = Map.of(
            Bone.HIPS, hips, Bone.CHEST, chest, Bone.HELM, chest.getChild("helm"), Bone.BLADE, blade,
            Bone.CAPE, cape, Bone.HEM, cape.getChild("hem"), Bone.CORE,
            chest.getChild("core_socket").getChild("core"),
            Bone.LEFT_BOOT, leftBoot, Bone.RIGHT_BOOT, rightBoot);
        rightArm = new Limb(chest, "right_upper_arm", "right_forearm", 15, 15);
        leftArm = new Limb(chest, "left_upper_arm", "left_forearm", 15, 15);
        rightLeg = new Limb(root, "right_thigh", "right_shin", 17, 18);
        leftLeg = new Limb(root, "left_thigh", "left_shin", 17, 18);
    }

    static LayerDefinition createLayer() {
        return PaladinGeometry.createLayer();
    }

    @Override
    public void setupAnim(State state) {
        super.setupAnim(state);
        var helm = bones.get(Bone.HELM);
        var cape = bones.get(Bone.CAPE);
        var leftBoot = bones.get(Bone.LEFT_BOOT);
        var rightBoot = bones.get(Bone.RIGHT_BOOT);
        float walk = state.walkAnimationSpeed * state.walkWeight;
        float step = state.walkAnimationPos * .63F;
        hips.y -= Math.abs(Mth.sin(step)) * walk * 2;
        helm.xRot += Mth.clamp(state.xRot * Mth.DEG_TO_RAD, -.18F, .24F) * state.walkWeight;
        cape.xRot += walk * .22F + Mth.sin(state.ageInTicks * .12F) * .025F;
        blade.zRot += WALK_BLADE_ROLL * state.walkWeight;
        blade.x += WALK_BLADE_SHIFT * state.walkWeight;
        stride(leftBoot, step, walk);
        stride(rightBoot, step + Mth.PI, walk);
        BossModelAnimator.apply(state.pose, bones);

        var grip = new Quaternionf().rotationZYX(blade.zRot, blade.yRot, blade.xRot);
        rightArm.reach(position(rightArm.upper), hand(grip, RIGHT_HAND), new Vec3(-1, 1, .25));
        leftArm.reach(position(leftArm.upper), hand(grip, LEFT_HAND), new Vec3(1, 1, .25));
        rightLeg.reach(position(hips).add(-6, 1, 0), position(rightBoot).add(0, -4, 0), KNEE);
        leftLeg.reach(position(hips).add(6, 1, 0), position(leftBoot).add(0, -4, 0), KNEE);
    }

    private static void stride(ModelPart boot, float step, float weight) {
        boot.z += Mth.cos(step) * weight * 6.5F;
        boot.y -= Math.max(0, Mth.sin(step)) * weight * 4F;
    }

    private Vec3 hand(Quaternionf grip, Vec3 offset) {
        var point = grip.transform(new Vector3f((float) offset.x, (float) offset.y, (float) offset.z));
        return position(blade).add(point.x, point.y, point.z);
    }

    private static Vec3 position(ModelPart part) {
        return new Vec3(part.x, part.y, part.z);
    }

    // Solve toward the fixed grips and soles after the clip has moved the chest and hips.
    private record Limb(ModelPart upper, ModelPart lower, float upperLength, float lowerLength) {
        Limb(ModelPart parent, String upper, String lower, float upperLength, float lowerLength) {
            this(parent.getChild(upper), parent.getChild(lower), upperLength, lowerLength);
        }

        void reach(Vec3 start, Vec3 end, Vec3 bend) {
            Vec3 offset = end.subtract(start);
            double distance = offset.length();
            Vec3 axis = offset.scale(1 / distance);
            Vec3 outward = bend.subtract(axis.scale(bend.dot(axis))).normalize();
            double span = Math.min(distance, upperLength + lowerLength - .001);
            double along = (span * span + upperLength * upperLength - lowerLength * lowerLength) / (2 * span);
            double height = Math.sqrt(Math.max(0, upperLength * upperLength - along * along));
            Vec3 joint = start.add(axis.scale(along)).add(outward.scale(height));
            aim(upper, start, joint, upperLength);
            aim(lower, joint, end, lowerLength);
        }

        private static void aim(ModelPart part, Vec3 start, Vec3 end, float length) {
            Vec3 direction = end.subtract(start);
            part.setPos((float) start.x, (float) start.y, (float) start.z);
            part.xRot = (float) Mth.atan2(direction.z, Math.hypot(direction.x, direction.y));
            part.zRot = (float) Mth.atan2(-direction.x, direction.y);
            part.yScale = (float) direction.length() / length;
        }
    }
}
