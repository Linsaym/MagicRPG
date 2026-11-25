package io.github.magicrpg.client;

import io.github.magicrpg.MagicRpg;
import io.github.magicrpg.entity.WastelandGiant;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

final class IronColossusModel extends EntityModel<WastelandGiantModel.State> {
    static final ModelLayerLocation LAYER = new ModelLayerLocation(MagicRpg.id("iron_colossus"), "main");

    // Angles are in degrees. The shoulder turns across the chest as the elbow straightens.
    private static final SweepPose REST = new SweepPose(0, 0, 0, 0, -3, 0, 0);
    private static final SweepPose WINDUP = new SweepPose(30, 8, 17, 27, 37, -27, -13);
    private static final SweepPose IMPACT = new SweepPose(-28, 27, -23, -37, -15, -8, 15);
    private static final SweepPose FOLLOW_THROUGH = new SweepPose(-49, 20, -13, -49, -30, -24, 23);
    private static final int WINDUP_END = 8;
    private static final int FOLLOW_THROUGH_END = 15;

    private final ModelPart chest;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart rightForearm;
    private final ModelPart leftArm;
    private final ModelPart leftForearm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    IronColossusModel(ModelPart root) {
        super(root);
        chest = root.getChild("chest");
        head = chest.getChild("head");
        rightArm = chest.getChild("right_arm");
        rightForearm = rightArm.getChild("forearm");
        leftArm = chest.getChild("left_arm");
        leftForearm = leftArm.getChild("forearm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
    }

    static LayerDefinition createLayer() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        var chest = root.addOrReplaceChild("chest", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-7, -16, -4, 14, 11, 8)
            .texOffs(46, 0).addBox(-6, -15, -5, 12, 8, 1)
            .texOffs(0, 21).addBox(-4, -5, -3, 8, 6, 6)
            .texOffs(30, 21).addBox(-5, 0, -4, 10, 3, 8)
            .texOffs(0, 109).addBox(-5, -6, -4.5F, 3, 4, 1), PartPose.offset(0, 6, 0));
        chest.addOrReplaceChild("head", CubeListBuilder.create()
            .texOffs(74, 0).addBox(-3, -8, -3, 6, 8, 6)
            .texOffs(100, 0).addBox(-3, -8, -4, 6, 3, 1)
            .texOffs(100, 6).addBox(-1, -5, -6, 2, 5, 3), PartPose.offset(0, -16, -.5F));

        var rightArm = addArm(chest, "right_arm", -9);
        addChain(rightArm);
        rightArm.addOrReplaceChild("moss", CubeListBuilder.create()
            .texOffs(0, 109).addBox(-2, 2, -4.6F, 3, 4, 1)
            .texOffs(10, 109).addBox(-2, 6, -4.6F, 1, 3, 1), PartPose.ZERO);
        addArm(chest, "left_arm", 9);
        addLeg(root, "right_leg", -3.5F);
        var leftLeg = addLeg(root, "left_leg", 3.5F);
        leftLeg.addOrReplaceChild("moss", CubeListBuilder.create()
            .texOffs(0, 109).addBox(0, 12, -4.1F, 3, 4, 1), PartPose.ZERO);

