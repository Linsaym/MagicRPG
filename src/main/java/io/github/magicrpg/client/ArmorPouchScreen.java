package io.github.magicrpg.client;

import io.github.magicrpg.menu.ArmorPouchMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ArmorPouchScreen extends AbstractContainerScreen<ArmorPouchMenu> {
    public ArmorPouchScreen(ArmorPouchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 141);
        inventoryLabelY = 47;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF35273E);
        graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xFFD5C5DD);
        graphics.fill(leftPos + 5, topPos + 21, leftPos + imageWidth - 5, topPos + 44, 0xFFAB94B8);
        for (var slot : menu.slots) {
            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFFF2E6F7);
            graphics.fill(x - 1, y - 1, x + 16, y + 16, 0xFF74627E);
            graphics.fill(x, y, x + 16, y + 16, 0xFFB7A6C1);
        }
    }
}
