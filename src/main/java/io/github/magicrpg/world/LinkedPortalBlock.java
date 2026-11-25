package io.github.magicrpg.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class LinkedPortalBlock extends Block implements Portal {
    static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final VoxelShape X_SHAPE = box(0, 0, 6, 16, 16, 10);
    private static final VoxelShape Z_SHAPE = box(6, 0, 0, 10, 16, 16);

    private final PortalRoute route;

    public LinkedPortalBlock(Properties properties, PortalRoute route) {
        super(properties);
        this.route = route;
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    public enum Activation {
        OPENED, INVALID_FRAME, LANDING_FULL
    }

    public Activation activate(ServerLevel level, BlockPos clicked, ServerPlayer player) {
        if (level.dimension().equals(route.dimension()) || level.getServer().getLevel(route.dimension()) == null
            || !level.mayInteract(player, clicked)) {
            return Activation.INVALID_FRAME;
        }
        for (var axis : PortalFrame.AXES) {
            for (var offset : PortalFrame.BORDER) {
                var frame = new PortalFrame(clicked.relative(PortalFrame.along(axis), -offset.getX())
                    .below(offset.getY()), axis);
                if (frame.hasBricks(level, route) && frame.interior().stream()
                    .allMatch(pos -> level.isEmptyBlock(pos) && level.mayInteract(player, pos))) {
                    if (!PortalLinks.prepareIslandArch(level, frame, player, route)) {
                        return Activation.LANDING_FULL;
                    }
                    frame.open(level, route);
                    return Activation.OPENED;
                }
            }
        }
        return Activation.INVALID_FRAME;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.X ? X_SHAPE : Z_SHAPE;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                InsideBlockEffectApplier effects, boolean intersects) {
        if (entity instanceof ServerPlayer player && player.canUsePortal(false)) {
            player.setAsInsidePortal(this, pos);
        }
    }

    @Override
    public @Nullable TeleportTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
        var state = level.getBlockState(pos);
        if (!(entity instanceof ServerPlayer player) || !state.is(this)) {
            return null;
        }
        var frame = PortalFrame.fromPortal(level, pos, state.getValue(AXIS), route);
        return frame.isOpen(level, route) ? PortalLinks.destination(level, player, frame, route) : null;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        ticks.scheduleTick(pos, this, 1);
        return state;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        var frame = PortalFrame.fromPortal(level, pos, state.getValue(AXIS), route);
        if (!frame.isOpen(level, route)) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        } else if (pos.equals(frame.base())) {
            // Corner bricks do not touch the portal blocks and cannot notify them directly.
            level.scheduleTick(pos, this, 20);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            var particle = switch (route) {
                case GOLDENWOOD -> ParticleTypes.WAX_ON;
                case MEADOWS -> ParticleTypes.END_ROD;
                case IRON_WASTELANDS -> ParticleTypes.ASH;
            };
            level.addParticle(particle, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                pos.getZ() + random.nextDouble(), 0, 0.025, 0);
        }
    }
}
