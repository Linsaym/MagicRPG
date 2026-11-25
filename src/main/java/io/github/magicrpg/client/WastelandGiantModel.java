package io.github.magicrpg.client;

import io.github.magicrpg.MagicRpg;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

final class WastelandGiantModel extends EntityModel<WastelandGiantModel.State> {
    static final ModelLayerLocation TITAN = new ModelLayerLocation(MagicRpg.id("flesh_titan"), "main");

    static final class State extends LivingEntityRenderState {
        float swing;
        float sweepTime;
    }

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    WastelandGiantModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        body = root.getChild("body");
        leftArm = root.getChild("left_arm");
        rightArm = root.getChild("right_arm");
        leftLeg = root.getChild("left_leg");
        rightLeg = root.getChild("right_leg");
    }

    static LayerDefinition createLayer() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        var torso = CubeListBuilder.create().texOffs(0, 0).addBox(-7, -2, -4, 14, 14, 8)
            .texOffs(0, 24).addBox(-5, 12, -3, 10, 3, 6)
            .texOffs(0, 36).addBox(-4, 1, -6, 10, 7, 2)
            .texOffs(0, 86).addBox(-7, -5, 1, 10, 11, 5)
            .texOffs(32, 86).addBox(-5, 11, -4, 9, 5, 3)
            .texOffs(62, 86).addBox(3, 4, -5, 4, 6, 2);
        root.addOrReplaceChild("body", torso, PartPose.offset(0, -3, 0));
        var skull = CubeListBuilder.create().texOffs(48, 0).addBox(-4, -8, -4, 8, 8, 8)
            .texOffs(48, 18).addBox(-4, -3, -6, 7, 3, 3)
            .texOffs(75, 0).addBox(-4, -7, -5, 7, 2, 2)
            .texOffs(96, 0).addBox(2, -6, -4, 3, 5, 4);
        root.addOrReplaceChild("head", skull, PartPose.offset(1, -3, -3));

        for (int side : new int[]{-1, 1}) {
            var arm = CubeListBuilder.create().texOffs(0, 48).addBox(-2.5F, -2, -3, 5, 13, 6);
            if (side < 0) {
                arm.texOffs(24, 48).addBox(-3.5F, 7, -3.5F, 7, 8, 7)
                    .texOffs(0, 70).addBox(-4, -5, -4, 8, 5, 8)
                    .texOffs(82, 48).addBox(-4, 2, -4, 3, 7, 5);
            } else {
                arm.texOffs(100, 48).addBox(-3, 7, -3, 6, 5, 6);
            }
            root.addOrReplaceChild(side < 0 ? "right_arm" : "left_arm", arm, PartPose.offset(side * 8, -1, 0));
            var leg = CubeListBuilder.create().texOffs(54, 48).addBox(-2.5F, 0, -3, 5, 10, 6)
                .texOffs(54, 68).addBox(-3, 9, -4, 6, 3, 8);
            if (side < 0) {
                leg.texOffs(86, 66).addBox(-3, 3, -4, 6, 4, 2);
            }
            root.addOrReplaceChild(side < 0 ? "right_leg" : "left_leg", leg, PartPose.offset(side * 3, 12, 0));
        }
        return LayerDefinition.create(mesh, 128, 128)
            .apply(MeshTransformer.scaling(5F * 16 / 35));
    }

    @Override
    public void setupAnim(State state) {
        super.setupAnim(state);
        body.xRot = .15F;
        head.xRot = state.xRot * Mth.DEG_TO_RAD + .20F;
        head.yRot = state.yRot * Mth.DEG_TO_RAD * .6F;
        float stride = Mth.cos(state.walkAnimationPos * .58F) * state.walkAnimationSpeed;
        float strike = Mth.sin(state.swing * Mth.PI);
        leftLeg.xRot = .7F * stride;
        rightLeg.xRot = -leftLeg.xRot;
        rightArm.xRot = .55F * stride - strike * 1.5F - .36F;
        leftArm.xRot = -.55F * stride - strike * .35F;
        rightArm.zRot = .12F;
        leftArm.zRot = -.12F;
    }
}
