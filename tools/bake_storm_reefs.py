"""Bake the hand-laid Storm Reefs map. Minecraft loads the resulting block layout."""

from math import cos, hypot, sin

import bake_goldenwood as island
from bake_goldenwood import AIR, CALCITE, WATER, beam, box, fixture, material, mix, segments, set_block, smooth

SEA_LEVEL = 100
ARENA = (0, -41)
ARENA_FLOOR = 104
PORTAL_Z = 64
BASALT = material("basalt")
BLACKSTONE = material("blackstone")
GRAVEL = material("gravel")
DEEPSLATE = material("deepslate")
PAVING = material("polished_deepslate")
TILES = material("deepslate_tiles")
COPPER = material("waxed_oxidized_cut_copper")
BRONZE = material("waxed_exposed_cut_copper")
PRISMARINE = material("prismarine")
BRICKS = material("prismarine_bricks")
DARK_PRISMARINE = material("dark_prismarine")
SEA_LIGHT = material("sea_lantern")
GLASS = material("cyan_stained_glass")
WOOD = material("dark_oak_planks")
RIB = material("stripped_dark_oak_log[axis=y]")
RAIL = material("iron_bars[east=true,west=true]")

# Each route is a sequence of (x, z, floor height) control points.
CAUSEWAY = [(1, 70, 105), (1, 46, 105), (1, 17, 105), (0, -12, 105), (0, -23, 104), (0, -39, 104)]
WRECK_PATH = [(1, 49, 105), (-15, 48, 104), (-26, 49, 102), (-43, 52, 101), (-53, 42, 103),
              (-53, 23, 105), (-44, 8, 106), (-30, -3, 108), (-23, -16, 105), (-13, -24, 104)]
TOWER_PATH = [(1, 22, 105), (8, 17, 105), (17, 13, 107), (24, 5, 111), (35, 2, 115), (44, 2, 115),
              (45, -9, 115), (36, -18, 110), (25, -23, 106), (16, -27, 104)]
LIGHTHOUSE_PATH = [(0, -61, 104), (0, -75, 105)]
ROUTES = [(CAUSEWAY, 4), (WRECK_PATH, 2), (TOWER_PATH, 2), (LIGHTHOUSE_PATH, 2)]
PATHS = {}
SHOULDERS = {}
SURFACE = {}

# Main shelf, western and eastern headlands, lighthouse promontory, southern breakwater.
LOBES = [(1, 6, 59, 74, 59), (-39, 21, 29, 40, 44), (39, -6, 29, 39, 43),
         (-6, -46, 36, 37, 50), (2, 60, 25, 22, 35), (-52, 28, 20, 18, 32), (49, 33, 13, 18, 29)]
BASINS = [(-36, 35, 21, 16), (29, 37, 18, 15)]


