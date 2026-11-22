package io.github.magicrpg.world;

import io.github.magicrpg.WayfarerMirrors;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class WayfarerMirrorBlock extends Block implements EntityBlock, Portal {
    private static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final Map<Direction, VoxelShape> FRAME = Shapes.rotateHorizontal(box(2, 1, 13, 14, 15, 16));
    private static final Map<Direction, VoxelShape> GLASS = Shapes.rotateHorizontal(box(4, 3, 13, 12, 13, 15));

    public WayfarerMirrorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public WayfarerMirrorEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WayfarerMirrorEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FRAME.get(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getEntityInsideCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                                      Entity entity) {
        return GLASS.get(state.getValue(FACING));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        var facing = context.getClickedFace();
        if (!facing.getAxis().isHorizontal()) {
            return null;
        }
        var pos = context.getClickedPos();
        var state = defaultBlockState().setValue(FACING, facing);
        if (!state.canSurvive(context.getLevel(), pos)) {
            return null;
        }
        var pair = context.getItemInHand().get(WayfarerMirrors.PAIR.get());
        if (pair != null && context.getLevel() instanceof ServerLevel level
            && !WayfarerMirrorLinks.get(level).canPlace(level, pair, WayfarerMirrorLinks.Endpoint.at(level, pos))) {
            return null;
        }
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        // BlockItem has copied the stack components into the block entity by this point.
        if (level instanceof ServerLevel server
            && server.getBlockEntity(pos) instanceof WayfarerMirrorEntity mirror) {
            var pair = mirror.pair();
            if (pair != null) {
                WayfarerMirrorLinks.get(server).add(pair, WayfarerMirrorLinks.Endpoint.at(server, pos));
            }
        }
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        var stack = new ItemStack(this);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof WayfarerMirrorEntity mirror
            && mirror.pair() != null) {
            stack.set(WayfarerMirrors.PAIR.get(), mirror.pair());
        }
        return List.of(stack);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        var facing = state.getValue(FACING);
        var wall = pos.relative(facing.getOpposite());
        return level.getBlockState(wall).isFaceSturdy(level, wall, facing);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        if (direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)) {
            ticks.scheduleTick(pos, this, 1);
        }
        return state;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                InsideBlockEffectApplier effects, boolean intersects) {
        if (!intersects || !(level instanceof ServerLevel server) || !(entity instanceof ServerPlayer player)
            || player.isSpectator() || player.isOnPortalCooldown() || !player.canUsePortal(false)) {
            return;
        }
        var facing = state.getValue(FACING);
        var offset = player.position().subtract(Vec3.atCenterOf(pos));
        double front = offset.x * facing.getStepX() + offset.z * facing.getStepZ();
        double side = offset.x * facing.getStepZ() - offset.z * facing.getStepX();
        // Vanilla starts its portal cooldown before asking for the destination.
        if (front > -0.375 && Math.abs(side) <= 0.25 && getPortalDestination(server, player, pos) != null) {
            player.setAsInsidePortal(this, pos);
        }
    }

    @Override
    public @Nullable TeleportTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
        if (!(entity instanceof ServerPlayer player)
            || !(level.getBlockEntity(pos) instanceof WayfarerMirrorEntity mirror) || mirror.pair() == null
            || !mirror.getBlockState().canSurvive(level, pos)) {
            return null;
        }
        var pair = mirror.pair();
        var other = WayfarerMirrorLinks.get(level).other(pair, WayfarerMirrorLinks.Endpoint.at(level, pos));
        var target = other == null ? null : level.getServer().getLevel(other.dimension());
        if (target == null) {
            return null;
        }
        var targetPos = other.pos();
        target.getChunkAt(targetPos);
        if (!(target.getBlockEntity(targetPos) instanceof WayfarerMirrorEntity exitMirror)
            || !pair.equals(exitMirror.pair()) || !exitMirror.getBlockState().canSurvive(target, targetPos)) {
            return null;
        }
        var facing = exitMirror.getBlockState().getValue(FACING);
        var front = targetPos.relative(facing);
        for (var exit : List.of(front, front.below())) {
            if (PortalLinks.isSafe(target, exit, player) && isClearOfMirrors(target, exit)
                && isClearOfMirrors(target, exit.above())) {
                return new TeleportTransition(target, Vec3.atBottomCenterOf(exit), Vec3.ZERO, facing.toYRot(), 0,
                    TeleportTransition.PLACE_PORTAL_TICKET.then(arrived -> arrived.setPortalCooldown(10)));
            }
        }
        return null;
    }

    private static boolean isClearOfMirrors(ServerLevel level, BlockPos pos) {
        return !(level.getBlockState(pos).getBlock() instanceof WayfarerMirrorBlock);
    }
}
