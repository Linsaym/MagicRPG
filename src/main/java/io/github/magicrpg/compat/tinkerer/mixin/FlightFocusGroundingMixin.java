package io.github.magicrpg.compat.tinkerer.mixin;

import io.github.magicrpg.boss.GroundedEffect;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "thaumictinkerer.foci.FlightFocus", remap = false)
abstract class FlightFocusGroundingMixin {
    @Inject(method = "onFocusRightClick", at = @At("HEAD"), cancellable = true)
    private void magicrpg$preventFlight(ItemStack wand, Level level, Player player, HitResult hit,
                                       CallbackInfoReturnable<InteractionResult> callback) {
        if (GroundedEffect.blocksFlight(player)) {
            callback.setReturnValue(InteractionResult.FAIL);
        }
    }
}