def terrain():
    for route, width in ROUTES:
        segments(route, width, PATHS)
        segments(route, width + 4, SHOULDERS)
    for x in range(-76, 76):
        for z in range(-89, 85):
            taper = max(depth * (1 - hypot((x - cx + 2.5 * sin(z / 9)) / rx,
                                           (z - cz + 1.8 * cos(x / 7)) / rz))
                        for cx, cz, rx, rz, depth in LOBES)
            if taper < 0:
                continue

            top = 105 + 2.2 * sin((x + z) / 12) + 2.9 * cos(z / 14) + 1.7 * sin(x / 6)
            radius = hypot(x - ARENA[0], z - ARENA[1])
            arena = smooth((27 - radius) / 5)
            top += (ARENA_FLOOR - top) * arena
            station = smooth((1.65 - hypot((x - 42) / 11, (z + 2) / 10)) / .65)
            top += (115 - top) * station
            landing = smooth((1.7 - hypot((x - 1) / 10, (z - PORTAL_Z) / 7)) / .7)
            top += (105 - top) * landing
            lighthouse = smooth((11 - hypot(x, z + 72)) / 4)
            top += (105 - top) * lighthouse

            basin = min(hypot((x - cx + 1.8 * sin(z / 4)) / rx, (z - cz + 2.3 * cos(x / 6)) / rz)
                        for cx, cz, rx, rz in BASINS)
            if basin < 1:
                basin_floor = 90 + 9 * basin + 4 * smooth((basin - .78) / .22)
                top = basin_floor
            elif basin < 1.17:
                top = max(top, SEA_LEVEL + 2)

            if (x, z) in PATHS:
                top = PATHS[x, z][1]
            elif (x, z) in SHOULDERS:
                distance, grade = SHOULDERS[x, z]
                top += (grade - top) * smooth((6 - distance) / 4)
                if basin >= .95:
                    top = max(top, SEA_LEVEL + 1)
            top = round(top)
            SURFACE[x, z] = top

            stone = mix(x, z, 26)
            bottom = max(27, top - 6 - round(taper * (1.1 + .10 * cos(x / 5 + z / 8))))
            box(x, bottom, z, x, top, z, BASALT if stone % 9 < 6 else DEEPSLATE)
            box(x, max(bottom, top - 5), z, x, top - 1, z, BLACKSTONE if stone % 7 == 0 else DEEPSLATE)
            if basin < 1 and top < SEA_LEVEL:
                set_block(x, top, z, PRISMARINE if stone % 5 < 2 else GRAVEL if stone % 5 < 4 else CALCITE)
                box(x, top + 1, z, x, SEA_LEVEL, z, WATER)
                if stone % 43 == 0 and top <= SEA_LEVEL - 2:
                    set_block(x, top, z, material("tube_coral_block" if basin < .65 else "brain_coral_block"))
                    set_block(x, top + 1, z, material("sea_pickle[pickles=2,waterlogged=true]"))
            elif (x, z) in PATHS:
                deck = WOOD if x < -21 and z > 42 else PAVING
                set_block(x, top, z, BRICKS if abs(x - 1) == 4 and z > 0 else deck)
            else:
                seam = abs(x / 9 + sin(z / 5)) < .19 or abs((x - 35) / 10 + sin(z / 7)) < .17
                if (basin < 1.22 and stone % 5 < 2) or (seam and stone % 5 != 0):
                    cap = CALCITE
                else:
                    cap = GRAVEL if stone % 7 == 0 else BASALT
                set_block(x, top, z, cap)


def marker(x, z, accent=COPPER):
    y = SURFACE[x, z]
    box(x, y, z, x, y + 1, z, PAVING)
    set_block(x, y + 2, z, SEA_LIGHT)
    set_block(x, y + 3, z, accent)


def breakwater():
    for x in range(-6, 9):
        for z in range(59, 70):
            set_block(x, 105, z, BRICKS if x in (-6, 8) or abs(z - PORTAL_Z) == 5 else PAVING)
            box(x, 106, z, x, 110, z, AIR)
    for x in range(-1, 3):
        for y in range(105, 110):
            if x in (-1, 2) or y in (105, 109):
                set_block(x, y, PORTAL_Z, DARK_PRISMARINE)
    for x in (-5, 7):
        marker(x, 61, BRONZE)
        marker(x, 67, BRONZE)
        box(x, 106, 62, x, 106, 66, BRICKS)
    for z in (12, 28, 43, 55):
        for x in (-4, 6):
            marker(x, z)
    for x, z in [(-13, 46), (-29, 51), (-48, 51), (-50, 19), (-29, -6),
                 (8, 20), (19, 13), (27, 7), (47, -11), (28, -25), (-5, -15), (5, -15)]:
        marker(x, z, BRONZE)


