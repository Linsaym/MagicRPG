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

final class StarHareModel extends EntityModel<LivingEntityRenderState> {
    static final ModelLayerLocation LAYER = new ModelLayerLocation(MagicRpg.id("star_hare"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart forelegs;
    private final ModelPart hindlegs;

    StarHareModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        head = body.getChild("head");
        leftEar = head.getChild("left_ear");
        rightEar = head.getChild("right_ear");
        forelegs = root.getChild("forelegs");
        hindlegs = root.getChild("hindlegs");
    }

    static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-3.5F, -3, -4, 7, 6, 9)
            .texOffs(32, 0).addBox(-1.5F, -2, 4.5F, 3, 3, 3), PartPose.offset(0, 19, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
            .texOffs(0, 18).addBox(-2.5F, -3, -3, 5, 5, 5)
            .texOffs(21, 18).addBox(-1.5F, 0, -4, 3, 2, 2), PartPose.offset(0, -2, -4));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create()
            .texOffs(33, 17).addBox(-1, -8, -1, 2, 8, 2), PartPose.offsetAndRotation(1.55F, -2, 0, -0.22F, 0, 0.15F));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create()
            .texOffs(43, 17).addBox(-1, -7, -1, 2, 7, 2), PartPose.offsetAndRotation(-1.55F, -2, 0, -0.12F, 0, -0.17F));
        root.addOrReplaceChild("forelegs", CubeListBuilder.create()
            .texOffs(0, 32).addBox(-2.5F, 0, -1.5F, 2, 4, 3)
            .texOffs(12, 32).addBox(0.5F, 0, -1.5F, 2, 4, 3), PartPose.offset(0, 20, -3));
        root.addOrReplaceChild("hindlegs", CubeListBuilder.create()
            .texOffs(23, 32).addBox(-4, -1, -2, 3, 4, 5)
            .texOffs(40, 32).addBox(1, -1, -2, 3, 4, 5), PartPose.offset(0, 21, 3));
        return LayerDefinition.create(mesh, 64, 48);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float stride = Mth.sin(state.walkAnimationPos * 1.25F) * state.walkAnimationSpeed;
        head.yRot = Mth.clamp(state.yRot, -35, 35) * Mth.DEG_TO_RAD;
        head.xRot = Mth.clamp(state.xRot, -25, 25) * Mth.DEG_TO_RAD;
        body.xRot = stride * 0.18F;
        forelegs.xRot = stride * 0.65F;
        hindlegs.xRot = -stride * 0.85F;
        leftEar.xRot += stride * 0.23F + Mth.sin(state.ageInTicks * 0.12F) * 0.035F;
        rightEar.xRot += stride * 0.23F;
    }
}
