package io.github.magicrpg.client;

import io.github.magicrpg.MagicRpg;
import io.github.magicrpg.entity.WoodlandMonster;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

final class WoodlandRenderer<T extends Mob> extends MobRenderer<T, WoodlandModel.State, WoodlandModel> {
    private final Identifier texture;

    WoodlandRenderer(EntityRendererProvider.Context context, WoodlandModel.Shape shape, float shadow) {
        super(context, new WoodlandModel(context.bakeLayer(shape.layer), shape), shadow);
        texture = MagicRpg.id("textures/entity/" + shape.id + ".png");
        if (shape == WoodlandModel.Shape.MOTH || shape == WoodlandModel.Shape.SPARK
            || shape == WoodlandModel.Shape.GUARDIAN) {
            var glow = RenderTypes.eyes(MagicRpg.id("textures/entity/" + shape.id + "_glow.png"));
            addLayer(new EyesLayer<>(this) {
                @Override
                public RenderType renderType() {
                    return glow;
                }
            });
        }
    }

    @Override
    public WoodlandModel.State createRenderState() {
        return new WoodlandModel.State();
    }

    @Override
    public void extractRenderState(T mob, WoodlandModel.State state, float partialTick) {
        super.extractRenderState(mob, state, partialTick);
        state.attackPose = mob instanceof WoodlandMonster monster ? monster.attackPose() : 0;
    }

    @Override
    public Identifier getTextureLocation(WoodlandModel.State state) {
        return texture;
    }
}
