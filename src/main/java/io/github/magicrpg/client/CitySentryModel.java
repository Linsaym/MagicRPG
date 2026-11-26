package io.github.magicrpg.client;

import io.github.magicrpg.MagicRpg;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

final class CitySentryModel extends EntityModel<CitySentryModel.State> {
    private static final int LIGHT = 0, DARK = 1, CAPE = 2, SKIN = 3, SHADOW = 4, STEEL = 5, TRIM = 6, LINEN = 7;
    private static final int RUNE = 8;

    private static CubeListBuilder cube(CubeListBuilder cubes, int material, float x, float y, float z,
                                       float width, float height, float depth) {
        return cubes.texOffs(material * 128, 0).addBox(x, y, z, width, height, depth);
    }

    enum Shape {
        STONE("city_stone_guard"), ARMOR("hollow_armor"), WORKER("buried_worker");

        final ModelLayerLocation layer;

        Shape(String id) {
            layer = new ModelLayerLocation(MagicRpg.id(id), "main");
        }
    }

    static final class State extends LivingEntityRenderState {
        float swing;
    }

    private final Shape shape;
    private final ModelPart head;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    CitySentryModel(ModelPart root, Shape shape) {
        super(root);
        this.shape = shape;
        head = root.getChild("head");
        leftArm = root.getChild("left_arm");
        rightArm = root.getChild("right_arm");
        leftLeg = root.getChild("left_leg");
        rightLeg = root.getChild("right_leg");
    }

    static LayerDefinition createLayer(Shape shape) {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        boolean stone = shape == Shape.STONE;
        boolean armor = shape == Shape.ARMOR;
        int coat = stone ? LIGHT : armor ? DARK : LINEN;
        int limbs = stone ? LIGHT : armor ? STEEL : LINEN;
        int width = stone ? 14 : armor ? 10 : 8;
        int torsoY = stone ? -1 : armor ? 2 : 7;
        int legsY = stone ? 12 : armor ? 13 : 16;
        var body = cube(CubeListBuilder.create(), coat, -width / 2F, 0, -3, width, legsY - torsoY, 6);
        cube(body, stone ? CAPE : TRIM, -width / 2F, legsY - torsoY - 2, -3.5F, width, 2, 7);
        if (stone || armor) {
            cube(body, DARK, -4, 2, -5, 8, 7, 2);
            cube(body, RUNE, -2, 3, -6, 4, 5, 1);
        } else {
            cube(body, CAPE, -3, 2, -4, 6, 7, 1);
        }
        root.addOrReplaceChild("body", body, PartPose.offset(0, torsoY, 0));

        var skull = CubeListBuilder.create();
        if (armor) {
            cube(skull, DARK, -4, -9, -4, 8, 9, 8);
            cube(skull, LIGHT, -4.5F, -9, -4.5F, 9, 4, 9);
            cube(skull, STEEL, -4.5F, -3, -4.5F, 9, 4, 9);
            cube(skull, SHADOW, -3, -5, -4.6F, 6, 2, 1);
            cube(skull, RUNE, -2, -5, -5, 4, 1, 1);
            cube(skull, STEEL, -4, -12, -2, 2, 5, 5);
            cube(skull, STEEL, 2, -12, -2, 2, 5, 5);
        } else {
            cube(skull, stone ? LIGHT : SKIN, -3.5F, -7, -4, 7, 8, 7);
            cube(skull, stone ? STEEL : LINEN, -4, -8, -4.5F, 8, 3, 8);
            for (int side : new int[]{-1, 1}) {
                cube(skull, stone ? RUNE : SHADOW, side * 2 - .5F, -4, -4.3F, 1, 1, 1);
            }
        }
        root.addOrReplaceChild("head", skull, PartPose.offset(0, torsoY, 0));

        for (int side : new int[]{-1, 1}) {
            var arm = cube(CubeListBuilder.create(), limbs, -2, -1, -2, 4, stone ? 13 : 9, 4);
            cube(arm, armor ? LIGHT : coat, -2.5F, -2, -2.5F, 5, 4, 5);
            if (stone) {
                cube(arm, DARK, -3, 5, -3, 6, 6, 6);
                cube(arm, RUNE, -1, 6, -4, 2, 4, 1);
            }
            if (armor && side < 0) {
                cube(arm, TRIM, -1, 6, -4, 2, 2, 7);
                cube(arm, STEEL, -1, 5, -17, 2, 3, 13);
                cube(arm, RUNE, -.5F, 4, -16, 1, 1, 11);
                cube(arm, DARK, -.6F, 6, -3, 1.2F, 2, 6);
            }
            if (shape == Shape.WORKER && side < 0) {
                cube(arm, DARK, -1, 7, -7, 2, 2, 8);
                cube(arm, TRIM, -3, 6, -8, 6, 4, 2);
            }
            root.addOrReplaceChild(side < 0 ? "right_arm" : "left_arm", arm,
                PartPose.offset(side * (width / 2F + 2), torsoY + 2, 0));

            var leg = cube(CubeListBuilder.create(), limbs, -2, 0, -2, 4, 24 - legsY, 4);
            cube(leg, DARK, -2, 22 - legsY, -3, 4, 2, 5);
            root.addOrReplaceChild(side < 0 ? "right_leg" : "left_leg", leg,
                PartPose.offset(side * (stone ? 4 : 2.5F), legsY, 0));
        }
        return LayerDefinition.create(mesh, 1152, 128);
    }

    @Override
    public void setupAnim(State state) {
        super.setupAnim(state);
        head.xRot = state.xRot * Mth.DEG_TO_RAD;
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        float stride = Mth.cos(state.walkAnimationPos * .66F) * state.walkAnimationSpeed;
        leftLeg.xRot = rightArm.xRot = stride * .7F;
        rightLeg.xRot = leftArm.xRot = -leftLeg.xRot;
        float strike = Mth.sin(state.swing * Mth.PI);
        switch (shape) {
            case STONE -> {
                rightArm.xRot -= .25F + strike * 1.5F;
                leftArm.xRot -= .25F + strike * 1.5F;
            }
            case ARMOR -> {
                rightArm.xRot = -.45F + stride * .3F - strike * 1.45F;
                rightArm.zRot = .16F - strike * .6F;
            }
            case WORKER -> {
                head.xRot += .22F;
                rightArm.xRot -= .55F + strike * .9F;
            }
        }
    }
}
