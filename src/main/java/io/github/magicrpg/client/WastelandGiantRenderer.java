package io.github.magicrpg.client;

import io.github.magicrpg.MagicRpg;
import io.github.magicrpg.entity.WastelandGiant;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

final class WastelandGiantRenderer extends MobRenderer<WastelandGiant, WastelandGiantModel.State,
    EntityModel<WastelandGiantModel.State>> {
    private static final RenderType COLOSSUS_EYES =
        RenderTypes.eyes(MagicRpg.id("textures/entity/iron_colossus_eyes.png"));
    private final Identifier texture;

    WastelandGiantRenderer(EntityRendererProvider.Context context, boolean colossus) {
        super(context, colossus ? new IronColossusModel(context.bakeLayer(IronColossusModel.LAYER))
            : new WastelandGiantModel(context.bakeLayer(WastelandGiantModel.TITAN)), colossus ? 1.8F : 1.3F);
        texture = MagicRpg.id("textures/entity/" + (colossus ? "iron_colossus" : "flesh_titan") + ".png");
        if (colossus) {
            addLayer(new EyesLayer<>(this) {
                @Override
                public RenderType renderType() {
                    return COLOSSUS_EYES;
                }
            });
        }
    }

    @Override
    public WastelandGiantModel.State createRenderState() {
        return new WastelandGiantModel.State();
    }

    @Override
    public void extractRenderState(WastelandGiant entity, WastelandGiantModel.State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.swing = entity.getSwingAnimation(partialTick);
        state.sweepTime = entity.sweepTime(partialTick);
    }

    @Override
    public Identifier getTextureLocation(WastelandGiantModel.State state) {
        return texture;
    }
}
