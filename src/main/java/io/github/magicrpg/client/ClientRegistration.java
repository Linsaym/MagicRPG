package io.github.magicrpg.client;

import io.github.magicrpg.Goldenwood;
import io.github.magicrpg.MagicRpg;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = MagicRpg.ID, value = Dist.CLIENT)
public final class ClientRegistration {
    private ClientRegistration() {}

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (var shape : WoodlandModel.Shape.values()) {
            event.registerLayerDefinition(shape.layer, () -> WoodlandModel.createLayer(shape));
        }
        event.registerLayerDefinition(AshHoundModel.LAYER, AshHoundModel::createLayer);
        event.registerLayerDefinition(StarHareModel.LAYER, StarHareModel::createLayer);
        event.registerLayerDefinition(PaladinModel.LAYER, PaladinModel::createLayer);
        for (var shape : CitySentryModel.Shape.values()) {
            event.registerLayerDefinition(shape.layer, () -> CitySentryModel.createLayer(shape));
        }
        event.registerLayerDefinition(ChalkGuardianModel.LAYER, ChalkGuardianModel::createLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Goldenwood.SPROUT.get(),
            context -> new WoodlandRenderer<>(context, WoodlandModel.Shape.SPROUT, 0.38F));
        event.registerEntityRenderer(Goldenwood.GUARDIAN.get(),
            context -> new WoodlandRenderer<>(context, WoodlandModel.Shape.GUARDIAN, 0.67F));
        event.registerEntityRenderer(Goldenwood.BEETLE.get(),
            context -> new WoodlandRenderer<>(context, WoodlandModel.Shape.BEETLE, 0.31F));
        event.registerEntityRenderer(Goldenwood.SPARK.get(),
            context -> new WoodlandRenderer<>(context, WoodlandModel.Shape.SPARK, 0.24F));
        event.registerEntityRenderer(Goldenwood.MOTH.get(),
            context -> new WoodlandRenderer<>(context, WoodlandModel.Shape.MOTH, 0.13F));
        event.registerEntityRenderer(Goldenwood.BOLT.get(), context -> new ThrownItemRenderer<>(context, 0.65F, true));
        event.registerEntityRenderer(MagicRpg.ASH_HOUND.get(), AshHoundRenderer::new);
        event.registerEntityRenderer(MagicRpg.STAR_HARE.get(), StarHareRenderer::new);
        event.registerEntityRenderer(MagicRpg.MEADOW_PALADIN.get(), PaladinRenderer::new);
        event.registerEntityRenderer(MagicRpg.CITY_STONE_GUARD.get(),
            context -> new CitySentryRenderer(context, CitySentryModel.Shape.STONE));
        event.registerEntityRenderer(MagicRpg.HOLLOW_ARMOR.get(),
            context -> new CitySentryRenderer(context, CitySentryModel.Shape.ARMOR));
        event.registerEntityRenderer(MagicRpg.BURIED_WORKER.get(),
            context -> new CitySentryRenderer(context, CitySentryModel.Shape.WORKER));
        event.registerEntityRenderer(MagicRpg.CHALK_GUARDIAN.get(), ChalkGuardianRenderer::new);
    }
}
