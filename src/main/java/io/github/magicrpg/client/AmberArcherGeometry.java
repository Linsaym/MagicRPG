package io.github.magicrpg.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.magicrpg.entity.AmberArcher;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelPart.Cube;
import net.minecraft.client.model.geom.ModelPart.Polygon;
import net.minecraft.client.model.geom.ModelPart.Vertex;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

final class AmberArcherGeometry {
    private static final float FLOOR = 24;
    static final float UPPER_ARM_LENGTH = 10.25F;
    static final float FOREARM_LENGTH = 9.25F;
    static final float THIGH_LENGTH = 10.55F;
    static final float SHIN_LENGTH = 8.58F;
    static final float BOW_STRING_Z = bowZ(76.5F);
    static final float BOW_UPPER_TIP = bowY(18);
    static final float BOW_LOWER_TIP = bowY(223);
    static final PartPose BOW_REST = PartPose.offsetAndRotation(16.05F, 1, -9.28F, -.14F, -1.07F, .555F);

    private static final float BODY_Y = -2.65F;
    private static final float HOOD_TOP = -32;
    private static final float HOOD_HEIGHT = 14.2F;
    private static final float HOOD_HALF_WIDTH = 7.8F;
    private static final float HOOD_HALF_DEPTH = 7.35F;
    private static final float ATLAS_SIZE = 1024;

    private static final Tile HOOD_FRONT = new Tile(4, 4, 181, 163);
    private static final Tile HOOD_SIDE = new Tile(192, 4, 154, 161);
    private static final Tile HOOD_BACK = new Tile(352, 4, 166, 161);
    private static final Tile HOOD_TOP_TEXTURE = new Tile(524, 4, 176, 162);
    private static final Tile CHEST = new Tile(708, 4, 134, 97);
    private static final Tile SKIRT_LEFT = new Tile(596, 180, 56, 112);
    private static final Tile SKIRT_RIGHT = new Tile(660, 180, 40, 100);
    private static final Tile SHOULDER_LEFT = new Tile(708, 180, 100, 92);
    private static final Tile SHOULDER_RIGHT = new Tile(816, 180, 100, 92);
    private static final Tile SHOULDER_SIDE = new Tile(708, 280, 104, 98);
    private static final Tile COLLAR_LEFT = new Tile(822, 280, 90, 28);
    private static final Tile COLLAR_RIGHT = new Tile(822, 316, 90, 28);
    private static final Tile COWL = new Tile(822, 352, 57, 26);
    private static final Tile TUNIC = new Tile(890, 352, 80, 31);
    private static final Tile CAPE = new Tile(4, 176, 280, 233);
    private static final Tile CAPE_LINING = new Tile(292, 176, 280, 233);
    private static final Tile TABARD = new Tile(4, 420, 114, 202);
    private static final Tile BOW = new Tile(126, 420, 88, 241);
    private static final Tile QUIVER = new Tile(222, 420, 76, 142);
    private static final Tile BELT = new Tile(306, 420, 169, 77);
    private static final Tile SLEEVE_LEFT = new Tile(483, 420, 76, 54);
    private static final Tile SLEEVE_RIGHT = new Tile(567, 420, 76, 54);
    private static final Tile BRACER_LEFT = new Tile(483, 482, 76, 106);
    private static final Tile BRACER_RIGHT = new Tile(567, 482, 76, 106);
    private static final Tile HAND_LEFT = new Tile(483, 596, 71, 33);
    private static final Tile HAND_RIGHT = new Tile(567, 596, 71, 33);
    private static final Tile TROUSER = new Tile(651, 420, 76, 87);
    private static final Tile GREAVE = new Tile(735, 420, 89, 72);
    private static final Tile BOOT = new Tile(651, 515, 85, 88);
    private static final Tile TOE = new Tile(744, 515, 90, 31);
    private static final Tile GOLD = new Tile(842, 515, 48, 10);
    private static final Tile GOLD_EDGE = new Tile(842, 533, 83, 10);
    private static final Tile BROWN = new Tile(842, 552, 117, 11);
    private static final Tile WOOD = new Tile(842, 570, 17, 21);
    private static final Tile LINEN = new Tile(874, 570, 29, 26);
    private static final Tile DARK_LINEN = new Tile(910, 570, 26, 22);
    private static final Tile OLIVE = new Tile(942, 570, 33, 13);
    private static final Tile SKIN = new Tile(842, 604, 41, 10);
    private static final Tile HAIR = new Tile(892, 604, 14, 33);
    private static final Tile FEATHER = new Tile(914, 604, 15, 21);
    private static final Tile STRING = new Tile(936, 604, 2, 23);
    private static final Tile QUIVER_LEATHER = new Tile(842, 649, 39, 39);

