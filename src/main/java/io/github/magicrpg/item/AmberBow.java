package io.github.magicrpg.item;

import io.github.magicrpg.Goldenwood;
import java.util.Comparator;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public final class AmberBow extends BowItem {
    private static final double RAIN_RANGE = 7;
    private static final int RAIN_COOLDOWN_TICKS = 20;
    private static final double RAIN_HEIGHT = 8;
    private static final double ARROW_SPACING = 0.65;
    private static final float ARROW_SPEED = 1.55F;
    private static final Map<ServerPlayer, Long> NEXT_RAIN_TICK = new WeakHashMap<>();

    public AmberBow(Properties properties) {
        super(properties);
    }

    @Override
    protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack ammo,
                                          boolean critical) {
        Projectile projectile = super.createProjectile(level, shooter, weapon, ammo, critical);
        AmberBinding.mark(projectile);
        return projectile;
    }

    public static void callRain(ServerPlayer player) {
        ServerLevel level = player.level();
        long gameTime = level.getGameTime();
        if (!player.isAlive() || player.isSpectator() || !player.getMainHandItem().is(Goldenwood.AMBER_BOW.get())
            || gameTime < NEXT_RAIN_TICK.getOrDefault(player, 0L)) {
            return;
        }
        var target = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(RAIN_RANGE),
                mob -> isEnemy(mob, player) && player.distanceToSqr(mob) <= RAIN_RANGE * RAIN_RANGE)
            .stream().min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
        if (target == null) {
            return;
        }

        NEXT_RAIN_TICK.put(player, gameTime + RAIN_COOLDOWN_TICKS);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT,
            SoundSource.PLAYERS, 1, 0.8F);
        double height = target.getBoundingBox().maxY + RAIN_HEIGHT;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                double arrowX = target.getX() + x * ARROW_SPACING;
                double arrowZ = target.getZ() + z * ARROW_SPACING;
                if (level.isLoaded(BlockPos.containing(arrowX, height, arrowZ))) {
                    var arrow = new Arrow(level, arrowX, height, arrowZ, new ItemStack(Items.ARROW), null);
                    arrow.setOwner(player);
                    arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
                    Projectile.spawnProjectileUsingShoot(arrow, level, ItemStack.EMPTY, 0, -1, 0, ARROW_SPEED, 0);
                }
            }
        }
    }

    private static boolean isEnemy(Mob mob, ServerPlayer player) {
        if (!mob.isAlive() || !mob.isPickable() || mob.isAlliedTo(player) || player.isAlliedTo(mob)) {
            return false;
        }
        if (mob.getTarget() == player) {
            return true;
        }
        if (mob instanceof NeutralMob neutral) {
            return neutral.isAngryAt(player, player.level());
        }
        return mob instanceof Enemy || mob.getType().getCategory() == MobCategory.MONSTER;
    }
}
