package io.github.magicrpg.client;

import io.github.magicrpg.MagicRpg;
import io.github.magicrpg.entity.StarHare;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

final class StarHareRenderer extends MobRenderer<StarHare, LivingEntityRenderState, StarHareModel> {
    private static final Identifier TEXTURE = MagicRpg.id("textures/entity/star_hare.png");
    private static final RenderType STARS = RenderTypes.eyes(MagicRpg.id("textures/entity/star_hare_stars.png"));

    StarHareRenderer(EntityRendererProvider.Context context) {
        super(context, new StarHareModel(context.bakeLayer(StarHareModel.LAYER)), 0.25F);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return STARS;
            }
        });
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }
}
