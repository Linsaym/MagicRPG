package io.github.magicrpg.compat.reforged.client;

import io.github.magicrpg.MagicRpg;
import io.github.magicrpg.compat.reforged.ReforgedScanning;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import thaumcraft.aspect.AspectHelper;
import thaumcraft.client.ClientConfig;
import thaumcraft.client.aspect.AspectRenderer;
import thaumcraft.client.aspect.AspectVisibility;

@EventBusSubscriber(modid = MagicRpg.ID, value = Dist.CLIENT)
public final class InventoryAspectOverlay {
    private static final int ICON_STEP = 20;
    private static final int MARGIN = 4;

    private InventoryAspectOverlay() {}

    @SubscribeEvent
    public static void render(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }

        boolean shift = Minecraft.getInstance().hasShiftDown();
        // Reforged draws its own overlay when Shift and display_aspects differ.
        if (shift != ClientConfig.showTags() || (!shift && !ReforgedScanning.isThaumometer(screen.getMenu().getCarried()))) {
            return;
        }

        var slot = screen.getHoveredSlot();
        if (slot == null || !slot.isActive() || slot.isFake() || !AspectVisibility.isScanned(slot.getItem())) {
            return;
        }
        var aspects = AspectHelper.getBonusTags(slot.getItem());
        if (aspects.size() == 0) {
            return;
        }

        int columns = Math.min(aspects.size(), Math.max(1, (screen.width - 2 * MARGIN) / ICON_STEP));
        int rows = (aspects.size() + columns - 1) / columns;
        int firstX = Mth.clamp(event.getMouseX() + 9, MARGIN,
            Math.max(MARGIN, screen.width - columns * ICON_STEP - MARGIN));
        int firstY = Mth.clamp(event.getMouseY() - 14 - rows * ICON_STEP, MARGIN,
            Math.max(MARGIN, screen.height - rows * ICON_STEP - MARGIN));

        var graphics = event.getGuiGraphics();
        graphics.nextStratum();
        var sorted = aspects.getAspectsSortedAmount();
        for (int index = 0; index < sorted.size(); index++) {
            var aspect = sorted.get(index);
            int x = firstX + index % columns * ICON_STEP;
            int y = firstY + index / columns * ICON_STEP;
            AspectRenderer.drawBack(graphics, x, y);
            if (AspectVisibility.isKnown(aspect)) {
                AspectRenderer.drawTag(graphics, x, y, aspect, aspects.getAmount(aspect));
            } else {
                AspectRenderer.drawUnknown(graphics, x, y);
            }
        }
    }
}
