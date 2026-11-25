package io.github.magicrpg.menu;

import io.github.magicrpg.MagicRpg;
import io.github.magicrpg.item.ArmorPouch;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class ArmorPouchMenu extends AbstractContainerMenu {
    private static final int POUCH_SIZE = ArmorPouch.SLOTS.size();
    private static final List<Identifier> EMPTY_ICONS = List.of(InventoryMenu.EMPTY_ARMOR_SLOT_HELMET,
        InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS);

    private final Inventory inventory;
    private final int pouchIndex;
    private final ItemStack serverPouch;

    public ArmorPouchMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, buffer.readVarInt(), null);
    }

    public ArmorPouchMenu(int id, Inventory inventory, int pouchIndex, ItemStack serverPouch) {
        super(MagicRpg.ARMOR_POUCH_MENU.get(), id);
        this.inventory = inventory;
        this.pouchIndex = pouchIndex;
        this.serverPouch = serverPouch;
        SimpleContainer contents = new SimpleContainer(POUCH_SIZE) {
            @Override
            public void setChanged() {
                if (serverPouch != null && stillValid(inventory.player)) {
                    ArmorPouch.save(serverPouch, getItems());
                    inventory.setChanged();
                }
            }
        };
        if (serverPouch != null) {
            var stored = ArmorPouch.contents(serverPouch);
            for (int i = 0; i < POUCH_SIZE; i++) {
                contents.setItem(i, stored.get(i), true);
            }
        }
        for (int i = 0; i < POUCH_SIZE; i++) {
            var equipmentSlot = ArmorPouch.SLOTS.get(i);
            addSlot(new Slot(contents, i, 53 + i * 18, 25) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return ArmorPouch.accepts(stack, equipmentSlot, inventory.player);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            }.setBackground(EMPTY_ICONS.get(i)));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, 9 + row * 9 + column, 8 + column * 18, 59 + row * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(inventory, i, 8 + i * 18, 117) {
                @Override
                public boolean mayPickup(Player player) {
                    return getContainerSlot() != pouchIndex;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return getContainerSlot() != pouchIndex;
                }
            });
        }
    }

    @Override
    public void clicked(int slot, int button, ContainerInput input, Player player) {
        // A number key or the offhand key can pull from a slot outside the clicked slot.
        if (stillValid(player) && slot != lockedMenuSlot() && !(input == ContainerInput.SWAP && button == pouchIndex)) {
            super.clicked(slot, button, input, player);
        }
    }

    private int lockedMenuSlot() {
        return pouchIndex == Inventory.SLOT_OFFHAND ? -1 : POUCH_SIZE + 27 + pouchIndex;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved = index < POUCH_SIZE
            ? moveItemStackTo(stack, POUCH_SIZE, slots.size(), true)
            : moveItemStackTo(stack, 0, POUCH_SIZE, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return serverPouch == null || player.isAlive()
            && inventory.getItem(pouchIndex) == serverPouch && serverPouch.is(MagicRpg.ARMOR_POUCH.get());
    }
}
