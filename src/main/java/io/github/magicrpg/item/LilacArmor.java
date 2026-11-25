package io.github.magicrpg.item;

import io.github.magicrpg.MagicRpg;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class LilacArmor {
    private static final int REGENERATION_AMPLIFIER = 3;
    private static final ArmorMaterial MATERIAL = new ArmorMaterial(18,
        Map.of(ArmorType.HELMET, 1, ArmorType.CHESTPLATE, 1, ArmorType.LEGGINGS, 1, ArmorType.BOOTS, 1),
        16, SoundEvents.ARMOR_EQUIP_IRON, 0, 0, ItemTags.REPAIRS_IRON_ARMOR,
        ResourceKey.create(EquipmentAssets.ROOT_ID, MagicRpg.id("lilac")));

    private LilacArmor() {}

    public static Item.Properties properties(Item.Properties properties, ArmorType type) {
        return properties.humanoidArmor(MATERIAL, type).rarity(Rarity.RARE);
    }

    public static void regenerate(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()
            || player.getHealth() >= player.getMaxHealth()) {
            return;
        }
        if (player.getItemBySlot(EquipmentSlot.HEAD).is(MagicRpg.LILAC_HELMET.get())
            && player.getItemBySlot(EquipmentSlot.CHEST).is(MagicRpg.LILAC_CHESTPLATE.get())
            && player.getItemBySlot(EquipmentSlot.LEGS).is(MagicRpg.LILAC_LEGGINGS.get())
            && player.getItemBySlot(EquipmentSlot.FEET).is(MagicRpg.LILAC_BOOTS.get())) {
            // Use the vanilla rate without changing any regeneration potion the player has.
            var regeneration = MobEffects.REGENERATION.value();
            if (regeneration.shouldApplyEffectTickThisTick(player.tickCount, REGENERATION_AMPLIFIER)) {
                regeneration.applyEffectTick(player.level(), player, REGENERATION_AMPLIFIER);
            }
        }
    }
}
