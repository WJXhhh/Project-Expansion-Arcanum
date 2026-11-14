"""Offline armor stand layout illustration from vanilla model boxes and texture.

Uses a small orthographic software renderer, not Minecraft's renderer. Lighting
and rasterization are approximate; the game uses InventoryScreen directly.
"""

from io import BytesIO
import math
from pathlib import Path
import re
from zipfile import ZipFile

from PIL import Image


def rotate(point, degrees):
    x, y, z = point
    a, b, c = map(math.radians, degrees)
    y, z = y * math.cos(a) - z * math.sin(a), y * math.sin(a) + z * math.cos(a)
    x, z = x * math.cos(b) + z * math.sin(b), -x * math.sin(b) + z * math.cos(b)
    return x * math.cos(c) - y * math.sin(c), x * math.sin(c) + y * math.cos(c), z


def draw_armor_stand(image, anchor, scale):
    cache = Path.home() / ".gradle/caches/forge_gradle"
    sources = cache / "minecraft_user_repo/net/minecraftforge/forge/1.20.1-47.4.0_mapped_official_1.20.1/forge-1.20.1-47.4.0_mapped_official_1.20.1-sources.jar"
    with ZipFile(sources) as jar:
        model = jar.read("net/minecraft/client/model/ArmorStandModel.java").decode()
        entity = jar.read("net/minecraft/world/entity/decoration/ArmorStand.java").decode()
    with ZipFile(cache / "minecraft_repo/versions/1.20.1/client.jar") as jar:
        texture = Image.open(BytesIO(jar.read("assets/minecraft/textures/entity/armorstand/wood.png"))).convert("RGBA")
    poses = {}
    for name, angles in re.findall(r"DEFAULT_(\w+)_POSE = new Rotations\(([^)]+)\)", entity):
        poses[name.lower()] = [float(v.strip().removesuffix("F")) for v in angles.split(",")]
    pattern = r'addOrReplaceChild\("([^"]+)",.*?texOffs\((\d+), (\d+)\).*?addBox\(([^)]+)\), PartPose\.(ZERO|offset\(([^)]+)\))'
    pixels = image.load()
    texels = texture.load()
    depth = {}

    def triangle(vertices, uvs, brightness):
        (ax, ay, az), (bx, by, bz), (cx, cy, cz) = vertices
        area = (by - cy) * (ax - cx) + (cx - bx) * (ay - cy)
        if abs(area) < 1e-7:
            return
        for y in range(max(0, math.floor(min(ay, by, cy))), min(image.height, math.ceil(max(ay, by, cy)))):
            for x in range(max(0, math.floor(min(ax, bx, cx))), min(image.width, math.ceil(max(ax, bx, cx)))):
                p = ((by - cy) * (x + .5 - cx) + (cx - bx) * (y + .5 - cy)) / area
                q = ((cy - ay) * (x + .5 - cx) + (ax - cx) * (y + .5 - cy)) / area
                r = 1 - p - q
                if min(p, q, r) < -1e-7:
                    continue
                z = p * az + q * bz + r * cz
                if z <= depth.get((x, y), -math.inf):
                    continue
                u, v = [sum(weight * uv[axis] for weight, uv in zip((p, q, r), uvs)) for axis in (0, 1)]
                red, green, blue, alpha = texels[min(63, max(0, int(u))), min(63, max(0, int(v)))]
                if alpha:
                    pixels[x, y] = (int(red * brightness), int(green * brightness), int(blue * brightness), alpha)
                    depth[x, y] = z

    for match in re.finditer(pattern, model):
        name, u, v, box, _, offset = match.groups()
        if name == "base_plate":
            continue
        x, y, z, width, height, length = [float(n.strip().removesuffix("F")) for n in box.split(",")]
        pivot = [float(n.strip().removesuffix("F")) for n in offset.split(",")] if offset else (0, 0, 0)
        pose = poses.get(name, (0, 0, 0))
        projected = []
        for px, py, pz in [(x, y, z), (x + width, y, z), (x + width, y + height, z), (x, y + height, z),
                           (x, y, z + length), (x + width, y, z + length),
                           (x + width, y + height, z + length), (x, y + height, z + length)]:
            part = rotate((px, py, pz), pose)
            px, py, pz = [part[i] + pivot[i] for i in range(3)]
            point = rotate((-px / 16, 1.501 - py / 16, pz / 16), (0, -30, 0))
            point = rotate(rotate(point, (0, 0, 180)), (25, 0, 0))
            projected.append((anchor[0] + scale * point[0], anchor[1] + scale * point[1], -scale * point[2]))
        u, v = int(u), int(v)
        faces = [
            ((0, 1, 2, 3), (u + length, v + length, width, height), 1.0),
            ((5, 4, 7, 6), (u + 2 * length + width, v + length, width, height), .78),
            ((4, 0, 3, 7), (u, v + length, length, height), .85),
            ((1, 5, 6, 2), (u + length + width, v + length, length, height), .85),
            ((4, 5, 1, 0), (u + length, v, width, length), 1.0),
            ((3, 2, 6, 7), (u + length + width, v, width, length), .65),
        ]
        for indices, (tu, tv, tw, th), brightness in faces:
            quad = [projected[i] for i in indices]
            uvs = [(tu, tv), (tu + tw, tv), (tu + tw, tv + th), (tu, tv + th)]
            for indices in [(0, 1, 2), (0, 2, 3)]:
                triangle([quad[i] for i in indices], [uvs[i] for i in indices], brightness)


def add_armor_stand_preview(folder, screen_source):
    values = {name: int(value) for name, value in re.findall(r"private static final int SMITHING_PREVIEW_(\w+) = (\d+);", screen_source)}
    # Base image uses leftPos=174, topPos=5 and a 2x GUI scale.
    with Image.open(folder / "station-panel-1.png") as raw:
        image = raw.convert("RGBA")
    draw_armor_stand(image, ((174 - 144 + values["X"]) * 2, (5 + 10 + values["Y"]) * 2), values["SCALE"] * 2)
    image.save(folder / "station-panel-1.png")
