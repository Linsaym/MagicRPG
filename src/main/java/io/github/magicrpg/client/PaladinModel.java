package io.github.magicrpg.client;

import io.github.magicrpg.MagicRpg;
import io.github.magicrpg.entity.MeadowPaladin;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

final class PaladinModel extends EntityModel<PaladinModel.State> {
    static final ModelLayerLocation LAYER = new ModelLayerLocation(MagicRpg.id("meadow_paladin"), "main");
    static final int LIGHT = 0, DARK = 1, CAPE = 2, SKIN = 3, SHADOW = 4, STEEL = 5, TRIM = 6, LINEN = 7;
    static final int RUNE = 8;
    private static final float ARM_LENGTH = 24;
    private final ModelPart torso;
    private final ModelPart head;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart cape;
    private final ModelPart sword;

    static final class State extends LivingEntityRenderState {
        int phase;
        float phaseTime;
    }

    PaladinModel(ModelPart root) {
        super(root);
        torso = root.getChild("torso");
        head = torso.getChild("head");
        leftArm = torso.getChild("left_arm");
        rightArm = torso.getChild("right_arm");
        leftLeg = root.getChild("left_leg");
        rightLeg = root.getChild("right_leg");
        cape = torso.getChild("cape");
        sword = torso.getChild("sword");
    }

    static CubeListBuilder cube(CubeListBuilder cubes, int material, float x, float y, float z,
                                float width, float height, float depth) {
        return cubes.texOffs(material * 128, 0).addBox(x, y, z, width, height, depth);
    }

    static LayerDefinition createLayer() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        var chest = cube(CubeListBuilder.create(), LIGHT, -13, -8, -7, 26, 20, 14);
        cube(chest, DARK, -10, 12, -6, 20, 12, 12);
        cube(chest, TRIM, -12, 14, -8, 24, 4, 16);
        cube(chest, DARK, -7, -6, -8, 14, 14, 2);
        cube(chest, TRIM, -6, -5, -10, 12, 3, 2);
        cube(chest, RUNE, -3, -2, -10, 6, 7, 2);
        var torso = root.addOrReplaceChild("torso", chest, PartPose.offset(0, -32, 0));

        var helmet = cube(CubeListBuilder.create(), SHADOW, -5, -11, -6, 10, 12, 11);
        cube(helmet, LIGHT, -7, -15, -7, 14, 5, 13);
        cube(helmet, DARK, -7, -12, 4, 14, 13, 3);
        cube(helmet, LIGHT, -7, -10, -5, 2, 13, 10);
        cube(helmet, LIGHT, 5, -10, -5, 2, 13, 10);
        cube(helmet, TRIM, -6, -11, -9, 12, 3, 3);
        cube(helmet, RUNE, -4, -6, -7, 8, 1, 1);
        cube(helmet, DARK, -1, -5, -7, 2, 3, 2);
        cube(helmet, TRIM, -2, -19, -3, 4, 5, 6);
        cube(helmet, RUNE, -1, -22, -2, 2, 4, 4);
        cube(helmet, DARK, -2, -1, -6.4F, 4, 1, .5F);
        torso.addOrReplaceChild("head", helmet, PartPose.offset(0, -9, 0));

        for (int side : new int[]{-1, 1}) {
            var arm = cube(CubeListBuilder.create(), DARK, -4, 0, -4, 8, 15, 8);
            cube(arm, LIGHT, -4, 11, -4, 8, 11, 8);
            cube(arm, TRIM, -4, 19, -4, 8, 3, 8);
            torso.addOrReplaceChild(side < 0 ? "right_arm" : "left_arm", arm,
                PartPose.offset(side * 15, -4, 0));
            var pauldron = cube(CubeListBuilder.create(), LIGHT, -6, -5, -6, 12, 9, 12);
            cube(pauldron, TRIM, -5, 3, -6.5F, 10, 2, 13);
            cube(pauldron, DARK, -6, -8, -5, 12, 4, 10);
            cube(pauldron, RUNE, -3, -9, -4, 6, 2, 8);
            torso.addOrReplaceChild(side < 0 ? "right_shoulder" : "left_shoulder", pauldron,
                PartPose.offset(side * 16, -5, 0));

            var leg = cube(CubeListBuilder.create(), DARK, -5, 0, -4, 10, 30, 8);
            cube(leg, LIGHT, -5, 14, -5, 10, 20, 9);
            cube(leg, TRIM, -4, 13, -6, 8, 5, 2);
            cube(leg, DARK, -5, 29, -8, 10, 6, 13);
            root.addOrReplaceChild(side < 0 ? "right_leg" : "left_leg", leg,
                PartPose.offset(side * 6, -11, 0));
        }

        var cloth = cube(CubeListBuilder.create(), CAPE, -14, 0, 0, 28, 34, 2);
        cube(cloth, CAPE, -14, 34, 0, 5, 5, 2);
        cube(cloth, CAPE, -6, 34, 0, 8, 8, 2);
        cube(cloth, CAPE, 5, 34, 0, 9, 6, 2);
        cube(cloth, DARK, -12, 3, 2, 1, 30, 1);
        cube(cloth, DARK, 11, 3, 2, 1, 30, 1);
        torso.addOrReplaceChild("cape", cloth, PartPose.offset(0, -6, 7));

