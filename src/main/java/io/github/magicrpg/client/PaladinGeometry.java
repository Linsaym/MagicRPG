package io.github.magicrpg.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

final class PaladinGeometry {
    static final float SOLES_Y = 24, HIPS_Y = -12, CHEST_Y = -21;
    static final float BOOT_STAGGER = 2.5F;
    static final float GRIP_Y = 7, GRIP_Z = -14, GRIP_PITCH = -2.28F;
    static final float SWORD_POINT_Y = 49;
    static final Vec3 RIGHT_HAND = new Vec3(0, 3, 0);
    static final Vec3 LEFT_HAND = new Vec3(0, -4, 0);

    private static final Texture IVORY = new Texture(0, 0);
    private static final Texture RECESSES = new Texture(128, 0);
    private static final Texture BRONZE = new Texture(256, 0);
    private static final Texture MAIL = new Texture(384, 0);
    private static final Texture CLOTH = new Texture(0, 128);
    private static final Texture STEEL = new Texture(128, 128);
    private static final Texture BLADE_DARK = new Texture(256, 128);
    private static final Texture LEATHER = new Texture(384, 128);
    private static final Texture CLOTH_FOLD = new Texture(0, 256);
    private static final Texture VIOLET = new Texture(128, 256);

    private static final Texture VISOR_PLATE = new Texture(256, 256);
    private static final Texture CORE_FACETS = new Texture(300, 256);
    private static final Texture VISOR_SLIT = new Texture(324, 256);
    private static final Texture BLADE_FULLER = new Texture(352, 256);
    private static final Texture FINGERS = new Texture(376, 256);
    private static final Texture TASSET = new Texture(408, 256);
    private static final Texture TASSET_TOP = new Texture(432, 256);
    private static final Texture BRACER = new Texture(456, 256);
    private static final Texture SHOULDER_TOP = new Texture(256, 304);
    private static final Texture SHOULDER_MID = new Texture(316, 304);
    private static final Texture SHOULDER_LOW = new Texture(372, 304);

    private PaladinGeometry() {}

    static LayerDefinition createLayer() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        var waist = box(MAIL, -9, -5, -5, 18, 10, 10);
        box(waist, RECESSES, -10, -3.5F, -5.8F, 20, 3, 11.6F);
        box(waist, CLOTH_FOLD, -5.25F, -.5F, -5.2F, 10.5F, 14, 1.2F);
        var hips = root.addOrReplaceChild("hips", waist, PartPose.offset(0, HIPS_Y, 0));

