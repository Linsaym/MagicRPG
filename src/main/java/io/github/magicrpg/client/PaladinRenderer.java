package io.github.magicrpg.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.magicrpg.MagicRpg;
import io.github.magicrpg.boss.animation.AnimationPlayer;
import io.github.magicrpg.boss.animation.BoneTransform;
import io.github.magicrpg.entity.MeadowPaladin;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

final class PaladinRenderer extends MobRenderer<MeadowPaladin, PaladinModel.State, PaladinModel> {
    private static final Identifier TEXTURE = MagicRpg.id("textures/entity/meadow_paladin.png");
    private static final RenderType RUNES = RenderTypes.eyes(MagicRpg.id("textures/entity/meadow_paladin_glow.png"));
    private final WeakHashMap<MeadowPaladin, AnimationPlayer<PaladinModel.Bone>> players = new WeakHashMap<>();

    PaladinRenderer(EntityRendererProvider.Context context) {
        super(context, new PaladinModel(context.bakeLayer(PaladinModel.LAYER)), 1);
        addLayer(new RenderLayer<>(this) {
            @Override
            public void submit(PoseStack pose, SubmitNodeCollector collector, int light,
                               PaladinModel.State state, float yaw, float pitch) {
                collector.order(1).submitModel(getParentModel(), state, pose, RUNES, light, OverlayTexture.NO_OVERLAY,
                    ARGB.gray(state.glow), null, state.outlineColor);
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
        var cue = entity.animationCue();
        boolean planted = cue.clip() == 0 && entity.hasPlantedSword() && entity.isAlive();
        var poseCue = planted ? PaladinAnimations.PLANTED_SWORD : cue;
        state.pose = players.computeIfAbsent(entity, ignored -> new AnimationPlayer<>(PaladinAnimations.CLIPS, 2))
            .sample(poseCue, entity.level().getGameTime(), partialTick);
        float coreScale = state.pose.getOrDefault(PaladinModel.Bone.CORE, BoneTransform.IDENTITY).scaleX();
        state.glow = Mth.clamp(.6F + (coreScale - 1) * 3, .20F, 1);
        if (planted) {
            state.walkAnimationSpeed = 0;
        }
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
