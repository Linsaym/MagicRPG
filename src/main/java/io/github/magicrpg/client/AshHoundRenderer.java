package io.github.magicrpg.client;

import io.github.magicrpg.MagicRpg;
import io.github.magicrpg.entity.AshHound;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

final class AshHoundRenderer extends MobRenderer<AshHound, AshHoundRenderer.State, AshHoundModel> {
    private static final Identifier TEXTURE = MagicRpg.id("textures/entity/ash_hound.png");

    AshHoundRenderer(EntityRendererProvider.Context context) {
        super(context, new AshHoundModel(context.bakeLayer(AshHoundModel.LAYER)), 0.53F);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(AshHound hound, State state, float partialTicks) {
        super.extractRenderState(hound, state, partialTicks);
        state.phase = hound.attackPhase();
    }

    @Override
    public Identifier getTextureLocation(State state) {
        return TEXTURE;
    }

    static final class State extends LivingEntityRenderState {
        AshHound.AttackPhase phase = AshHound.AttackPhase.IDLE;
    }
}
