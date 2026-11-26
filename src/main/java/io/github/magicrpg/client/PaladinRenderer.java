package io.github.magicrpg.client;

import io.github.magicrpg.MagicRpg;
import io.github.magicrpg.client.animation.BossModelAnimator;
import io.github.magicrpg.entity.MeadowPaladin;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

final class PaladinRenderer extends MobRenderer<MeadowPaladin, PaladinModel.State, PaladinModel> {
    private static final Identifier TEXTURE = MagicRpg.id("textures/entity/meadow_paladin.png");
    private static final RenderType RUNES = RenderTypes.eyes(MagicRpg.id("textures/entity/meadow_paladin_glow.png"));
    private final BossModelAnimator<PaladinModel.Bone> animator = new BossModelAnimator<>(PaladinAnimations.CLIPS, 2);

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
        state.pose = animator.sample(entity, partialTick);
        var cue = entity.animationCue();
        var clip = PaladinAnimations.CLIPS.get(cue.clip());
        state.walkWeight = 1;
        if (clip != null) {
            float elapsed = cue.elapsed(entity.level().getGameTime(), partialTick);
            float fromEdge = Math.min(elapsed, clip.duration() - elapsed);
            state.walkWeight = Mth.clamp(1 - fromEdge / 6, 0, 1);
        }
    }

    @Override
    public Identifier getTextureLocation(PaladinModel.State state) {
        return TEXTURE;
    }
}
