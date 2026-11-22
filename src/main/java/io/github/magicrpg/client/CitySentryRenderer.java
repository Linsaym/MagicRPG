package io.github.magicrpg.client;

import io.github.magicrpg.entity.CitySentry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

final class CitySentryRenderer extends MobRenderer<CitySentry, CitySentryModel.State, CitySentryModel> {
    CitySentryRenderer(EntityRendererProvider.Context context, CitySentryModel.Shape shape) {
        super(context, new CitySentryModel(context.bakeLayer(shape.layer), shape), .4F);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return PaladinRenderer.RUNES;
            }
        });
    }

    @Override
    public CitySentryModel.State createRenderState() {
        return new CitySentryModel.State();
    }

    @Override
    public void extractRenderState(CitySentry entity, CitySentryModel.State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.swing = entity.getSwingAnimation(partialTick);
    }

    @Override
    public Identifier getTextureLocation(CitySentryModel.State state) {
        return PaladinRenderer.TEXTURE;
    }
}
