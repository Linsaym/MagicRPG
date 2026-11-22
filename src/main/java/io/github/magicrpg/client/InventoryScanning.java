package io.github.magicrpg.client;

import io.github.magicrpg.MagicRpg;
import io.github.magicrpg.compat.reforged.ReforgedScanning;
import io.github.magicrpg.network.ScanInventorySlotPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

@EventBusSubscriber(modid = MagicRpg.ID, value = Dist.CLIENT)
public final class InventoryScanning {
    private static final int SCAN_TICKS = 20;
    private static final int RESPONSE_TIMEOUT = 80;
    private static final int FEEDBACK_TICKS = 50;
    private static @Nullable AbstractContainerScreen<?> activeScreen;
    private static @Nullable Slot activeSlot;
    private static ItemStack target = ItemStack.EMPTY;
    private static int ticks;
    private static int feedbackTicks;
    private static @Nullable Component feedback;

    private InventoryScanning() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || player.isSpectator()
            || !(minecraft.gui.screen() instanceof AbstractContainerScreen<?> screen)
            || screen.getMenu() != player.containerMenu
            || !ReforgedScanning.isThaumometer(screen.getMenu().getCarried())) {
            reset();
            return;
        }
        var slot = screen.getHoveredSlot();
        if (slot == null || !slot.isActive() || slot.isFake() || !slot.hasItem()) {
            reset();
            return;
        }
        var stack = slot.getItem();
        if (activeScreen != screen || activeSlot != slot || !ItemStack.isSameItemSameComponents(stack, target)) {
            reset();
            activeScreen = screen;
            activeSlot = slot;
            target = stack.copyWithCount(1);
        }
        if (feedbackTicks > 0 && --feedbackTicks == 0) {
            feedback = null;
        }

        boolean creativePicker = screen instanceof CreativeModeInventoryScreen && player.isCreative();
        if (ReforgedScanning.hasScanned(player, stack)) {
            if (creativePicker && ticks >= SCAN_TICKS && ticks < RESPONSE_TIMEOUT) {
                showFeedback("gui.magicrpg.scan.complete");
            }
            ticks = RESPONSE_TIMEOUT;
            return;
        }
        if (ticks >= SCAN_TICKS) {
            if (creativePicker && ticks < RESPONSE_TIMEOUT && ++ticks == RESPONSE_TIMEOUT) {
                showFeedback("gui.magicrpg.scan.retry");
            }
            return;
        }
        if (++ticks < SCAN_TICKS) {
            return;
        }
        int inventorySlot = slot.container == player.getInventory() ? slot.getContainerSlot() : -1;
        var specimen = creativePicker && inventorySlot == -1 ? stack.getItem().getDefaultInstance() : stack;
        if (!ReforgedScanning.canScan(player, specimen)) {
            ticks = RESPONSE_TIMEOUT;
            if (creativePicker) {
                showFeedback("gui.magicrpg.scan.unknown");
            }
            return;
        }
        var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        var request = creativePicker
            ? ScanInventorySlotPayload.creativePicker(inventorySlot, itemId)
            : new ScanInventorySlotPayload(screen.getMenu().containerId, slot.index, itemId);
        ClientPacketDistributor.sendToServer(request);
    }

    @SubscribeEvent
    public static void render(ScreenEvent.Render.Post event) {
        var screen = activeScreen;
        var slot = activeSlot;
        if (screen == null || slot == null || event.getScreen() != screen || screen.getHoveredSlot() != slot) {
            return;
        }
        var graphics = event.getGuiGraphics();
        if (feedback != null) {
            var font = Minecraft.getInstance().font;
            int width = font.width(feedback);
            int x = Mth.clamp(screen.getLeftPos() + slot.x + 8 - width / 2, 4, Math.max(4, screen.width - width - 4));
            int y = Math.min(screen.height - 12, screen.getTopPos() + slot.y + 18);
            graphics.nextStratum();
            graphics.fill(x - 3, y - 2, x + width + 3, y + 10, 0xF0362B44);
            graphics.text(font, feedback, x, y, 0xFFFFE6AC);
        } else if (ticks > 0 && ticks < SCAN_TICKS) {
            int x = screen.getLeftPos() + slot.x + 1;
            int y = screen.getTopPos() + slot.y + 13;
            graphics.fill(x, y, x + 14, y + 3, 0xFF362B44);
            graphics.fill(x, y + 1, x + ticks * 14 / SCAN_TICKS, y + 2, 0xFFE2BD76);
        }
    }

    private static void showFeedback(String key) {
        feedback = Component.translatable(key);
        feedbackTicks = FEEDBACK_TICKS;
    }

    private static void reset() {
        activeScreen = null;
        activeSlot = null;
        target = ItemStack.EMPTY;
        ticks = 0;
        feedback = null;
        feedbackTicks = 0;
    }
}
