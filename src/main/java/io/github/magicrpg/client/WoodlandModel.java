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

final class WoodlandModel extends EntityModel<WoodlandModel.State> {
    enum Shape {
        SPROUT("sprout_ent"), GUARDIAN("guardian_ent"), BEETLE("amber_bark_beetle"),
        SPARK("ruin_spark"), MOTH("honey_moth");

        final String id;
        final ModelLayerLocation layer;

        Shape(String id) {
            this.id = id;
            layer = new ModelLayerLocation(MagicRpg.id(id), "main");
        }
    }

    static final class State extends LivingEntityRenderState {
        int attackPose;
    }

    private final Shape shape;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart left;
    private final ModelPart right;
    private final ModelPart hindLeft;
    private final ModelPart hindRight;

    WoodlandModel(ModelPart root, Shape shape) {
        super(root);
        this.shape = shape;
        body = root.getChild("body");
        head = body.getChild("head");
        left = body.getChild("left");
        right = body.getChild("right");
        hindLeft = body.getChild("hind_left");
        hindRight = body.getChild("hind_right");
    }

    static LayerDefinition createLayer(Shape shape) {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        switch (shape) {
            case SPROUT -> sprout(root);
            case GUARDIAN -> guardian(root);
            case BEETLE -> beetle(root);
            case SPARK -> spark(root);
            case MOTH -> moth(root);
        }
        return LayerDefinition.create(mesh, 128, 96);
    }

