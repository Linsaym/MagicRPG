"""Project reference art into named regions of an entity texture atlas."""

import argparse
import json
import tempfile
from pathlib import Path

from PIL import Image


def image_size(value):
    if not isinstance(value, list) or len(value) != 2 or any(type(n) is not int or n <= 0 for n in value):
        raise ValueError("Atlas size must be [width, height] in positive pixels.")
    return tuple(value)


def rectangle(value, size, label):
    if not isinstance(value, list) or len(value) != 4 or any(type(n) is not int for n in value):
        raise ValueError(f"{label} must be [left, top, right, bottom] in pixels.")
    left, top, right, bottom = value
    if not (0 <= left < right <= size[0] and 0 <= top < bottom <= size[1]):
        raise ValueError(f"{label} is empty or lies outside the {size[0]} x {size[1]} image.")
    return left, top, right, bottom


def perspective(value, source_size, target_size, label):
    if not isinstance(value, list) or len(value) != 4:
        raise ValueError(f"{label} must contain four [x, y] corners.")
    for point in value:
        if not isinstance(point, list) or len(point) != 2 or any(
            type(n) not in (int, float) or not 0 <= n <= limit for n, limit in zip(point, source_size)
        ):
            raise ValueError(f"{label} has a corner outside the reference or an invalid coordinate.")

    for index in range(4):
        a, b, c = (value[(index + offset) % 4] for offset in range(3))
        turn = (b[0] - a[0]) * (c[1] - b[1]) - (b[1] - a[1]) * (c[0] - b[0])
        if turn <= 0:
            raise ValueError(f"{label} corners must form a convex quad in TL, TR, BR, BL order.")

    # Map a unit square onto the source quad, then scale from output pixels to the unit square.
    (x0, y0), (x1, y1), (x2, y2), (x3, y3) = value
    diagonal_x = x0 - x1 + x2 - x3
    diagonal_y = y0 - y1 + y2 - y3
    dx_right, dy_right = x1 - x2, y1 - y2
    dx_bottom, dy_bottom = x3 - x2, y3 - y2
    determinant = dx_right * dy_bottom - dx_bottom * dy_right
    g = (diagonal_x * dy_bottom - dx_bottom * diagonal_y) / determinant
    h = (dx_right * diagonal_y - diagonal_x * dy_right) / determinant
    width, height = target_size
    return (
        (x1 - x0 + g * x1) / width, (x3 - x0 + h * x3) / height, x0,
        (y1 - y0 + g * y1) / width, (y3 - y0 + h * y3) / height, y0,
        g / width, h / height,
    )


def project_region(reference, region, size, name):
    if not isinstance(region, dict):
        raise ValueError(f"{name} must be an object with uv and a crop or quad.")
    if ("crop" in region) == ("quad" in region):
        raise ValueError(f"{name} must specify exactly one of crop or quad.")
    bounds = rectangle(region["uv"], size, f"{name}.uv")
    left, top, right, bottom = bounds
    target_size = (right - left, bottom - top)
    if "crop" in region:
        fragment = reference.crop(rectangle(region["crop"], reference.size, f"{name}.crop"))
        if fragment.size != target_size:
            fragment = fragment.resize(target_size, Image.Resampling.NEAREST)
    else:
        mapping = perspective(region["quad"], reference.size, target_size, f"{name}.quad")
        fragment = reference.transform(target_size, Image.Transform.PERSPECTIVE, mapping, Image.Resampling.NEAREST)
    return bounds, fragment


def load_image(path):
    with Image.open(path) as image:
        return image.convert("RGBA")


def save_png(atlas, path):
    with tempfile.NamedTemporaryFile(dir=path.parent, suffix=".png", delete=False) as temporary:
        staged = Path(temporary.name)
    try:
        atlas.save(staged, format="PNG")
        staged.replace(path)
    finally:
        staged.unlink(missing_ok=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("manifest", type=Path, help="JSON with atlas size and named regions")
    parser.add_argument("--reference", type=Path, required=True, help="source image")
    parser.add_argument("--base", type=Path, help="existing atlas to copy before writing projected regions")
    action = parser.add_mutually_exclusive_group(required=True)
    action.add_argument("--output", type=Path, help="write the PNG atlas")
    action.add_argument("--check", type=Path, help="compare the projected regions to an existing atlas")
    args = parser.parse_args()
    if args.check and args.base:
        parser.error("--base applies only to --output.")

    try:
        manifest = json.loads(args.manifest.read_text(encoding="utf-8-sig"))
        if not isinstance(manifest, dict):
            raise ValueError("The manifest must be a JSON object.")
        size = image_size(manifest["size"])
        regions = manifest["regions"]
        if not isinstance(regions, dict) or not regions:
            raise ValueError("The manifest must include at least one named region.")
        reference = load_image(args.reference)
        existing = args.check or args.base
        atlas = load_image(existing) if existing else Image.new("RGBA", size)
        if atlas.size != size:
            raise ValueError(f"{existing} must be {size[0]} x {size[1]} pixels.")

        for name, region in regions.items():
            bounds, fragment = project_region(reference, region, size, name)
            if args.check:
                if fragment.tobytes() != atlas.crop(bounds).tobytes():
                    raise ValueError(f"{name} differs from {args.check} at {list(bounds)}.")
                print(f"{name} matches every pixel at {list(bounds)}.")
            else:
                atlas.paste(fragment, bounds[:2])
                print(f"{name} covers atlas pixels {list(bounds)}.")

        if args.output:
            save_png(atlas, args.output)
            print(f"Wrote {args.output}")
    except (KeyError, OSError, ValueError) as error:
        parser.error(str(error))


if __name__ == "__main__":
    main()