    private AmberArcherGeometry() {}

    static ModelPart createRoot() {
        var root = new Part();
        var body = torso();
        body.child("hood", hood(), PartPose.offset(0, HOOD_TOP + HOOD_HEIGHT - BODY_Y, 0));
        body.child("mantle", cape(), PartPose.offsetAndRotation(0, -14.3F, 4.23F, .24F, 0, .04F));
        body.child("tabard", tabard(), PartPose.offset(0, 2.05F, -5.25F));
        body.child("quiver", quiver(), PartPose.offsetAndRotation(-5.65F, -8.6F, 7.05F, .06F, .04F, -.53F));
        body.child("bow", bow(), BOW_REST);
        body.child("draw_hand", new Part(), PartPose.offsetAndRotation(-12.4F, 6.85F, -.65F, -.05F, -.03F, -.13F));
        body.child("right_glove", hand(HAND_LEFT), PartPose.ZERO);
        addArm(body, "left", SHOULDER_RIGHT, SLEEVE_RIGHT, BRACER_RIGHT);
        addArm(body, "right", SHOULDER_LEFT, SLEEVE_LEFT, BRACER_LEFT);
        root.child("body", body, PartPose.offset(0, BODY_Y, 0));
        addLeg(root, "left", 4.45F, -2.35F, -.15F);
        addLeg(root, "right", -4.45F, 1.15F, .18F);

        float scale = AmberArcher.HEIGHT * 16 / (FLOOR - HOOD_TOP);
        return root.bake(PartPose.offset(0, FLOOR * (1 - scale), 0).withScale(scale));
    }

    private static Part hood() {
        var hood = new Part();
        float w = HOOD_HALF_WIDTH;
        float d = HOOD_HALF_DEPTH;
        float roof = hoodY(46);
        float firstStep = hoodY(69);
        float secondStep = hoodY(115);
        float chin = hoodY(162);
        float edge = hoodX(6);

        hood.box(-w, -HOOD_HEIGHT, -d, w, roof, d, new Faces(
            HOOD_FRONT.pixels(0, 0, 181, 46), HOOD_BACK.slice(0, 0, 1, 46 / 163F),
            hoodSide(0, 46), hoodSide(0, 46).flipU(), HOOD_TOP_TEXTURE, DARK_LINEN));
        hood.box(-w, roof, -d, edge, 0, d, new Faces(
            HOOD_FRONT.pixels(0, 46, 6, 163), HOOD_BACK.slice(.966F, 46 / 163F, 1, 1),
            hoodSide(46, 163), DARK_LINEN, DARK_LINEN, DARK_LINEN));
        hood.box(-edge, roof, -d, w, 0, d, new Faces(
            HOOD_FRONT.pixels(175, 46, 181, 163), HOOD_BACK.slice(0, 46 / 163F, .034F, 1),
            DARK_LINEN, hoodSide(46, 163).flipU(), DARK_LINEN, DARK_LINEN));
        hood.box(edge, roof, d - .95F, -edge, 0, d, new Faces(
            DARK_LINEN, HOOD_BACK.slice(.034F, 46 / 163F, .966F, 1),
            DARK_LINEN, DARK_LINEN, DARK_LINEN, DARK_LINEN));

        hoodTrim(hood, 6, 44, roof, firstStep, 46, 69);
        hoodTrim(hood, 137, 175, roof, firstStep, 46, 69);
        hoodTrim(hood, 6, 22, firstStep, secondStep, 69, 115);
        hoodTrim(hood, 159, 175, firstStep, secondStep, 69, 115);

        // Face and trim share the same projected front view. The face sits behind the cloth rim.
        hood.box(hoodX(19), roof, -d + .79F, hoodX(162), chin, 5.87F,
            HAIR, HOOD_FRONT.pixels(19, 46, 162, 162));
        hood.box(edge, firstStep, -d + 1.28F, hoodX(19), chin, -.4F,
            HAIR, HOOD_FRONT.pixels(6, 69, 19, 162));
        hood.box(hoodX(162), firstStep, -d + 1.28F, -edge, chin, -.4F,
            HAIR, HOOD_FRONT.pixels(162, 69, 175, 162));

        for (int side : new int[]{-1, 1}) {
            hoodEmbroidery(hood, side, 33, 27, 60, 59, .23F);
            hoodEmbroidery(hood, side, 44, 44, 77, 79, .31F);
            hoodEmbroidery(hood, side, 19, 95, 72, 119, .28F);
            hoodEmbroidery(hood, side, 47, 117, 73, 140, .21F);
            hoodEmbroidery(hood, side, 69, 40, 114, 67, .52F);
            hoodEmbroidery(hood, side, 70, 58, 99, 79, .65F);
            hoodEmbroidery(hood, side, 44, 82, 73, 104, .4F);
        }
        return hood;
    }

