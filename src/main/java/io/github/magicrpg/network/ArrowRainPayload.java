package io.github.magicrpg.network;

import io.github.magicrpg.MagicRpg;
import io.github.magicrpg.item.AmberBow;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public enum ArrowRainPayload implements CustomPacketPayload {
    INSTANCE;

    private static final Type<ArrowRainPayload> TYPE = new Type<>(MagicRpg.id("arrow_rain"));

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, StreamCodec.unit(INSTANCE), (request, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                AmberBow.callRain(player);
            }
        });
    }

    @Override
    public Type<ArrowRainPayload> type() {
        return TYPE;
    }
}
