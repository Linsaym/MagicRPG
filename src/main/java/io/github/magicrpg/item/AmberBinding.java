package io.github.magicrpg.item;

import io.github.magicrpg.MagicRpg;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class AmberBinding {
    private static final String BINDING_ARROW = "magicrpg.amber_binding_arrow";
    private static final int DURATION_TICKS = 60;
    private static final DustParticleOptions DUST = new DustParticleOptions(0xffb131, 0.85F);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
        DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MagicRpg.ID);
    private static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> BOUND_UNTIL =
        ATTACHMENTS.register("amber_bound_until",
            () -> AttachmentType.builder(() -> 0L).sync(ByteBufCodecs.VAR_LONG).build());

    private AmberBinding() {}

    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, AmberBinding::onImpact);
        NeoForge.EVENT_BUS.addListener(AmberBinding::onTeleport);
        NeoForge.EVENT_BUS.addListener(AmberBinding::onTick);
    }

    static void mark(Projectile projectile) {
        projectile.getPersistentData().putBoolean(BINDING_ARROW, true);
    }

    public static boolean isBound(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }
        Long until = entity.getExistingDataOrNull(BOUND_UNTIL);
        return until != null && entity.level().getGameTime() < until && living.isAlive();
    }

    private static void onImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (!(projectile.level() instanceof ServerLevel level)
            || !projectile.getPersistentData().getBooleanOr(BINDING_ARROW, false)
            || !(event.getRayTraceResult() instanceof EntityHitResult hit)) {
            return;
        }
        Entity hitEntity = hit.getEntity();
        Entity target = hitEntity instanceof EnderDragonPart part ? part.parentMob : hitEntity;
        if (!(target instanceof LivingEntity living) || !living.isAlive() || living.isSpectator()
            || living instanceof Player defender && projectile.getOwner() instanceof Player attacker
                && !attacker.canHarmPlayer(defender)) {
            return;
        }
        living.stopRiding();
        living.setDeltaMovement(Vec3.ZERO);
        living.setData(BOUND_UNTIL, level.getGameTime() + DURATION_TICKS);
    }

    private static void onTeleport(EntityTeleportEvent event) {
        if (isBound(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private static void onTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!entity.hasData(BOUND_UNTIL)) {
            return;
        }
        if (!isBound(entity)) {
            if (entity.level() instanceof ServerLevel) {
                entity.removeData(BOUND_UNTIL);
            }
            return;
        }
        entity.setDeltaMovement(Vec3.ZERO);
        entity.resetFallDistance();
        if (entity.level() instanceof ServerLevel level && entity.tickCount % 5 == 0) {
            double spread = entity.getBbWidth() * 0.4;
            level.sendParticles(DUST, entity.getX(), entity.getY() + 0.12, entity.getZ(), 6, spread, 0.12, spread, 0);
        }
    }
}