    private static float hoodX(float texel) {
        return (2 * texel / 181 - 1) * HOOD_HALF_WIDTH;
    }

    private static float hoodY(float texel) {
        return (texel / 163 - 1) * HOOD_HEIGHT;
    }

    private static Tile hoodSide(int top, int bottom) {
        return HOOD_SIDE.slice(0, top / 163F, 1, bottom / 163F);
    }

    private static void hoodTrim(Part hood, int left, int right, float top, float bottom, int v0, int v1) {
        hood.box(hoodX(left), top, -HOOD_HALF_DEPTH - .025F, hoodX(right), bottom, -HOOD_HALF_DEPTH + 1.11F,
            GOLD_EDGE, HOOD_FRONT.pixels(left, v0, right, v1));
    }

    private static void hoodEmbroidery(Part hood, int side, int left, int top, int right, int bottom, float depth) {
        float front = (1 - 2 * right / 154F) * HOOD_HALF_DEPTH;
        float back = (1 - 2 * left / 154F) * HOOD_HALF_DEPTH;
        float inner = side * (HOOD_HALF_WIDTH - .025F);
        float outer = side * (HOOD_HALF_WIDTH + depth);
        Tile tile = HOOD_SIDE.pixels(left, top, right, bottom);
        hood.box(Math.min(inner, outer), hoodY(top), front, Math.max(inner, outer), hoodY(bottom), back,
            new Faces(tile, tile, tile, tile.flipU(), tile, tile));
    }

    private static Part torso() {
        var body = new Part();
        body.box(-6.8F, -14.45F, -4.20F, 6.8F, 4.1F, 4.20F, QUIVER_LEATHER);
        body.box(-7.2F, -14.6F, -4.68F, 7.2F, -8.15F, 4.65F, OLIVE, COWL);
        body.box(-1.09F, -15.75F, -4.75F, 1.09F, -13.18F, 2.7F, SKIN);
        addCollar(body, "left_collar", -3.75F, .40F, COLLAR_LEFT);
        addCollar(body, "right_collar", 3.75F, -.40F, COLLAR_RIGHT);

        var clasp = new Part();
        clasp.box(-.92F, -2.35F, -.37F, .92F, 2.35F, .42F, GOLD);
        clasp.box(-.89F, -2.05F, -.46F, .88F, -.7F, -.37F, GOLD_EDGE);
        body.child("clasp", clasp, PartPose.offsetAndRotation(6, -11.05F, -5.03F, -.07F, -.06F, .94F));

        chestPatch(body, 20, 0, 68, 40, -5.56F, LINEN);
        chestPatch(body, 90, 0, 134, 61, -5.56F, LINEN);
        chestPatch(body, 68, 18, 90, 40, -5.56F, GOLD_EDGE);
        chestPatch(body, 0, 40, 90, 97, -5.66F, LINEN);
        chestPatch(body, 90, 61, 122, 97, -5.66F, LINEN);

        // Raise the crest above the plate while keeping its front UVs in the same position.
        crest(body, CHEST, 78, 0, .083F, -9.42F, .083F, -6.11F,
            new float[][]{{68, 19}, {90, 19}, {90, 39}, {79, 51}, {68, 39}});
        crest(body, CHEST, 78, 0, .083F, -9.42F, .083F, -6.25F,
            new float[][]{{44, 39}, {65, 44}, {79, 57}, {79, 65}, {44, 59}});
        crest(body, CHEST, 78, 0, .083F, -9.42F, .083F, -6.25F,
            new float[][]{{79, 57}, {93, 44}, {120, 39}, {120, 59}, {79, 65}});
        chestPatch(body, 68, 57, 90, 77, -6.29F, GOLD_EDGE);

        body.box(-7.15F, -1.37F, -4.84F, 7.15F, 2.7F, 4.72F, BROWN, BELT.pixels(2, 1, 165, 49));
        body.box(-7.38F, .52F, -5.4F, 7.37F, 2.82F, 4.95F, BROWN, BELT.pixels(1, 19, 168, 48));
        body.box(-7.34F, 2.82F, -5.13F, 1.3F, 4.65F, 4.74F, BROWN, BELT.pixels(1, 48, 103, 75));
        body.box(-7.1F, .43F, -6.02F, -3.05F, 2.89F, -5.38F, GOLD_EDGE, BELT.pixels(1, 20, 49, 47));
        body.box(1.1F, .48F, -5.91F, 4.72F, 2.83F, -5.38F, LINEN, BELT.pixels(97, 21, 139, 47));
        body.box(4.7F, -1.42F, -5.25F, 6.77F, .49F, -4.82F, LINEN, BELT.pixels(138, 1, 162, 20));
        body.box(-6.68F, 3.84F, -4.05F, 6.82F, 10.7F, 4.07F, OLIVE, TUNIC);
        return body;
    }

