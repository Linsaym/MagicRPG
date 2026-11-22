package io.github.magicrpg.client;

import io.github.magicrpg.MagicRpg;
import io.github.magicrpg.entity.ChalkGuardian;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

final class ChalkGuardianRenderer extends MobRenderer<ChalkGuardian, LivingEntityRenderState, ChalkGuardianModel> {
    private static final Identifier TEXTURE = MagicRpg.id("textures/entity/chalk_guardian.png");

    ChalkGuardianRenderer(EntityRendererProvider.Context context) {
        super(context, new ChalkGuardianModel(context.bakeLayer(ChalkGuardianModel.LAYER)), 0.38F);
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
