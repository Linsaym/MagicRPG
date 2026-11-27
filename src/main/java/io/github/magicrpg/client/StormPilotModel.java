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

final class StormPilotModel extends EntityModel<StormPilotModel.State> {
    static final ModelLayerLocation LAYER = new ModelLayerLocation(MagicRpg.id("storm_pilot"), "main");

    enum Bone { BODY, LEFT_FIN, RIGHT_FIN, LENS }

    static final class State extends LivingEntityRenderState {
        Map<Bone, BoneTransform> pose = Map.of();
    }

    private final ModelPart body;
    private final Map<Bone, ModelPart> bones;

    StormPilotModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        bones = Map.of(Bone.BODY, body, Bone.LEFT_FIN, body.getChild("left_fin"),
            Bone.RIGHT_FIN, body.getChild("right_fin"), Bone.LENS, body.getChild("lens"));
    }

    static LayerDefinition createLayer() {
        var mesh = new MeshDefinition();
        var body = mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-9, -11, -8, 18, 22, 16)
            .texOffs(70, 0).addBox(-7, -14, -6, 14, 3, 12)
            .texOffs(75, 17).addBox(-6, 11, -5, 12, 4, 10)
            .texOffs(0, 40).addBox(-11, -5, -9, 22, 4, 18)
            .texOffs(0, 66).addBox(-7, -8, -13, 14, 14, 5)
            .texOffs(80, 38).addBox(-2, 16, -2, 4, 8, 4), PartPose.offset(0, -11, 0));
        body.addOrReplaceChild("lens", CubeListBuilder.create()
            .texOffs(46, 66).addBox(-5, -5, -1.5F, 10, 10, 3), PartPose.offset(0, -1, -13));
        fin(body, "left_fin", 1);
        fin(body, "right_fin", -1);
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static void fin(PartDefinition body, String name, int side) {
        body.addOrReplaceChild(name, CubeListBuilder.create().mirror(side < 0)
            .texOffs(0, 92).addBox(side < 0 ? -6 : 0, -12, -4, 6, 19, 8)
            .texOffs(30, 92).addBox(side < 0 ? -11 : 6, -16, -2, 5, 16, 5)
            .texOffs(56, 92).addBox(side < 0 ? -8 : 0, 7, -3, 8, 4, 6)
            .texOffs(84, 93).addBox(side < 0 ? -6 : 0, -9, -5, 6, 13, 1),
            PartPose.offsetAndRotation(side * 9, -2, 0, 0, 0, side * -.12F));
    }

    @Override
    public void setupAnim(State state) {
        super.setupAnim(state);
        body.y += Mth.sin(state.ageInTicks * .095F) * 1.15F;
        body.zRot += Mth.sin(state.ageInTicks * .05F) * .035F;
        BossModelAnimator.apply(state.pose, bones);
    }
}
