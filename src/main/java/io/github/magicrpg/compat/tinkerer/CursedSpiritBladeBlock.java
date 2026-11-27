package io.github.magicrpg.compat.tinkerer;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class CursedSpiritBladeBlock {
    private static final String TINKERER_MOD_ID = "thaumictinkerer";
    private static final Identifier BLADE = Identifier.fromNamespaceAndPath(TINKERER_MOD_ID, "blood_sword");
    private static final float MAX_BLOCKED_HIT_DAMAGE = 3.0F;

    private CursedSpiritBladeBlock() {}

    public static void register() {
        if (ModList.get().isLoaded(TINKERER_MOD_ID)) {
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, CursedSpiritBladeBlock::limitIncomingDamage);
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, CursedSpiritBladeBlock::limitDamageBeforeAbsorption);
        }
    }

    private static void limitIncomingDamage(LivingIncomingDamageEvent event) {
        // Use the reduced hit for Minecraft's damage cooldown and armor calculations.
        if (event.getAmount() > MAX_BLOCKED_HIT_DAMAGE && isBlockingWithBlade(event.getEntity())) {
            event.setAmount(MAX_BLOCKED_HIT_DAMAGE);
        }
    }

    private static void limitDamageBeforeAbsorption(LivingDamageEvent.Pre event) {
        // Account for late damage bonuses, including Goldenwood's Sun Ring.
        if (event.getNewDamage() > MAX_BLOCKED_HIT_DAMAGE && isBlockingWithBlade(event.getEntity())) {
            event.setNewDamage(MAX_BLOCKED_HIT_DAMAGE);
        }
    }

    private static boolean isBlockingWithBlade(LivingEntity entity) {
        return entity instanceof Player player && player.isUsingItem()
            && BLADE.equals(BuiltInRegistries.ITEM.getKey(player.getUseItem().getItem()));
    }
}
