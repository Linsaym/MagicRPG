package io.github.magicrpg.mixin;

import io.github.magicrpg.item.AmberBinding;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
abstract class AmberBindingMovementMixin {
    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void magicrpg$holdMovement(MoverType type, Vec3 movement, CallbackInfo callback) {
        Entity entity = (Entity) (Object) this;
        if (AmberBinding.isBound(entity) && magicrpg$controlsMovement(entity)) {
            callback.cancel();
        }
    }

    @Inject(method = "setPosRaw", at = @At("HEAD"), cancellable = true)
    private void magicrpg$holdPosition(double x, double y, double z, CallbackInfo callback) {
        Entity entity = (Entity) (Object) this;
        // Handle movement that bypasses Entity.move, including serverbound player movement.
        // Remote clients still accept authoritative position updates and interpolation.
        if (AmberBinding.isBound(entity) && magicrpg$controlsMovement(entity)) {
            callback.cancel();
        }
    }

    @Unique
    private static boolean magicrpg$controlsMovement(Entity entity) {
        return !entity.level().isClientSide() || entity instanceof Player player && player.isLocalPlayer();
    }
}
