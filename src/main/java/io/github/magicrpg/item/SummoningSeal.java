package io.github.magicrpg.item;

import io.github.magicrpg.MagicRpg;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;

public final class SummoningSeal extends Item {
    private final Supplier<? extends EntityType<? extends Mob>> summon;
    private final SimpleParticleType particles;
    private final SoundEvent sound;
    private final SoundSource soundSource;

    public SummoningSeal(Properties properties) {
        this(properties, MagicRpg.ASH_HOUND, ParticleTypes.LARGE_SMOKE,
            SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE);
    }

    public SummoningSeal(Properties properties, Supplier<? extends EntityType<? extends Mob>> summon,
                         SimpleParticleType particles, SoundEvent sound, SoundSource soundSource) {
        super(properties);
        this.summon = summon;
        this.particles = particles;
        this.sound = sound;
        this.soundSource = soundSource;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || context.getClickedFace() != Direction.UP) {
            return InteractionResult.PASS;
        }
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS_SERVER;
        }

        BlockPos surface = context.getClickedPos();
        BlockPos spawn = surface.above();
        EntityType<? extends Mob> type = summon.get();
        if (type.getCategory() == MobCategory.MONSTER && level.getDifficulty() == Difficulty.PEACEFUL) {
            return refuse(player, "peaceful");
        }
        if (!level.getBlockState(surface).isFaceSturdy(level, surface, Direction.UP)
            || !level.mayInteract(player, surface) || !level.mayInteract(player, spawn)
            || !player.mayUseItemAt(spawn, Direction.UP, context.getItemInHand())) {
            return refuse(player, "surface");
        }

        Mob mob = type.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (mob == null) {
            return refuse(player, "failed");
        }
        mob.snapTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, player.getYRot() + 180, 0);
        AABB space = mob.getBoundingBox();
        if (!level.isInWorldBounds(spawn) || space.maxY > level.getMaxY() + 1
            || !level.getWorldBorder().isWithinBounds(space) || !level.noCollision(mob, space)
            || level.containsAnyLiquid(space)
            || !level.getEntities(mob, space, entity -> entity.isAlive() && !entity.isSpectator() && entity.isPickable()).isEmpty()) {
            return refuse(player, "space");
        }

        mob.setPersistenceRequired();
        if (!level.addFreshEntity(mob)) {
            return refuse(player, "failed");
        }
        context.getItemInHand().consume(1, player);
        level.sendParticles(particles, mob.getX(), mob.getY() + 0.4, mob.getZ(), 14, 0.32, 0.28, 0.32, 0.025);
        level.playSound(null, spawn, sound, soundSource, 0.8F, 0.8F);
        return InteractionResult.SUCCESS_SERVER;
    }

    private InteractionResult refuse(Player player, String reason) {
        player.sendOverlayMessage(Component.translatable(getDescriptionId() + "." + reason));
        return InteractionResult.FAIL;
    }
}
