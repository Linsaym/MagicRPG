package io.github.magicrpg.compat.reforged;

import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.registry.ModItems;
import thaumcraft.registry.ModSounds;
import thaumcraft.research.Knowledge;
import thaumcraft.research.ScanKind;
import thaumcraft.research.ScanManager;

public final class ReforgedScanning {
    private ReforgedScanning() {}

    public static boolean isThaumometer(ItemStack stack) {
        return stack.is(ModItems.THAUMOMETER.get());
    }

    public static boolean hasScanned(Player player, ItemStack stack) {
        return Knowledge.get(player).isScanned(ScanKind.OBJECT, ScanManager.itemKey(stack));
    }

    public static boolean canScan(Player player, ItemStack stack) {
        var target = ScanManager.resolveStack(stack);
        return target != null && ScanManager.validScan(target.aspects(), player);
    }

    public static void scanInventoryStack(ServerPlayer player, ItemStack stack) {
        var target = ScanManager.resolveStack(stack);
        if (target != null && !ScanManager.hasBeenScanned(player, target) && ScanManager.completeScan(player, target)) {
            player.connection.send(new ClientboundSoundPacket(ModSounds.CAMERACLACK, SoundSource.PLAYERS,
                player.getX(), player.getY(), player.getZ(), 0.45F, 1, player.getRandom().nextLong()));
        }
    }
}
