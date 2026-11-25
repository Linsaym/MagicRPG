"""Bake the Lilac Meadows map and its initial inhabitants into the mod resource."""
from math import cos, hypot, sin

import bake_goldenwood as map_data
from bake_goldenwood import AIR, CALCITE, COPPER, DARK, DIRT, FIXTURES, LANTERN, WATER
from bake_goldenwood import box, get_block, material, mix, segments, set_block

WHITE = material("magicrpg:white_stone")
GRASS = material("magicrpg:lilac_grass")
PAVING = material("polished_diorite")
PATH = material("packed_mud")
BRICK = material("smooth_quartz")
RUBBLE = material("tuff")
WOOD = material("dark_oak_log")
FENCE = material("dark_oak_fence[east=true,west=true]")
RAIL = material("dark_oak_fence[north=true,south=true]")
FLOWER = material("allium")
SLAB = material("polished_diorite_slab")
WORKBENCH = material("crafting_table")
STONECUTTER = material("stonecutter[facing=south]")
AMETHYST = material("amethyst_block")
GLASS = material("purple_stained_glass")
LIGHT = material("pearlescent_froglight")
VEIN = material("crying_obsidian")
BASALT = material("polished_basalt[axis=y]")
DEEPSLATE = material("deepslate_tiles")
PETALS = material("pink_petals[flower_amount=4]")
PERCH = material("waxed_oxidized_cut_copper")
MARBLE = material("quartz_bricks")
PILLAR = material("quartz_pillar[axis=y]")
GOLD = material("waxed_exposed_cut_copper")
BOOKS = material("bookshelf")
CARPET = material("purple_carpet")
STAIRS = {
    direction: material(f"quartz_stairs[facing={direction}]")
    for direction in ("north", "south", "west", "east")
}
SURFACE = {}

LOOP = [(0, 48, 112), (-22, 42, 114), (-40, 22, 115), (-43, -9, 113), (-33, -33, 109),
        (-16, -48, 104), (0, -52, 104), (19, -48, 107), (41, -23, 112),
        (47, 6, 114), (35, 30, 113), (18, 42, 112), (0, 43, 112), (0, 48, 112)]
RIDGES = [[(-41, 30), (-48, 18), (-50, 2)], [(-36, 20), (-37, 10), (-38, 2)],
          [(41, 13), (47, 25), (41, 39)], [(10, -25), (25, -34), (31, -35)]]


