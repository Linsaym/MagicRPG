package io.github.magicrpg.compat.reforged.mixin;

import io.github.magicrpg.boss.GroundedEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import thaumcraft.item.armor.Hover;

@Mixin(value = Hover.class, remap = false)
abstract class HoverGroundingMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE",
        target = "Lthaumcraft/item/armor/Hover;isHovering(Lnet/minecraft/world/item/ItemStack;)Z"))
    private static boolean magicrpg$canHover(ItemStack harness, Player player) {
        if (GroundedEffect.blocksFlight(player)) {
            if (player.level().isClientSide()) {
                player.getAbilities().flying = false;
            }
            return false;
        }
        return Hover.isHovering(harness);
    }
}
