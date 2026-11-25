package io.github.magicrpg.entity;

import io.github.magicrpg.MagicRpg;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;

public final class AncientMerchant extends WanderingTrader {
    public AncientMerchant(EntityType<? extends AncientMerchant> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    protected void updateTrades(ServerLevel level) {
        var offers = getOffers();
        offers.add(armor(6, 8, MagicRpg.LILAC_HELMET.toStack()));
        offers.add(armor(8, 10, MagicRpg.LILAC_CHESTPLATE.toStack()));
        offers.add(armor(6, 8, MagicRpg.LILAC_LEGGINGS.toStack()));
        offers.add(armor(4, 6, MagicRpg.LILAC_BOOTS.toStack()));
    }

    private static MerchantOffer armor(int hearts, int rivets, ItemStack armor) {
        return new MerchantOffer(new ItemCost(MagicRpg.CHALK_HEART.get(), hearts),
            Optional.of(new ItemCost(MagicRpg.ECHO_RIVET.get(), rivets)), armor, Integer.MAX_VALUE, 0, 0);
    }
}
