package io.github.magicrpg.client;

import io.github.magicrpg.MagicRpg;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = MagicRpg.ID, value = Dist.CLIENT)
public final class ClientRegistration {
    private ClientRegistration() {}

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(AshHoundModel.LAYER, AshHoundModel::createLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MagicRpg.ASH_HOUND.get(), AshHoundRenderer::new);
    }
}