    private static void addCollar(Part body, String name, float x, float roll, Tile tile) {
        var fold = new Part();
        fold.box(-4.0F, -1.05F, -.65F, 4.0F, 1.05F, .7F, OLIVE, tile);
        body.child(name, fold, PartPose.offsetAndRotation(x, -12.65F, -4.53F, -.02F, 0, roll));
    }

    private static void chestPatch(Part body, int l, int t, int r, int b, float z, Tile side) {
        body.box((l - 78) * .083F, -9.42F + t * .083F, z,
            (r - 78) * .083F, -9.42F + b * .083F, -4.13F, side, CHEST.pixels(l, t, r, b));
    }

    private static Part tabard() {
        var tabard = new Part();
        tabardPatch(tabard, 0, 9, 113, 33);
        tabardPatch(tabard, 12, 33, 102, 173);
        tabardPatch(tabard, 27, 173, 87, 189);
        tabardPatch(tabard, 42, 189, 71, 202);
        crest(tabard, TABARD, 56.5F, 9, .083F, 0, .075F, -.6F,
            new float[][]{{48, 84}, {64, 84}, {64, 101}, {56, 106}, {48, 101}});
        crest(tabard, TABARD, 56.5F, 9, .083F, 0, .075F, -.58F,
            new float[][]{{32, 99}, {46, 100}, {54, 114}, {56, 120}, {32, 112}});
        crest(tabard, TABARD, 56.5F, 9, .083F, 0, .075F, -.58F,
            new float[][]{{57, 114}, {65, 100}, {80, 99}, {81, 112}, {57, 120}});
        var left = new Part();
        left.box(-1.83F, 0, -.45F, 1.83F, 10.65F, .52F, DARK_LINEN, SKIRT_LEFT);
        tabard.child("left_skirt", left, PartPose.offsetAndRotation(-5.46F, .83F, .43F, -.05F, .10F, .17F));
        var right = new Part();
        right.box(-1.20F, 0, -.36F, 1.20F, 8.95F, .63F, DARK_LINEN, SKIRT_RIGHT);
        tabard.child("right_skirt", right, PartPose.offsetAndRotation(6.39F, .62F, .47F, -.04F, -.08F, -.15F));
        return tabard;
    }

    private static void tabardPatch(Part part, int l, int t, int r, int b) {
        part.box((l - 56.5F) * .083F, (t - 9) * .075F, -.37F,
            (r - 56.5F) * .083F, (b - 9) * .075F, .42F, OLIVE, TABARD.pixels(l, t, r, b));
    }

    private static void crest(Part part, Tile image, float center, float top, float sx, float y, float sy, float z,
                              float[][] outline) {
        Vertex[] front = new Vertex[outline.length];
        for (int i = 0; i < outline.length; i++) {
            float u = outline[i][0];
            float v = outline[i][1];
            front[i] = new Vertex((u - center) * sx, y + (v - top) * sy, z, image.u(u / image.w), image.v(v / image.h));
        }
        part.quad(Direction.NORTH, front[0], front[1], front[2], front[3]);
        if (front.length == 5) {
            part.quad(Direction.NORTH, front[0], front[3], front[4], front[4]);
        }
        for (int i = 0; i < front.length; i++) {
            var a = front[i];
            var b = front[(i + 1) % front.length];
            var direction = Math.abs(b.x() - a.x()) > Math.abs(b.y() - a.y())
                ? (b.x() > a.x() ? Direction.UP : Direction.DOWN)
                : (b.y() > a.y() ? Direction.EAST : Direction.WEST);
            part.face(direction, GOLD_EDGE, new Point(a.x(), a.y(), a.z()), new Point(b.x(), b.y(), b.z()),
                new Point(b.x(), b.y(), b.z() + .61F), new Point(a.x(), a.y(), a.z() + .61F));
        }
    }

