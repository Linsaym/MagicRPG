package io.github.magicrpg.compat.reforged.client.mixin;

import java.util.List;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.item.PrimalCharmItem;

@Mixin(targets = "thaumcraft.client.research.PageRenderer", remap = false)
abstract class ResearchPageTooltipMixin {
    @Shadow @Nullable List<Component> tooltip;
    @Shadow ItemStack tooltipStack;

    @Unique private @Nullable ScreenPosition magicrpg$hoveredCharmSlot;
    @Unique private @Nullable ScreenPosition magicrpg$previousCharmSlot;
    @Unique private @Nullable Component magicrpg$charmFlavor;
    @Unique private int magicrpg$charmFlavorIndex;

    @Inject(method = "hover", at = @At("RETURN"))
    private void magicrpg$rememberCharmSlot(ItemStack stack, int x, int y, boolean link, CallbackInfo callback) {
        if (stack.getItem() instanceof PrimalCharmItem) {
            magicrpg$hoveredCharmSlot = new ScreenPosition(x, y);
        }
    }

    @Inject(method = "flushTooltip", at = @At("HEAD"))
    private void magicrpg$stabilizeCharmFlavor(CallbackInfo callback) {
        ScreenPosition slot = magicrpg$hoveredCharmSlot;
        magicrpg$hoveredCharmSlot = null;
        if (slot == null || tooltip == null || !(tooltipStack.getItem() instanceof PrimalCharmItem)) {
            magicrpg$previousCharmSlot = null;
            magicrpg$charmFlavor = null;
            return;
        }

        int flavorIndex = -1;
        for (int index = 0; index < tooltip.size(); index++) {
            if (tooltip.get(index).getContents() instanceof TranslatableContents contents
                && contents.getKey().startsWith("tc.primalcharm.")) {
                flavorIndex = index;
                break;
            }
        }

        // Reforged rolls this line again while rendering. Keep the first roll for this hover.
        if (!slot.equals(magicrpg$previousCharmSlot)) {
            magicrpg$previousCharmSlot = slot;
            magicrpg$charmFlavorIndex = flavorIndex;
            magicrpg$charmFlavor = flavorIndex < 0 ? null : tooltip.get(flavorIndex);
            return;
        }

        if (flavorIndex >= 0) {
            tooltip.remove(flavorIndex);
        }
        if (magicrpg$charmFlavor != null) {
            tooltip.add(Math.min(magicrpg$charmFlavorIndex, tooltip.size()), magicrpg$charmFlavor);
        }
    }
}
