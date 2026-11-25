package io.github.magicrpg.client;

import io.github.magicrpg.MagicRpg;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.monster.zombie.BabyZombieModel;

enum WastelandZombieLayers {
    HOLLOWED("hollowed"),
    THAUMATURGE("zombie_thaumaturge");

    final String name;
    final ModelLayerLocation adult;
    final ModelLayerLocation baby;

    WastelandZombieLayers(String name) {
        this.name = name;
        adult = new ModelLayerLocation(MagicRpg.id(name), "main");
        baby = new ModelLayerLocation(MagicRpg.id(name), "baby");
    }

    LayerDefinition adultLayer() {
        var mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0);
        var root = mesh.getRoot();
        var head = root.getChild("head");
        var body = root.getChild("body");
        var rightArm = root.getChild("right_arm");
        if (this == THAUMATURGE) {
            head.addOrReplaceChild("hood", CubeListBuilder.create().texOffs(64, 0)
                .addBox(-4.5F, -8.5F, -4.5F, 9, 2, 9).texOffs(102, 0)
                .addBox(-5, -6.5F, -4.5F, 1, 5, 8).texOffs(102, 0)
                .addBox(4, -6.5F, -4.5F, 1, 5, 8).texOffs(64, 14)
                .addBox(-4, -6.5F, 3.5F, 8, 6, 2), PartPose.ZERO);
            body.addOrReplaceChild("mantle", CubeListBuilder.create().texOffs(88, 18)
                .addBox(-4.5F, 0, -2.5F, 9, 3, 5).texOffs(88, 29)
                .addBox(-3, 2, -3, 5, 7, 1).texOffs(104, 40)
                .addBox(-1, 4, -4, 2, 2, 1), PartPose.ZERO);
            rightArm.addOrReplaceChild("cuff", CubeListBuilder.create().texOffs(108, 26)
                .addBox(-3.5F, 5, -2.5F, 5, 3, 5), PartPose.ZERO);
            root.getChild("right_leg").addOrReplaceChild("hem", CubeListBuilder.create().texOffs(64, 44)
                .addBox(-2.5F, -.5F, -2.5F, 5, 8, 5), PartPose.ZERO);
            root.getChild("left_leg").addOrReplaceChild("hem", CubeListBuilder.create().texOffs(90, 48)
                .addBox(-2.5F, -.5F, -2.5F, 5, 5, 5), PartPose.ZERO);
        } else {
            head.addOrReplaceChild("work_cap", CubeListBuilder.create().texOffs(64, 0)
                .addBox(-4.5F, -8.5F, -4.5F, 9, 2, 9).texOffs(64, 14)
                .addBox(-4.5F, -6.5F, -6.5F, 9, 1, 3), PartPose.ZERO);
            body.addOrReplaceChild("apron", CubeListBuilder.create().texOffs(88, 18)
                .addBox(-3, 3, -2.8F, 6, 9, 1).texOffs(88, 29)
                .addBox(0, 7, -3.4F, 3, 3, 1).texOffs(104, 40)
                .addBox(-3, 1, -3, 2, 6, 1).texOffs(100, 52)
                .addBox(-1, 2, 1.8F, 4, 5, 2), PartPose.ZERO);
            rightArm.addOrReplaceChild("shoulder", CubeListBuilder.create().texOffs(108, 26)
                .addBox(-3.5F, -2.5F, -2.5F, 5, 3, 5), PartPose.ZERO);
            root.getChild("left_leg").addOrReplaceChild("patch", CubeListBuilder.create().texOffs(64, 44)
                .addBox(-2, 4, -2.7F, 4, 4, 1), PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    LayerDefinition babyLayer() {
        return BabyZombieModel.createBodyLayer(CubeDeformation.NONE).apply(mesh -> {
            var root = mesh.getRoot();
            var head = root.getChild("head");
            if (this == THAUMATURGE) {
                head.addOrReplaceChild("hood", CubeListBuilder.create().texOffs(0, 32)
                    .addBox(-3.5F, -6.5F, -3.5F, 7, 2, 7).texOffs(0, 44)
                    .addBox(-4, -4.5F, -3.5F, 1, 3, 6).texOffs(0, 44)
                    .addBox(3, -4.5F, -3.5F, 1, 3, 6), PartPose.ZERO);
            } else {
                head.addOrReplaceChild("work_cap", CubeListBuilder.create().texOffs(0, 32)
                    .addBox(-3.5F, -6.5F, -3.5F, 7, 2, 7).texOffs(0, 44)
                    .addBox(-3, -5, -5, 6, 1, 2), PartPose.ZERO);
            }
            root.getChild("body").addOrReplaceChild("vest", CubeListBuilder.create().texOffs(32, 32)
                .addBox(-1.5F, -1.5F, -1.5F, 3, 3, 1), PartPose.ZERO);
            return mesh;
        });
    }
}