    private static Part cape() {
        var cape = new Part();
        // Each vertical fold spreads from the neckline toward the stepped hem.
        float[] widths = {-1, -.81F, -.65F, -.45F, -.28F, .28F, .45F, .65F, .81F, 1};
        float[] folds = {-.18F, .60F, -.15F, .46F, -.21F, -.21F, .46F, -.15F, .60F, -.18F};
        float[] hem = {202, 210, 215, 223, 233, 223, 215, 210, 202};
        for (int i = 0; i < hem.length; i++) {
            float u0 = (widths[i] + 1) / 2;
            float u1 = (widths[i + 1] + 1) / 2;
            float v = hem[i] / 233;
            Point a = capePoint(widths[i], folds[i], 0);
            Point b = capePoint(widths[i + 1], folds[i + 1], 0);
            Point c = capePoint(widths[i + 1], folds[i + 1], v);
            Point d = capePoint(widths[i], folds[i], v);
            cape.face(Direction.NORTH, CAPE_LINING.slice(u0, 0, u1, v), a, b, c, d);
            cape.face(Direction.SOUTH, CAPE.slice(1 - u1, 0, 1 - u0, v), b.back(.58F), a.back(.58F),
                d.back(.58F), c.back(.58F));
            cape.face(Direction.DOWN, GOLD_EDGE, d, c, c.back(.58F), d.back(.58F));

            if (i == 0) {
                cape.face(Direction.WEST, OLIVE, a, a.back(.58F), d.back(.58F), d);
            }
            if (i == hem.length - 1) {
                cape.face(Direction.EAST, OLIVE, b.back(.58F), b, c, c.back(.58F));
            }
            if (i + 1 < hem.length && hem[i] != hem[i + 1]) {
                Point step = capePoint(widths[i + 1], folds[i + 1], hem[i + 1] / 233);
                cape.face(hem[i] > hem[i + 1] ? Direction.EAST : Direction.WEST, GOLD_EDGE,
                    c, c.back(.58F), step.back(.58F), step);
            }
        }
        cape.box(-8.2F, .2F, -.22F, 8.2F, 3.15F, .84F, OLIVE, CAPE_LINING.slice(.16F, .01F, .84F, .12F));
        return cape;
    }

    private static Point capePoint(float width, float fold, float v) {
        float spread = Mth.lerp(v, 7.95F, 15.70F);
        return new Point(width * spread, v * 30.25F, fold * (.48F + v * .72F) - .87F * Math.abs(width) * v);
    }

    private static Part quiver() {
        var quiver = new Part();
        var a = new Point(-2.7F, -7.7F, -1.35F);
        var b = new Point(2.7F, -7.7F, -1.35F);
        var c = new Point(2.05F, 7.75F, -1.16F);
        var d = new Point(-2.05F, 7.75F, -1.16F);
        quiver.face(Direction.NORTH, QUIVER, a, b, c, d);
        quiver.face(Direction.SOUTH, QUIVER, b.back(4.24F), a.back(4.24F), d.back(3.48F), c.back(3.48F));
        quiver.face(Direction.WEST, QUIVER_LEATHER, a.back(4.24F), a, d, d.back(3.48F));
        quiver.face(Direction.EAST, QUIVER_LEATHER, b, b.back(4.24F), c.back(3.48F), c);
        quiver.face(Direction.UP, WOOD, a.back(4.24F), b.back(4.24F), b, a);
        quiver.face(Direction.DOWN, GOLD_EDGE, d, c, c.back(3.48F), d.back(3.48F));
        quiver.box(-2.74F, -4.25F, -1.49F, 2.74F, -2.45F, 3.02F, GOLD_EDGE);
        quiver.box(-2.15F, 6.02F, -1.34F, 2.15F, 7.74F, 2.53F, GOLD_EDGE);
        for (int i = 0; i < 3; i++) {
            var arrow = new Part();
            arrow.box(-.26F, -7.2F, -.26F, .26F, .9F, .26F, WOOD);
            arrow.box(-1.18F, -6.17F, -.33F, 1.18F, -3.35F, .35F, FEATHER);
            arrow.box(-.33F, -6.17F, -1.18F, .33F, -3.35F, 1.18F, FEATHER);
            arrow.box(-.35F, -7.21F, -.35F, .35F, -6.1F, .35F, GOLD);
            float y = i == 1 ? -6.87F : -7.4F - i * .32F;
            quiver.child("arrow_" + i, arrow, PartPose.offsetAndRotation(-1.94F + i * 1.93F, y,
                i == 1 ? 1.78F : .58F, .05F - i * .06F, .06F, .11F - i * .09F));
        }
        return quiver;
    }

