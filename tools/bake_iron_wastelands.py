"""Bake the Iron Wastelands city, which ships as a fixed block layout."""

from math import cos, hypot, sin

import bake_goldenwood as island
from bake_goldenwood import AIR, box, get_block, material, mix, set_block

FLOOR = 104
ASH = material("magicrpg:ashen_soil")
RUST = material("magicrpg:rusted_iron")
PLATES = material("magicrpg:metal_plates")
SCRAP = material("magicrpg:iron_rubble")
BASALT = material("smooth_basalt")
ROCK = material("deepslate")
MASONRY = material("deepslate_bricks")
CRACKED = material("cracked_deepslate_bricks")
PAVING = material("polished_deepslate")
GRATE = material("waxed_weathered_copper_grate")
COPPER = material("waxed_exposed_cut_copper")
GIRDERS = material("iron_bars[north=true,south=true]")
RAIL = material("iron_bars[east=true,west=true]")
CHAINS = material("iron_chain[axis=y]")
GLASS = material("gray_stained_glass")
LIGHT = material("ochre_froglight")
LAMP = material("lantern")
HANGING = material("lantern[hanging=true]")
IRON = material("iron_block")
COAL = material("coal_block")
SLAB = material("polished_deepslate_slab")
WOOD = material("dark_oak_planks")
TABLE = material("dark_oak_slab[type=top]")
WALL_SHELF = material("dark_oak_slab")
BOOKS = material("bookshelf")
WORKBENCH = material("crafting_table")
BIN = material("composter")
CAULDRON = material("cauldron")
COT = material("gray_wool")
PILLOW = material("light_gray_carpet")
AWNING = material("brown_wool")
POT = material("flower_pot")
MACHINE = material("polished_blackstone_bricks")
TILE = material("polished_andesite")
BEAM = material("stripped_dark_oak_log[axis=x]")
VENT = material("polished_basalt[axis=x]")
TRACK = material("iron_chain[axis=z]")
CHAIR = {direction: material(f"dark_oak_stairs[facing={direction}]") for direction in ("north", "south")}
STAIRS = {direction: material(f"polished_deepslate_stairs[facing={direction}]")
          for direction in ("north", "south")}
SURFACE = {}


def fissure(z):
    return 35 + round(3 * sin(z / 17))