        var chest = addCuirass(hips);
        addHelmet(chest);
        addLimbs(root, chest, -1, "right");
        addLimbs(root, chest, 1, "left");
        addCape(chest);
        addBlade(chest);
        return LayerDefinition.create(mesh, 512, 384);
    }

    private static PartDefinition addCuirass(PartDefinition hips) {
        var plates = box(MAIL, -9, -6.75F, -4.75F, 18, 27.75F, 10.5F);
        box(plates, IVORY, -9.5F, -7, -5, 19, 20, 11);
        box(plates, IVORY, -9.5F, -6.7F, -8.1F, 19, 7.6F, 4.2F);
        box(plates, IVORY, -6.6F, .9F, -8.1F, 13.2F, 14.15F, 4.2F);
        box(plates, IVORY, -6.4F, 14.5F, -7.3F, 12.8F, 5.8F, 7.3F);
        box(plates, RECESSES, -6.55F, 19.6F, -6.85F, 13.1F, 2, 12.2F);
        box(plates, RECESSES, -5.5F, -9.3F, -4.8F, 11, 3.2F, 9.6F);
        box(plates, BRONZE, -4.2F, -7.8F, -8.25F, 8.4F, 1.55F, 3.65F);
        var chest = hips.addOrReplaceChild("chest", plates, PartPose.offset(0, CHEST_Y, 0));

        for (int side : new int[]{-1, 1}) {
            chest.addOrReplaceChild("flank_" + side, box(IVORY, -2, 0, -1.65F, 4, 13, 3.7F),
                PartPose.offsetAndRotation(side * 8.55F, -4.7F, -6.25F, .05F, -side * .32F, side * .31F));
            chest.addOrReplaceChild("collar_" + side, box(BRONZE, -2.4F, -.8F, -1.2F, 4.8F, 1.65F, 3.5F),
                PartPose.offsetAndRotation(side * 5.9F, -7.7F, -6.45F, 0, -side * .36F, -side * .21F));
        }

        var socket = box(BRONZE, -3.25F, -3.25F, -.9F, 6.5F, 6.5F, 1.5F);
        box(socket, RECESSES, -2.65F, -2.65F, -1.1F, 5.3F, 5.3F, .5F);
        var frame = chest.addOrReplaceChild("core_socket", socket,
            PartPose.offsetAndRotation(0, -2.6F, -8.25F, 0, 0, Mth.PI / 4));
        frame.addOrReplaceChild("core", box(CORE_FACETS, -4, -4, -2, 8, 8, 2),
            PartPose.ZERO.withScale(.56F).translated(0, 0, -1.08F));
        return chest;
    }

    private static void addHelmet(PartDefinition chest) {
        var helm = box(IVORY, -7.15F, -13.35F, -4.5F, 14.3F, 13.3F, 11);
        box(helm, RECESSES, -5.9F, -10.6F, -6.85F, 11.8F, 5.2F, 3.4F);
        box(helm, IVORY, -7.4F, -13.4F, -7, 14.8F, 3.7F, 10);
        box(helm, VISOR_PLATE, -6, -7.4F, -7.2F, 12, 7, 7);
        box(helm, IVORY, -3.9F, -.52F, -7.18F, 7.8F, 1.1F, 6.85F);
        box(helm, VISOR_SLIT, -5.5F, -8.85F, -6.98F, 11, 1, 1);
        for (int side : new int[]{-1, 1}) {
            box(helm, IVORY, side * 6.55F - 1.2F, -10, -6.45F, 2.4F, 9.15F, 10.65F);
        }
        box(helm, BRONZE, -7, -12.05F, -7.45F, 14, 1.45F, 2);
        box(helm, BRONZE, -1.15F, -12.8F, -7.75F, 2.3F, 2.55F, 2.4F);
        box(helm, IVORY, -6.35F, -15.1F, -6.25F, 12.7F, 2.4F, 11.7F);
        box(helm, IVORY, -4.3F, -17.2F, -5.95F, 8.6F, 2.25F, 9.4F);
        box(helm, IVORY, -2.2F, -19.2F, -5.75F, 4.4F, 2.2F, 7.5F);
        box(helm, VIOLET, -1.9F, -20.4F, 1.9F, 3.8F, 7.1F, 3.3F);
        chest.addOrReplaceChild("helm", helm, PartPose.offset(0, -9, 0));
    }

    private static void addShoulder(PartDefinition chest, int side, String name) {
        var lining = box(RECESSES, -5.1F, -.75F, -4.65F, 10.2F, 10.9F, 9.9F);
        var shoulder = chest.addOrReplaceChild(name + "_pauldron", lining,
            PartPose.offsetAndRotation(side * 13.8F, -5, .2F, 0, 0, side * .25F));
        shoulder.addOrReplaceChild("upper_plate", box(SHOULDER_TOP, -6.5F, -2, -6.5F, 13, 5, 13),
            PartPose.offsetAndRotation(side * .6F, -1.25F, -.9F, .27F, 0, 0));
        shoulder.addOrReplaceChild("ridge", box(IVORY, -2.45F, -1.4F, -3, 4.9F, 2.7F, 6.2F),
            PartPose.offsetAndRotation(-side * 1.6F, -3.65F, -1.1F, .27F, 0, 0));
        shoulder.addOrReplaceChild("middle_plate", box(SHOULDER_MID, -6, -1.75F, -6, 12, 4, 12),
            PartPose.offsetAndRotation(side * 1.45F, 3.2F, -1.85F, .23F, 0, 0));
        shoulder.addOrReplaceChild("lower_plate", box(SHOULDER_LOW, -5, -1.6F, -5.5F, 10, 4, 11),
            PartPose.offsetAndRotation(side * 1.8F, 7.1F, -2.5F, .18F, 0, 0));
    }

    private static void addLimbs(PartDefinition root, PartDefinition chest, int side, String name) {
        addShoulder(chest, side, name);
        var upper = box(MAIL, -3.55F, 0, -3.65F, 7.1F, 15, 7.3F);
        box(upper, RECESSES, -3.8F, 9.8F, -3.85F, 7.6F, 4.4F, 7.7F);
        chest.addOrReplaceChild(name + "_upper_arm", upper, PartPose.offset(side * 13, -4, 0));
        var forearm = box(MAIL, -3.1F, -2.1F, -3.15F, 6.2F, 16.6F, 6.3F);
        box(forearm, BRACER, -4.5F, 2, -4.25F, 9, 9, 8);
        box(forearm, IVORY, -4.05F, 10.85F, -3.9F, 8.1F, 2.8F, 7.6F);
        chest.addOrReplaceChild(name + "_forearm", forearm, PartPose.ZERO);

        var thigh = root.addOrReplaceChild(name + "_thigh", box(MAIL, -4.15F, 0, -4, 8.3F, 17, 8),
            PartPose.ZERO);
        var tasset = box(TASSET, -3.5F, .9F, -1.45F, 7, 15, 2);
        box(tasset, TASSET_TOP, -4, -.1F, -2.45F, 8, 8, 2);
        thigh.addOrReplaceChild("thigh_plates", tasset,
            PartPose.offsetAndRotation(side * 1.5F, .4F, -4.1F, -.075F, -side * .14F, -side * .14F));

        var shin = box(RECESSES, -3.7F, -2, -3.65F, 7.4F, 19, 7.5F);
        box(shin, IVORY, -4.05F, 1.95F, -4.95F, 8.1F, 13.2F, 5.15F);
        box(shin, RECESSES, -4.3F, -1.9F, -5.2F, 8.6F, 4.35F, 6.4F);
        box(shin, IVORY, -3.85F, -1.4F, -5.6F, 7.7F, 2.85F, 3.1F);
        root.addOrReplaceChild(name + "_shin", shin, PartPose.ZERO);
        var boot = box(RECESSES, -4.85F, -1.5F, -8.15F, 9.7F, 1.5F, 13);
        box(boot, IVORY, -4.65F, -4.8F, -7.95F, 9.3F, 3.5F, 12.6F);
        box(boot, IVORY, -4.15F, -6.3F, -5.05F, 8.3F, 2.35F, 8.9F);
        root.addOrReplaceChild(name + "_boot", boot,
            PartPose.offsetAndRotation(side * 7.8F, SOLES_Y, side * BOOT_STAGGER, 0, side * -.12F, 0));
    }

    private static void addCape(PartDefinition chest) {
        var cloth = box(CLOTH_FOLD, -9.5F, 0, 0, 19, 24, 1.3F);
        box(cloth, CLOTH, -12, 1, -.35F, 5, 23, 1.95F);
        box(cloth, CLOTH, 7, 1, -.35F, 5, 23, 1.95F);
        var cape = chest.addOrReplaceChild("cape", cloth, PartPose.offsetAndRotation(0, -6.1F, 6.55F, .13F, 0, 0));
        var hem = box(CLOTH_FOLD, -9.5F, -.24F, .05F, 19, 25, 1.3F);
        box(hem, CLOTH, -13.75F, -.24F, -.35F, 6.5F, 25.8F, 1.95F);
        box(hem, CLOTH, 7.25F, -.24F, -.35F, 6.5F, 25.8F, 1.95F);
        cape.addOrReplaceChild("hem", hem, PartPose.offset(0, 24, 0));
    }

    private static void addBlade(PartDefinition chest) {
        var sword = box(LEATHER, -1.4F, -8.4F, -1.4F, 2.8F, 17.4F, 2.8F);
        box(sword, VIOLET, -1.5F, -8.5F, -1.5F, 3, 4.5F, 3);
        box(sword, BRONZE, -2.35F, -10.55F, -2.1F, 4.7F, 2.35F, 4.2F);
        box(sword, BRONZE, -7.7F, 8.05F, -1.7F, 15.4F, 2.75F, 3.4F);
        box(sword, BRONZE, -2.15F, 7.4F, -2.15F, 4.3F, 4.1F, 4.3F);
        float taperStart = 43.5F;
        box(sword, STEEL, -4.2F, 10.25F, -.84F, 8.4F, taperStart - 10.25F, 1.68F);
        box(sword, BLADE_FULLER, -2.5F, taperStart - 34, -1, 5, 34, 2);
        var blade = chest.addOrReplaceChild("blade", sword,
            PartPose.offsetAndRotation(0, GRIP_Y, GRIP_Z, GRIP_PITCH, 0, 0));

        // A rotated square forms the bevels and closes the blade at a single point.
        float halfWidth = 4.2F;
        float pointScale = (SWORD_POINT_Y - taperStart) / halfWidth;
        var point = blade.addOrReplaceChild("point", CubeListBuilder.create(),
            PartPose.ZERO.scaled(1, pointScale, 1).translated(0, taperStart, 0));
        addBevel(point, "edge", STEEL, halfWidth, .8F);
        addBevel(point, "inner", BLADE_DARK, 2.5F, .97F);

        addGauntlet(blade, "right_gauntlet", RIGHT_HAND);
        addGauntlet(blade, "left_gauntlet", LEFT_HAND);
    }

    private static void addBevel(PartDefinition point, String name, Texture texture, float halfWidth, float depth) {
        float halfSide = halfWidth / Mth.SQRT_OF_TWO;
        point.addOrReplaceChild(name, box(texture, -halfSide, -halfSide, -depth, halfSide * 2, halfSide * 2, depth * 2),
            PartPose.rotation(0, 0, Mth.PI / 4));
    }

    private static void addGauntlet(PartDefinition blade, String name, Vec3 hand) {
        blade.addOrReplaceChild(name, box(FINGERS, -3, -3, -3, 6, 6, 6), PartPose.offset(0, (float) hand.y, 0));
    }

    private static CubeListBuilder box(Texture texture, float x, float y, float z,
                                       float width, float height, float depth) {
        return box(CubeListBuilder.create(), texture, x, y, z, width, height, depth);
    }

    private static CubeListBuilder box(CubeListBuilder cubes, Texture texture, float x, float y, float z,
                                       float width, float height, float depth) {
        return cubes.texOffs(texture.u, texture.v).addBox(x, y, z, width, height, depth);
    }

    private record Texture(int u, int v) {}
}