    private static void addArm(Part body, String side, Tile shoulderTexture, Tile sleeveTexture, Tile bracerTexture) {
        var upper = new Part();
        upper.box(-2.94F, -.9F, -2.74F, 2.94F, UPPER_ARM_LENGTH + .62F, 2.8F, OLIVE, sleeveTexture);
        upper.box(-4.08F, -1.73F, -3.63F, 4.08F, 5.65F, 3.56F, new Faces(
            shoulderTexture, shoulderTexture, SHOULDER_SIDE, SHOULDER_SIDE.flipU(), LINEN, GOLD_EDGE));
        upper.box(-4.19F, 4.03F, -3.76F, 4.19F, 5.9F, 3.69F, GOLD_EDGE, shoulderTexture.slice(0, .73F, 1, 1));
        body.child(side + "_upper", upper, PartPose.ZERO);

        var forearm = new Part();
        forearm.box(-2.67F, -.97F, -2.67F, 2.67F, 3.63F, 2.67F, SKIN, bracerTexture.slice(0, 0, 1, .34F));
        forearm.box(-2.82F, 3.12F, -2.82F, 2.82F, FOREARM_LENGTH + .24F, 2.83F, BROWN,
            bracerTexture.slice(0, .54F, 1, 1));
        forearm.box(-2.94F, 2.55F, -2.94F, 2.94F, 4.14F, 2.95F, GOLD_EDGE, bracerTexture.slice(0, .34F, 1, .55F));
        body.child(side + "_forearm", forearm, PartPose.ZERO);
    }

    private static Part hand(Tile texture) {
        var hand = new Part();
        hand.box(-2.55F, -.57F, -2.42F, 2.55F, 2.37F, 2.5F, SKIN, texture);
        return hand;
    }

    private static void addLeg(Part root, String side, float x, float z, float yaw) {
        var thigh = new Part();
        thigh.box(-3.11F, -.65F, -2.93F, 3.11F, THIGH_LENGTH + .68F, 2.97F, QUIVER_LEATHER, TROUSER);
        root.child(side + "_thigh", thigh, PartPose.ZERO);
        var shin = new Part();
        shin.box(-2.99F, -.86F, -2.86F, 2.99F, SHIN_LENGTH + 2.52F, 2.94F, BOOT);
        shin.box(-3.21F, -.36F, -3.18F, 3.21F, 4.05F, 3.1F, DARK_LINEN, GREAVE);
        shin.box(-3.4F, 3.82F, -3.42F, 3.4F, 5.32F, 3.31F, GOLD_EDGE);
        root.child(side + "_shin", shin, PartPose.ZERO);
        var foot = new Part();
        foot.box(-2.65F, -3.36F, -2.58F, 2.65F, -1.95F, 2.62F, BOOT);
        foot.box(-3.48F, -2.42F, -4.8F, 3.48F, 0, 3.39F, new Faces(TOE, TOE, BOOT, BOOT, WOOD, BOOT));
        root.child(side + "_foot", foot, PartPose.offsetAndRotation(x, FLOOR, z, 0, yaw, 0));
    }