def terrain():
    for x in range(-78, 79):
        for z in range(-73, 73):
            edge = hypot((x + 3 * sin(z / 16)) / 73, z / 68)
            rim = 1 + .07 * sin(x / 8) + .035 * cos((x - z) / 5)
            if edge >= rim:
                continue
            seam = abs(x - fissure(z))
            if seam <= 2:
                continue
            flat = -56 <= x <= 61 and -51 <= z <= 59
            top = FLOOR if flat else FLOOR + round(2 * sin(x / 7) * cos(z / 8) - 6 * max(0, edge - .78))
            depth = max(3, round(50 * (1 - (edge / rim) ** 2)) + mix(x // 3, z // 3, 1) % 5)
            if seam < 8:
                depth = min(depth, (seam - 2) * 6)
            bottom = top - depth
            if depth >= 5:
                box(x, bottom, z, x, top - 5, z, ROCK)
            box(x, max(bottom, top - 4), z, x, top - 1, z, BASALT)
            set_block(x, top, z, SCRAP if mix(x, z, 2) % 10 < 2 else ASH)
            SURFACE[x, z] = top


def street(x1, z1, x2, z2, axis, floor=FLOOR):
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            if (x, z) not in SURFACE:
                continue
            cross = x if axis == "z" else z
            low, high = (x1, x2) if axis == "z" else (z1, z2)
            wear = mix(x, z, 3) % 12
            block = SCRAP if wear == 0 else CRACKED if wear < 3 else PAVING
            if cross in (low, high):
                block = PLATES if wear else RUST
            set_block(x, floor, z, block)


def iron_bridge(x1, x2, z, width, y=FLOOR):
    box(x1, y - 1, z, x2, y, z + width - 1, PLATES)
    for rail_z in (z, z + width - 1):
        box(x1, y + 1, rail_z, x2, y + 1, rail_z, RAIL)
        for x in range(x1, x2 + 1, 4):
            box(x, y - 3, rail_z, x, y + 2, rail_z, RUST)
            set_block(x, y + 3, rail_z, LAMP)
        for x in range(x1 + 1, x2):
            drop = min(x - x1, x2 - x, 6)
            set_block(x, y - 2 - drop, rail_z, RUST)
    for x in range(x1 + 2, x2, 4):
        box(x, y, z + 1, x, y, z + width - 2, GRATE)


def gate_and_roads():
    street(-6, -53, 8, 61, "z")
    street(-52, 12, 29, 22, "x")
    street(-53, -29, 33, -21, "x")
    street(-53, -34, -47, 35, "z")
    street(44, -37, 49, 25, "z")
    street(38, -29, 61, -21, "x")
    street(39, 12, 59, 22, "x")
    iron_bridge(27, 45, 12, 11)
    iron_bridge(27, 45, -29, 9)

    box(-5, FLOOR - 1, 52, 6, FLOOR, 60, PLATES)
    box(-1, FLOOR, 56, 2, FLOOR, 56, IRON)
    box(-1, FLOOR + 1, 56, -1, FLOOR + 3, 56, IRON)
    box(2, FLOOR + 1, 56, 2, FLOOR + 3, 56, IRON)
    box(-1, FLOOR + 4, 56, 2, FLOOR + 4, 56, IRON)
    for x in (-5, 6):
        box(x, FLOOR + 1, 53, x, FLOOR + 3, 53, RUST)
        set_block(x, FLOOR + 4, 53, LAMP)
    for z in (39, 27, 8, -18, -37):
        for x in (-7, 9):
            box(x, FLOOR + 1, z, x, FLOOR + 3, z, RUST)
            set_block(x, FLOOR + 4, z, LAMP)


def furnished_floor(x1, z1, x2, z2, y, use):
    # Leave the stairwell and the landing clear of doors and furniture.
    partition = x1 + 7
    span = (z1 + z2) // 2
    for z in range(z1 + 1, z2):
        if abs(z - span) > 1 and z not in (z1 + 2, z2 - 2):
            box(partition, y + 1, z, partition, y + 3, z, MASONRY if (z + y) % 6 else CRACKED)
    for z in (z1 + 2, span, z2 - 2):
        box(partition - 1, y, z, partition + 1, y, z, TILE)

    x = x2 - 2
    back = z1 + 2
    front = z2 - 2
    if use == "housing":
        for bed_x in (x, x - 3):
            box(bed_x, y + 1, back, bed_x, y + 1, back + 2, COT)
            set_block(bed_x, y + 2, back, PILLOW)
        set_block(x - 1, y + 1, back, WALL_SHELF)
        set_block(x - 1, y + 2, back, LAMP)
        box(x - 3, y + 1, front, x, y + 1, front, WOOD)
        set_block(x, y + 1, front - 1, CAULDRON)
        set_block(x - 2, y + 2, front, POT)
        set_block(x - 1, y + 1, front - 2, CHAIR["north"])
    elif use in ("offices", "archive"):
        box(x + 1, y + 1, back, x + 1, y + 2, back + 3, BOOKS)
        if use == "archive":
            box(x - 2, y + 1, back - 1, x, y + 2, back - 1, BOOKS)
        set_block(x, y + 1, front, WOOD)
        set_block(x - 2, y + 1, front, WOOD)
        set_block(x - 1, y + 1, front, TABLE)
        set_block(x - 1, y + 1, front - 1, CHAIR["north"])
        set_block(x - 2, y + 2, front, LAMP)
    else:
        for z in (back, front):
            box(x - 2, y + 1, z, x, y + 1, z, WOOD)
            set_block(x, y + 2, z, BIN)
            set_block(x - 1, y + 2, z, RUST)
        set_block(x + 1, y + 1, front - 2, WORKBENCH)
        set_block(x + 1, y + 2, front - 2, LAMP)


def ruined_tower(x1, z1, x2, z2, floors, breach_x, breach_z, use):
    wall = MASONRY
    roof = FLOOR + 6 * floors
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            border = x in (x1, x2) or z in (z1, z2)
            spine = x in (x1, x2) and z in (z1, z2)
            damage = max(0, 12 - 2 * (abs(x - breach_x) + abs(z - breach_z)))
            height = roof - damage - mix(x, z, 4) % (3 if spine else 5)
            box(x, FLOOR - 1, z, x, FLOOR, z, MASONRY)
            for y in range(FLOOR + 1, height + 1):
                story = (y - FLOOR) % 6
                stripe = (x - x1 if z in (z1, z2) else z - z1) % 4
                door = z == z2 and abs(x - (x1 + x2) // 2) <= 2 and y < FLOOR + 5
                if door:
                    continue
                if story == 0:
                    set_block(x, y, z, PLATES if border else PAVING)
                elif spine:
                    set_block(x, y, z, RUST)
                elif border and (stripe == 0 or story in (1, 5)):
                    set_block(x, y, z, CRACKED if mix(x + y, z, 5) % 4 == 0 else wall)
                elif border and mix(x, z + y, 6) % 4 == 0:
                    set_block(x, y, z, GLASS)

    for story in range(floors - 1):
        y = FLOOR + story * 6
        if get_block(x2 - 3, y, z2 - 2) != AIR:
            furnished_floor(x1, z1, x2, z2, y, use)
        for x in range(x1 + 2, x2, 4):
            for z in (z1, z2):
                if get_block(x, y + 5, z) != AIR:
                    set_block(x, y + 2, z, SLAB)
                    set_block(x, y + 5, z, PLATES)

    center = (x1 + x2) // 2
    for x in (center - 3, center + 3):
        box(x, FLOOR + 1, z2 + 1, x, FLOOR + 4, z2 + 1, RUST)
        set_block(x, FLOOR + 1, z2 + 2, SLAB)
    box(center - 3, FLOOR + 4, z2 + 1, center + 3, FLOOR + 4, z2 + 1, BEAM)
    set_block(center, FLOOR + 3, z2 + 1, HANGING)

    # The intact stairwell doubles as a lookout above the western streets.
    for story in range(floors - 1):
        for step in range(6):
            z = z1 + 2 + (step if story % 2 == 0 else 5 - step)
            x = x1 + 2 + story % 2 * 2
            y = FLOOR + story * 6 + step + 1
            direction = "south" if story % 2 == 0 else "north"
            box(x, y, z, x + 1, y, z, STAIRS[direction])
            box(x, y + 1, z, x + 1, y + 3, z, AIR)
        set_block(x1 + 5, FLOOR + story * 6 + 1, z1 + 3, LAMP)


def factory(x1, z1, x2, z2, back):
    box(x1, FLOOR, z1, x2, FLOOR, z2, PLATES)
    for x in range(x1, x2 + 1, 4):
        for z in (z1, z2):
            box(x, FLOOR + 1, z, x, FLOOR + 9, z, RUST)
        box(x, FLOOR + 9, z1, x, FLOOR + 9, z2, RUST)
        if mix(x, z1, 7) % 3 != 0:
            box(x, FLOOR + 10, z1, min(x + 2, x2), FLOOR + 10, z2, PLATES)
        set_block(x, FLOOR + 8, (z1 + z2) // 2, HANGING)
    for z in range(z1 + 1, z2):
        for x in (x1, x2):
            box(x, FLOOR + 1, z, x, FLOOR + 3 + mix(x, z, 8) % 3, z, CRACKED)
            if z % 3 != 0:
                set_block(x, FLOOR + 5, z, GIRDERS)
    for x in range(x1 + 4, x2 - 2, 8):
        box(x, FLOOR + 1, back - 3, x + 3, FLOOR + 4, back, RUST)
        box(x, FLOOR + 2, back, x + 3, FLOOR + 3, back, GRATE)
        set_block(x + 1, FLOOR + 2, back + 1, LIGHT)
        box(x + 1, FLOOR + 5, back - 2, x + 2, FLOOR + 14 + mix(x, back, 9) % 6, back - 1, BASALT)
    box(x1 + 2, FLOOR + 1, z1 + 3, x2 - 2, FLOOR + 1, z1 + 5, COAL)
    for x in range(x1 + 2, x2 - 1, 3):
        box(x, FLOOR + 1, z1 + 3, x, FLOOR + 1, z1 + 5, GRATE)

    # Keep a service lane through the center between the benches and the smelters.
    for x in range(x1 + 2, x2 - 1, 4):
        box(x, FLOOR + 1, z2 - 2, x + 1, FLOOR + 1, z2 - 2, WOOD)
        set_block(x, FLOOR + 1, z2 - 3, CHAIR["south"])
        set_block(x + 1, FLOOR + 2, z2 - 2, BIN if (x - x1) % 3 else LAMP)
    set_block(x1 + 2, FLOOR + 1, z2 - 5, WORKBENCH)
    set_block(x2 - 2, FLOOR + 1, z2 - 5, CAULDRON)
    for x in (x1 + 1, x2 - 1):
        box(x, FLOOR + 6, z1 + 1, x, FLOOR + 6, z2 - 1, PLATES)
        for z in range(z1 + 3, z2 - 1, 4):
            set_block(x, FLOOR + 7, z, GIRDERS)
    pipe_z = z1 + 2
    box(x1 + 2, FLOOR + 7, pipe_z, x2 - 2, FLOOR + 7, pipe_z, VENT)
    for x in (x1 + 2, x2 - 2):
        box(x, FLOOR + 2, pipe_z, x, FLOOR + 7, pipe_z, MACHINE)
        set_block(x, FLOOR + 3, pipe_z - 1, COPPER)


def market_stall(x, z, torn=False):
    box(x, FLOOR, z, x + 5, FLOOR, z + 3, TILE)
    for post_x in (x, x + 5):
        for post_z in (z, z + 3):
            box(post_x, FLOOR + 1, post_z, post_x, FLOOR + 4, post_z, WOOD)
    box(x, FLOOR + 4, z, x + 5, FLOOR + 4, z + 3, AWNING)
    if torn:
        box(x + 3, FLOOR + 4, z, x + 4, FLOOR + 4, z + 1, AIR)
    box(x + 1, FLOOR + 1, z + 3, x + 4, FLOOR + 1, z + 3, TABLE)
    set_block(x + 1, FLOOR + 2, z + 3, POT)
    set_block(x + 3, FLOOR + 1, z + 1, BIN)
    set_block(x + 4, FLOOR + 1, z + 1, WOOD)


def street_details():
    for x in (-8, 10):
        for z in range(-50, 45):
            if (x, z) not in SURFACE or 11 <= z <= 23 or -30 <= z <= -20:
                continue
            if get_block(x, FLOOR + 1, z) == AIR and (x, z) not in CLEAR_GROUND:
                set_block(x, FLOOR, z, TILE if z % 8 else CRACKED)
                if z % 13 < 8:
                    set_block(x, FLOOR + 1, z, SLAB)
    # A broken service track runs beside the smelters on the east side of the avenue.
    for z in range(-19, 10):
        for x in (14, 16):
            if get_block(x, FLOOR + 1, z) == AIR and (x, z) not in CLEAR_GROUND:
                set_block(x, FLOOR + 1, z, TRACK)
        if z % 4 == 0:
            for x in (14, 15, 16):
                set_block(x, FLOOR, z, BEAM)
    market_stall(-24, 23, torn=True)
    market_stall(-18, 23)
    for x, z in ((-12, 26), (-9, -44), (24, 24), (42, 24)):
        if (x, z) in SURFACE and get_block(x, FLOOR + 1, z) == AIR:
            set_block(x, FLOOR + 1, z, SLAB)
            set_block(x + 1, FLOOR + 1, z, SLAB)
            set_block(x + 1, FLOOR + 2, z, POT)


def city():
    ruined_tower(-45, -17, -28, 9, 5, -28, 5, "offices")
    ruined_tower(-24, -16, -11, 5, 4, -11, -8, "housing")
    ruined_tower(-44, 25, -25, 42, 3, -25, 28, "housing")
    ruined_tower(-34, -44, -17, -32, 4, -21, -32, "archive")
    ruined_tower(11, -50, 25, -36, 3, 11, -41, "depot")
    factory(13, -18, 29, 8, -10)
    factory(47, -18, 63, 8, -8)
    factory(44, 26, 56, 36, 32)
    for x, z in ((20, -15), (22, -6), (55, -4)):
        box(x - 1, FLOOR, z, x + 1, FLOOR, z, COPPER)
        box(x, FLOOR, z - 1, x, FLOOR, z + 1, COPPER)
        set_block(x, FLOOR, z, GRATE)

    # Gantry above the avenue with clearance for colossi.
    for x in (-10, 11):
        box(x - 1, FLOOR + 1, -2, x + 1, FLOOR + 13, 0, RUST)
    box(-10, FLOOR + 11, -1, 11, FLOOR + 11, 0, PLATES)
    box(-10, FLOOR + 13, 0, 11, FLOOR + 13, 0, RUST)
    for x in range(-10, 12, 3):
        box(x, FLOOR + 12, 0, x, FLOOR + 13, 0, GRATE)
    for x in (-5, 2, 7):
        box(x, FLOOR + 9, -1, x, FLOOR + 10, -1, CHAINS)
        set_block(x, FLOOR + 8, -1, HANGING)


def wrecks():
    # Fallen head beside the approach, with a dark visor and exposed neck.
    box(-21, 103, 35, -12, 109, 43, RUST)
    box(-20, 109, 36, -13, 110, 42, PLATES)
    box(-20, 107, 43, -13, 108, 43, COAL)
    for x in (-19, -14):
        set_block(x, 107, 43, COPPER)
    box(-17, 105, 33, -15, 107, 35, GRATE)
    box(-23, 104, 29, -16, 106, 33, PLATES)
    for x in (-25, -23, -21, -19):
        box(x, 104, 28, x, 105, 31, RUST)

    # The remains of a second construct are visible above the far boulevard.
    box(-15, 104, -47, -11, 113, -43, RUST)
    box(-10, 104, -51, -6, 108, -47, PLATES)
    box(-15, 112, -48, -7, 115, -42, RUST)
    box(-16, 114, -46, -15, 117, -43, PLATES)
    box(-13, 115, -46, -9, 116, -43, GRATE)
    for z in (-45, -43, -41):
        box(-13, 104, z, -7, 104, z, SCRAP)

    for (x, z), y in SURFACE.items():
        if (-9 <= x <= 11 or 11 <= z <= 23 or -30 <= z <= -20
                or (x, z) in CLEAR_GROUND or get_block(x, y, z) not in (ASH, SCRAP)
                or get_block(x, y + 1, z) != AIR):
            continue
        sample = mix(x, z, 10) % 130
        if sample < 8:
            set_block(x, y + 1, z, SCRAP if sample < 4 else SLAB if sample < 7 else RUST)


def inhabitants():
    for name, points in MOBS.items():
        width, height = DIMENSIONS[name]
        for x, z in points:
            y = FLOOR + 1
            if get_block(x, y - 1, z) == AIR or any(
                get_block(xx, yy, zz) != AIR
                for xx in range(x - width, x + width + 1)
                for zz in range(z - width, z + width + 1)
                for yy in range(y, y + height)
            ):
                raise ValueError(f"Blocked {name} at {(x, y, z)}")
            island.FIXTURES[x, y, z] = "magicrpg:" + name


MOBS = {
    "iron_colossus": [(-3, 19), (3, -24)],
    "flesh_titan": [(3, 31), (7, -13)],
    "hollowed": [(11, 28), (14, 30), (10, -26), (15, -26), (52, 19)],
    "zombie_thaumaturge": [(16, 26), (-40, 2)],
    "large_iron_golem": [(20, 16), (4, -14), (54, -22)],
}
DIMENSIONS = {"iron_colossus": (2, 8), "flesh_titan": (1, 5),
              "hollowed": (0, 2), "zombie_thaumaturge": (0, 2), "large_iron_golem": (1, 5)}
CLEAR_GROUND = {
    (x + dx, z + dz)
    for name, points in MOBS.items()
    for x, z in points
    for dx in range(-DIMENSIONS[name][0], DIMENSIONS[name][0] + 1)
    for dz in range(-DIMENSIONS[name][0], DIMENSIONS[name][0] + 1)
}


if __name__ == "__main__":
    island.OUTPUT = island.OUTPUT.with_name("iron_wastelands.bin.gz")
    terrain()
    gate_and_roads()
    city()
    street_details()
    wrecks()
    inhabitants()
    island.export(allow_inhabitants=True)
