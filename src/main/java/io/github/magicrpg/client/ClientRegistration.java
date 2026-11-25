package io.github.magicrpg.client;

import io.github.magicrpg.Goldenwood;
import io.github.magicrpg.IronWastelands;
import io.github.magicrpg.MagicRpg;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.IronGolemRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.WanderingTraderRenderer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

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
        event.registerLayerDefinition(IronColossusModel.LAYER, IronColossusModel::createLayer);
        event.registerLayerDefinition(WastelandGiantModel.TITAN, WastelandGiantModel::createLayer);
        for (var layers : WastelandZombieLayers.values()) {
            event.registerLayerDefinition(layers.adult, layers::adultLayer);
            event.registerLayerDefinition(layers.baby, layers::babyLayer);
        }
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
        event.registerEntityRenderer(MagicRpg.ANCIENT_MERCHANT.get(), AncientMerchantRenderer::new);
        event.registerEntityRenderer(IronWastelands.IRON_COLOSSUS.get(), context -> new WastelandGiantRenderer(context, true));
        event.registerEntityRenderer(IronWastelands.LARGE_IRON_GOLEM.get(), IronGolemRenderer::new);
        event.registerEntityRenderer(IronWastelands.FLESH_TITAN.get(), context -> new WastelandGiantRenderer(context, false));
        event.registerEntityRenderer(IronWastelands.HOLLOWED.get(),
            context -> new WastelandZombieRenderer(context, WastelandZombieLayers.HOLLOWED));
        event.registerEntityRenderer(IronWastelands.ZOMBIE_THAUMATURGE.get(),
            context -> new WastelandZombieRenderer(context, WastelandZombieLayers.THAUMATURGE));
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(MagicRpg.ARMOR_POUCH_MENU.get(), ArmorPouchScreen::new);
    }

    @SubscribeEvent
    public static void registerEquipment(RegisterClientExtensionsEvent event) {
        event.registerItem(new LilacArmorModel.Extension(EquipmentSlot.HEAD), MagicRpg.LILAC_HELMET.get());
        event.registerItem(new LilacArmorModel.Extension(EquipmentSlot.CHEST), MagicRpg.LILAC_CHESTPLATE.get());
        event.registerItem(new LilacArmorModel.Extension(EquipmentSlot.LEGS), MagicRpg.LILAC_LEGGINGS.get());
        event.registerItem(new LilacArmorModel.Extension(EquipmentSlot.FEET), MagicRpg.LILAC_BOOTS.get());
    }

    private static final class AncientMerchantRenderer extends WanderingTraderRenderer {
        private static final Identifier TEXTURE = MagicRpg.id("textures/entity/ancient_merchant.png");

        private AncientMerchantRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public Identifier getTextureLocation(VillagerRenderState state) {
            return TEXTURE;
        }
    }
}