    private static Part bow() {
        var bow = new Part();
        // The profile coordinates are texels in the separate view of the bow.
        int[][] profile = {
            {1, 60, 84}, {19, 59, 87}, {55, 3, 46}, {66, 3, 27}, {78, 3, 27}, {81, 3, 36},
            {97, 3, 36}, {103, 13, 43}, {117, 13, 43}, {123, 17, 35}, {132, 17, 35}, {140, 14, 43},
            {146, 14, 43}, {151, 3, 43}, {160, 3, 36}, {166, 3, 32}, {178, 3, 32}, {190, 3, 51},
            {225, 53, 87}, {231, 59, 87}, {241, 60, 86}
        };
        for (int i = 1; i < profile.length; i++) {
            Tile edge = switch (i) {
                case 1, 4, 8, 14, 16, 20 -> GOLD_EDGE;
                default -> WOOD;
            };
            bowSection(bow, profile[i - 1], profile[i], edge);
        }
        bow.box(-1.13F, bowY(1), bowZ(60), 1.13F, bowY(1) + .06F, bowZ(84), WOOD);
        bow.box(-1.13F, bowY(241) - .06F, bowZ(60), 1.13F, bowY(241), bowZ(86), WOOD);
        var glove = new Part();
        glove.box(-1.79F, -.60F, -1.8F, 1.79F, 2.48F, 2.03F, SKIN, HAND_RIGHT);
        bow.child("glove", glove, PartPose.offsetAndRotation(.02F, -1.44F, 2.02F, -.32F, -.10F, -.17F));
        var upper = new Part();
        var lower = new Part();
        upper.box(-.098F, 0, -.098F, .098F, 1, .098F, STRING);
        lower.box(-.098F, 0, -.098F, .098F, 1, .098F, STRING);
        bow.child("upper_string", upper, PartPose.ZERO);
        bow.child("lower_string", lower, PartPose.ZERO);
        var nock = new Part();
        var arrow = new Part();
        arrow.box(-.19F, -.16F, -12.45F, .19F, .18F, .27F, WOOD);
        arrow.box(-.38F, -.36F, -13.05F, .38F, .36F, -11.87F, GOLD);
        arrow.box(-.61F, -.34F, -2.65F, .61F, .34F, -.61F, FEATHER);
        nock.child("arrow", arrow, PartPose.ZERO);
        bow.child("nock", nock, PartPose.offset(0, -.4F, BOW_STRING_Z));
        return bow;
    }

    private static void bowSection(Part bow, int[] top, int[] bottom, Tile edge) {
        float y0 = bowY(top[0]);
        float y1 = bowY(bottom[0]);
        var a = new Point(-1.13F, y0, bowZ(top[1]));
        var b = new Point(-1.13F, y0, bowZ(top[2]));
        var c = new Point(-1.13F, y1, bowZ(bottom[2]));
        var d = new Point(-1.13F, y1, bowZ(bottom[1]));
        var ar = a.across(2.26F);
        var br = b.across(2.26F);
        var cr = c.across(2.26F);
        var dr = d.across(2.26F);
        bow.quad(Direction.WEST, bowVertex(a, top[1], top[0]), bowVertex(b, top[2], top[0]),
            bowVertex(c, bottom[2], bottom[0]), bowVertex(d, bottom[1], bottom[0]));
        bow.quad(Direction.EAST, bowVertex(ar, top[1], top[0]), bowVertex(br, top[2], top[0]),
            bowVertex(cr, bottom[2], bottom[0]), bowVertex(dr, bottom[1], bottom[0]));
        bow.face(Direction.NORTH, edge, a, ar, dr, d);
        bow.face(Direction.SOUTH, edge, br, b, c, cr);
    }

    private static Vertex bowVertex(Point point, float u, float v) {
        return point.vertex(BOW, u / BOW.w, v / BOW.h);
    }

    private static float bowY(float v) {
        return (v - 129) * .137F;
    }

    private static float bowZ(float u) {
        return (u - 22) * .137F;
    }

    private record Tile(float x, float y, float w, float h) {
        Tile pixels(float left, float top, float right, float bottom) {
            return new Tile(x + left, y + top, right - left, bottom - top);
        }

        Tile slice(float left, float top, float right, float bottom) {
            return pixels(left * w, top * h, right * w, bottom * h);
        }

        Tile flipU() {
            return new Tile(x + w, y, -w, h);
        }

        float u(float fraction) {
            return (x + fraction * w) / ATLAS_SIZE;
        }

        float v(float fraction) {
            return (y + fraction * h) / ATLAS_SIZE;
        }
    }

    private record Faces(Tile front, Tile back, Tile west, Tile east, Tile top, Tile bottom) {}

    private record Point(float x, float y, float z) {
        Point back(float depth) {
            return new Point(x, y, z + depth);
        }

        Point across(float width) {
            return new Point(x + width, y, z);
        }

        Vertex vertex(Tile tile, float u, float v) {
            return new Vertex(x, y, z, tile.u(u), tile.v(v));
        }
    }

    private static final class Part {
        private final List<Cube> cubes = new ArrayList<>();
        private final Map<String, ModelPart> children = new LinkedHashMap<>();

        void child(String name, Part child, PartPose pose) {
            children.put(name, child.bake(pose));
        }

        ModelPart bake(PartPose pose) {
            var part = new ModelPart(List.copyOf(cubes), children);
            part.setInitialPose(pose);
            part.resetPose();
            return part;
        }

        void box(float x0, float y0, float z0, float x1, float y1, float z1, Tile tile) {
            box(x0, y0, z0, x1, y1, z1, tile, tile);
        }

