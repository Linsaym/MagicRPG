package io.github.magicrpg.boss;

import io.github.magicrpg.MagicRpg;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class GroundedEffect extends MobEffect {
    static final int REFRESH_TICKS = 10;
    private static final int DURATION = 30;
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, MagicRpg.ID);
    public static final DeferredHolder<MobEffect, MobEffect> GROUNDED = EFFECTS.register("grounded", GroundedEffect::new);

    private GroundedEffect() {
        super(MobEffectCategory.HARMFUL, 0x955443);
        addAttributeModifier(NeoForgeMod.CREATIVE_FLIGHT, MagicRpg.id("grounded"), -1,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(NeoForgeMod.GLIDING_FLIGHT, MagicRpg.id("grounded"), -1,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }

    public static boolean blocksFlight(Player player) {
        return !player.isCreative() && !player.isSpectator() && player.hasEffect(GROUNDED);
    }

    static void apply(ServerPlayer player, BossMob boss) {
        player.addEffect(new MobEffectInstance(GROUNDED, DURATION, 0, false, false, true), boss);
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        if (entity instanceof ServerPlayer player && blocksFlight(player)) {
            stopFlight(player);
        }
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (entity instanceof ServerPlayer player) {
            if (!blocksFlight(player)) {
                return false;
            }
            stopFlight(player);
        }
        return true;
    }

    private static void stopFlight(ServerPlayer player) {
        if (player.isFallFlying()) {
            player.stopFallFlying();
        }
        var abilities = player.getAbilities();
        if (abilities.flying) {
            abilities.flying = false;
            player.onUpdateAbilities();
        }
    }
}
