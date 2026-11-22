package io.github.magicrpg.compat.reforged.client.mixin;

import io.github.magicrpg.compat.reforged.ReforgedIntegration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import thaumcraft.client.research.ResearchBrowserScreen;

@Mixin(value = ResearchBrowserScreen.class, remap = false)
abstract class ResearchBackgroundMixin {
    @Shadow
    private static String selectedCategory;

    @ModifyArgs(method = "drawMap", at = @At(value = "INVOKE", ordinal = 0,
        target = "Lthaumcraft/client/platform/GuiBlit;blit("
            + "Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/resources/Identifier;IIIIIIIIIII)V"))
    private void magicrpg$centerBeastsBackground(Args args) {
        if (ReforgedIntegration.BEASTS_CATEGORY.equals(selectedCategory)) {
            // Sample the center of the parchment while the research map pans over it.
            int regionWidth = args.get(8);
            int regionHeight = args.get(9);
            int textureWidth = args.get(10);
            int textureHeight = args.get(11);
            args.set(6, (textureWidth - regionWidth) / 2);
            args.set(7, (textureHeight - regionHeight) / 2);
        }
    }
}
