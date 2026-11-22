package io.github.magicrpg.client;

import io.github.magicrpg.MagicRpg;
import io.github.magicrpg.entity.ChalkGuardian;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

final class ChalkGuardianRenderer extends MobRenderer<ChalkGuardian, LivingEntityRenderState, ChalkGuardianModel> {
    private static final Identifier TEXTURE = MagicRpg.id("textures/entity/chalk_guardian.png");
    private static final RenderType CRYSTAL = RenderTypes.eyes(MagicRpg.id("textures/entity/chalk_guardian_crystal.png"));

    ChalkGuardianRenderer(EntityRendererProvider.Context context) {
        super(context, new ChalkGuardianModel(context.bakeLayer(ChalkGuardianModel.LAYER)), 0.38F);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return CRYSTAL;
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
