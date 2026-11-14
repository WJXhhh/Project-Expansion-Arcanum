"""Render the station icons directly from their Java drawing method.

Uses a small Java2D adapter for GuiGraphics/PoseStack; does not launch Minecraft.
Run again after changing the icons to inspect their pixel coverage at GUI scales
1 through 4. Also exports a transparent 48x48 stonecutter icon. Generated
rendering files stay in build/gui-preview.
"""

import argparse
from pathlib import Path
import shutil
import subprocess

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "src/main/java/com/wjx/forge/projectexa/gui/GUIArcaneTransmutationTablet.java"

ADAPTER = r"""
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import javax.imageio.ImageIO;

public class StationIconPreview {
    static class Axis {
        static final Axis ZP = new Axis();
        double rotationDegrees(float degrees) { return Math.toRadians(degrees); }
    }
    static class PoseStack {
        final Graphics2D graphics;
        final ArrayDeque<AffineTransform> stack = new ArrayDeque<>();
        PoseStack(Graphics2D graphics) { this.graphics = graphics; }
        void pushPose() { stack.push(graphics.getTransform()); }
        void popPose() { graphics.setTransform(stack.pop()); }
        void translate(double x, double y, double z) { graphics.translate(x, y); }
        void scale(float x, float y, float z) { graphics.scale(x, y); }
        void mulPose(double radians) { graphics.rotate(radians); }
    }
    static class GuiGraphics {
        final Graphics2D graphics;
        final PoseStack pose;
        GuiGraphics(Graphics2D graphics) {
            this.graphics = graphics;
            this.pose = new PoseStack(graphics);
        }
        PoseStack pose() { return pose; }
        void fill(int x1, int y1, int x2, int y2, int argb) {
            graphics.setColor(new Color(argb, true));
            graphics.fill(new Rectangle2D.Double(Math.min(x1, x2), Math.min(y1, y2),
                    Math.abs(x2 - x1), Math.abs(y2 - y1)));
        }
    }

    __DRAW_METHOD__

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        StationIconPreview preview = new StationIconPreview();
        for (int scale : new int[] {1, 2, 3, 4, 8}) {
            BufferedImage image = new BufferedImage(18 * scale, 78 * scale,
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D canvas = image.createGraphics();
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_OFF);
            canvas.scale(scale, scale);
            GuiGraphics graphics = new GuiGraphics(canvas);
            for (int station = 0; station < 4; station++) {
                int y = station * 20;
                graphics.fill(0, y, 18, y + 18, 0xFF202020);
                graphics.fill(1, y + 1, 17, y + 17, 0xFF515151);
                graphics.fill(2, y + 2, 16, y + 16, 0xFF373737);
                preview.drawStationIcon(graphics, station, 3, y + 3);
            }
            canvas.dispose();
            ImageIO.write(image, "png", new File(args[0], "scale-" + scale + ".png"));
        }
        BufferedImage icon = new BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB);
        Graphics2D canvas = icon.createGraphics();
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);
        canvas.scale(4, 4);
        preview.drawStationIcon(new GuiGraphics(canvas), 2, 0, 0);
        canvas.dispose();
        ImageIO.write(icon, "png", new File(args[0], "stonecutter-icon.png"));
    }
}
"""


def drawing_method():
    text = SOURCE.read_text(encoding="utf-8")
    start = text.index("    private void drawStationIcon(")
    opening = text.index("{", start)
    depth = 1
    end = opening + 1
    while depth:
        if text[end] == "{":
            depth += 1
        elif text[end] == "}":
            depth -= 1
        end += 1
    return text[start:end]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=ROOT / "build/gui-preview/station-icons.png")
    parser.add_argument("--reference", type=Path, help="Optional source icon or vanilla stonecutter screenshot")
    args = parser.parse_args()
    folder = args.output.resolve().parent
    folder.mkdir(parents=True, exist_ok=True)
    javac = shutil.which("javac")
    if not javac:
        raise SystemExit("A JDK with javac is required for the drawing adapter.")
    java = Path(javac).with_name("java.exe" if Path(javac).suffix == ".exe" else "java")
    generated = folder / "StationIconPreview.java"
    generated.write_text(ADAPTER.replace("__DRAW_METHOD__", drawing_method()), encoding="utf-8")
    subprocess.run([javac, "-encoding", "UTF-8", "-d", str(folder), str(generated)], check=True)
    subprocess.run([str(java), "-cp", str(folder), "StationIconPreview", str(folder)], check=True)

    sheet = Image.new("RGB", (850, 730), "#17191b")
    draw = ImageDraw.Draw(sheet)
    font_path = Path("C:/Windows/Fonts/consola.ttf")
    font = ImageFont.truetype(str(font_path), 17) if font_path.exists() else ImageFont.load_default()
    draw.text((24, 18), "OFFLINE PREVIEW - actual Java icon method / no antialiasing", font=font, fill="#dedede")
    draw.text((24, 45), "1x-4x: native display pixels. 8x: enlarged geometry.", font=font, fill="#a0a0a0")
    for scale, x in [(1, 30), (2, 100), (3, 190), (4, 300), (8, 450)]:
        draw.text((x, 85), str(scale) + "x", font=font, fill="#dedede")
        with Image.open(folder / ("scale-" + str(scale) + ".png")) as strip:
            sheet.paste(strip, (x, 115), strip)
    if args.reference:
        with Image.open(args.reference) as reference:
            reference = reference.convert("RGBA")
            factor = min(210 / reference.width, 240 / reference.height)
            reference = reference.resize(
                (max(1, round(reference.width * factor)), max(1, round(reference.height * factor))),
                Image.Resampling.NEAREST,
            )
            sheet.paste(reference, (620, 155), reference)
        draw.text((620, 115), "Input reference", font=font, fill="#dedede")
    args.output.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(args.output)
    print(args.output.resolve())


if __name__ == "__main__":
    main()