def terrain():
    trail = {}
    ridge = {}
    segments(LOOP, 4, trail)
    for points in RIDGES:
        segments([(x, z, 0) for x, z in points], 5, ridge)
    for x in range(-68, 69):
        for z in range(-67, 70):
            edge = hypot((x - 2 * sin(z * .09)) / 63, (z - 1) / 64)
            rim = 1 + .055 * sin(x * .18 + z * .16) + .046 * cos(z * .15)
            if edge > rim:
                continue
            h = round(113 + 2.5 * sin(x * .09) * cos(z * .12) + 1.8 * cos((x + z) * .12)
                      - max(0, edge - .82) * 26)
            if (x, z) in ridge:
                h += round((5 - ridge[x, z][0]) * .75)
            distance, graded = trail.get((x, z), (5, h))
            if distance <= 4:
                h = round(h + (graded - h) * min(1, (5 - distance) / 3))
            court = -21 <= x <= 21 and 46 <= z <= 60
            if court or (abs(x) <= 5 and 40 <= z <= 47):
                h = 112
            if abs(x) <= 4 and -54 <= z <= -50:
                h = 104
            pond = hypot((x - 10) / 9, (z - 35) / 6)
            if 1 <= pond <= 1.22:
                h = max(h, 111)
            if pond < 1:
                h = 108 if pond < .65 else 109
            bottom = round(48 + 57 * edge ** 2.45 + 6 * sin(x * .22) * cos(z * .19))
            box(x, min(bottom, h), z, x, h, z, WHITE)
            for y in range(min(bottom, h), h - 2):
                seam = sin(y * .39 + x * .08 - z * .06)
                if y < bottom + 4 or seam > .84:
                    set_block(x, y, z, BASALT if seam > .94 else RUBBLE)
            n = mix(x, z, 3)
            top = GRASS
            if pond < 1:
                top = CALCITE if n % 4 else PAVING
            elif court or distance < 1.65:
                top = WHITE if court else PATH if n % 5 else PAVING
            elif ridge.get((x, z), (9,))[0] < 2.5:
                top = WHITE if n % 3 else CALCITE
            elif distance > 2.5:
                set_block(x, h - 1, z, DIRT)
            set_block(x, h, z, top)
            if pond < 1:
                box(x, h + 1, z, x, 110, z, WATER)
            elif top == GRASS and not (-5 <= x <= 5 and 35 <= z <= 47):
                if n % (6 if sin(x * .4 + z * .15) > .4 else 32) == 0:
                    set_block(x, h + 1, z, FLOWER if n % 3 else PETALS)
            SURFACE[x, z] = h

    # Old viewing terrace along the western edge of the loop.
    box(-51, 111, 17, -40, 115, 24, WHITE)
    box(-51, 115, 17, -40, 115, 24, PAVING)
    box(-51, 116, 17, -51, 116, 24, BRICK)
    for z in (17, 24):
        box(-51, 116, z, -49, 117, z, CALCITE)
        set_block(-50, 118, z, LANTERN)
    for x, z, kind in [(-30, 30, "star_hare"), (-26, 32, "star_hare"), (26, 35, "star_hare"),
                       (29, 36, "star_hare"), (-47, 7, "chalk_guardian"), (45, 22, "chalk_guardian")]:
        y = SURFACE[x, z] + 1
        set_block(x, y, z, AIR)
        FIXTURES[x, y, z] = "magicrpg:" + kind


