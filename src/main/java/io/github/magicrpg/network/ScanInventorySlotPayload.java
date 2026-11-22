package io.github.magicrpg.network;

import io.github.magicrpg.MagicRpg;
import io.github.magicrpg.compat.reforged.ReforgedScanning;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record ScanInventorySlotPayload(int menuId, int slotIndex, Identifier itemId) implements CustomPacketPayload {
    private static final Type<ScanInventorySlotPayload> TYPE = new Type<>(MagicRpg.id("scan_inventory_slot"));
    private static final int CREATIVE_PICKER = -1;

    public static ScanInventorySlotPayload creativePicker(int inventorySlot, Identifier itemId) {
        return new ScanInventorySlotPayload(CREATIVE_PICKER, inventorySlot, itemId);
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ScanInventorySlotPayload::menuId,
            ByteBufCodecs.VAR_INT, ScanInventorySlotPayload::slotIndex,
            Identifier.STREAM_CODEC, ScanInventorySlotPayload::itemId,
            ScanInventorySlotPayload::new
        ), (request, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                request.scan(player);
            }
        });
    }

    @Override
    public Type<ScanInventorySlotPayload> type() {
        return TYPE;
    }

    private void scan(ServerPlayer player) {
        if (menuId == CREATIVE_PICKER) {
            scanCreativePicker(player);
            return;
        }

        var menu = player.containerMenu;
        if (player.isSpectator() || menu.containerId != menuId || slotIndex < 0 || slotIndex >= menu.slots.size()
            || !menu.stillValid(player) || !ReforgedScanning.isThaumometer(menu.getCarried())) {
            return;
        }

        var slot = menu.getSlot(slotIndex);
        var stack = slot.getItem();
        if (slot.isActive() && !slot.isFake() && !stack.isEmpty()
            && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(itemId)) {
            ReforgedScanning.scanInventoryStack(player, stack);
        }
    }

    private void scanCreativePicker(ServerPlayer player) {
        // The creative picker and its cursor exist only on the client.
        if (!player.isCreative() || player.containerMenu != player.inventoryMenu) {
            return;
        }
        var item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
        if (item == null || !item.isEnabled(player.level().enabledFeatures())) {
            return;
        }
        var inventory = player.getInventory();
        if (slotIndex >= 0 && slotIndex < inventory.getContainerSize()) {
            var stack = inventory.getItem(slotIndex);
            if (stack.is(item)) {
                ReforgedScanning.scanInventoryStack(player, stack);
            }
        } else if (slotIndex == -1 && !item.getDefaultInstance().isEmpty()) {
            ReforgedScanning.scanInventoryStack(player, item.getDefaultInstance());
        }
    }
}
