package io.github.magicrpg.item;

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
            if (!route.portal().activate(server, clicked, player)) {
                player.sendOverlayMessage(Component.translatable(route.invalidFrameMessage()));
                return InteractionResult.FAIL;
            }
            context.getItemInHand().consume(1, player);
            level.playSound(null, clicked, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1, 0.75F);
        }
        return InteractionResult.SUCCESS;
    }
}
