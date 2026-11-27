package io.github.magicrpg.item;

import io.github.magicrpg.world.LinkedPortalBlock.Activation;
import io.github.magicrpg.world.PortalRoute;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public final class CharredFang extends Item {
    public CharredFang(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var clicked = context.getClickedPos();
        var route = PortalRoute.forFrame(level.getBlockState(clicked));
        if (route == null || context.getPlayer() == null) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server && context.getPlayer() instanceof ServerPlayer player) {
            var activation = route.portal().activate(server, clicked, player);
            var message = switch (activation) {
                case OPENED -> null;
                case INVALID_FRAME -> route.invalidFrameMessage();
                case LANDING_FULL -> route.landingFullMessage();
                case ALREADY_OPEN -> getDescriptionId() + ".already_open";
                case OPENING_BLOCKED -> getDescriptionId() + ".opening_blocked";
                case PROTECTED -> getDescriptionId() + ".protected";
                case WRONG_RETURN_ARCH -> getDescriptionId() + ".wrong_return_arch";
                case DESTINATION_UNAVAILABLE -> getDescriptionId() + ".destination_unavailable";
            };
            if (message != null) {
                player.sendOverlayMessage(Component.translatable(message));
                return activation == Activation.ALREADY_OPEN ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
            }
            context.getItemInHand().consume(1, player);
            level.playSound(null, clicked, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1, 0.75F);
        }
        return InteractionResult.SUCCESS;
    }
}
