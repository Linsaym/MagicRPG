package io.github.magicrpg.compat.reforged.client.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import thaumcraft.aspect.Aspect;
import thaumcraft.client.screen.ResearchTableScreen;
import thaumcraft.menu.ResearchTableMenu;
import thaumcraft.research.HexGrid;
import thaumcraft.research.PlayerKnowledge;
import thaumcraft.research.ResearchNote;

@Mixin(value = ResearchTableScreen.class, remap = false)
abstract class ResearchTableTooltipMixin extends AbstractContainerScreen<ResearchTableMenu> {
    @Shadow @Final private static int HEX_SIZE;
    @Shadow @Final private static int GRID_X;
    @Shadow @Final private static int GRID_Y;

    protected ResearchTableTooltipMixin(ResearchTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Shadow
    private ResearchNote note() {
        throw new AssertionError();
    }

    @Shadow
    private PlayerKnowledge knowledge() {
        throw new AssertionError();
    }

    // Seed Reforged's hovered aspect before it checks the palette and renders the tooltip.
    @ModifyVariable(method = "drawAspectTooltips", at = @At(value = "INVOKE",
        target = "Lthaumcraft/client/screen/ResearchTableScreen;visibleAspects()Ljava/util/List;"), ordinal = 0)
    private Aspect magicrpg$findHoveredGridAspect(Aspect hovered, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ResearchNote note = note();
        if (note == null || note.key().isEmpty()) {
            return hovered;
        }

        HexGrid.Cell cell = HexGrid.fromPixel(mouseX - (leftPos + GRID_X), mouseY - (topPos + GRID_Y), HEX_SIZE);
        for (ResearchNote.Hex hex : note.hexes()) {
            if (hex.q() == cell.q() && hex.r() == cell.r()) {
                Aspect aspect = hex.aspect().orElse(null);
                return aspect != null && knowledge().hasDiscoveredAspect(aspect) ? aspect : hovered;
            }
        }
        return hovered;
    }

    @ModifyArgs(method = "drawAspectTooltips", at = @At(value = "INVOKE",
        target = "Lthaumcraft/client/aspect/AspectRenderer;drawBack(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"))
    private void magicrpg$centerComponentBackground(Args args) {
        int x = args.get(1);
        int y = args.get(2);
        // drawBack already expands two pixels on each side of the 16-pixel icon.
        args.set(1, x + 2);
        args.set(2, y + 2);
    }
}
