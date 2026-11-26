package io.github.magicrpg.client;

import io.github.magicrpg.MagicRpg;
import io.github.magicrpg.entity.CitySentry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

final class CitySentryRenderer extends MobRenderer<CitySentry, CitySentryModel.State, CitySentryModel> {
    private static final Identifier TEXTURE = MagicRpg.id("textures/entity/buried_city.png");
    private static final RenderType RUNES = RenderTypes.eyes(MagicRpg.id("textures/entity/buried_city_runes.png"));

    CitySentryRenderer(EntityRendererProvider.Context context, CitySentryModel.Shape shape) {
        super(context, new CitySentryModel(context.bakeLayer(shape.layer), shape), .4F);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return RUNES;
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
        return TEXTURE;
    }
}
