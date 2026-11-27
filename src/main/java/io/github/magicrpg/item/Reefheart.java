package io.github.magicrpg.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public final class Reefheart extends Item {
    private static final double RADIUS = 6;
    private static final int DURATION = 30 * 20;
    private static final int COOLDOWN = 60 * 20;

    public Reefheart(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS_SERVER;
        }
        var stack = player.getItemInHand(hand);
        if (player.isSpectator() || player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }
        var area = player.getBoundingBox().inflate(RADIUS);
        for (var diver : server.getEntitiesOfClass(ServerPlayer.class, area, candidate -> candidate.isAlive()
            && !candidate.isSpectator() && candidate.distanceToSqr(player) <= RADIUS * RADIUS)) {
            diver.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, DURATION), player);
            diver.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, DURATION), player);
            server.sendParticles(ParticleTypes.SPLASH, diver.getX(), diver.getY() + .9, diver.getZ(),
                12, .35, .55, .35, .08);
        }
        player.getCooldowns().addCooldown(stack, COOLDOWN);
        server.playSound(null, player.blockPosition(), SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, .85F, 1.2F);
        return InteractionResult.SUCCESS_SERVER;
    }
}