        var weapon = cube(CubeListBuilder.create(), DARK, -1, -10, -1, 2, 18, 2);
        cube(weapon, TRIM, -2, -12, -2, 4, 3, 4);
        cube(weapon, TRIM, -7, 7, -2, 14, 3, 4);
        cube(weapon, STEEL, -2.5F, 10, -.75F, 5, 42, 1.5F);
        cube(weapon, LIGHT, -1.5F, 52, -.75F, 3, 4, 1.5F);
        cube(weapon, RUNE, -.5F, 12, -1, 1, 38, 2);
        var grip = torso.addOrReplaceChild("sword", weapon, PartPose.ZERO);
        grip.addOrReplaceChild("upper_hand",
            cube(CubeListBuilder.create(), DARK, -2.5F, -2.5F, -2.5F, 5, 5, 5), PartPose.offset(0, -6, 0));
        grip.addOrReplaceChild("lower_hand",
            cube(CubeListBuilder.create(), DARK, -2.5F, -2.5F, -2.5F, 5, 5, 5), PartPose.offset(0, 3, 0));
        return LayerDefinition.create(mesh, 1152, 128);
    }

    private record Pose(float twist, float lean, float x, float y, float z, float blade) {
        Pose to(Pose end, float progress) {
            float t = Mth.clamp(progress, 0, 1);
            t = t * t * (3 - 2 * t);
            return new Pose(Mth.lerp(t, twist, end.twist), Mth.lerp(t, lean, end.lean),
                Mth.lerp(t, x, end.x), Mth.lerp(t, y, end.y), Mth.lerp(t, z, end.z), Mth.lerp(t, blade, end.blade));
        }
    }

    private static final Pose REST = new Pose(0, .04F, 0, -1, -17, 0);
    private static final Pose READY = new Pose(0, 0, 0, 11, -17, -1.13F);
    private static final Pose SIDE = new Pose(-1.43F, -.07F, 0, 9, -17, -1.57F);
    private static final Pose ACROSS = new Pose(1.24F, .09F, 0, 11, -17, -1.59F);
    private static final Pose RAISED = new Pose(0, -.13F, 0, -22, -7, -3.85F);
    private static final Pose EMBEDDED = new Pose(0, .13F, 0, 8, -23, -.74F);

    @Override
    public void setupAnim(State state) {
        super.setupAnim(state);
        float time = state.phaseTime;
        Pose pose = switch (state.phase) {
            case MeadowPaladin.SWEEP -> time < 20 ? READY.to(SIDE, time / 20)
                : SIDE.to(ACROSS, (time - 20) / (MeadowPaladin.SWEEP_WINDUP - 20));
            case MeadowPaladin.OVERHEAD -> time < 27 ? READY.to(RAISED, time / 23)
                : RAISED.to(EMBEDDED, (time - 27) / (MeadowPaladin.OVERHEAD_WINDUP - 27));
            case MeadowPaladin.RESONANCE -> time < 32 ? READY.to(RAISED, time / 18)
                : RAISED.to(EMBEDDED, (time - 32) / (MeadowPaladin.RESONANCE_WINDUP - 32));
            case MeadowPaladin.RECOVER -> ACROSS.to(READY, time / MeadowPaladin.RECOVERY_DURATION);
            case MeadowPaladin.STUCK -> time < 32 ? EMBEDDED
                : EMBEDDED.to(READY, (time - 32) / (MeadowPaladin.STUCK_DURATION - 32));
            case MeadowPaladin.ADVANCE -> READY;
            default -> REST;
        };
        torso.yRot = pose.twist;
        torso.xRot = pose.lean;
        float stride = Mth.cos(state.walkAnimationPos * .65F) * state.walkAnimationSpeed;
        leftLeg.xRot = stride * .5F;
        rightLeg.xRot = -leftLeg.xRot;
        head.yRot = -pose.twist * .46F;
        head.xRot = Mth.clamp(state.xRot * Mth.DEG_TO_RAD, -.24F, .22F) - pose.lean * .4F;
        cape.xRot = .12F + .12F * Math.abs(stride) + .035F * Mth.sin(state.ageInTicks * .1F);
        cape.yRot = -.22F * pose.twist;
        sword.x = pose.x;
        sword.y = pose.y;
        sword.z = pose.z;
        sword.xRot = pose.blade;
        reach(rightArm, pose, -6);
        reach(leftArm, pose, 3);
    }

    private static void reach(ModelPart arm, Pose pose, float gripOffset) {
        float dx = pose.x - arm.x;
        float dy = pose.y + gripOffset * Mth.cos(pose.blade) - arm.y;
        float dz = pose.z + gripOffset * Mth.sin(pose.blade) - arm.z;
        float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        arm.zRot = (float) Mth.atan2(-dx, dy);
        arm.xRot = (float) Mth.atan2(dz, Mth.sqrt(dx * dx + dy * dy));
        arm.yScale = length / ARM_LENGTH;
    }
}
