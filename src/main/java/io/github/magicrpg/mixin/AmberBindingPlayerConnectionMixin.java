package io.github.magicrpg.mixin;

import io.github.magicrpg.item.AmberBinding;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
abstract class AmberBindingPlayerConnectionMixin {
    @Shadow public ServerPlayer player;
    @Shadow private boolean clientIsFloating;

    @Inject(method = "tick", at = @At("HEAD"))
    private void magicrpg$allowAmberSuspension(CallbackInfo callback) {
        if (AmberBinding.isBound(player)) {
            clientIsFloating = false;
        }
    }
}
