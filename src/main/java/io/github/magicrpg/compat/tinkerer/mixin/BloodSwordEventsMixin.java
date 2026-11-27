package io.github.magicrpg.compat.tinkerer.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "thaumictinkerer.gear.BloodSwordEvents", remap = false)
abstract class BloodSwordEventsMixin {
    @Redirect(method = "onIncomingDamage", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/player/Player;isUsingItem()Z"))
    private static boolean magicrpg$skipLegacyBlock(Player player) {
        // This branch replaces the hit with magic damage and skips later damage events via a shared counter.
        // Leave the separate strike-cost branch in Tinkerer's handler active.
        return false;
    }
}
