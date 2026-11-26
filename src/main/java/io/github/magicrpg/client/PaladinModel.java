package io.github.magicrpg.client;

import io.github.magicrpg.MagicRpg;
import io.github.magicrpg.boss.animation.BoneTransform;
import io.github.magicrpg.client.animation.BossModelAnimator;
import java.util.Map;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
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
    }

    private static final int IVORY = 0, SHALE = 1, BRONZE = 2, MAIL = 3;
    private static final int CLOTH = 4, EDGE = 5, FULLER = 6, LEATHER = 7;
    private static final int EMBER = 8, AMETHYST = 9;
    private static final float GRIP_HEIGHT = 7, GRIP_DEPTH = -14, GRIP_PITCH = -2.28F;
    private static final Vec3 RIGHT_HAND = new Vec3(0, 3, 0);
    private static final Vec3 LEFT_HAND = new Vec3(0, -4, 0);
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
            Bone.CAPE, cape, Bone.HEM, cape.getChild("hem"), Bone.CORE, chest.getChild("core"),
            Bone.LEFT_BOOT, leftBoot, Bone.RIGHT_BOOT, rightBoot);
        rightArm = new Limb(chest, "right_upper_arm", "right_forearm", 15, 15);
        leftArm = new Limb(chest, "left_upper_arm", "left_forearm", 15, 15);
        rightLeg = new Limb(root, "right_thigh", "right_shin", 17, 18);
        leftLeg = new Limb(root, "left_thigh", "left_shin", 17, 18);
    }

    private static CubeListBuilder box(CubeListBuilder cubes, int material, float x, float y, float z,
                                       float width, float height, float depth) {
        return cubes.texOffs(material % 4 * 128, material / 4 * 128).addBox(x, y, z, width, height, depth);
    }

    private static CubeListBuilder box(int material, float x, float y, float z,
                                       float width, float height, float depth) {
        return box(CubeListBuilder.create(), material, x, y, z, width, height, depth);
    }

    static LayerDefinition createLayer() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        var waist = box(MAIL, -10, -6, -5, 20, 9, 10);
        box(waist, BRONZE, -11, -3, -6, 22, 3, 12);
        box(waist, AMETHYST, -3, -3.5F, -7, 6, 4, 1);
        box(waist, CLOTH, -5, 0, -6, 10, 14, 2);
        box(waist, BRONZE, -5, 13, -6.2F, 10, 2, 2.4F);
        var hips = root.addOrReplaceChild("hips", waist, PartPose.offset(0, -12, 0));

        var cuirass = box(SHALE, -10, -6, -5, 20, 26, 11);
        box(cuirass, IVORY, -12, -8, -6, 24, 7, 13);
        box(cuirass, IVORY, -11, -1, -6.5F, 22, 8, 13);
        box(cuirass, BRONZE, -10, 7, -7, 20, 2, 13.5F);
        box(cuirass, IVORY, -9, 10, -6, 18, 5, 12);
        box(cuirass, IVORY, -8, 16, -5.75F, 16, 4, 11);
        box(cuirass, SHALE, -5, -4, -7.5F, 10, 10, 2);
        box(cuirass, BRONZE, -4, -3, -8.4F, 8, 8, 2);
        var chest = hips.addOrReplaceChild("chest", cuirass, PartPose.offset(0, -21, 0));
        var core = box(AMETHYST, -2.8F, -2.8F, -1, 5.6F, 5.6F, 1.5F);
        box(core, EMBER, -1.5F, -1.5F, -1.4F, 3, 3, .6F);
        chest.addOrReplaceChild("core", core, PartPose.offsetAndRotation(0, 1, -8.2F, 0, 0, Mth.PI / 4));

        var helm = box(MAIL, -5, -12, -5, 10, 13, 10);
        box(helm, IVORY, -6.5F, -15, -5.8F, 13, 5, 12);
        box(helm, SHALE, -6, -10, 2, 12, 12, 4.5F);
        box(helm, BRONZE, -6.5F, -10.5F, -7, 13, 2, 2);
        box(helm, EMBER, -4, -7.5F, -5.9F, 8, 1.2F, 1);
        box(helm, IVORY, -5.5F, -5.5F, -6.2F, 11, 7, 3);
        box(helm, BRONZE, -1.4F, -9, -7.5F, 2.8F, 10, 2);
        for (int side : new int[]{-1, 1}) {
            box(helm, IVORY, side * 5.5F - 1.3F, -9, -5.5F, 2.6F, 11, 10);
            box(helm, BRONZE, side * 4.3F - 1.1F, -18, -3.5F, 2.2F, 5, 7);
        }
        box(helm, AMETHYST, -1.25F, -20, -2, 2.5F, 6, 5);
        chest.addOrReplaceChild("helm", helm, PartPose.offset(0, -9, 0));

        for (int side : new int[]{-1, 1}) {
            addLimbs(root, chest, side, side < 0 ? "right" : "left");
        }
        addCape(chest);
        addBlade(chest);
        return LayerDefinition.create(mesh, 512, 384);
    }

    private static void addLimbs(PartDefinition root,
                                 PartDefinition chest,
                                 int side, String name) {
        var shoulder = box(SHALE, -6.5F, -4, -5.5F, 13, 10, 11);
        box(shoulder, IVORY, -7, -7, -6.5F, 14, 6, 13);
        box(shoulder, BRONZE, -6, -1, -6.8F, 12, 2, 13.6F);
        box(shoulder, IVORY, side * 4 - 4.5F, 2, -6, 9, 3, 12);
        box(shoulder, side < 0 ? IVORY : AMETHYST, side * 2 - 3, -10, -4, 6, 4, 9);
        chest.addOrReplaceChild(name + "_pauldron", shoulder,
            PartPose.offsetAndRotation(side * 14, -4, 0, 0, 0, side * -.15F));

        var upper = box(MAIL, -3.4F, 0, -3.5F, 6.8F, 15, 7);
        box(upper, SHALE, -3.7F, 5, -4, 7.4F, 6, 8);
        box(upper, BRONZE, -3.6F, 12, -4.1F, 7.2F, 3, 8.2F);
        chest.addOrReplaceChild(name + "_upper_arm", upper, PartPose.offset(side * 13, -4, 0));
        var forearm = box(MAIL, -3, -2, -3, 6, 16, 6);
        box(forearm, IVORY, -4, 2, -4, 8, 8, 8);
        box(forearm, BRONZE, -3.4F, 10, -3.6F, 6.8F, 2, 7.2F);
        chest.addOrReplaceChild(name + "_forearm", forearm, PartPose.ZERO);

        var thigh = box(MAIL, -4.2F, 0, -4, 8.4F, 17, 8);
        box(thigh, SHALE, -4.4F, 0, -5.5F, 8.8F, 8, 5);
        box(thigh, IVORY, -4.45F, 6, -5.7F, 8.9F, 5, 2);
        root.addOrReplaceChild(name + "_thigh", thigh, PartPose.ZERO);
        var shin = box(SHALE, -4, -2, -3.8F, 8, 19, 7.6F);
        box(shin, BRONZE, -4.4F, -3, -5, 8.8F, 5, 6);
        box(shin, IVORY, -4.5F, 2, -5.1F, 9, 10, 3.5F);
        box(shin, IVORY, -3.6F, 12, -4.5F, 7.2F, 4, 2.5F);
        root.addOrReplaceChild(name + "_shin", shin, PartPose.ZERO);
        var boot = box(SHALE, -4.5F, -4, -6.8F, 9, 4, 11);
        box(boot, IVORY, -4, -4.5F, -7.2F, 8, 2.5F, 7);
        box(boot, BRONZE, -4.8F, -1.5F, -7.5F, 9.6F, 1.5F, 12);
        root.addOrReplaceChild(name + "_boot", boot,
            PartPose.offsetAndRotation(side * 7.5F, 24, side * 2.5F, 0, side * -.12F, 0));
    }

    private static void addCape(PartDefinition chest) {
        var cloth = box(CLOTH, -12, 0, 0, 24, 17, 1.4F);
        box(cloth, BRONZE, -11.5F, 1, 1.4F, 1, 16, .3F);
        box(cloth, BRONZE, 10.5F, 1, 1.4F, 1, 16, .3F);
        var cape = chest.addOrReplaceChild("cape", cloth, PartPose.offsetAndRotation(0, -6, 7.2F, .20F, 0, 0));
        var hem = box(CLOTH, -12, 0, 0, 24, 15, 1.4F);
        box(hem, CLOTH, -11, 15, 0, 8, 7, 1.4F);
        box(hem, CLOTH, 1, 15, 0, 10, 5, 1.4F);
        box(hem, BRONZE, -11.5F, 0, 1.4F, 1, 13, .3F);
        box(hem, BRONZE, 10.5F, 0, 1.4F, 1, 13, .3F);
        cape.addOrReplaceChild("hem", hem, PartPose.offset(0, 17, 0));
    }

    private static void addBlade(PartDefinition chest) {
        var sword = box(LEATHER, -1.5F, -9, -1.5F, 3, 17, 3);
        box(sword, BRONZE, -2.7F, -12, -2.4F, 5.4F, 3, 4.8F);
        box(sword, AMETHYST, -1.5F, -13, -1.5F, 3, 2, 3);
        box(sword, BRONZE, -8, 6, -2, 16, 3, 4);
        box(sword, SHALE, -8, 9, -1.6F, 2.7F, 3, 3.2F);
        box(sword, SHALE, 5.3F, 9, -1.6F, 2.7F, 3, 3.2F);
        box(sword, EDGE, -4.4F, 9, -1.05F, 8.8F, 8, 2.1F);
        box(sword, EDGE, -3.55F, 17, -1, 7.1F, 33, 2);
        box(sword, EDGE, -2.7F, 50, -.9F, 5.4F, 3, 1.8F);
        box(sword, EDGE, -1.5F, 53, -.75F, 3, 3, 1.5F);
        box(sword, FULLER, -2.25F, 11, -1.34F, 4.5F, 39, 2.68F);
        box(sword, AMETHYST, -.7F, 17, -1.52F, 1.4F, 25, 3.04F);
        box(sword, EMBER, -.55F, 11, -1.55F, 1.1F, 5, 3.1F);
        var blade = chest.addOrReplaceChild("blade", sword,
            PartPose.offsetAndRotation(0, GRIP_HEIGHT, GRIP_DEPTH, GRIP_PITCH, 0, 0));
        blade.addOrReplaceChild("right_gauntlet", box(IVORY, -3, -2.7F, -2.9F, 6, 5.4F, 5.8F),
            PartPose.offset(0, (float) RIGHT_HAND.y, 0));
        blade.addOrReplaceChild("left_gauntlet", box(IVORY, -2.9F, -2.7F, -2.8F, 5.8F, 5.4F, 5.6F),
            PartPose.offset(0, (float) LEFT_HAND.y, 0));
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
        blade.zRot += .23F * state.walkWeight;
        blade.x += 2.5F * state.walkWeight;
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
