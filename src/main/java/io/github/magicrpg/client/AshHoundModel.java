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
import net.minecraft.util.Mth;

final class AshHoundModel extends EntityModel<AshHoundRenderer.State> {
    static final ModelLayerLocation LAYER = new ModelLayerLocation(MagicRpg.id("ash_hound"), "main");

    private final ModelPart torso;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart tail;
    private final ModelPart frontRight;
    private final ModelPart frontLeft;
    private final ModelPart backRight;
    private final ModelPart backLeft;

    AshHoundModel(ModelPart root) {
        super(root);
        torso = root.getChild("torso");
        head = torso.getChild("head");
        jaw = head.getChild("jaw");
        tail = torso.getChild("tail");
        frontRight = root.getChild("front_right");
        frontLeft = root.getChild("front_left");
        backRight = root.getChild("back_right");
        backLeft = root.getChild("back_left");
    }

    static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition torso = root.addOrReplaceChild("torso", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-4.5F, -5, -7, 9, 9, 10)
            .texOffs(38, 0).addBox(-3.5F, -3, 3, 7, 6, 7)
            .texOffs(68, 0).addBox(-1, -8, -5, 2, 3, 3)
            .texOffs(78, 0).addBox(-1, -7, -1, 2, 2, 3)
            .texOffs(88, 0).addBox(-1, -5, 3, 2, 2, 3), PartPose.offset(0, 13, 0));
        PartDefinition head = torso.addOrReplaceChild("head", CubeListBuilder.create()
            .texOffs(0, 21).addBox(-4, -4, -5, 8, 6, 6)
            .texOffs(30, 21).addBox(-3, -1, -9, 6, 3, 4)
            .texOffs(52, 21).addBox(-4, -8, -2, 2, 4, 2)
            .texOffs(62, 21).addBox(2, -8, -2, 2, 4, 2)
            .texOffs(92, 21).addBox(-2.8F, 1.7F, -8, 1, 2, 1)
            .texOffs(92, 21).addBox(1.8F, 1.7F, -8, 1, 2, 1), PartPose.offset(0, -3, -6));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(74, 21)
            .addBox(-2.5F, 0, -5, 5, 1, 4), PartPose.offset(0, 1.8F, -4));
        torso.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 40)
            .addBox(-1, -1, 0, 2, 2, 8).texOffs(22, 40).addBox(-1.5F, -1.5F, 6, 3, 3, 3),
            PartPose.offsetAndRotation(0, -2, 8, 0.32F, 0, 0));

        addLeg(root, "front_right", -3.7F, 14, -4, 34, 10);
        addLeg(root, "front_left", 3.7F, 14, -4, 52, 10);
        addLeg(root, "back_right", -3.0F, 15, 6, 70, 9);
        addLeg(root, "back_left", 3.0F, 15, 6, 88, 9);
        return LayerDefinition.create(mesh, 128, 64);
    }

    private static void addLeg(PartDefinition root, String name, float x, int y, int z, int u, int height) {
        root.addOrReplaceChild(name, CubeListBuilder.create()
            .texOffs(u, 39).addBox(-1.5F, 0, -1.5F, 3, height - 2, 3)
            .texOffs(u, 52).addBox(-1.5F, height - 2, -2.5F, 3, 2, 5), PartPose.offset(x, y, z));
    }

    @Override
    public void setupAnim(AshHoundRenderer.State state) {
        super.setupAnim(state);
        head.yRot = Mth.clamp(state.yRot, -35, 35) * Mth.DEG_TO_RAD;
        head.xRot = Mth.clamp(state.xRot, -25, 25) * Mth.DEG_TO_RAD;
        switch (state.phase) {
            case IDLE -> {
                float swing = Mth.cos(state.walkAnimationPos * 0.78F) * state.walkAnimationSpeed;
                frontRight.xRot = backLeft.xRot = swing;
                frontLeft.xRot = backRight.xRot = -swing;
                tail.yRot = Mth.sin(state.ageInTicks * 0.15F) * 0.15F;
            }
            case WINDUP -> {
                torso.y += 1.7F;
                torso.xRot = -0.18F;
                head.xRot -= 0.10F;
                jaw.xRot = 0.28F;
                frontRight.xRot = frontLeft.xRot = -0.24F;
                backRight.xRot = backLeft.xRot = 0.33F;
                tail.xRot = 0.65F;
            }
            case DASH -> {
                torso.xRot = 0.14F;
                jaw.xRot = 0.4F;
                frontRight.xRot = frontLeft.xRot = -0.65F;
                backRight.xRot = backLeft.xRot = 0.65F;
                tail.xRot = -0.15F;
            }
            case RECOVERY -> {
                head.xRot = 0.31F;
                jaw.xRot = 0.12F + Mth.sin(state.ageInTicks * 0.6F) * 0.08F;
                tail.xRot = 0.7F;
            }
        }
    }
}
