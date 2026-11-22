package io.github.magicrpg.client;

import io.github.magicrpg.MagicRpg;
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

final class ChalkGuardianModel extends EntityModel<LivingEntityRenderState> {
    static final ModelLayerLocation LAYER = new ModelLayerLocation(MagicRpg.id("chalk_guardian"), "main");

    private final ModelPart head;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    ChalkGuardianModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        leftArm = root.getChild("left_arm");
        rightArm = root.getChild("right_arm");
        leftLeg = root.getChild("left_leg");
        rightLeg = root.getChild("right_leg");
    }

    static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-4.5F, -6, -3, 9, 8, 6)
            .texOffs(32, 0).addBox(-3.5F, 2, -2.5F, 7, 2, 5), PartPose.offset(0, 14, 0));
        root.addOrReplaceChild("head", CubeListBuilder.create()
            .texOffs(0, 16).addBox(-3.5F, -5, -3.5F, 7, 6, 6), PartPose.offset(0, 10, -1));
        addArm(root, "right_arm", -5.1F, 0, 31, 0.12F);
        addArm(root, "left_arm", 5.1F, 19, 31, -0.12F);
        addLeg(root, "right_leg", -2.4F, 30, 17);
        addLeg(root, "left_leg", 2.4F, 48, 17);
        return LayerDefinition.create(mesh, 64, 48);
    }

    private static void addArm(PartDefinition root, String name, float x, int u, int v, float angle) {
        root.addOrReplaceChild(name, CubeListBuilder.create()
            .texOffs(u, v).addBox(-2, -1, -2, 4, 9, 4), PartPose.offsetAndRotation(x, 10, 0, 0, 0, angle));
    }

    private static void addLeg(PartDefinition root, String name, float x, int u, int v) {
        root.addOrReplaceChild(name, CubeListBuilder.create()
            .texOffs(u, v).addBox(-2, 0, -2.5F, 4, 6, 4), PartPose.offset(x, 18, 0));
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        head.yRot = Mth.clamp(state.yRot, -45, 45) * Mth.DEG_TO_RAD;
        head.xRot = Mth.clamp(state.xRot, -25, 25) * Mth.DEG_TO_RAD;
        float stride = Mth.cos(state.walkAnimationPos * 0.72F) * state.walkAnimationSpeed;
        leftLeg.xRot = rightArm.xRot = stride * 0.65F;
        rightLeg.xRot = leftArm.xRot = -stride * 0.65F;
    }
}