def sea_stacks():
    for cx, cz, height, width in [(-59, 33, 16, 4), (-45, 4, 13, 5), (-18, 59, 11, 4),
                                   (22, 60, 13, 4), (54, 38, 20, 5), (56, 29, 12, 3),
                                   (-32, -42, 14, 4), (33, -48, 13, 5), (22, -67, 12, 4)]:
        base = SURFACE[cx, cz] - 2
        for y in range(base, base + height):
            progress = (y - base) / height
            core = width * (1 - .74 * progress)
            lean = round(progress * 2)
            for dx in range(-width, width + 1):
                for dz in range(-width, width + 1):
                    if abs(dx) * .6 + abs(dz) + abs(dx - dz) * .28 <= core:
                        x, z = cx + dx + lean, cz + dz
                        if (x, z) not in PATHS:
                            stone = PRISMARINE if y % 11 == 0 and mix(x, z) % 3 != 0 else BASALT
                            set_block(x, y, z, stone)
    for x, z in [(-58, 25), (-56, 16), (-29, 64), (-43, 17), (-32, -12),
                 (58, 13), (44, 22), (53, 42), (26, 46), (8, 60), (30, -8)]:
        y = SURFACE[x, z]
        if y > SEA_LEVEL and (x, z) not in PATHS:
            set_block(x, y + 1, z, material("dead_tube_coral_fan"))

    # Eroded arch over the trail from the causeway to the signal tower.
    points = [(15, SURFACE[15, 8], 8), (16, 115, 8), (18, 120, 9), (21, 120, 11),
              (24, 117, 13), (25, SURFACE[25, 15], 15)]
    for start, end in zip(points, points[1:]):
        beam(start, end, 2, BASALT)
    beam((16, 116, 8), (23, 119, 12), 1, PRISMARINE)


