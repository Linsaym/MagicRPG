package io.github.magicrpg.client;

import io.github.magicrpg.MagicRpg;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;

final class WastelandZombieRenderer extends ZombieRenderer {
    private final Identifier texture;
    private final Identifier babyTexture;

    WastelandZombieRenderer(EntityRendererProvider.Context context, WastelandZombieLayers layers) {
        super(context, layers.adult, layers.baby, ModelLayers.ZOMBIE_ARMOR, ModelLayers.ZOMBIE_BABY_ARMOR);
        texture = MagicRpg.id("textures/entity/" + layers.name + ".png");
        babyTexture = MagicRpg.id("textures/entity/" + layers.name + "_baby.png");
    }

    @Override
    public Identifier getTextureLocation(ZombieRenderState state) {
        return state.isBaby ? babyTexture : texture;
    }
}
