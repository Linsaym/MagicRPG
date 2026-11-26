package io.github.magicrpg.boss;

import io.github.magicrpg.MagicRpg;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = MagicRpg.ID)
final class BossResources {
    private static final String OWNER = "magicrpg:boss_summon_owner";

    private BossResources() {}

    static void markSummon(Entity summon, Entity owner) {
        summon.getPersistentData().putString(OWNER, owner.getUUID().toString());
    }

    static boolean isSummonOf(Entity summon, Entity owner) {
        return summon.getPersistentData().getString(OWNER).filter(id -> id.equals(owner.getUUID().toString())).isPresent();
    }

    private static boolean isSummon(Entity entity) {
        return entity.getPersistentData().contains(OWNER);
    }

    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() && event.getLevel() instanceof ServerLevel && isSummon(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onTravel(EntityTravelToDimensionEvent event) {
        if (isSummon(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onDrops(LivingDropsEvent event) {
        if (isSummon(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onExperience(LivingExperienceDropEvent event) {
        if (isSummon(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % 5 == 0) {
            BossArena.get(level).expire(level);
        }
    }
}
