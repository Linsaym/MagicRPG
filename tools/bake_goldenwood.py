"""Export the authored Goldenwood block layout. Minecraft only reads the exported map.

Run from any directory with Python 3. The map uses local coordinates around (0, 0).
Routes are (x, z, floor elevation); their order describes the walk through each area.
"""

from collections import defaultdict
from itertools import groupby
from math import atan2, ceil, cos, hypot, sin, sqrt
from pathlib import Path
from struct import pack
import gzip


HEIGHT = 192
OUTPUT = Path(__file__).resolve().parents[1] / "src/main/resources/data/magicrpg/island/goldenwood.bin.gz"
PALETTE = ["minecraft:air"]
WORLD = {}
FIXTURES = {}
LANDMARKS = {}
SURFACE = {}
ROAD = {}
RIVER = {}


def material(name):
    state = name if ":" in name else "minecraft:" + name
    if state not in PALETTE:
        PALETTE.append(state)
    return PALETTE.index(state)


AIR = 0
TURF = material("magicrpg:amber_grass")
HONEY = material("magicrpg:honeystone")
HONEY_BRICK = material("magicrpg:honeystone_bricks")
LOG = material("magicrpg:goldenwood_log")
LOG_X = material("magicrpg:goldenwood_log[axis=x]")
LOG_Z = material("magicrpg:goldenwood_log[axis=z]")
ROT = material("magicrpg:rotten_goldenwood_log[axis=z]")
LEAVES = material("magicrpg:goldenwood_leaves[persistent=true]")
GLOW = material("magicrpg:sunlit_leaves[persistent=true]")
FLOWER = material("magicrpg:honeybell")
PLANKS = material("magicrpg:goldenwood_planks")
DIRT = material("dirt")
PATH = material("packed_mud")
GRAVEL = material("gravel")
MOSS_BLOCK = material("moss_block")
MUD = material("mud_bricks")
TUFF = material("tuff")
ANDESITE = material("andesite")
DEEP = material("deepslate")
CALCITE = material("calcite")
STONE = material("stone_bricks")
MOSS = material("mossy_stone_bricks")
CRACKED = material("cracked_stone_bricks")
SMOOTH = material("smooth_sandstone")
CUT = material("cut_sandstone")
SAND = material("sand")
WATER = material("water")
FALL = material("water[level=8]")
DARK = material("dark_oak_planks")
BEAM = material("stripped_dark_oak_log")
FENCE = material("dark_oak_fence[north=true,south=true]")
FENCE_X = material("dark_oak_fence[east=true,west=true]")
LANTERN = material("lantern")
HANGING = material("lantern[hanging=true]")
CHAIN = material("iron_chain")
SHROOM = material("shroomlight")
COBBLE = material("cobbled_deepslate")
TILE = material("deepslate_tiles")
COPPER = material("waxed_weathered_cut_copper")
COPPER_TRIM = material("waxed_exposed_cut_copper")
COPPER_GRATE = material("waxed_weathered_copper_grate")


def mix(x, z, salt=0):
    number = (x * 374761393 + z * 668265263 + salt * 1274126177) & 0xffffffff
    number = ((number ^ (number >> 13)) * 1274126177) & 0xffffffff
    return number ^ (number >> 16)


def clamp(value, lower=0.0, upper=1.0):
    return max(lower, min(value, upper))


def smooth(value):
    value = clamp(value)
    return value * value * (3 - 2 * value)


def set_block(x, y, z, block):
    if not 0 <= y < HEIGHT:
        raise ValueError(f"Block outside build height at {(x, y, z)}")
    WORLD.setdefault((x, z), bytearray(HEIGHT))[y] = block


def get_block(x, y, z):
    column = WORLD.get((x, z))
    return column[y] if column is not None and 0 <= y < HEIGHT else AIR


def box(x1, y1, z1, x2, y2, z2, block):
    fill = bytes([block]) * (y2 - y1 + 1)
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            column = WORLD.setdefault((x, z), bytearray(HEIGHT))
            column[y1:y2 + 1] = fill


def ball(cx, cy, cz, rx, ry, rz, fn):
    for x in range(int(cx - rx), ceil(cx + rx + 1)):
        for z in range(int(cz - rz), ceil(cz + rz + 1)):
            for y in range(int(cy - ry), ceil(cy + ry + 1)):
                r = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 + ((z - cz) / rz) ** 2
                if r <= 1.10:
                    fn(x, y, z, r)


def segments(points, width, result):
    """Rasterize a graded centerline and its shoulders into the terrain plan."""
    for (ax, az, ay), (bx, bz, by) in zip(points, points[1:]):
        dx, dz = bx - ax, bz - az
        length = dx * dx + dz * dz
        for x in range(min(ax, bx) - width, max(ax, bx) + width + 1):
            for z in range(min(az, bz) - width, max(az, bz) + width + 1):
                t = clamp(((x - ax) * dx + (z - az) * dz) / length)
                d = hypot(x - ax - dx * t, z - az - dz * t)
                if d <= width and d < result.get((x, z), (width + 1, 0))[0]:
                    result[x, z] = d, ay + (by - ay) * t


PROMENADE = [(0, 113, 111), (0, 97, 108), (-7, 78, 103), (-7, 59, 99),
              (-15, 40, 96), (-27, 23, 94), (-32, 4, 96), (-34, -15, 98)]
WEST_ROAD = [(-7, 65, 100), (-29, 61, 95), (-50, 54, 92), (-65, 39, 92),
             (-81, 18, 96), (-91, 1, 100), (-105, -19, 104), (-106, -42, 107),
             (-106, -64, 107)]
FOREST_LOOP = [(-106, -64, 107), (-99, -73, 107), (-86, -73, 107), (-77, -79, 106), (-61, -77, 103),
               (-47, -61, 99), (-39, -38, 97), (-34, -15, 98)]
ORCHARD = [(-34, -15, 98), (-28, -38, 98), (-13, -53, 96), (5, -64, 89),
           (13, -66, 86), (51, -66, 86), (65, -76, 94), (81, -83, 105), (94, -87, 122)]