    private static void sprout(PartDefinition root) {
        var body = root.addOrReplaceChild("body", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-4, -6, -3, 8, 8, 6)
            .texOffs(0, 38).addBox(-7, -8, -4, 14, 4, 9), PartPose.offset(0, 16, 0));
        body.addOrReplaceChild("head", CubeListBuilder.create()
            .texOffs(0, 20).addBox(-4, -4, -4, 8, 5, 7)
            .texOffs(46, 37).addBox(-5, -8, -4, 10, 4, 9)
            .texOffs(86, 36).addBox(-2, -10, 1, 5, 2, 5), PartPose.offset(0, -6, -1));
        arm(body, "left", 5, -4, 28, 0, 3, 8, 3, -0.30F);
        arm(body, "right", -5, -4, 44, 0, 3, 8, 3, 0.30F);
        foot(body, "hind_left", 2.5F, 2, 62, 0, 3, 6, 5);
        foot(body, "hind_right", -2.5F, 2, 80, 0, 3, 6, 5);
    }

    private static void guardian(PartDefinition root) {
        var body = root.addOrReplaceChild("body", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-7, -17, -5, 14, 19, 10)
            .texOffs(50, 0).addBox(-4, -12, -6, 8, 8, 2)
            .texOffs(70, 0).addBox(-10, -19, -4, 20, 4, 9), PartPose.offset(0, 9, 0));
        body.addOrReplaceChild("head", CubeListBuilder.create()
            .texOffs(0, 30).addBox(-5, -8, -4, 10, 9, 8)
            .texOffs(38, 37).addBox(-8, -9, -3, 16, 4, 7)
            .texOffs(86, 20).addBox(3, -15, -1, 3, 7, 3)
            .texOffs(101, 20).addBox(-6, -13, 0, 3, 6, 3), PartPose.offset(0, -16, -1));
        var left = arm(body, "left", 9, -14, 0, 55, 5, 18, 5, -0.14F);
        left.addOrReplaceChild("branch", CubeListBuilder.create().texOffs(43, 55)
            .addBox(0, -6, -1, 3, 9, 2), PartPose.offsetAndRotation(2, 5, 0, 0, 0, 0.52F));
        var right = arm(body, "right", -9, -14, 22, 55, 5, 18, 5, 0.14F);
        right.addOrReplaceChild("branch", CubeListBuilder.create().texOffs(55, 55)
            .addBox(-3, -7, -1, 3, 9, 2), PartPose.offsetAndRotation(-2, 3, 0, 0, 0, -0.46F));
        foot(body, "hind_left", 4, 1, 70, 52, 5, 14, 7);
        foot(body, "hind_right", -4, 1, 96, 52, 5, 14, 7);
    }

    private static void beetle(PartDefinition root) {
        var body = root.addOrReplaceChild("body", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-3, -3, -2, 6, 4, 7)
            .texOffs(29, 0).addBox(-4, -5, -1, 8, 3, 8), PartPose.offset(0, 21, 0));
        body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 22).addBox(-2.5F, -3, -3, 5, 4, 4)
            .texOffs(20, 24).addBox(-3, -1, -5, 2, 2, 3).texOffs(32, 24).addBox(1, -1, -5, 2, 2, 3),
            PartPose.offset(0, 0, -2));
        beetleLegs(body, "left", 3, -1, 1, 0);
        beetleLegs(body, "right", -3, -1, -1, 14);
        beetleLegs(body, "hind_left", 3, 3, 1, 28);
        beetleLegs(body, "hind_right", -3, 3, -1, 42);
    }

    private static void beetleLegs(PartDefinition body, String name, int x, int z, int sign, int u) {
        var legs = CubeListBuilder.create().texOffs(u, 38).addBox(Math.min(0, sign * 4), 0, 0, 4, 1, 1)
            .texOffs(u, 43).addBox(sign * 3 - 0.5F, 0, 0, 1, 3, 1);
        if (z == -1) {
            legs.texOffs(u, 48).addBox(Math.min(0, sign * 3), 0, -2, 3, 1, 1)
                .texOffs(u, 53).addBox(sign * 2 - 0.5F, 0, -2, 1, 3, 1);
        }
        body.addOrReplaceChild(name, legs, PartPose.offset(x, 0, z));
    }

    private static void spark(PartDefinition root) {
        var body = root.addOrReplaceChild("body", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-3, -3, -3, 6, 6, 6), PartPose.offset(0, 16, 0));
        body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(26, 0).addBox(-2, -2, -2, 4, 4, 4),
            PartPose.offsetAndRotation(0, 0, 0, 0.68F, 0.44F, 0.68F));
        shard(body, "left", 5, 0, 0, 0);
        shard(body, "right", -5, 0, 0, 15);
        shard(body, "hind_left", 0, 0, 5, 30);
        shard(body, "hind_right", 0, 0, -5, 45);
    }

    private static void shard(PartDefinition body, String name, int x, int y, int z, int u) {
        body.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, 22).addBox(-1.5F, -5, -1, 3, 9, 2)
            .texOffs(u, 35).addBox(-1, 3, -0.5F, 2, 3, 1),
            PartPose.offsetAndRotation(x, y, z, z * 0.07F, 0, x * 0.07F));
    }

    private static void moth(PartDefinition root) {
        var body = root.addOrReplaceChild("body", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-1, -1, -3, 2, 2, 6), PartPose.offset(0, 22, 0));
        body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 13).addBox(-1.5F, -1.5F, -2, 3, 3, 2)
            .texOffs(14, 14).addBox(-2, -2.7F, -2.5F, 1, 2, 1)
            .texOffs(20, 14).addBox(1, -2.7F, -2.5F, 1, 2, 1), PartPose.offset(0, 0, -2));
        wing(body, "left", 1, -2, 37, 0, 6, 4);
        wing(body, "right", -1, -2, 57, 0, 6, 4);
        wing(body, "hind_left", 1, 2, 37, 14, 4, 4);
        wing(body, "hind_right", -1, 2, 57, 14, 4, 4);
    }

    private static void wing(PartDefinition body, String name, int side, int z, int u, int v, int width, int depth) {
        var wing = CubeListBuilder.create().texOffs(u, v).mirror(side < 0)
            .addBox(side < 0 ? -width : 0, -0.5F, -1, width, 1, depth);
        body.addOrReplaceChild(name, wing, PartPose.offset(side, 0, z));
    }

    private static PartDefinition arm(PartDefinition body, String name, float x, int y, int u, int v,
                                      int width, int height, int depth, float angle) {
        return body.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v)
            .addBox(-width / 2F, -1, -depth / 2F, width, height, depth),
            PartPose.offsetAndRotation(x, y, 0, 0, 0, angle));
    }

    private static void foot(PartDefinition body, String name, float x, int y, int u, int v,
                             int width, int height, int depth) {
        body.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v)
            .addBox(-width / 2F, 0, -depth / 2F - 1, width, height, depth), PartPose.offset(x, y, 0));
    }

    @Override
    public void setupAnim(State state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        float step = Mth.cos(state.walkAnimationPos * 0.85F) * state.walkAnimationSpeed;
        switch (shape) {
            case SPROUT, GUARDIAN -> {
                head.yRot = Mth.clamp(state.yRot, -40, 40) * Mth.DEG_TO_RAD;
                head.xRot = Mth.clamp(state.xRot, -25, 25) * Mth.DEG_TO_RAD;
                hindLeft.xRot = step * 0.85F;
                hindRight.xRot = -hindLeft.xRot;
                left.xRot = -step * 0.75F;
                right.xRot = -left.xRot;
                if (state.attackPose == 1) {
                    if (shape == Shape.GUARDIAN) {
                        left.xRot = -2.5F + Mth.sin(age * 0.27F) * 0.08F;
                        right.xRot = left.xRot + 0.1F;
                        body.xRot = -0.11F;
                    } else {
                        head.zRot = Mth.sin(age * 1.95F) * 0.16F;
                        body.y += 1 + Mth.sin(age * 1.5F) * 0.32F;
                        left.zRot -= 0.40F;
                        right.zRot += 0.40F;
                    }
                } else if (state.attackPose == 2) {
                    body.xRot = 0.32F;
                    left.xRot = right.xRot = -0.95F;
                    hindLeft.xRot = hindRight.xRot = 0.38F;
                }
            }
            case BEETLE -> {
                float scuttle = Mth.cos(state.walkAnimationPos * 1.9F) * state.walkAnimationSpeed;
                left.yRot = hindRight.yRot = scuttle * 0.52F;
                right.yRot = hindLeft.yRot = -left.yRot;
                head.yRot = Mth.sin(age * 0.29F) * 0.14F;
            }
            case SPARK -> {
                body.y += Mth.sin(age * 0.25F) * 0.65F;
                body.yRot = age * (state.attackPose == 1 ? 0.125F : 0.055F);
                head.xRot += age * 0.095F;
                left.zRot += 0.21F * Mth.sin(age * 0.16F);
                right.zRot -= 0.21F * Mth.cos(age * 0.16F);
                if (state.attackPose == 1) {
                    body.xScale = body.yScale = body.zScale = 1.20F + Mth.sin(age * 0.9F) * 0.10F;
                }
            }
            case MOTH -> {
                float flap = Mth.sin(age * 1.5F) * 1.03F;
                left.zRot = flap;
                right.zRot = -flap;
                hindLeft.zRot = flap * 0.84F;
                hindRight.zRot = -hindLeft.zRot;
                body.xRot = Mth.sin(age * 0.31F) * 0.18F;
            }
        }
    }
}

