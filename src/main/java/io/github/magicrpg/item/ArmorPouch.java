package io.github.magicrpg.item;

import io.github.magicrpg.MagicRpg;
import io.github.magicrpg.menu.ArmorPouchMenu;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

public final class ArmorPouch extends Item {
    public static final List<EquipmentSlot> SLOTS = List.of(
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    public ArmorPouch(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS_SERVER;
        }
        ItemStack pouch = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() : Inventory.SLOT_OFFHAND;
            serverPlayer.openMenu(new SimpleMenuProvider(
                (id, inventory, owner) -> new ArmorPouchMenu(id, inventory, slot, pouch), pouch.getHoverName()),
                buffer -> buffer.writeVarInt(slot));
        } else {
            return swap(player, pouch);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    private static InteractionResult swap(Player player, ItemStack pouch) {
        var stored = contents(pouch);
        var worn = NonNullList.withSize(SLOTS.size(), ItemStack.EMPTY);
        for (int i = 0; i < SLOTS.size(); i++) {
            EquipmentSlot slot = SLOTS.get(i);
            ItemStack equipped = player.getItemBySlot(slot);
            ItemStack replacement = stored.get(i);
            if (!equipped.isEmpty() && (!player.canUseSlot(slot) || !accepts(equipped, slot, player)
                || !player.isCreative() && EnchantmentHelper.has(equipped, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE))) {
                player.sendOverlayMessage(Component.translatable("item.magicrpg.armor_pouch.locked"));
                return InteractionResult.FAIL;
            }
            if (equipped.getCount() > 1 || !replacement.isEmpty()
                && (replacement.getCount() != 1 || !player.canUseSlot(slot) || !accepts(replacement, slot, player))) {
                player.sendOverlayMessage(Component.translatable("item.magicrpg.armor_pouch.invalid"));
                return InteractionResult.FAIL;
            }
            worn.set(i, equipped);
        }
        for (int i = 0; i < SLOTS.size(); i++) {
            player.setItemSlot(SLOTS.get(i), stored.get(i));
        }
        save(pouch, worn);
        player.getInventory().setChanged();
        return InteractionResult.SUCCESS_SERVER;
    }

    public static boolean accepts(ItemStack stack, EquipmentSlot slot, Player player) {
        return !stack.is(MagicRpg.ARMOR_POUCH.get()) && stack.canEquip(slot, player);
    }

    public static NonNullList<ItemStack> contents(ItemStack pouch) {
        var contents = NonNullList.withSize(SLOTS.size(), ItemStack.EMPTY);
        pouch.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(contents);
        return contents;
    }

    public static void save(ItemStack pouch, List<ItemStack> contents) {
        pouch.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
    }
}
