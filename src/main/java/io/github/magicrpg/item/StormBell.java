package io.github.magicrpg.item;

import io.github.magicrpg.StormReefs;
import io.github.magicrpg.boss.BossRespawns;
import io.github.magicrpg.entity.StormPilot;
import io.github.magicrpg.world.PortalRoute;
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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

public final class StormBell extends Item {
    public StormBell(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var clicked = context.getClickedPos();
        var player = context.getPlayer();
        if (player == null || !level.getBlockState(clicked).is(Blocks.LODESTONE)) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS_SERVER;
        }
        var perch = StormReefs.PILOT_PERCH;
        if (!server.dimension().equals(PortalRoute.STORM_REEFS.dimension()) || !clicked.equals(StormReefs.LODESTONE)) {
            return refuse(player, "altar");
        }
        if (server.getDifficulty() == Difficulty.PEACEFUL) {
            return refuse(player, "peaceful");
        }
        if (!server.mayInteract(player, clicked) || !server.mayInteract(player, perch)
            || !player.mayUseItemAt(clicked, context.getClickedFace(), context.getItemInHand())
            || !server.areEntitiesLoaded(ChunkPos.pack(perch))) {
            return refuse(player, "blocked");
        }
        if (!server.getEntitiesOfClass(StormPilot.class, new AABB(perch).inflate(2), pilot -> !pilot.isRemoved()).isEmpty()) {
            return refuse(player, "busy");
        }
        if (BossRespawns.isPending(server, StormReefs.STORM_PILOT.get(), perch)) {
            return refuse(player, "returning");
        }

        var pilot = StormReefs.STORM_PILOT.get().create(server, EntitySpawnReason.MOB_SUMMONED);
        if (pilot == null) {
            return refuse(player, "blocked");
        }
        pilot.anchorAt(perch);
        var space = pilot.getBoundingBox();
        var floor = perch.below();
        if (!server.getWorldBorder().isWithinBounds(space) || !server.noCollision(pilot, space)
            || server.containsAnyLiquid(space) || !server.getBlockState(floor).isFaceSturdy(server, floor, Direction.UP)
            || !server.getEntities(pilot, space, entity -> entity.isAlive() && !entity.isSpectator()
                && entity.isPickable()).isEmpty() || !server.addFreshEntity(pilot)) {
            return refuse(player, "blocked");
        }

        context.getItemInHand().consume(1, player);
        server.playSound(null, clicked, SoundEvents.BELL_RESONATE, SoundSource.BLOCKS, 1, .7F);
        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, pilot.getX(), pilot.getY() + 2, pilot.getZ(),
            30, .7, .7, .7, .12);
        return InteractionResult.SUCCESS_SERVER;
    }

    private InteractionResult refuse(Player player, String reason) {
        player.sendOverlayMessage(Component.translatable(getDescriptionId() + "." + reason));
        return InteractionResult.FAIL;
    }
}
