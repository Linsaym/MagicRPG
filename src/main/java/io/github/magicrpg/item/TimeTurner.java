package io.github.magicrpg.item;

import io.github.magicrpg.compat.reforged.TimeTurnerMagic;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class TimeTurner extends Item {
    private static final long DURATION_NANOS = TimeUnit.SECONDS.toNanos(10);
    private static final int SPEED_MULTIPLIER = 3;
    private static final int WARNING_COUNT = 5;
    private static final Map<MinecraftServer, Acceleration> ACTIVE = new WeakHashMap<>();

    public TimeTurner(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS_SERVER;
        }

        var server = serverLevel.getServer();
        var tickRate = server.tickRateManager();
        if (ACTIVE.containsKey(server) || tickRate.isFrozen() || tickRate.isSprinting()) {
            return refuse(player, "busy");
        }

        var wandHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (!TimeTurnerMagic.consumeVis(player.getItemInHand(wandHand))) {
            return refuse(player, "needs_vis");
        }

        float previousRate = tickRate.tickrate();
        float acceleratedRate = previousRate * SPEED_MULTIPLIER;
        tickRate.setTickRate(acceleratedRate);
        ACTIVE.put(server, new Acceleration(previousRate, acceleratedRate, System.nanoTime() + DURATION_NANOS));

        int warning = serverLevel.getRandom().nextInt(WARNING_COUNT) + 1;
        var message = Component.translatable("item.magicrpg.time_turner.warning." + warning, player.getName())
            .withStyle(ChatFormatting.DARK_PURPLE);
        server.getPlayerList().broadcastSystemMessage(message, false);
        return InteractionResult.SUCCESS_SERVER;
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        var acceleration = ACTIVE.get(server);
        if (acceleration == null) {
            return;
        }
        var tickRate = server.tickRateManager();
        if (tickRate.tickrate() != acceleration.acceleratedRate()) {
            ACTIVE.remove(server);
        } else if (System.nanoTime() - acceleration.endsAt() >= 0) {
            ACTIVE.remove(server);
            tickRate.setTickRate(acceleration.previousRate());
        }
    }

    private static InteractionResult refuse(Player player, String reason) {
        player.sendOverlayMessage(Component.translatable("item.magicrpg.time_turner." + reason));
        return InteractionResult.FAIL;
    }

    private record Acceleration(float previousRate, float acceleratedRate, long endsAt) {}
}
