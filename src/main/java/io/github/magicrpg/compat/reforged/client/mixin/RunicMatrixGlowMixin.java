package io.github.magicrpg.compat.reforged.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.client.geom.QuadSink;
import thaumcraft.client.render.RunicMatrixRendering;

@Mixin(value = RunicMatrixRendering.class, remap = false)
abstract class RunicMatrixGlowMixin {
    @Redirect(method = "lambda$submit$0", at = @At(value = "INVOKE",
        target = "Lthaumcraft/client/geom/InfusionHaloGeometry;render(Lthaumcraft/client/geom/QuadSink;IZFFI)V"))
    private static void magicrpg$skipInfusionRays(QuadSink sink, int craftCount, boolean large,
                                                float u, float v, int light) {
        // The halo renderer draws the white rays during infusion.
    }

    @Redirect(method = "submit", at = @At(value = "INVOKE", ordinal = 6,
        target = "Lnet/minecraft/util/Mth;sin(D)F"))
    private static float magicrpg$slowGlowWave(double originalPhase, SubmitNodeCollector collector, PoseStack poseStack,
                                             int light, boolean animated, boolean active, boolean crafting,
                                             float startUp, int instability, int craftCount, float ticks) {
        // The first six sine calls position the cubes. The last one controls their glow.
        double cubePhase = originalPhase - ticks * 0.25;
        double wavePhase = ticks * (2.0 * Math.PI / 100.0);
        return (float) Math.sin(wavePhase + cubePhase);
    }

    @Inject(method = "submit", at = @At(value = "INVOKE", ordinal = 1, shift = At.Shift.AFTER,
        target = "Lthaumcraft/client/render/RunicMatrixRendering;cube(Lcom/mojang/blaze3d/vertex/PoseStack;IIIFFF)V"))
    private static void magicrpg$offsetGlowLayer(SubmitNodeCollector collector, PoseStack poseStack, int light,
                                                boolean animated, boolean active, boolean crafting, float startUp,
                                                int instability, int craftCount, float ticks, CallbackInfo callback) {
        // Keep the emissive faces slightly ahead of the opaque faces in the depth buffer.
        poseStack.scale(1.008F, 1.008F, 1.008F);
    }
}
