package io.github.magicrpg.compat.reforged.client.mixin;

import io.github.magicrpg.MagicRpg;
import io.github.magicrpg.compat.reforged.ReforgedIntegration;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.client.platform.GuiBlit;
import thaumcraft.client.research.ResearchPageScreen;
import thaumcraft.research.ResearchPage;

@Mixin(value = ResearchPageScreen.class, remap = false)
abstract class ResearchIllustrationMixin {
    private static final int WIDTH = 124;
    private static final int HEIGHT = 100;

    // Reforged 1.1.3 recognizes image pages but leaves their render branch empty.
    @Inject(method = "drawPage", at = @At("HEAD"))
    private void magicrpg$drawIllustration(GuiGraphicsExtractor graphics, @Coerce Object displayPage,
                                           int side, int bookX, int bookY, CallbackInfo callback) {
        if (((ResearchDisplayPageAccessor) displayPage).magicrpg$getSource() instanceof ResearchPage.Image image) {
            Identifier texture = image.image();
            if (texture.getNamespace().equals(MagicRpg.ID)
                && texture.getPath().startsWith(ReforgedIntegration.ILLUSTRATION_PATH)) {
                int x = bookX + 54 + side * 152 - WIDTH / 2;
                int y = bookY + 90 - HEIGHT / 2;
                GuiBlit.blit(graphics, texture, x, y, WIDTH, HEIGHT, 0, 0,
                    WIDTH * 2, HEIGHT * 2, WIDTH * 2, HEIGHT * 2, -1);
            }
        }
    }
}
