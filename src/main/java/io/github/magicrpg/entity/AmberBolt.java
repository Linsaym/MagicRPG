package io.github.magicrpg.entity;

import io.github.magicrpg.Goldenwood;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class AmberBolt extends ThrowableItemProjectile {
    public AmberBolt(EntityType<? extends AmberBolt> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static void fire(ServerLevel level, LivingEntity caster, LivingEntity target) {
        var bolt = new AmberBolt(Goldenwood.BOLT.get(), level);
        bolt.setOwner(caster);
        bolt.setPos(caster.getX(), caster.getEyeY(), caster.getZ());
        bolt.shoot(target.getX() - bolt.getX(), target.getY(0.67) - bolt.getY(),
            target.getZ() - bolt.getZ(), 0.76F, 0);
        level.addFreshEntity(bolt);
    }

    @Override
    protected Item getDefaultItem() {
        return Goldenwood.SPARK_SHARD.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            level().addParticle(ParticleTypes.WAX_ON, getX(), getY(), getZ(), 0, 0, 0);
        } else if (tickCount > 55) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (level() instanceof ServerLevel server) {
            hit.getEntity().hurtServer(server, damageSources().indirectMagic(this, getOwner()), 5);
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.WAX_ON, getX(), getY(), getZ(), 8, 0.15, 0.15, 0.15, 0.02);
            discard();
        }
    }
}
