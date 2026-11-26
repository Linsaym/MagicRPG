package io.github.magicrpg.boss;

import io.github.magicrpg.boss.animation.AnimationCue;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

public abstract class BossMob extends PathfinderMob {
    private static final EntityDataAccessor<Long> ANIMATION = SynchedEntityData.defineId(
        BossMob.class, EntityDataSerializers.LONG);

    protected BossMob(EntityType<? extends BossMob> type, Level level) {
        super(type, level);
    }

    protected abstract BossFight<?> bossFight();

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ANIMATION, 0L);
    }

    public final AnimationCue animationCue() {
        return AnimationCue.decode(entityData.get(ANIMATION));
    }

    final void playAnimation(int clip, long now) {
        entityData.set(ANIMATION, new AnimationCue(clip, (int) now).encode());
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server && isAlive()) {
            bossFight().tick(server);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            amount *= bossFight().damageMultiplier(level.getGameTime());
        }
        boolean hurt = amount > 0 && super.hurtServer(level, source, amount);
        if (hurt && isAlive()) {
            bossFight().onHurt(source, amount);
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        endFight();
    }

    @Override
    public void onRemoval(RemovalReason reason) {
        super.onRemoval(reason);
        endFight();
    }

    private void endFight() {
        if (level() instanceof ServerLevel level) {
            bossFight().reset(level);
        }
    }
}