        return LayerDefinition.create(mesh, 128, 128).apply(MeshTransformer.scaling(8F * 16 / 42));
    }

    private static PartDefinition addArm(PartDefinition chest, String name, float x) {
        var arm = chest.addOrReplaceChild(name, CubeListBuilder.create()
            .texOffs(x < 0 ? 76 : 0, 37).addBox(-3, -2, -4.5F, 6, 7, 9)
            .texOffs(32, 37).addBox(-2, 5, -2.5F, 4, 7, 5)
            .texOffs(52, 37).addBox(-1.5F, 11, -2, 3, 3, 4), PartPose.offset(x, -14, 0));
        arm.addOrReplaceChild("forearm", CubeListBuilder.create()
            .texOffs(0, 54).addBox(-2.5F, 1, -3, 5, 9, 6)
            .texOffs(24, 54).addBox(-3, 7, -3.5F, 6, 4, 7)
            .texOffs(52, 54).addBox(-2.5F, 10, -2.5F, 5, 3, 5)
            .texOffs(76, 54).addBox(-2.5F, 12, -2.5F, 2, 3, 3)
            .texOffs(76, 54).addBox(.5F, 12, -2.5F, 2, 3, 3), PartPose.offset(0, 12, 0));
        return arm;
    }

    private static PartDefinition addLeg(PartDefinition root, String name, float x) {
        return root.addOrReplaceChild(name, CubeListBuilder.create()
            .texOffs(0, 73).addBox(-2.5F, 0, -2.5F, 5, 7, 5)
            .texOffs(22, 73).addBox(-1.5F, 6, -2, 3, 3, 4)
            .texOffs(39, 73).addBox(-2.5F, 8, -3, 5, 6, 6)
            .texOffs(63, 73).addBox(-2, 9, -3.5F, 4, 4, 1)
            .texOffs(0, 90).addBox(-3, 13, -4, 6, 3, 7), PartPose.offset(x, 8, 0));
    }

    private static void addChain(PartDefinition arm) {
        addLink(arm, "front_top", -.5F, -1, -4.6F, Mth.HALF_PI, 0);
        addLink(arm, "crown", -.5F, -2.8F, -2, Mth.HALF_PI, Mth.HALF_PI);
        addLink(arm, "back_top", -.5F, -2.8F, .6F, Mth.HALF_PI, 0);
        addLink(arm, "back", -.5F, -1.5F, 3.5F, .72F, Mth.HALF_PI);
        addLink(arm, "front_drop", -.5F, 1, -4.8F, .15F, Mth.HALF_PI);
        addLink(arm, "front_middle", -.5F, 3.7F, -5.1F, 0, 0);
        addLink(arm, "front_end", -.5F, 6.2F, -5.1F, 0, Mth.HALF_PI);
    }

    private static void addLink(PartDefinition arm, String name, float x, float y, float z, float pitch, float yaw) {
        arm.addOrReplaceChild(name, CubeListBuilder.create()
            .texOffs(40, 93).addBox(-1.5F, -2, -.5F, 3, 1, 1)
            .texOffs(40, 93).addBox(-1.5F, 1, -.5F, 3, 1, 1)
            .texOffs(50, 93).addBox(-1.5F, -1, -.5F, 1, 2, 1)
            .texOffs(50, 93).addBox(.5F, -1, -.5F, 1, 2, 1), PartPose.offsetAndRotation(x, y, z, pitch, yaw, 0));
    }

    @Override
    public void setupAnim(WastelandGiantModel.State state) {
        super.setupAnim(state);
        float walk = Mth.cos(state.walkAnimationPos * .52F) * state.walkAnimationSpeed;
        float activity = 1 - Mth.clamp(Math.min(state.sweepTime, WastelandGiant.SWEEP_DURATION - state.sweepTime) / 4,
            0, 1);
        rightLeg.xRot = walk * .62F * activity;
        leftLeg.xRot = -rightLeg.xRot;
        rightArm.xRot = -walk * .45F * activity;
        leftArm.xRot = -rightArm.xRot;
        leftArm.zRot = -.06F;
        leftForearm.xRot = -.09F;

        var pose = SweepPose.at(state.sweepTime);
        chest.yRot = pose.twist * Mth.DEG_TO_RAD;
        chest.xRot = pose.lean * Mth.DEG_TO_RAD;
        head.xRot = Mth.clamp(state.xRot * .65F - pose.lean * .55F, -25, 34) * Mth.DEG_TO_RAD;
        head.yRot = Mth.clamp(state.yRot * .65F - pose.twist * .8F, -44, 44) * Mth.DEG_TO_RAD;
        rightArm.xRot += pose.pitch * Mth.DEG_TO_RAD;
        rightArm.yRot = pose.yaw * Mth.DEG_TO_RAD;
        rightArm.zRot = pose.roll * Mth.DEG_TO_RAD;
        rightForearm.xRot = pose.elbow * Mth.DEG_TO_RAD;
        leftArm.xRot += pose.brace * Mth.DEG_TO_RAD;
    }

    private record SweepPose(float twist, float lean, float pitch, float yaw, float roll, float elbow, float brace) {
        private static SweepPose at(float time) {
            if (time <= 0 || time >= WastelandGiant.SWEEP_DURATION) {
                return REST;
            }
            if (time <= WINDUP_END) {
                return REST.towards(WINDUP, time / WINDUP_END);
            }
            if (time <= WastelandGiant.SWEEP_IMPACT_TICK) {
                return WINDUP.towards(IMPACT, (time - WINDUP_END) / (WastelandGiant.SWEEP_IMPACT_TICK - WINDUP_END));
            }
            if (time <= FOLLOW_THROUGH_END) {
                return IMPACT.towards(FOLLOW_THROUGH, (time - WastelandGiant.SWEEP_IMPACT_TICK)
                    / (FOLLOW_THROUGH_END - WastelandGiant.SWEEP_IMPACT_TICK));
            }
            return FOLLOW_THROUGH.towards(REST, (time - FOLLOW_THROUGH_END)
                / (WastelandGiant.SWEEP_DURATION - FOLLOW_THROUGH_END));
        }

        private SweepPose towards(SweepPose next, float progress) {
            float eased = progress * progress * (3 - 2 * progress);
            return new SweepPose(Mth.lerp(eased, twist, next.twist), Mth.lerp(eased, lean, next.lean),
                Mth.lerp(eased, pitch, next.pitch), Mth.lerp(eased, yaw, next.yaw), Mth.lerp(eased, roll, next.roll),
                Mth.lerp(eased, elbow, next.elbow), Mth.lerp(eased, brace, next.brace));
        }
    }
}