def crystal(x, y, z, height, lean=1):
    """A broad rooted crystal with a leaning crown and a lit fracture at its base."""
    box(x - 2, y - 1, z - 2, x + 2, y, z + 2, RUBBLE)
    for step in range(height):
        cx = x + lean * (step // 4)
        radius = 2 if step < height // 2 else 1 if step < height - 1 else 0
        for dx in range(-radius, radius + 1):
            for dz in range(-radius, radius + 1):
                if abs(dx) + abs(dz) <= radius + 1:
                    set_block(cx + dx, y + step, z + dz,
                              GLASS if step > height - 4 or abs(dx) + abs(dz) > radius else AMETHYST)
    set_block(x, y, z + 2, LIGHT)
    set_block(x + 2, y, z, VEIN)


def chest(x, y, z, facing, contents):
    set_block(x, y, z, material(f"chest[facing={facing}]"))
    FIXTURES[x, y, z] = "magicrpg:chests/" + contents


def landmarks():
    for x, z, height, lean in [(22, 31, 17, -1), (27, 28, 10, 1), (18, 26, 7, -1),
                              (-18, 21, 13, 1), (-24, 20, 8, -1), (35, -13, 12, -1),
                              (39, -16, 7, 1), (-23, -27, 11, 1), (4, -15, 8, -1)]:
        crystal(x, SURFACE[x, z] + 1, z, height, lean)

    # An open observatory at the end of the western terrace.
    for x in range(-53, -43):
        for z in range(17, 25):
            radius = hypot(x + 48, z - 21)
            if radius <= 5:
                box(x, 112, z, x, 115, z, WHITE)
                set_block(x, 115, z, PERCH if 3 <= radius <= 4 else DEEPSLATE)
    for x, z, height in [(-51, 18, 6), (-51, 24, 8), (-45, 17, 4)]:
        box(x, 116, z, x, 115 + height, z, CALCITE)
        set_block(x, 116 + height, z, LIGHT)
    # Carry the trail onto the terrace with a clear approach and two low markers.
    for x in range(-45, -38):
        for z in range(20, 23):
            box(x, 112, z, x, 115, z, WHITE)
            set_block(x, 115, z, PERCH if z == 21 else PAVING)
            box(x, 116, z, x, 118, z, AIR)
    for z in (19, 23):
        set_block(-42, 116, z, PERCH)
        set_block(-42, 117, z, LIGHT)
    set_block(-49, 116, 21, PERCH)
    chest(-49, 117, 21, "east", "meadow_observatory")


def stairs(x, z, floor, dx, dz, steps, width, rise, tunnel=False):
    """Start one step after a landing. Stair blocks are placed above the lower tread."""
    direction = {(0, -1): "north", (0, 1): "south", (1, 0): "east", (-1, 0): "west"}
    high_side = direction[dx * rise, dz * rise]
    for i in range(1, steps + 1):
        level = floor + rise * i
        for offset in range(-(width // 2), width // 2 + 1):
            px, pz = x + dx * i + dz * offset, z + dz * i + dx * offset
            box(px, level - 1, pz, px, level, pz, WHITE)
            box(px, level + 1, pz, px, level + (4 if tunnel else 3), pz, AIR)
            set_block(px, level + (rise < 0), pz, STAIRS[high_side])


def house(cx, cz, workshop, front):
    depth = 4
    half = 6
    box(cx - half, 81, cz - depth, cx + half, 81, cz + depth, DARK)
    box(cx - half, 82, cz - depth, cx + half, 87, cz + depth, MARBLE)
    box(cx - half + 1, 82, cz - depth + 1, cx + half - 1, 87, cz + depth - 1, AIR)
    for x in (cx - half, cx + half):
        for z in (cz - depth, cz + depth):
            box(x, 82, z, x, 88, z, WOOD)
    box(cx - half, 87, cz - depth, cx + half, 87, cz - depth, GOLD)
    box(cx - half, 87, cz + depth, cx + half, 87, cz + depth, GOLD)
    for x in (cx - 4, cx + 4):
        set_block(x, 85, cz - depth, material("orange_stained_glass"))
        set_block(x, 85, cz + depth, material("orange_stained_glass"))
    doorway = cz + depth * front
    box(cx - 1, 82, doorway, cx + 1, 84, doorway, AIR)
    for strip in range(-5, 6):
        y = 88 + (5 - abs(strip)) // 2
        box(cx - half - 1, y, cz + strip, cx + half + 1, y, cz + strip, COPPER)
    set_block(cx - 2, 85, doorway, LANTERN)
    if workshop:
        for dx, block in [(-4, WORKBENCH), (-3, STONECUTTER), (3, CALCITE), (4, WHITE)]:
            set_block(cx + dx, 82, cz - 2 * front, block)
        set_block(cx + 4, 83, cz - 2 * front, SLAB)
        for dx, dz in [(-2, 0), (2, 0)]:
            FIXTURES[cx + dx, 82, cz + dz] = "magicrpg:buried_worker"
        if cx > 0:
            chest(cx, 82, cz - 2 * front, "south", "meadow_workshop")
    else:
        box(cx - 4, 82, cz - 2 * front, cx - 2, 82, cz - 2 * front, DARK)
        set_block(cx + 3, 82, cz - 2 * front, material("bookshelf"))


def wing(side):
    """A paired hall with a street entrance and a second stair back by the arena."""
    near, far = sorted((side * 35, side * 50))
    box(near, 88, -17, far, 104, 17, MARBLE)
    box(near + 1, 89, -16, far - 1, 101, 16, AIR)
    for x in range(near + 1, far):
        for z in range(-16, 17):
            local_x = abs(x)
            floor = DEEPSLATE if local_x in (37, 48) or abs(z) in (6, 15) else PAVING
            if local_x == 42 or z == 0:
                floor = GOLD if local_x == 42 and z == 0 else PERCH
            set_block(x, 88, z, floor)
    for z in (-14, -7, 0, 7, 14):
        for x in (side * 36, side * 49):
            box(x, 89, z, x, 99, z, PILLAR)
            box(x, 89, z - 1, x, 89, z + 1, DEEPSLATE)
            set_block(x, 97, z + 1, LIGHT)
        box(near + 1, 100, z, far - 1, 100, z, GOLD)
        set_block(side * 42, 99, z, LIGHT)
    for z in (-12, 12):
        stairs(28 * side, z, 81, side, 0, 7, 5, 1, True)
        lo, hi = sorted((side * 35, side * 37))
        box(lo, 89, z - 2, hi, 93, z + 2, AIR)
        box(lo, 94, z - 2, hi, 94, z + 2, GOLD)
    if side < 0:
        for offset in (-11, -5, 5, 11):
            box(-47, 89, offset, -45, 95, offset, BOOKS)
            set_block(-44, 90, offset, LANTERN)
        box(-47, 89, -2, -45, 89, 2, DARK)
        set_block(-46, 90, 0, material("lectern[facing=east]"))
    else:
        for z in (-8, 0, 8):
            crystal(46, 89, z, 7 if z == 0 else 4, -1)
            box(38, 89, z - 1, 39, 89, z + 1, DEEPSLATE)
            set_block(39, 90, z, material("copper_bulb[lit=true]"))
    FIXTURES[side * 40, 89, -8] = "magicrpg:city_stone_guard"
    FIXTURES[side * 41, 89, 7] = "magicrpg:hollow_armor"


def crypt():
    box(-14, 64, -13, 14, 77, 10, MARBLE)
    box(-13, 66, -12, 13, 76, 9, AIR)
    for x in range(-13, 14):
        for z in range(-12, 10):
            floor = DEEPSLATE if abs(x) < 4 else PAVING
            if abs(x) == 4 or z in (-10, 3, 8):
                floor = PERCH
            set_block(x, 65, z, floor)
    for x in (-6, 6):
        for z in (-10, -4, 2, 8):
            box(x, 66, z, x, 73, z, PILLAR)
            set_block(x, 74, z, GOLD)
            box(-6, 75, z, 6, 75, z, MARBLE)
            set_block(0, 74, z, LIGHT)
        for z in (-7, -1, 5):
            box(x - 1, 66, z - 1, x + 1, 66, z + 1, DEEPSLATE)
            box(x, 67, z - 1, x, 67, z + 1, GOLD)
            set_block(x, 68, z, AMETHYST)
    for x in (-4, 4):
        stairs(x, 24, 81, 0, -1, 16, 3, -1, True)
        set_block(x, 82, 25, LIGHT)
        for z in (12, 17, 22):
            y = 81 - (24 - z)
            marker = x + (2 if x > 0 else -2)
            box(marker, y + 1, z, marker, y + 3, z, PILLAR)
            set_block(marker, y + 4, z, LANTERN)
    for z in range(-10, 8):
        set_block(0, 66, z, CARPET)
    box(-3, 66, -12, 3, 69, -11, PILLAR)
    box(-2, 67, -10, 2, 71, -10, AMETHYST)
    set_block(0, 70, -9, LIGHT)
    chest(0, 66, -9, "south", "meadow_crypt")
    FIXTURES[-9, 66, -6] = "magicrpg:hollow_armor"
    FIXTURES[9, 66, -6] = "magicrpg:hollow_armor"


def city():
    # Vault: 30 clear blocks above the central floor, with stepped stone ribs towards the walls.
    for x in range(-36, 37):
        for z in range(-31, 33):
            if abs(x) + abs(z + 1) > 59:
                continue
            ceiling = min(SURFACE[x, z] - 2, 111 - max(0, abs(x) - 28) // 2)
            box(x, 81, z, x, 81, z, PAVING if mix(x, z) % 4 else BRICK)
            box(x, 82, z, x, ceiling, z, AIR)
            set_block(x, ceiling + 1, z, CALCITE if mix(x, z) % 5 == 0 else WHITE)

    # Main sightline from the pond stair to the city gates.
    stairs(0, 42, 112, 0, -1, 8, 3, -1, True)
    box(-2, 105, 33, 2, 108, 33, AIR)
    box(-9, 104, 28, 9, 104, 33, BRICK)
    box(-9, 105, 28, 9, 105, 28, FENCE)
    for x in (-8, -2, 2, 8):
        set_block(x, 105, 30, LANTERN)
    box(9, 104, 30, 32, 104, 32, DARK)
    box(9, 105, 30, 16, 105, 30, FENCE)
    for x in (-20, 20):
        box(x - 3, 104, -3, x + 3, 104, 32, DARK)
        box(x - 3, 105, -3, x - 3, 105, 29, RAIL)
        box(x + 3, 105, -3, x + 3, 105, 29, RAIL)
        for z in (3, 15, 27):
            box(x, 94, z, x, 103, z, WOOD)
            set_block(x + 2, 105, z, LANTERN)
    # Two bridges span the central sightline at different distances.
    for z, x_start, x_end in [(10, -19, 19), (-1, -19, 4)]:
        box(x_start, 104, z - 1, x_end, 104, z + 1, DARK)
        for side in (-2, 2):
            box(x_start, 104, z + side, x_end, 104, z + side, DARK)
            box(x_start, 105, z + side, x_end, 105, z + side, FENCE)
        for edge in (-17, 17):
            if x_start <= edge <= x_end:
                box(edge, 105, z - 1, edge, 105, z + 1, AIR)
        for x in (-16, 0):
            box(x, 96, z, x, 103, z, AIR)
            set_block(x, 103, z, material("lantern[hanging=true]"))
    box(4, 105, -2, 4, 105, 0, RAIL)
    # Lower route branches east directly from the balcony. Upper route descends down the west wall.
    box(28, 104, 27, 32, 104, 32, DARK)
    stairs(30, 28, 104, -1, 0, 23, 3, -1)
    for x in (10, 17, 24):
        y = 104 - (30 - x)
        set_block(x, y + 1, 26, BRICK)
        set_block(x, y + 2, 26, LANTERN)
    box(-32, 104, -2, -20, 104, 1, DARK)
    box(-23, 105, -2, -23, 105, 1, AIR)
    stairs(-30, 0, 104, 0, -1, 23, 3, -1)
    for z in (-5, -12, -19):
        y = 104 + z
        set_block(-28, y, z, BRICK)
        set_block(-28, y + 1, z, LANTERN)
    for x, z in [(-20, 13), (20, 15), (-5, 10)]:
        FIXTURES[x, 105, z] = "magicrpg:hollow_armor"

    for x in (-13, 13):
        house(x, 21, False, -1)
        house(x, 5, True, 1)

    # Luminous channels converge on the gate. The old bridges still control the crossings.
    for x in (-24, 24):
        box(x - 1, 80, -6, x + 1, 80, 25, RUBBLE)
        box(x - 1, 81, -6, x + 1, 81, 25, GLASS)
        box(x - 2, 81, -6, x - 2, 81, 25, BASALT)
        box(x + 2, 81, -6, x + 2, 81, 25, BASALT)
        for z in range(-5, 26, 4):
            set_block(x, 80, z, LIGHT)
        for z in (23, 12, 0):
            box(x - 1, 81, z, x + 1, 81, z + 1, DARK)
        lo, hi = sorted((x, 8 if x > 0 else -8))
        box(lo, 80, -7, hi, 80, -5, RUBBLE)
        box(lo, 81, -7, hi, 81, -5, GLASS)
        for channel_x in range(lo, hi + 1, 4):
            set_block(channel_x, 80, -6, LIGHT)
        FIXTURES[x, 82, 12] = "magicrpg:city_stone_guard"

    for x in (-31, -17, -7, 7, 17, 31):
        for z in (-19, -5, 12, 27):
            if get_block(x, 82, z) == AIR and get_block(x, 81, z) != AIR:
                set_block(x, 82, z, BRICK)
                set_block(x, 83, z, LANTERN)
    for x in (-13, 13):
        set_block(x, 91, 21, LANTERN)
        set_block(x, 91, 5, LANTERN)

    for x in range(-16, 17):
        for z in range(-26, -7):
            radius = hypot(x, (z + 19) * 1.55)
            set_block(x, 81, z, PERCH if 6.5 < radius < 7.9 or 10.7 < radius < 12
                      else DEEPSLATE if radius < 14 else PAVING)
    for x in (-7, 7):
        for z in (-24, -14):
            set_block(x, 81, z, LIGHT)
    # Keep the dueling floor open. The surrounding colonnades frame the gate.
    for x in (-16, 16):
        for z in (-23, -15, -7):
            box(x, 82, z, x, 94, z, PILLAR)
            set_block(x, 93, z, LIGHT)
        box(x, 95, -25, x, 95, -6, GOLD)
    for x in range(-15, 16):
        for z in range(-7, 0):
            set_block(x, 81, z, PERCH if abs(x) in (5, 11) else DEEPSLATE if abs(x) < 5 else PAVING)
    # Tall gate with copper lintel, buttresses and an open postern up to the northern meadow.
    box(-19, 82, -29, 19, 96, -27, WHITE)
    box(-5, 82, -30, 5, 91, -26, AIR)
    box(-4, 92, -27, 4, 92, -26, COPPER)
    box(-3, 93, -27, 3, 93, -26, COPPER)
    for x in (-17, -11, -6, 6, 11, 17):
        box(x - 1, 82, -26, x + 1, 94, -26, CALCITE)
        set_block(x, 86, -25, BRICK)
        set_block(x, 87, -25, LANTERN)
        set_block(x, 92, -25, BRICK)
        set_block(x, 93, -25, LANTERN)
    for x in (-8, 8):
        box(x, 96, -29, x, 109, -27, WHITE)
        set_block(x, 96, -26, material("lantern[hanging=true]"))
    # The oath seal crowns the gate, visible from the entrance balcony.
    for x in range(-10, 11):
        for y in range(94, 110):
            radius = hypot(x, y - 101)
            if radius < 7.7:
                set_block(x, y, -26, PERCH if radius > 6.5 else DEEPSLATE)
            if abs(x) + abs(y - 101) < 5:
                set_block(x, y, -25, LIGHT if x == 0 or y == 101 else AMETHYST)
    for x in (-24, 24):
        crystal(x, 82, -17, 16, 1 if x < 0 else -1)
    # Warm lanterns trace the balconies above the violet light on the lower floor.
    for x in (-20, 20):
        for z in (3, 15, 27):
            y = min(109, SURFACE[x, z] - 2)
            box(x, 108, z, x, y, z, material("iron_chain[axis=y]"))
            set_block(x, 107, z, material("lantern[hanging=true]"))
    box(16, 82, -26, 18, 82, -24, DEEPSLATE)
    chest(17, 83, -24, "south", "meadow_gate")
    stairs(0, -28, 81, 0, -1, 23, 5, 1, True)
    for i in (4, 9, 14, 19):
        set_block(2, 82 + i, -28 - i, LANTERN)
    FIXTURES[0, 82, -17] = "magicrpg:meadow_paladin"
    wing(-1)
    wing(1)
    crypt()


def spawners():
    floor_spawners = [
        (-20, 105, 22, "hollow_armor"),
        (20, 105, 7, "hollow_armor"),
        (-25, 82, 18, "city_stone_guard"),
        (25, 82, 18, "city_stone_guard"),
        (-44, 89, -2, "hollow_armor"),
        (43, 89, -3, "city_stone_guard"),
        (10, 66, 0, "hollow_armor"),
    ]
    for x, y, z, kind in floor_spawners:
        if get_block(x, y, z) != AIR or get_block(x, y - 1, z) == AIR:
            raise ValueError(f"Invalid spawner position at {(x, y, z)}")
        set_block(x, y - 1, z, DEEPSLATE)
        set_block(x, y, z, material("spawner"))
        FIXTURES[x, y, z] = "magicrpg:" + kind


def export():
    for pos, fixture in FIXTURES.items():
        if get_block(*pos) == material("spawner"):
            continue
        if fixture.startswith("magicrpg:chests/"):
            if get_block(pos[0], pos[1] + 1, pos[2]) != AIR:
                raise ValueError(f"Blocked chest at {pos}")
        elif get_block(*pos) != AIR or get_block(pos[0], pos[1] - 1, pos[2]) == AIR:
            raise ValueError(f"Invalid inhabitant marker at {pos}")
    map_data.export(allow_inhabitants=True)


if __name__ == "__main__":
    map_data.OUTPUT = map_data.OUTPUT.with_name("lilac_meadows.bin.gz")
    terrain()
    landmarks()
    city()
    spawners()
    export()
