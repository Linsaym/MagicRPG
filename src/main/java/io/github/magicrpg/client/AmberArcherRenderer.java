package io.github.magicrpg.client;

import io.github.magicrpg.MagicRpg;
import io.github.magicrpg.client.animation.BossModelAnimator;
import io.github.magicrpg.entity.AmberArcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

final class AmberArcherRenderer extends MobRenderer<AmberArcher, AmberArcherModel.State, AmberArcherModel> {
    private static final Identifier TEXTURE = MagicRpg.id("textures/entity/amber_archer.png");
    private final BossModelAnimator<AmberArcherModel.Bone> animator = new BossModelAnimator<>(AmberArcherAnimations.CLIPS, 2);

    AmberArcherRenderer(EntityRendererProvider.Context context) {
        super(context, new AmberArcherModel(AmberArcherGeometry.createRoot()), .43F);
    }

    @Override
    public AmberArcherModel.State createRenderState() {
        return new AmberArcherModel.State();
    }

    @Override
    public void extractRenderState(AmberArcher entity, AmberArcherModel.State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.pose = animator.sample(entity, partialTick);
        var cue = entity.animationCue();
        var clip = AmberArcherAnimations.CLIPS.get(cue.clip());
        state.walkWeight = 1;
        if (clip != null) {
            float elapsed = cue.elapsed(entity.level().getGameTime(), partialTick);
            state.walkWeight = Mth.clamp(1 - Math.min(elapsed, clip.duration() - elapsed) / 5, 0, 1);
        }
    }

    @Override
    public Identifier getTextureLocation(AmberArcherModel.State state) {
        return TEXTURE;
    }
}