CASTLE_ROAD = [(-7, 59, 99), (11, 60, 95), (28, 66, 90), (46, 65, 86), (100, 65, 86),
               (108, 51, 91), (123, 35, 98), (132, 12, 105), (124, -8, 110),
               (112, -18, 115), (100, -30, 122), (100, -37, 122)]
WATERFALL_TRAIL = [(103, 60, 88), (116, 70, 88), (128, 78, 88), (139, 87, 89)]
RIVERBED = [(35, -117, 83), (27, -98, 83), (29, -72, 83), (31, -54, 83), (42, -35, 83),
            (48, -16, 83), (43, 3, 83), (47, 22, 83), (62, 39, 83), (74, 56, 83),
            (81, 73, 83), (93, 84, 83), (104, 92, 83), (118, 100, 83), (139, 107, 83)]

for route in (PROMENADE, WEST_ROAD, FOREST_LOOP, ORCHARD, CASTLE_ROAD, WATERFALL_TRAIL):
    segments(route, 13, ROAD)
segments(RIVERBED, 23, RIVER)


def terrace(x, z, cx, cz, sx, sz, shoulder):
    distance = hypot(max(0, abs(x - cx) - sx), max(0, abs(z - cz) - sz))
    return smooth(1 - distance / shoulder)


def terrain():
    terraces = [(0, 131, 34, 23, 12, 111), (-17, -19, 15, 19, 22, 98),
                (100, -63, 27, 29, 20, 122), (-106, -65, 23, 19, 18, 107),
                (-62, 46, 18, 12, 15, 92), (-64, -64, 10, 9, 9, 103)]
    for z in range(-174, 173):
        for x in range(-192, 193):
            angle = atan2(z / 157, x / 179)
            rim = 1 + .058 * cos(3 * angle + .65) + .041 * sin(5 * angle - 1) + .034 * cos(9 * angle)
            r = hypot(x / 179, z / 157) / rim
            rough = (sin(x * .12 + z * .043) + cos(z * .19 - x * .07) + .6 * sin(x * .38 - z * .27))
            lip = max(0, 1 - r)
            arrival = terrace(x, z, 0, 132, 34, 22, 9)
            # The headland under the arrival court and the projecting waterfall lip.
            outcrop = terrace(x, z, 121, 96, 15, 10, 22)
            if r > 1 + .009 * rough and max(arrival, outcrop) < .48:
                continue
            height = (95 + 19 * sin(x * .024 + z * .012) * cos(z * .017)
                      + 8 * cos(z * .034 + .5) + 3.0 * rough
                      + 13 * sin(z * .019 - .7) ** 2)
            road_dist, road_y = ROAD.get((x, z), (40, 0))
            trail_weight = smooth((13 - road_dist) / 10)
            height += (road_y - height) * trail_weight
            for cx, cz, sx, sz, shoulder, ty in terraces:
                height += (ty - height) * terrace(x, z, cx, cz, sx, sz, shoulder)

            river_dist, _ = RIVER.get((x, z), (40, 0))
            width = 5.5 + 1.7 * sin(z * .073) + 4 * terrace(x, z, 40, -30, 10, 17, 22)
            bank = smooth((river_dist - width + 1) / 14)
            bed = 79 + .9 * sin(x * .34 + z * .29) + 2 * smooth((river_dist - width + 2) / 6)
            height = bed + (max(height, 85.0) - bed) * bank if river_dist < width + 13 else height
            # The landing remains flat even where the outer rock tapers.
            bevel = smooth(lip * 13)
            height -= (1 - bevel) * 7 * (1 - max(arrival, outcrop))
            top = round(height)
            bottom = min(top - 2, round(15 + 57 * min(1, r) ** 1.7 + 5 * rough
                                        + 4 * sin(x * .22 + z * .08)))
            bottom = max(7, bottom)
            if r > .95 and bottom >= top - 4:
                bottom = top - 5

            n = mix(x, z)
            col = bytearray(HEIGHT)
            for y in range(bottom, top - 4):
                vein = (y + int(rough * 2) + int(5 * sin(x * .04 + z * .023))) % 23
                block = DEEP if y < bottom + 7 or y < 39 else TUFF
                if vein in (8, 9, 10):
                    block = ANDESITE
                if vein == 16 and y > 51:
                    block = HONEY if n % 7 else CALCITE
                if (x + z * 2 + y // 3) % 19 < 3 and y > 49:
                    block = COBBLE
                col[y] = block
            wet = river_dist < width + 2 and top < 83
            cliff = r > .94 and arrival < .3 and outcrop < .3 and n % 5 != 0
            subsoil = HONEY if road_dist > 4 or n % 4 == 0 else DIRT
            for y in range(max(bottom, top - 4), top):
                col[y] = subsoil if y > top - 3 or n % 3 else ANDESITE
            if arrival == 1:
                col[top] = HONEY if top == 111 else SMOOTH
            elif wet:
                col[top] = (SAND, GRAVEL, TUFF, HONEY)[n % 4]
            elif road_dist <= 3.5:
                col[top] = (PATH, PATH, PATH, STONE, MOSS, GRAVEL, MUD)[n % 7]
            elif cliff:
                col[top] = (HONEY, TUFF, ANDESITE)[n % 3]
            else:
                col[top] = TURF
            if wet:
                col[top + 1:84] = bytes([WATER]) * (83 - top)
            WORLD[x, z] = col
            SURFACE[x, z] = top


def beam(a, b, radius, block):
    ax, ay, az = a
    bx, by, bz = b
    dx, dy, dz = bx - ax, by - ay, bz - az
    size = ceil(max(abs(dx), abs(dy), abs(dz)))
    for i in range(size + 1):
        t = i / max(1, size)
        cx, cy, cz = round(ax + dx * t), round(ay + dy * t), round(az + dz * t)
        for x in range(cx - radius, cx + radius + 1):
            for y in range(cy - radius, cy + radius + 1):
                for z in range(cz - radius, cz + radius + 1):
                    if (x - cx) ** 2 + (y - cy) ** 2 + (z - cz) ** 2 <= radius ** 2:
                        set_block(x, y, z, block)


def lamp(x, y, z, tall=3, facing=1):
    set_block(x, y, z, CUT)
    if tall == 0:
        set_block(x, y + 1, z, LANTERN)
        return
    for dy in range(1, tall + 1):
        set_block(x, y + dy, z, BEAM if dy == 1 else FENCE)
    set_block(x, y + tall + 1, z, DARK)
    set_block(x + facing, y + tall + 1, z, DARK)
    set_block(x + facing, y + tall, z, HANGING)


def stairs(block, direction):
    return material(f"{block}_stairs[facing={direction}]")


def fixture(x, y, z, block, contents):
    set_block(x, y, z, material(block))
    FIXTURES[x, y, z] = "magicrpg:" + contents


def landmark(name, x, y, z):
    LANDMARKS["magicrpg:" + name] = (x, y, z)


def colossus():
    x, z = -17, -18
    for dy in range(46):
        y = 96 + dy
        cx = x + round(3 * sin(dy / 22))
        cz = z + round(2 * sin(dy / 16))
        radius = 3.6 + 4.9 * (1 - min(1, dy / 42)) ** 1.8
        for px in range(cx - 11, cx + 12):
            for pz in range(cz - 11, cz + 12):
                angle = atan2(pz - cz, px - cx)
                lobes = .9 * cos(5 * angle + dy * .055) + .4 * sin(9 * angle - dy * .13)
                if hypot(px - cx, pz - cz) < radius + lobes:
                    sapwood = hypot(px - cx, pz - cz) > radius - .8
                    bark = ANDESITE if mix(px, pz) % 9 == 0 else SMOOTH
                    set_block(px, y, pz, bark if sapwood and mix(px, pz) % 8 == 0 else LOG)

    roots = [(-2, -33), (2, -16), (-2, 2), (-12, 2), (-23, 1), (-32, -3),
             (-29, -28), (-24, -40), (-9, -40)]
    for ex, ez in roots:
        ey = SURFACE[ex, ez]
        middle = ((x + ex) // 2, 101, (z + ez) // 2)
        beam((x, 105, z), middle, 3, LOG)
        beam(middle, (ex, ey - 1, ez), 2, LOG_X if abs(ex - x) > abs(ez - z) else LOG_Z)
        beam((ex, ey, ez), (ex + (ex - x) // 4, ey - 3, ez + (ez - z) // 4), 1, LOG)

    clusters = [(-17, 153, -15, 20, 9, 17), (-36, 145, -11, 16, 8, 17),
                (3, 149, -12, 17, 9, 16), (-24, 149, -32, 19, 8, 14),
                (-10, 144, 1, 17, 7, 14), (13, 144, -26, 13, 7, 12),
                (-44, 143, -27, 11, 7, 11), (-26, 145, 5, 11, 7, 11),
                (-19, 161, -22, 11, 7, 11), (-4, 159, -24, 11, 6, 11),
                (2, 141, 11, 10, 5, 9), (-48, 141, -10, 9, 6, 9)]
    for i, (cx, cy, cz, rx, ry, rz) in enumerate(clusters):
        elbow = ((x + cx) // 2, cy - 9, (z + cz) // 2)
        beam((x + 2, 126 + i % 3 * 3, z + 1), elbow, 3, LOG)
        beam(elbow, (cx, cy, cz), 2, LOG_X if abs(cx - x) > abs(cz - z) else LOG_Z)
        crown(cx, cy, cz, rx, ry, rz, 4, i)
    for i, (px, pz, length) in enumerate([
        (-43, -7, 7), (-31, 5, 11), (-21, 10, 9), (-15, 1, 11), (7, 4, 8),
        (11, -9, 10), (15, -22, 9), (-3, -37, 8), (-24, -39, 7), (-39, -32, 6),
        (-2, 9, 6), (-30, -4, 8), (-48, -18, 7), (1, 18, 4), (-38, 1, 6),
    ]):
        y = next((py for py in range(126, 172) if get_block(px, py, pz) in (LEAVES, GLOW)), None)
        if y is not None:
            for py in range(y - length, y):
                set_block(px, py, pz, CHAIN)
            set_block(px, y - length - 1, pz, HANGING)
            if i % 3 == 0:
                set_block(px, y, pz, GLOW)
    landmark("goldenwood_colossus", x - 15, 99, z + 1)


def crown(cx, cy, cz, rx, ry, rz, glow, seed):
    def place(x, y, z, distance):
        n = mix(x + 31 * y, z, seed)
        if distance > .80 + (n % 37) / 120 or get_block(x, y, z) not in (AIR, LEAVES):
            return
        patches = sin(x * .31 + z * .07) + sin(y * .39 - z * .23)
        lit = n % 29 < glow and (patches > .1 or n % 7 == 0)
        set_block(x, y, z, GLOW if lit else LEAVES)
        if lit and distance < .45 and n % 13 == 0:
            set_block(x, y, z, SHROOM)
    ball(cx, cy, cz, rx, ry, rz, place)


def tree(x, z, size, seed):
    y = SURFACE[x, z]
    if get_block(x, y, z) != TURF:
        return
    bend = -1 if seed % 2 else 1
    r = 2 if size >= 22 else 1
    beam((x, y, z), (x + bend, y + size * 2 // 3, z), r, LOG)
    beam((x + bend, y + size * 2 // 3, z), (x + bend * 3, y + size, z - bend), 1, LOG)
    radius = size * .38
    for ox, oz in ((0, 0), (5, 3), (-5, 0), (1, -5)):
        tip = (x + ox, y + size - (abs(ox) + abs(oz)) // 2, z + oz)
        beam((x + bend, y + size * 2 // 3, z), tip, 1, LOG)
        crown(*tip, radius * .78, radius * .55, radius * .75, 1, seed)
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        end = SURFACE.get((x + dx * 4, z + dz * 4), y)
        beam((x, y + 3, z), (x + dx * 4, end, z + dz * 4), 1, LOG)


def arrival_court():
    y = 111
    for x in range(-34, 35):
        for z in range(109, 155):
            edge = abs(x) >= 32 or z >= 152
            gutter = abs(x) == 29 or z in (109, 152)
            if edge or gutter:
                set_block(x, y, z, CUT if gutter else SMOOTH)
            if (abs(x) in (32, 33) or z in (153, 154)) and (z > 112 or abs(x) > 32):
                set_block(x, y + 1, z, STONE if (x + z) % 7 else HONEY_BRICK)
            if z in (114, 122, 130, 138, 146) and -28 <= x <= 29:
                set_block(x, y, z, MUD if x % 8 else CUT)
            mosaic = abs(x) + abs(z - 112)
            if mosaic <= 7 and z <= 115:
                set_block(x, y, z, HONEY_BRICK if mosaic in (3, 7) else SMOOTH)
            if abs(x) in (27, 28) and z % 8 in (3, 4, 5) and 115 < z < 152:
                set_block(x, y, z, MOSS_BLOCK if z % 8 == 4 else CUT)
                if z % 8 == 4:
                    set_block(x, y + 1, z, FLOWER)
    for z in (109, 122, 136, 150):
        for x in (-31, 31):
            lamp(x, y + 1, z, 4, 1 if x < 0 else -1)
    # Low side wings frame the first look down the valley.
    for x in (-9, 9):
        box(x - 2, y, 108, x + 2, y + 1, 111, CUT)
        box(x - 1, y + 2, 109, x + 1, y + 3, 110, SMOOTH)
        set_block(x, y + 4, 109, LANTERN)
        box(x - 2, y - 4, 105, x + 2, y, 107, STONE)
    box(-5, y, 109, 5, y, 109, SMOOTH)
    for x in (-33, 33):
        for z in (112, 129, 145):
            for y1 in range(89, 112):
                s = 0 if y1 < 102 else 1
                box(x - s, y1, z - 2, x + s, y1, z + 2, CUT if y1 % 5 == 0 else STONE)


def path_details():
    posts = [(-5, 98), (-3, 79), (-12, 65), (-13, 51), (-22, 38), (-31, 25),
             (-34, 10), (-39, -8), (-39, -29), (-29, -44), (-7, -53), (-35, 57),
             (-50, 59), (-78, 34), (-95, 9), (-109, -17), (-100, -43),
             (-86, -78), (-73, -74), (-53, -65), (-46, -43),
             (10, 55), (24, 63), (40, 60), (108, 59), (115, 45), (127, 33),
             (136, 16), (129, -5), (117, -13), (108, -24), (85, -88),
             (71, -75), (116, 74), (130, 82), (137, 87)]
    for index, (x, z) in enumerate(posts):
        y = SURFACE.get((x, z))
        if y is not None and get_block(x, y + 1, z) == AIR and get_block(x, y, z) != WATER:
            lamp(x, y, z, 3 if index % 4 else 2, 1 if index % 2 else -1)
    # Two roadside markers and a collapsed arch between village and upper road.
    for i, (x, z) in enumerate([(-90, 2), (-42, -46)]):
        y = SURFACE[x, z]
        for dx in (-3, 3):
            py = SURFACE[x + dx, z]
            box(x + dx - 1, py, z - 1, x + dx + 1, py + 1, z + 1, MOSS)
            box(x + dx, py + 2, z, x + dx, py + 4 + i, z, CUT)
        if i == 0:
            box(x - 3, y + 5, z, x - 1, y + 5, z, CRACKED)
            set_block(x - 2, y + 4, z, COPPER_GRATE)
            landmark("waystone_remains", x, y + 1, z)
        else:
            box(x - 3, y + 6, z, x + 1, y + 6, z, CUT)
            set_block(x - 1, y + 5, z, HANGING)


def bridge(x1, x2, z, y, stone):
    block = CUT if stone else DARK
    for x in range(x1, x2 + 1):
        for dz in range(-3, 4):
            offset = 1 if min(x - x1, x2 - x) >= 5 and stone else 0
            deck = y + offset
            if not -2 <= dz <= 2:
                set_block(x, deck + 1, z + dz, STONE if stone else FENCE_X)
            else:
                for py in range(deck + 1, deck + 5):
                    set_block(x, py, z + dz, AIR)
            set_block(x, deck - 1, z + dz, STONE if stone else PLANKS)
            set_block(x, deck, z + dz, block if (x + dz) % 5 else HONEY_BRICK)
    for x in range(x1 + 3, x2 - 1, 11):
        for dz in (-3, 3):
            floor = min(y - 3, SURFACE.get((x, z + dz), y - 9))
            box(x - 1, floor, z + dz, x + 1, y, z + dz, STONE if stone else BEAM)
            lamp(x, y + 2 if stone else y + 1, z + dz, 0)
        for dx in (-4, -3, 3, 4):
            if x1 < x + dx < x2:
                box(x + dx, y - 2, z - 3, x + dx, y - 1, z + 3, STONE if stone else BEAM)
    for x in (x1, x2):
        box(x - 1, y - 3, z - 3, x + 1, y - 1, z + 3, STONE)
    if not stone:
        landmark("broken_crossing", x1 + 8, y + 1, z)


def house(x, z, half, depth, ruined=False):
    y = 92
    box(x - half, y - 3, z - depth, x + half, y, z + depth, STONE)
    box(x - half + 1, y, z - depth + 1, x + half - 1, y, z + depth - 1, PLANKS)
    for xx in range(x - half, x + half + 1):
        for zz in range(z - depth, z + depth + 1):
            wall_x = abs(xx - x) == half
            wall_z = abs(zz - z) == depth
            if not (wall_x or wall_z):
                continue
            for dy in range(1, 6):
                if ruined and mix(xx, zz) % 7 < dy - 1:
                    continue
                if wall_z and zz > z and abs(xx - x) <= 1 and dy <= 3:
                    continue
                beam_pos = (wall_z and (xx - x) % 4 == 0) or (wall_x and (zz - z) % 4 == 0)
                window = dy in (2, 3) and (abs(xx - x) == 2 if wall_z else abs(zz - z) == 2)
                set_block(xx, y + dy, zz, BEAM if (beam_pos or dy == 5) else
                          material("orange_stained_glass") if window else CUT if dy == 1 else SMOOTH)
    for offset in range(half + 2):
        for zz in range(z - depth - 1, z + depth + 2):
            if ruined and zz < z + 1 and mix(offset, zz, 3) % 6 < 4:
                continue
            for sign, face in ((-1, "east"), (1, "west")):
                xx = x + sign * (half + 1 - offset)
                edge = zz in (z - depth - 1, z + depth + 1)
                roof = stairs("dark_oak" if edge else "deepslate_tile", face)
                set_block(xx, y + 5 + offset, zz, roof)
                if abs(zz - z) == depth:
                    for py in range(y + 6, y + 5 + offset):
                        set_block(xx, py, zz, BEAM if xx == x else SMOOTH)
    box(x, y + 6 + half, z - depth - 1, x, y + 6 + half, z + depth + 1, TILE)
    for xx in (x - half, x + half):
        set_block(xx, y + 5, z + depth + 1, DARK)
        set_block(xx, y + 4, z + depth + 1, HANGING)


def village():
    house(-69, 43, 5, 5)
    house(-49, 45, 5, 4, True)
    house(-67, 57, 4, 4, True)
    house(-82, 51, 4, 4)
    fixture(-72, 93, 40, "barrel", "chests/abandoned_hamlet")
    fixture(-53, 93, 46, "barrel", "chests/abandoned_hamlet")
    fixture(-65, 93, 55, "barrel", "chests/abandoned_hamlet")
    for x in range(-63, -54):
        for z in range(43, 52):
            if (x + 59) ** 2 + (z - 47) ** 2 <= 16:
                set_block(x, 92, z, MUD if mix(x, z) % 3 else STONE)
    for x in (-61, -57):
        for z in (45, 49):
            box(x, 93, z, x, 96, z, BEAM)
    box(-62, 96, 44, -56, 96, 50, DARK)
    box(-61, 97, 45, -57, 97, 49, COPPER)
    set_block(-59, 95, 50, HANGING)
    landmark("abandoned_hamlet", -60, 93, 52)


def masonry(x, y, z, old=False):
    n = mix(x + y * 61, z)
    if old:
        return (STONE, STONE, MOSS, CRACKED, HONEY_BRICK)[n % 5]
    return (SMOOTH, SMOOTH, SMOOTH, CUT, CUT, HONEY_BRICK)[n % 6]


def tower(x, y, z, radius, height, old=False, roof=False):
    thickness = 2
    rim = [(-radius, -radius), (-radius + 2, -radius), (radius - 2, -radius),
           (radius, -radius + 2), (radius, radius - 2), (radius - 2, radius),
           (-radius + 2, radius), (-radius, radius - 2)]
    for dx in range(-radius, radius + 1):
        for dz in range(-radius, radius + 1):
            d = max(abs(dx), abs(dz))
            if abs(dx) + abs(dz) > 2 * radius - 2:
                continue
            box(x + dx, y - 4, z + dz, x + dx, y, z + dz, STONE)
            for dy in range(1, height + 1):
                ring = d > radius - thickness or abs(dx) + abs(dz) > 2 * radius - 4
                if dy == height and d != radius:
                    set_block(x + dx, y + dy, z + dz, TILE if old else PLANKS)
                if not ring:
                    continue
                if dz >= radius - 1 and abs(dx) <= 1 and dy <= 4:
                    continue
                slit = (dy % 8 in (5, 6) and (dx == 0 or dz == 0))
                if not slit and not (old and dy > height - 6 and dx + dz < dy - height + radius // 2):
                    val = masonry(x + dx, y + dy, z + dz, old)
                    if dy in (3, height - 1, height):
                        val = CUT if not old else STONE
                    set_block(x + dx, y + dy, z + dz, val)
            if d == radius and (dx + dz) % 3 != 1:
                set_block(x + dx, y + height + 1, z + dz, STONE if old else CUT)

    # A climbable timber stair runs around the inside, with landings at the bends.
    step_r = radius - 2
    square = [(dx, step_r) for dx in range(-step_r + 1, step_r)]
    square += [(step_r, dz) for dz in range(step_r, -step_r, -1)]
    square += [(dx, -step_r) for dx in range(step_r, -step_r, -1)]
    square += [(-step_r, dz) for dz in range(-step_r, step_r + 1)]
    for i, dy in enumerate(range(1, height + 1)):
        dx, dz = square[i % len(square)]
        if i and i % 6 == 0:
            lx, lz = dx - (1 if dx > 0 else -1 if dx < 0 else 0), dz - (1 if dz > 0 else -1 if dz < 0 else 0)
            set_block(x + lx, y + dy, z + lz, DARK)
        set_block(x + dx, y + dy, z + dz, DARK)
        if dy > height - 4 and dy < height:
            set_block(x + dx, y + height, z + dz, AIR)
    if roof:
        for lift in range(radius + 1):
            r = radius + 1 - lift
            for dx in range(-r, r + 1):
                for dz in range(-r, r + 1):
                    if max(abs(dx), abs(dz)) == r and abs(dx) + abs(dz) <= 2 * r - (1 if r > 1 else 0):
                        set_block(x + dx, y + height + 3 + lift, z + dz, COPPER_TRIM if lift in (0, radius) else COPPER)
        box(x, y + height + 3 + radius, z, x, y + height + 6 + radius, z, COPPER_TRIM)
        set_block(x, y + height + 7 + radius, z, LANTERN)
    for i, (dx, dz) in enumerate(rim[::2]):
        if not old or i % 2 == 0:
            beam((x + dx, y - 3, z + dz), (x + dx, y + height - 2, z + dz), 0, CUT if not old else STONE)


def curtain(x1, z1, x2, z2, y, height, old=False, gate=None):
    horizontal = z1 == z2
    length = max(x2 - x1, z2 - z1)
    for i in range(length + 1):
        x, z = (x1 + i, z1) if horizontal else (x1, z1 + i)
        along = x if horizontal else z
        height_here = height - (max(0, 5 - abs(i - length // 3)) if old else 0)
        opening = abs(along - gate) if gate is not None else 100
        for offset in (-1, 0, 1):
            xx, zz = (x, z + offset) if horizontal else (x + offset, z)
            for dy in range(-3, height_here + 1):
                if opening <= 3 and 1 <= dy <= 7 - opening:
                    continue
                tile = masonry(xx, y + dy, zz, old)
                if dy in (0, height_here):
                    tile = STONE if old else CUT
                set_block(xx, y + dy, zz, tile)
            if abs(offset) == 1 and i % 4 < 2:
                set_block(xx, y + height_here + 1, zz, STONE if old else HONEY_BRICK)
        if opening in (3, 4):
            set_block(x, y + 5, z, LANTERN)


def great_hall():
    x, y, z = 100, 122, -68
    half, depth = 10, 12
    box(x - half - 1, y - 3, z - depth - 1, x + half + 1, y, z + depth + 1, STONE)
    for dx in range(-half, half + 1):
        for dz in range(-depth, depth + 1):
            wall_x, wall_z = abs(dx) == half, abs(dz) == depth
            aisle = abs(dx) <= 2
            floor = (CUT if (dx + dz) % 2 else HONEY_BRICK) if aisle else DARK if dz % 4 else PLANKS
            set_block(x + dx, y, z + dz, floor)
            if not (wall_x or wall_z):
                continue
            for dy in range(1, 14):
                door = wall_z and dz > 0 and abs(dx) <= 3 and dy <= 7 - abs(dx)
                if door:
                    continue
                rib = wall_z and dx % 5 == 0 or wall_x and dz % 6 == 0
                window = wall_x and abs(dz % 6 - 3) <= 1 and 4 <= dy <= 9 - abs(dz % 6 - 3)
                val = material("orange_stained_glass") if window else CUT if rib or dy in (1, 12) else masonry(x + dx, y + dy, z + dz)
                set_block(x + dx, y + dy, z + dz, val)
    for dz in range(-depth - 2, depth + 3):
        trim = abs(dz) >= depth + 1 or dz % 6 == 0
        for dy in range(12):
            r = 11 - dy
            for sign, face in ((-1, "east"), (1, "west")):
                val = stairs("waxed_exposed_cut_copper" if trim else "waxed_weathered_cut_copper", face)
                set_block(x + sign * r, y + 13 + dy, z + dz, val)
                if abs(dz) == depth and dy >= 2:
                    box(x + sign * r, y + 14, z + dz, x + sign * r, y + 12 + dy, z + dz,
                        SMOOTH if r % 3 else CUT)
                if dz % 6 == 0:
                    set_block(x + sign * r, y + 12 + dy, z + dz, BEAM)
        set_block(x, y + 25, z + dz, COPPER_TRIM)
    for dz in (-8, 0, 8):
        box(x, y + 11, z + dz, x, y + 24, z + dz, CHAIN)
        set_block(x, y + 10, z + dz, HANGING)
        for dx in (-7, 7):
            box(x + dx, y + 1, z + dz, x + dx, y + 8, z + dz, BEAM)
            set_block(x + dx, y + 1, z + dz, CUT)
            set_block(x + dx, y + 9, z + dz, LANTERN)
    box(x - 4, y + 1, z - 9, x + 4, y + 1, z - 7, HONEY_BRICK)
    box(x - 2, y + 2, z - 9, x + 2, y + 2, z - 8, CUT)
    box(x - 1, y + 3, z - 10, x + 1, y + 6, z - 10, COPPER_GRATE)
    for dx in (-2, 2):
        lamp(x + dx, y + 3, z - 8, 0)
    fixture(x, y + 3, z - 9, "chest[facing=south]", "chests/broken_spire")


def castles():
    x, y, z = 100, 122, -63
    box(78, y, -86, 122, y, -39, PATH)
    for dx in range(-4, 5):
        box(x + dx, y, -55, x + dx, y, -35, MUD if dx % 2 else CUT)
    curtain(78, -38, 122, -38, y, 9, gate=x)
    curtain(78, -86, 122, -86, y, 10, gate=94)
    curtain(77, -85, 77, -39, y, 9)
    curtain(123, -85, 123, -39, y, 9)
    for tx, tz, h in [(77, -38, 21), (123, -38, 19), (77, -86, 24), (123, -86, 22)]:
        tower(tx, y, tz, 6, h, roof=True)
    great_hall()
    for dx in (-12, 12):
        for dz in (-14, -1, 15):
            lamp(x + dx, y, z + dz, 3, 1 if dx < 0 else -1)
    # The southeast gate tower contains a shadowed cage under its landing.
    fixture(124, 124, -40, "spawner", "ruin_spark")
    lamp(99, 122, -35, 0)
    lamp(101, 122, -35, 0)

    box(-123, 107, -76, -91, 107, -54, PATH)
    curtain(-124, -57, -89, -57, 107, 7, old=True, gate=-106)
    curtain(-124, -78, -89, -78, 107, 5, old=True, gate=-111)
    curtain(-124, -77, -124, -58, 107, 5, old=True)
    curtain(-89, -77, -89, -58, 107, 4, old=True, gate=-73)
    tower(-121, 107, -73, 7, 25, old=True)
    tower(-92, 107, -61, 5, 13, old=True)
    # An opening into the broken spire at ground level from inside the courtyard.
    box(-123, 108, -67, -119, 110, -66, AIR)
    for xx, zz in [(-111, -63), (-101, -61), (-100, -72)]:
        lamp(xx, 107, zz, 0)
    fixture(-123, 133, -77, "chest[facing=east]", "chests/broken_spire")
    fixture(-120, 109, -76, "spawner", "ruin_spark")
    landmark("broken_spire", -121, 108, -65)
    landmark("collapsed_watchpost", -92, 108, -56)


def cliff_walk():
    # The switchback runs around the waterfall, below the clouds of spray.
    route = [(137, 88, 89), (148, 88, 84), (152, 92, 82), (152, 115, 71),
             (149, 124, 66), (147, 129, 63), (139, 130, 62)]
    plan = {}
    segments(route, 3, plan)
    for (x, z), (distance, height) in plan.items():
        y = round(height)
        rail = distance > 2.15
        # Keep the lookout mouth and the sanctuary threshold open.
        rail = rail and (x > 140 or z < 126) and (x > 139 or z > 91)
        box(x, y - 2, z, x, y, z, STONE if rail else CUT if mix(x, z) % 4 else HONEY_BRICK)
        box(x, y + 1, z, x, y + 5, z, AIR)
        if rail:
            set_block(x, y + 1, z, STONE if (x + z) % 4 else HONEY_BRICK)
        if rail and (x, z) in {(152, 89), (154, 96), (154, 108), (152, 121), (148, 128)}:
            set_block(x, y + 2, z, LANTERN)
    for x, z in [(146, 88), (152, 98), (152, 108), (149, 120), (145, 128)]:
        y = round(plan[x, z][1])
        beam((x, y - 2, z), (x - 8, y - 11, z - 3), 1, STONE)
        beam((x, y - 1, z), (x, y - 8, z), 1, CUT)


def waterfall_and_sanctuary():
    # A fixed stone spillway feeds two ribbons of falling water.
    for x in range(113, 148):
        z = round(98 + (x - 113) * .35)
        for dz in range(-3, 4):
            style = HONEY if mix(x, z + dz) % 3 else CALCITE
            box(x, 78, z + dz, x, 82, z + dz, style)
            box(x, 83, z + dz, x, 88, z + dz, AIR)
            if abs(dz) == 3:
                box(x, 83, z + dz, x, 84, z + dz, style)
            else:
                set_block(x, 83, z + dz, WATER)
    for x, z, length, breadth in ((148, 110, 49, 2), (146, 106, 34, 1)):
        for offset in range(-breadth, breadth + 1):
            top = 83 - abs(offset)
            for py in range(top - length + offset * 2, top + 1):
                set_block(x + (1 if py < top - 19 and offset >= 0 else 0), py, z + offset, FALL)
        # Pale stone behind the fall makes it readable from the bridge.
        for y in range(48, 82):
            xx = x - 3 - (82 - y) // 8
            for dz in range(-3, 4):
                set_block(xx, y, z + dz, CALCITE if dz % 3 else HONEY)
    for shift, z in enumerate(range(101, 116, 3)):
        x = 145 - shift
        beam((x - 4, 81, z), (x - 17, 60 - shift * 2, z), 2, TUFF)
    cliff_walk()

    y = 62
    # Tie the hanging sanctuary to a broad shelf of the island's exposed rock.
    def ledge(x, py, z, radius):
        if radius < .84 + (mix(x + py, z) % 29) / 135:
            set_block(x, py, z, TUFF if py % 8 < 5 else ANDESITE if py % 8 < 7 else HONEY)

    ball(108, 57, 125, 20, 11, 15, ledge)
    ball(121, 53, 130, 17, 8, 11, ledge)
    for z in (119, 125, 138):
        beam((106, 71, z - 5), (130, 54, z + 1), 3, HONEY)
    box(115, y - 4, 120, 143, y, 140, STONE)
    box(116, y + 1, 121, 139, y + 8, 139, AIR)
    for x in range(116, 140):
        for z in range(121, 140):
            for dy in range(1, 10):
                door = x == 139 and abs(z - 130) <= 3 and dy <= 7 - abs(z - 130)
                wall = x in (116, 139) or z in (121, 139)
                if (wall and not door) or dy == 9:
                    set_block(x, y + dy, z, masonry(x, y + dy, z, True))
            if abs(z - 130) <= 2:
                set_block(x, y, z, CUT if (x + z) % 2 else HONEY_BRICK)
            elif z in (123, 124, 136, 137) and 119 <= x <= 134:
                set_block(x, y, z, WATER)
    for x in (118, 124, 131, 138):
        for z in (125, 135):
            box(x, y + 1, z, x, y + 7, z, CUT)
            box(x - 1, y + 1, z - 1, x + 1, y + 1, z + 1, STONE)
            box(x - 1, y + 7, z - 1, x + 1, y + 7, z + 1, SMOOTH)
            set_block(x, y + 6, z + (1 if z < 130 else -1), HANGING)
    # A low inner screen hides the reliquary alcove; both side aisles pass behind it.
    box(120, y + 1, 127, 120, y + 5, 133, STONE)
    box(120, y + 6, 126, 120, y + 6, 134, HONEY_BRICK)
    for z in (126, 134):
        box(120, y + 5, z, 120, y + 5, z, CUT)
    box(117, y + 1, 129, 118, y + 1, 131, HONEY_BRICK)
    fixture(118, y + 2, 130, "chest[facing=east]", "chests/sunken_sanctuary")
    for z in (128, 132):
        set_block(117, y + 1, z, CUT)
        set_block(117, y + 2, z, LANTERN)
    for z in (121, 139):
        for x in (117, 130, 139):
            beam((x, y - 3, z), (x - 5, y - 13, z - 2), 2, STONE)
    for z in (126, 134):
        box(140, y + 1, z, 141, y + 6, z, CUT)
        lamp(141, y + 6, z, 0)
    set_block(139, y + 8, 130, CHAIN)
    set_block(139, y + 7, 130, HANGING)
    landmark("sunken_sanctuary", 140, 63, 130)


def root_altar():
    x, z, y = -64, -65, 103
    for dx in range(-8, 9):
        for dz in range(-7, 8):
            if (dx / 8) ** 2 + (dz / 7) ** 2 <= 1:
                set_block(x + dx, y, z + dz, masonry(x + dx, y, z + dz, True))
    for dx, dz in ((-6, -3), (-5, 4), (3, -5), (6, 2), (0, 6)):
        box(x + dx, y + 1, z + dz, x + dx, y + 4 + mix(dx, dz) % 4, z + dz, CUT)
    for end in ((x - 7, y, z + 2), (x - 2, y, z + 6), (x + 6, y, z - 4)):
        beam((x - 1, y + 2, z - 3), end, 1, LOG)
    box(x - 2, y + 1, z - 2, x + 2, y + 1, z + 1, HONEY_BRICK)
    fixture(x, y + 2, z - 1, "barrel", "chests/rootbound_altar")
    fixture(x - 3, y + 1, z - 4, "spawner", "guardian_ent")
    for dx in (-3, 3):
        lamp(x + dx, y, z - 1, 0)
    landmark("rootbound_altar", x, y + 1, z)


def woodland():
    groves = [
        [(-19, 105, 13), (-28, 94, 19), (-17, 81, 15), (18, 92, 18), (23, 105, 13), (24, 78, 13)],
        [(-60, 77, 16), (-49, 73, 12), (-83, 71, 19), (-95, 58, 18), (-96, 41, 20),
         (-72, 26, 16), (-96, 21, 17), (-111, 32, 22), (-112, 8, 18), (-131, 17, 18)],
        [(-144, -18, 17), (-127, -35, 21), (-93, -23, 17), (-131, -48, 15), (-83, -45, 18),
         (-64, -21, 18), (-58, -35, 15), (-84, -8, 16), (-74, 6, 13), (-57, 15, 17)],
        [(-80, -96, 22), (-64, -109, 19), (-51, -93, 17), (-33, -90, 15), (-29, -108, 21),
         (-14, -123, 20), (-4, -108, 16), (14, -117, 15), (6, -82, 14), (-12, -77, 13)],
        [(58, -113, 17), (67, -96, 13), (73, -107, 19), (84, -110, 20), (113, -106, 17),
         (131, -89, 18), (141, -71, 17), (145, -48, 21), (139, -26, 16), (148, -13, 20)],
        [(107, -5, 17), (104, 14, 20), (112, 27, 13), (139, 38, 15), (125, 55, 18),
         (122, 88, 15), (87, 101, 19), (109, 113, 15), (68, 115, 18), (50, 121, 17),
         (59, 75, 13), (45, 99, 13), (30, 37, 14), (14, 20, 14), (64, 11, 17)],
    ]
    protected = [(-17, -18, 48), (100, -63, 30), (-106, -65, 26),
                 (0, 126, 40), (-62, 46, 26), (-64, -65, 12)]
    for seed, grove in enumerate(groves):
        for x, z, size in grove:
            if (x, z) in SURFACE:
                tree(x, z, size, seed + x)
        # Shallow understory clusters follow the authored groves.
        for gx, gz, _ in grove[::2]:
            for dx in range(-17, 18, 5):
                for dz in range(-17, 18, 5):
                    n = mix(gx + dx, gz + dz, seed + 11)
                    x, z = gx + dx + n % 5, gz + dz + n % 7
                    if (x, z) not in SURFACE or ROAD.get((x, z), (30, 0))[0] < 9:
                        continue
                    if any(hypot(x - px, z - pz) < radius for px, pz, radius in protected):
                        continue
                    y = SURFACE[x, z]
                    if n % 16 == 0 and 4 < RIVER.get((x, z), (40, 0))[0]:
                        tree(x, z, 5 + n % 5, seed)
                    elif n % 5 == 0 and get_block(x, y, z) == TURF and get_block(x, y + 1, z) == AIR:
                        set_block(x, y, z, DIRT)
                        set_block(x, y + 1, z, material("bush"))
    for x, z in [(-76, 31), (-55, 28), (-90, 58), (-46, 54), (-71, -35), (-51, -55),
                 (-26, -73), (28, 45), (21, -38), (62, 17), (90, 36), (133, 41)]:
        for dz in range(4):
            y = SURFACE[x, z + dz]
            if get_block(x, y, z + dz) == TURF and get_block(x, y + 1, z + dz) == AIR:
                set_block(x, y + 1, z + dz, ROT)
    for x, z in sorted(SURFACE):
        y = SURFACE[x, z]
        if get_block(x, y, z) != TURF or get_block(x, y + 1, z) != AIR:
            continue
        d = ROAD.get((x, z), (30, 0))[0]
        wet = RIVER.get((x, z), (40, 0))[0]
        clustering = sin(x * .19 + z * .038) + cos(z * .21)
        n = mix(x, z, 11)
        if (5 < wet < 16 and n % 11 < 2) or (4 < d < 7 and clustering > .55 and n % 9 == 0):
            set_block(x, y, z, MOSS_BLOCK)
            set_block(x, y + 1, z, FLOWER if n % 4 else material("firefly_bush"))
        elif n % 30 == 0 and clustering > .2:
            set_block(x, y, z, DIRT)
            set_block(x, y + 1, z, material("short_grass" if n % 5 else "fern"))
        elif n % 42 == 0 and d > 6:
            set_block(x, y + 1, z, TUFF if n % 3 == 0 else MOSS)


def export(allow_inhabitants=False):
    def string(value):
        data = value.encode("ascii")
        return pack(">H", len(data)) + data

    if len(PALETTE) > 256:
        raise ValueError("The island block palette exceeds 256 states")
    tiles = sorted({(x // 16, z // 16) for (x, z), column in WORLD.items() if any(column)})
    fixtures = defaultdict(list)
    for (x, y, z), contents in sorted(FIXTURES.items()):
        name = PALETTE[get_block(x, y, z)]
        if not (allow_inhabitants and name == "minecraft:air") and not name.startswith(("minecraft:barrel", "minecraft:chest", "minecraft:spawner")):
            raise ValueError(f"Missing block for fixture at {(x, y, z)}")
        fixtures[x // 16, z // 16].append((x & 15, y, z & 15, contents))
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with OUTPUT.open("wb") as target, gzip.GzipFile(fileobj=target, filename="", mode="wb", mtime=0) as data:
        data.write(b"GWI1" + pack(">H", len(PALETTE)))
        for state in PALETTE:
            data.write(string(state))
        data.write(pack(">B", len(LANDMARKS)))
        for name, (x, y, z) in sorted(LANDMARKS.items()):
            data.write(string(name) + pack(">hBh", x, y, z))
        data.write(pack(">H", len(tiles)))
        for tx, tz in tiles:
            data.write(pack(">hh", tx, tz))
            for z in range(tz * 16, tz * 16 + 16):
                for x in range(tx * 16, tx * 16 + 16):
                    col = WORLD.get((x, z))
                    runs = bytearray()
                    if col:
                        y = 0
                        for block, group in groupby(col):
                            length = len(list(group))
                            if block:
                                runs.extend((y, length, block))
                            y += length
                    data.write(bytes([len(runs) // 3]) + runs)
            data.write(pack(">B", len(fixtures[tx, tz])))
            for x, y, z, contents in fixtures[tx, tz]:
                data.write(pack(">BBB", x, y, z) + string(contents))
    print(f"Exported {len(tiles)} chunks, {len(PALETTE)} block states and {len(FIXTURES)} fixtures ({OUTPUT.stat().st_size:,} bytes)")


def author_island():
    terrain()
    woodland()
    arrival_court()
    path_details()
    colossus()
    bridge(43, 101, 65, 86, False)
    bridge(10, 54, -66, 86, True)
    village()
    castles()
    root_altar()
    waterfall_and_sanctuary()


if __name__ == "__main__":
    author_island()
    export()