def shipwreck():
    cx, cz = -36, 35
    for offset in range(-12, 14):
        z = cz + offset
        width = 1 + round(5 * (1 - (abs(offset) / 14) ** 1.5))
        floor = 94 + abs(offset) // 8
        box(cx - width, floor, z, cx + width, floor, z, WOOD)
        for side in (-1, 1):
            if side == -1 or offset not in range(1, 8):
                x = cx + side * width
                box(x, floor + 1, z, x, 99, z, RIB if offset % 4 == 0 else WOOD)
    box(-39, 101, 36, -33, 101, 44, WOOD)
    box(-41, 101, 41, -32, 101, 43, WOOD)
    box(-43, 101, 43, -41, 101, 51, WOOD)
    box(-43, 98, 48, -43, 101, 48, RIB)
    box(-36, 101, 41, -36, 113, 41, RIB)
    beam((-36, 111, 41), (-29, 109, 42), 1, WOOD)
    for x in range(-35, -29):
        for y in range(106 + (x + 35) // 2, 110):
            if (x, y) not in [(-34, 106), (-32, 108), (-30, 109)]:
                set_block(x, y, 43, material("light_gray_wool"))
    for x, z in [(-35, 40), (-41, 46)]:
        set_block(x, 101, z, SEA_LIGHT)
        island.FIXTURES[x, 102, z] = "minecraft:drowned"
    set_block(-36, 95, 35, SEA_LIGHT)
    fixture(-35, 96, 35, "chest[facing=south,waterlogged=true]", "chests/storm_reefs_wreck")


def signal_tower():
    for x in range(36, 49):
        for z in range(-7, 6):
            box(x, 113, z, x, 115, z, TILES)
            box(x, 116, z, x, 122, z, AIR)
            if x not in (36, 48) and z not in (-7, 5):
                continue
            for y in range(116, 122):
                if y < 120 and ((x == 36 and 0 <= z <= 3) or (z == -7 and 42 <= x <= 46)
                                or (z == 5 and 45 <= x <= 46)):
                    continue
                window = y in (118, 119) and (x == 48 and -4 <= z <= 2 or z == 5 and 40 <= x <= 44
                                               or z == -7 and 38 <= x <= 40)
                set_block(x, y, z, GLASS if window else PAVING if y in (116, 121) else TILES)
    for tier in range(3):
        box(35 + tier, 122 + tier, -8 + tier, 49 - tier, 122 + tier, 6 - tier, COPPER)
    box(37, 125, -1, 40, 130, 2, TILES)
    box(38, 131, 0, 39, 133, 1, BRONZE)
    set_block(38, 134, 0, material("lightning_rod"))
    beam((38, 130, 0), (33, 130, 0), 1, BRONZE)
    set_block(33, 129, 0, SEA_LIGHT)
    set_block(45, 116, 1, material("crafting_table"))
    set_block(44, 116, 1, material("cartography_table"))
    set_block(45, 116, 3, material("lantern"))
    fixture(44, 116, 3, "barrel[facing=up]", "chests/storm_reefs_station")
    for z in range(7, 10):
        box(40, 115, z, 47, 115, z, TILES)
    for x in range(40, 48):
        set_block(x, 116, 9, RAIL)
    box(46, 116, 6, 47, 116, 8, AIR)


def storm_ring():
    cx, cz = ARENA
    for x in range(cx - 23, cx + 24):
        for z in range(cz - 23, cz + 24):
            dx, dz = x - cx, z - cz
            radius = hypot(dx, dz)
            if radius > 23:
                continue
            box(x, ARENA_FLOOR + 1, z, x, 110, z, AIR)
            if 4.1 <= radius <= 4.9:
                tile = SEA_LIGHT if mix(x, z) % 3 == 0 else DARK_PRISMARINE
            elif 19.5 <= radius < 20.5:
                tile = BRONZE if (dx + dz) % 3 == 0 else COPPER
            elif 9.7 <= radius <= 10.3 or 14.7 <= radius <= 15.3:
                tile = PAVING
            elif abs(dx) <= 1 or abs(dz) <= 1 or abs(abs(dx) - abs(dz)) <= .55:
                tile = BRICKS
            else:
                tile = TILES if radius <= 20 else PAVING
            box(x, ARENA_FLOOR - 2, z, x, ARENA_FLOOR - 1, z, DEEPSLATE)
            set_block(x, ARENA_FLOOR, z, tile)
            if 22.1 <= radius and (abs(dx) > 4 or abs(dz) < 9):
                set_block(x, ARENA_FLOOR + 1, z, PAVING)
    set_block(cx, ARENA_FLOOR, cz + 8, material("lodestone"))
    for x in (cx - 1, cx + 1):
        set_block(x, ARENA_FLOOR, cz + 8, SEA_LIGHT)
    for side in (-1, 1):
        for end in (-1, 1):
            x, z = cx + side * 17, cz + end * 16
            box(x - 1, 102, z - 1, x + 1, 108, z + 1, TILES)
            beam((x, 108, z), (x, 118, z), 1, PAVING)
            beam((x, 118, z), (x - side * 4, 124, z - end * 3), 1, COPPER)
            beam((x - side * 4, 124, z - end * 3), (x - side * 7, 126, z - end * 6), 1, BRONZE)
            set_block(x - side * 7, 126, z - end * 6, SEA_LIGHT)


def lighthouse():
    cx, cz = 0, -72
    for x in range(-7, 8):
        for z in range(cz - 7, cz + 8):
            dx, dz = x - cx, z - cz
            shape = max(abs(dx), abs(dz)) + .42 * min(abs(dx), abs(dz))
            for y in range(104, 144):
                if y < 107:
                    block = PAVING if shape <= 6.8 else AIR
                elif y < 127:
                    if shape > 5.7 or abs(dx) <= 1 and dz > 0 and y <= 110:
                        continue
                    if y in (116, 126) and (dx, dz) != (0, 1):
                        block = TILES
                    elif shape >= 4.4:
                        slit = y in (111, 112, 119, 120, 123, 124) and (dx == 0 or dz == 0)
                        block = GLASS if slit else PRISMARINE if y in (114, 122, 126) else PAVING
                    else:
                        continue
                elif y < 135:
                    if shape > 5.7:
                        continue
                    if y in (127, 134) and (dx, dz) != (0, 1):
                        block = COPPER
                    elif shape >= 4.4:
                        block = BRONZE if abs(dx) >= 3 and abs(dz) >= 3 else GLASS
                    else:
                        continue
                else:
                    roof = 5.7 - (y - 135) * .65
                    block = (BRONZE if y in (135, 143) else COPPER) if shape <= roof else AIR
                if block != AIR:
                    set_block(x, y, z, block)
    box(0, 107, cz, 0, 133, cz, PAVING)
    box(0, 107, cz + 1, 0, 132, cz + 1, material("ladder[facing=south]"))
    box(-1, 129, cz - 2, 1, 132, cz - 1, SEA_LIGHT)
    for x in (-2, 2):
        set_block(x, 107, cz + 4, SEA_LIGHT)


def author_island():
    terrain()
    sea_stacks()
    breakwater()
    shipwreck()
    signal_tower()
    storm_ring()
    lighthouse()


if __name__ == "__main__":
    island.OUTPUT = island.OUTPUT.with_name("storm_reefs.bin.gz")
    author_island()
    island.export(allow_inhabitants=True)
