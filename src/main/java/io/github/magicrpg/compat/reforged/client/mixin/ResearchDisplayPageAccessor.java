package io.github.magicrpg.compat.reforged.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import thaumcraft.research.ResearchPage;

@Mixin(targets = "thaumcraft.client.research.ResearchPageScreen$DisplayPage", remap = false)
public interface ResearchDisplayPageAccessor {
    @Accessor("source")
    ResearchPage magicrpg$getSource();
}
