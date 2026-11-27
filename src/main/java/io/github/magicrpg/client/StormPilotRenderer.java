package io.github.magicrpg.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.magicrpg.MagicRpg;
import io.github.magicrpg.client.animation.BossModelAnimator;
import io.github.magicrpg.entity.StormPilot;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

final class StormPilotRenderer extends MobRenderer<StormPilot, StormPilotModel.State, StormPilotModel> {
    private static final Identifier TEXTURE = MagicRpg.id("textures/entity/storm_pilot.png");
    private static final RenderType LIGHT = RenderTypes.eyes(MagicRpg.id("textures/entity/storm_pilot_glow.png"));
    private final BossModelAnimator<StormPilotModel.Bone> animator = new BossModelAnimator<>(StormPilotAnimations.CLIPS, 3);

    StormPilotRenderer(EntityRendererProvider.Context context) {
        super(context, new StormPilotModel(context.bakeLayer(StormPilotModel.LAYER)), .6F);
        addLayer(new RenderLayer<>(this) {
            @Override
            public void submit(PoseStack pose, SubmitNodeCollector collector, int light,
                               StormPilotModel.State state, float yaw, float pitch) {
                collector.order(1).submitModel(getParentModel(), state, pose, LIGHT, light, OverlayTexture.NO_OVERLAY,
                    -1, null, state.outlineColor);
            }
        });
    }

    @Override
    public StormPilotModel.State createRenderState() {
        return new StormPilotModel.State();
    }

    @Override
    public void extractRenderState(StormPilot pilot, StormPilotModel.State state, float partialTick) {
        super.extractRenderState(pilot, state, partialTick);
        state.pose = animator.sample(pilot, partialTick);
    }

    @Override
    public Identifier getTextureLocation(StormPilotModel.State state) {
        return TEXTURE;
    }
}
