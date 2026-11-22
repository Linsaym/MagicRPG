package io.github.magicrpg.client;

import io.github.magicrpg.MagicRpg;
import io.github.magicrpg.entity.MeadowPaladin;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

final class PaladinRenderer extends MobRenderer<MeadowPaladin, PaladinModel.State, PaladinModel> {
    static final Identifier TEXTURE = MagicRpg.id("textures/entity/buried_city.png");
    static final RenderType RUNES = RenderTypes.eyes(MagicRpg.id("textures/entity/buried_city_runes.png"));

    PaladinRenderer(EntityRendererProvider.Context context) {
        super(context, new PaladinModel(context.bakeLayer(PaladinModel.LAYER)), 1);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return RUNES;
            }
        });
    }

    @Override
    public PaladinModel.State createRenderState() {
        return new PaladinModel.State();
    }

    @Override
    public void extractRenderState(MeadowPaladin entity, PaladinModel.State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.phase = entity.phase();
        state.phaseTime = entity.phaseTick() + partialTick;
    }

    @Override
    public Identifier getTextureLocation(PaladinModel.State state) {
        return TEXTURE;
    }
}