        void box(float x0, float y0, float z0, float x1, float y1, float z1, Tile material, Tile front) {
            box(x0, y0, z0, x1, y1, z1, new Faces(front, material, material, material, material, material));
        }

        void box(float x0, float y0, float z0, float x1, float y1, float z1, Faces tiles) {
            var a = new Point(x0, y0, z0);
            var b = new Point(x1, y0, z0);
            var c = new Point(x1, y1, z0);
            var d = new Point(x0, y1, z0);
            var e = new Point(x0, y0, z1);
            var f = new Point(x1, y0, z1);
            var g = new Point(x1, y1, z1);
            var h = new Point(x0, y1, z1);
            var cube = new MeshCube(x0, y0, z0, x1 - x0, y1 - y0, z1 - z0, EnumSet.allOf(Direction.class));
            cube.polygons[0] = facePolygon(Direction.NORTH, tiles.front, a, b, c, d);
            cube.polygons[1] = facePolygon(Direction.SOUTH, tiles.back, f, e, h, g);
            cube.polygons[2] = facePolygon(Direction.WEST, tiles.west, e, a, d, h);
            cube.polygons[3] = facePolygon(Direction.EAST, tiles.east, b, f, g, c);
            cube.polygons[4] = facePolygon(Direction.UP, tiles.top, e, f, b, a);
            cube.polygons[5] = facePolygon(Direction.DOWN, tiles.bottom, d, c, g, h);
            cubes.add(cube);
        }

        void face(Direction direction, Tile tile, Point a, Point b, Point c, Point d) {
            addQuad(direction, facePolygon(direction, tile, a, b, c, d));
        }

        void quad(Direction direction, Vertex a, Vertex b, Vertex c, Vertex d) {
            addQuad(direction, polygon(direction, a, b, c, d));
        }

        // ModelPart uses the same quad submission for authored faces and cuboids.
        private void addQuad(Direction direction, Polygon polygon) {
            float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
            for (var vertex : polygon.vertices()) {
                minX = Math.min(minX, vertex.x());
                minY = Math.min(minY, vertex.y());
                minZ = Math.min(minZ, vertex.z());
                maxX = Math.max(maxX, vertex.x());
                maxY = Math.max(maxY, vertex.y());
                maxZ = Math.max(maxZ, vertex.z());
            }
            var cube = new MeshCube(minX, minY, minZ, maxX - minX, maxY - minY, maxZ - minZ,
                EnumSet.of(direction));
            cube.polygons[0] = polygon;
            cubes.add(cube);
        }

        private static Polygon facePolygon(Direction direction, Tile tile, Point a, Point b, Point c, Point d) {
            return polygon(direction, a.vertex(tile, 0, 0), b.vertex(tile, 1, 0),
                c.vertex(tile, 1, 1), d.vertex(tile, 0, 1));
        }

        private static Polygon polygon(Direction direction, Vertex a, Vertex b, Vertex c, Vertex d) {
            var ab = new Vector3f(b.x() - a.x(), b.y() - a.y(), b.z() - a.z());
            var ac = new Vector3f(c.x() - a.x(), c.y() - a.y(), c.z() - a.z());
            var normal = ab.cross(ac).normalize();
            // Model coordinates increase downward.
            if (normal.dot(direction.getStepX(), -direction.getStepY(), direction.getStepZ()) < 0) {
                return new Polygon(new Vertex[]{a, d, c, b}, normal.negate());
            }
            return new Polygon(new Vertex[]{a, b, c, d}, normal);
        }
    }

    private static final class MeshCube extends Cube {
        MeshCube(float x, float y, float z, float width, float height, float depth, EnumSet<Direction> faces) {
            super(0, 0, x, y, z, width, height, depth, 0, 0, 0, false, ATLAS_SIZE, ATLAS_SIZE, faces);
        }

        @Override
        public void compile(PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay, int color) {
            // Sodium caches the constructor's cuboid. This mesh replaces those faces with authored polygons.
            var position = new Vector3f();
            var normal = new Vector3f();
            for (var polygon : polygons) {
                pose.transformNormal(polygon.normal(), normal);
                for (var vertex : polygon.vertices()) {
                    pose.pose().transformPosition(vertex.worldX(), vertex.worldY(), vertex.worldZ(), position);
                    consumer.addVertex(position.x(), position.y(), position.z(), color, vertex.u(), vertex.v(),
                        overlay, light, normal.x(), normal.y(), normal.z());
                }
            }
        }
    }
}
