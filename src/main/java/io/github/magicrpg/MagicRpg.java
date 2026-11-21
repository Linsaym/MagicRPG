package io.github.magicrpg;

import io.github.magicrpg.compat.reforged.ReforgedIntegration;
import io.github.magicrpg.entity.AshHound;
import io.github.magicrpg.item.SummoningSeal;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(MagicRpg.ID)
public final class MagicRpg {
    public static final String ID = "magicrpg";

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, ID);

    public static final DeferredItem<SummoningSeal> SEAL = ITEMS.registerItem("ash_hound_seal", SummoningSeal::new);
    public static final DeferredItem<Item> FANG = ITEMS.registerSimpleItem("charred_fang");
    public static final DeferredHolder<EntityType<?>, EntityType<AshHound>> ASH_HOUND = ENTITIES.register(
        "ash_hound", id -> EntityType.Builder.of(AshHound::new, MobCategory.MONSTER)
            .sized(0.95F, 1.25F)
            .eyeHeight(0.98F)
            .clientTrackingRange(8)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, id))
    );

    public MagicRpg(IEventBus bus) {
        ITEMS.register(bus);
        ENTITIES.register(bus);
        bus.addListener(this::registerAttributes);
        bus.addListener(this::addCreativeItems);
        bus.addListener(ReforgedIntegration::setup);
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ASH_HOUND.get(), AshHound.attributes().build());
    }

    private void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.INGREDIENTS)) {
            event.accept(SEAL);
            event.accept(FANG);
        }
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ID, path);
    }
}
