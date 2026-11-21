package io.github.magicrpg.item;

import io.github.magicrpg.MagicRpg;
import io.github.magicrpg.entity.AshHound;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;

public final class SummoningSeal extends Item {
    public SummoningSeal(Properties properties) {
        super(properties);
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
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return refuse(player, "peaceful");
        }
        if (!level.getBlockState(surface).isFaceSturdy(level, surface, Direction.UP)
            || !level.mayInteract(player, surface) || !level.mayInteract(player, spawn)
            || !player.mayUseItemAt(spawn, Direction.UP, context.getItemInHand())) {
            return refuse(player, "surface");
        }

        AshHound hound = MagicRpg.ASH_HOUND.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (hound == null) {
            return refuse(player, "failed");
        }
        hound.snapTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, player.getYRot() + 180, 0);
        AABB space = hound.getBoundingBox();
        if (!level.isInWorldBounds(spawn) || space.maxY > level.getMaxY() + 1
            || !level.getWorldBorder().isWithinBounds(space) || !level.noCollision(hound, space)
            || level.containsAnyLiquid(space)
            || !level.getEntities(hound, space, entity -> entity.isAlive() && !entity.isSpectator() && entity.isPickable()).isEmpty()) {
            return refuse(player, "space");
        }

        hound.setPersistenceRequired();
        if (!level.addFreshEntity(hound)) {
            return refuse(player, "failed");
        }
        context.getItemInHand().consume(1, player);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, hound.getX(), hound.getY() + 0.4, hound.getZ(), 14, 0.32, 0.28, 0.32, 0.025);
        level.playSound(null, spawn, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 0.8F, 0.8F);
        return InteractionResult.SUCCESS_SERVER;
    }

    private InteractionResult refuse(Player player, String reason) {
        player.sendOverlayMessage(Component.translatable("item.magicrpg.ash_hound_seal." + reason));
        return InteractionResult.FAIL;
    }
}
