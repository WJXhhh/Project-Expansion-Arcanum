"""Offline layout preview using the actual station panel and slot geometry.

Does not launch Minecraft. Item sprites and Minecraft's font renderer are not
emulated; the preview focuses on panel sizes, slot alignment and text spacing.
"""

from pathlib import Path
from io import BytesIO
import json
import re
import subprocess
from zipfile import ZipFile

from PIL import Image, ImageDraw, ImageFont

from render_station_icons import ADAPTER, ROOT, SOURCE, drawing_method
from render_smithing_armor_stand import add_armor_stand_preview


def method(text, signature):
    start = text.index(signature)
    end = text.index("{", start) + 1
    depth = 1
    while depth:
        if text[end] == "{":
            depth += 1
        elif text[end] == "}":
            depth -= 1
        end += 1
    return text[start:end]


def main():
    folder = ROOT / "build/gui-preview"
    folder.mkdir(parents=True, exist_ok=True)
    screen = SOURCE.read_text(encoding="utf-8")
    container = (ROOT / "src/main/java/com/wjx/forge/projectexa/gui/container/ContainerArcaneTransmutationTablet.java").read_text(encoding="utf-8")
    animation = (ROOT / "src/main/java/com/wjx/forge/projectexa/gui/animation/StationPanelAnimation.java").read_text(encoding="utf-8")
    animation = animation[animation.index("public final class"):].replace("public final class", "static final class", 1)
    constants = "\n".join(re.findall(r"    public static final int (?:STATION_PANEL_\w+|STONE_\w+) = .*?;", container))
    methods = "\n".join([
        method(container, "    public static int stationSlotX("),
        method(container, "    public static int stationSlotY("),
        method(screen, "    private void renderStationPanel("),
        method(screen, "    private static void drawStationArrow("),
        method(screen, "    private static void drawSlot("),
        method(screen, "    private void drawStationButton("),
        method(screen, "    private void positionStationButtons("),
        method(screen, "    private int stationPanelDimension("),
        method(screen, "    private int stationPanelWidth("),
        method(screen, "    private int stationPanelHeight("),
        method(screen, "    private int stationPanelLeft("),
        method(screen, "    private float stationPanelOpenness("),
        method(screen, "    private int stationPanelOffset("),
        method(screen, "    private void drawStationTransitionFrame("),
    ])
    stub = r'''
    int leftPos = 174, topPos = 5, selectedStation, stoneScroll, width = 360, imageWidth = 176;
    boolean craftingPanelVisible;
    final StationPanelAnimation stationAnimation = new StationPanelAnimation(0, 0);
    static class Button {
        int x, y;
        boolean visible = true;
        void setX(int value) { x = value; }
        void setY(int value) { y = value; }
        int getX() { return x; }
        int getY() { return y; }
    }
    final Button[] stationButtons = {new Button(), new Button(), new Button(), new Button()};
    final Font font = new Font("SimSun", Font.PLAIN, 9);
    static class Component {
        static String translatable(String key) {
            return switch (key) {
                case "container.upgrade" -> "升级装备";
                case "container.stonecutter" -> "切石机";
                default -> "修复与命名";
            };
        }
    }
    static class Recipe { int getResultItem(Object registry) { return 0; } }
    static class Stonecutter {
        java.util.List<Recipe> getRecipes() {
            return java.util.Collections.nCopies(24, new Recipe());
        }
        int getSelectedRecipeIndex() { return 2; }
    }
    static class Menu { Stonecutter getStonecutter() { return new Stonecutter(); } }
    static class Level { Object registryAccess() { return null; } }
    static class Minecraft { final Level level = new Level(); }
    final Menu menu = new Menu();
    final Minecraft minecraft = new Minecraft();
    '''
    extra_graphics = r'''
        void renderItem(int item, int x, int y) { }
        void drawString(Font font, String text, int x, int y, int color, boolean shadow) {
            graphics.setFont(font);
            graphics.setColor(new Color(color, true));
            graphics.drawString(text, x, y + 8);
        }
    '''
    adapter = ADAPTER.replace("__DRAW_METHOD__", drawing_method() + animation + constants + methods + stub)
    adapter = adapter.replace("        PoseStack pose()", extra_graphics + "\n        PoseStack pose()")
    main_start = adapter.index("    public static void main(")
    adapter = adapter[:main_start] + r'''
    private static void renderAnimation(File folder, BufferedImage texture, boolean narrow) throws Exception {
        StationIconPreview preview = new StationIconPreview();
        preview.width = narrow ? 320 : 360;
        preview.topPos = narrow ? 11 : 5;
        preview.leftPos = (preview.width - 176 + Math.min(164, preview.width - 176)) / 2;
        preview.stationAnimation.reset(1, 0);
        BufferedImage[] panels = new BufferedImage[4];
        for (int station = 1; station <= 3; station++) {
            BufferedImage frame = ImageIO.read(new File(folder, "station-panel-" + station + ".png"));
            panels[station] = frame.getSubimage(60, 30, 280, 248);
        }
        panels[0] = new BufferedImage(152, 178, BufferedImage.TYPE_INT_ARGB);
        Graphics2D craft = panels[0].createGraphics();
        craft.drawImage(texture, 0, 0, 152, 178, 180, 32, 256, 121, null);
        craft.dispose();
        int[] times = {400, 1000, 1600, 2200, 2800, 3300, 3660, 3720};
        int[] targets = {2, 3, 0, 1, 1, 1, 1, 1};
        String[] labels = {"直接切换切石机", "直接切换铁砧", "直接切换工作台", "直接切换锻造台", "点击当前按钮：收起", "展开", "再次点击当前按钮：收起", "途中展开"};
        String label = "锻造台";
        int event = 0;
        for (int ms = 0; ms <= 4200; ms += 20) {
            if (event < times.length && ms == times[event]) {
                preview.stationAnimation.select(targets[event], ms);
                label = labels[event++];
            }
            preview.stationAnimation.update(ms);
            preview.selectedStation = preview.stationAnimation.station();
            preview.craftingPanelVisible = preview.selectedStation == 0;
            preview.positionStationButtons();
            BufferedImage frame = new BufferedImage(preview.width * 2, narrow ? 556 : 530, BufferedImage.TYPE_INT_RGB);
            Graphics2D canvas = frame.createGraphics();
            canvas.setColor(new Color(0x17191b));
            canvas.fillRect(0, 0, frame.getWidth(), frame.getHeight());
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            canvas.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            canvas.scale(2, 2);
            canvas.setColor(new Color(0xdddddd));
            canvas.setFont(new Font("SimSun", Font.PLAIN, 10));
            canvas.drawString("离线动画预览 · " + (narrow ? "窄窗口 · " : "") + label, 10, 13);
            canvas.setFont(new Font("SimSun", Font.PLAIN, 8));
            canvas.drawString("实际动画代码；盔甲架模型示意，字体替代", 10, 26);
            canvas.translate(0, 32);
            canvas.drawImage(texture, preview.leftPos, preview.topPos, preview.leftPos + 176, preview.topPos + 217,
                    0, 0, 176, 217, null);
            if (preview.selectedStation >= 0) {
                int x = preview.stationPanelLeft(), y = preview.topPos + STATION_PANEL_Y;
                int w = preview.stationPanelWidth(), h = preview.stationPanelHeight();
                int offset = preview.stationPanelOffset();
                int split = x + Math.round(w * preview.stationAnimation.switchProgress());
                int previous = preview.stationAnimation.previousStation();
                if (previous >= 0) preview.drawStationTransitionFrame(new GuiGraphics(canvas));
                for (int pass = 0; pass < 2; pass++) {
                    int station = pass == 0 ? previous : preview.selectedStation;
                    if (station < 0) continue;
                    int inset = previous >= 0 ? 4 : 0;
                    int clipLeft = pass == 0 ? Math.max(split, x + inset) : x + inset;
                    int clipRight = pass == 0 ? x + w - inset : Math.min(split, x + w - inset);
                    Shape clip = canvas.getClip();
                    canvas.clipRect(clipLeft, y + inset, Math.max(0, clipRight - clipLeft), h - inset * 2);
                    canvas.drawImage(panels[station], x + offset, y, station == 0 ? 76 : 140,
                            station == 0 ? 89 : 124, null);
                    canvas.setClip(clip);
                }
            }
            GuiGraphics graphics = new GuiGraphics(canvas);
            for (int i = 0; i < 4; i++) {
                int x = preview.stationButtons[i].getX(), y = preview.stationButtons[i].getY();
                graphics.fill(x, y, x + 18, y + 18, 0xFF202020);
                graphics.fill(x + 1, y + 1, x + 17, y + 17,
                        i == preview.stationAnimation.target() ? 0xFFB0A39C : 0xFF515151);
                graphics.fill(x + 2, y + 2, x + 16, y + 16, 0xFF373737);
                preview.drawStationIcon(graphics, i, x + 3, y + 3);
            }
            canvas.dispose();
            ImageIO.write(frame, "png", new File(folder, String.format((narrow ? "narrow-motion-" : "motion-") + "%03d.png", ms / 20)));
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 2 && args[2].equals("animate")) {
            renderAnimation(new File(args[0]), ImageIO.read(new File(args[1])), args.length > 3);
            return;
        }
        StationIconPreview preview = new StationIconPreview();
        for (int state = 0; state < 4; state++) {
            preview.selectedStation = 0;
            preview.craftingPanelVisible = true;
            preview.stationAnimation.reset(0, 0);
            for (int station = 0; station < 4; station++) {
                preview.stationButtons[station].visible = station == 0 || state == 2
                        || state == 1 && station == 1 || state == 3 && station == 3;
            }
            preview.positionStationButtons();
            BufferedImage buttons = new BufferedImage(54, 234, BufferedImage.TYPE_INT_ARGB);
            Graphics2D canvas = buttons.createGraphics();
            canvas.scale(3, 3);
            canvas.translate(-preview.stationButtons[0].getX(), -preview.stationButtons[0].getY());
            GuiGraphics graphics = new GuiGraphics(canvas);
            for (int station = 0; station < 4; station++) {
                Button button = preview.stationButtons[station];
                if (button.visible) preview.drawStationButton(graphics, station,
                        button.getX(), button.getY(), true, station == 0, false);
            }
            canvas.dispose();
            ImageIO.write(buttons, "png", new File(args[0], "station-unlocks-" + state + ".png"));
        }
        for (Button button : preview.stationButtons) button.visible = true;
        String positions = "[";
        for (int slot = 0; slot < 3; slot++) {
            if (slot > 0) positions += ",";
            positions += "[" + (preview.leftPos + stationSlotX(1, slot)) * 2 + ","
                    + (preview.topPos + stationSlotY(1, slot)) * 2 + "]";
        }
        java.nio.file.Files.writeString(java.nio.file.Path.of(args[0], "smithing-slot-positions.json"), positions + "]");
        BufferedImage texture = ImageIO.read(new File(args[1]));
        for (int variant = 0; variant < 6; variant++) {
            int station = variant < 3 ? variant + 1 : variant == 3 ? 2 : -1;
            boolean narrow = variant == 3 || variant == 5;
            preview.width = narrow ? 320 : 360;
            preview.topPos = narrow ? 11 : 5;
            preview.leftPos = (preview.width - 176 + Math.min(164, preview.width - 176)) / 2;
            preview.selectedStation = station;
            preview.stationAnimation.reset(station, 0);
            preview.craftingPanelVisible = station == 0;
            preview.positionStationButtons();
            BufferedImage image = new BufferedImage(preview.width * 2, (narrow ? 240 : 227) * 2, BufferedImage.TYPE_INT_ARGB);
            Graphics2D canvas = image.createGraphics();
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            canvas.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            canvas.scale(2, 2);
            GuiGraphics graphics = new GuiGraphics(canvas);
            canvas.drawImage(texture, preview.leftPos, preview.topPos, preview.leftPos + 176, preview.topPos + 217,
                    0, 0, 176, 217, null);
            if (preview.selectedStation >= 0) preview.renderStationPanel(graphics);
            for (int i = 0; i < 4; i++) {
                int x = preview.stationButtons[i].getX(), y = preview.stationButtons[i].getY();
                graphics.fill(x, y, x + 18, y + 18, 0xFF202020);
                graphics.fill(x + 1, y + 1, x + 17, y + 17, i == station ? 0xFFB0A39C : 0xFF515151);
                graphics.fill(x + 2, y + 2, x + 16, y + 16, 0xFF373737);
                preview.drawStationIcon(graphics, i, x + 3, y + 3);
            }
            if (station == 3) {
                graphics.drawString(preview.font, "钻石镐", preview.leftPos + STATION_PANEL_X + 12,
                        preview.topPos + STATION_PANEL_Y + 29, 0xFFCCCCCC, false);
                graphics.drawString(preview.font, "附魔花费：3", preview.leftPos + STATION_PANEL_X + 10,
                        preview.topPos + STATION_PANEL_Y + 99, 0xFF80FF20, false);
            }
            canvas.dispose();
            String name = station < 0 ? (narrow ? "collapsed-narrow" : "collapsed") : (narrow ? "narrow" : "" + station);
            ImageIO.write(image, "png", new File(args[0], "station-panel-" + name + ".png"));
        }
    }
}
'''
    generated = folder / "StationIconPreview.java"
    generated.write_text(adapter, encoding="utf-8")
    java_bin = Path("C:/Program Files/Java/jdk-21.0.11/bin")
    subprocess.run([str(java_bin / "javac.exe"), "-encoding", "UTF-8", "-d", str(folder), str(generated)], check=True)
    subprocess.run([str(java_bin / "java.exe"), "-Djava.awt.headless=true", "-cp", str(folder), "StationIconPreview", str(folder),
                    str(ROOT / "src/main/resources/assets/projectexa/textures/gui/arcane_transmutation_tablet.png")], check=True)
    add_armor_stand_preview(folder, screen)
    render_smithing_hints(folder)
    subprocess.run([str(java_bin / "java.exe"), "-Djava.awt.headless=true", "-cp", str(folder), "StationIconPreview", str(folder),
                    str(ROOT / "src/main/resources/assets/projectexa/textures/gui/arcane_transmutation_tablet.png"), "animate"], check=True)
    render_motion_preview(folder)
    subprocess.run([str(java_bin / "java.exe"), "-Djava.awt.headless=true", "-cp", str(folder), "StationIconPreview", str(folder),
                    str(ROOT / "src/main/resources/assets/projectexa/textures/gui/arcane_transmutation_tablet.png"), "animate", "narrow"], check=True)
    render_motion_preview(folder, narrow=True)
    render_collapse_preview(folder)
    render_station_unlocks(folder)
    sheet = Image.new("RGB", (1480, 1060), "#17191b")
    draw = ImageDraw.Draw(sheet)
    font = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 22)
    draw.text((20, 12), "离线布局预览 · 2× GUI 比例（字体替代；盔甲架为模型示意，非游戏截图）", font=font, fill="#dedede")
    for station, position in [(1, (10, 90)), (2, (750, 90)), (3, (10, 570))]:
        draw.text((position[0] + 20, position[1] - 32), ["", "锻造台", "切石机", "铁砧"][station], font=font, fill="#dedede")
        with Image.open(folder / f"station-panel-{station}.png") as panel:
            sheet.paste(panel, position, panel)
    draw.text((770, 538), "窄窗口 · 顶部横向标签（320 GUI 宽）", font=font, fill="#dedede")
    with Image.open(folder / "station-panel-narrow.png") as panel:
        sheet.paste(panel, (750, 570), panel)
    output = folder / "station-panels.png"
    sheet.save(output)
    print(output)


def render_station_unlocks(folder):
    sheet = Image.new("RGB", (1000, 355), "#17191b")
    draw = ImageDraw.Draw(sheet)
    font = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 21)
    small = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 17)
    draw.text((20, 12), "工具按钮显示规则 · 实际绘制与排列代码的离线预览", font=font, fill="#dedede")
    for column, (state, title) in enumerate(((0, "尚未学习工具方块"), (3, "仅学习铁砧"), (2, "三种工具均已学习"))):
        x = 20 + column * 330
        draw.text((x, 55), title, font=small, fill="#dedede")
        with Image.open(folder / f"station-unlocks-{state}.png") as buttons:
            sheet.paste(buttons, (x, 88), buttons)
        rank = 0
        for station, name in enumerate(("工作台", "锻造台", "切石机", "铁砧")):
            enabled = station == 0 or state == 2 or state == 1 and station == 1 or state == 3 and station == 3
            if not enabled:
                continue
            status = "始终可用" if station == 0 else "已学习"
            draw.text((x + 68, 103 + rank * 60), f"{name} · {status}", font=small, fill="#cccccc")
            rank += 1
    output = folder / "station-unlocks.png"
    sheet.save(output)
    print(output)


def render_motion_preview(folder, narrow=False):
    frames = []
    prefix = "narrow-motion" if narrow else "motion"
    for index in range(211):
        with Image.open(folder / f"{prefix}-{index:03}.png") as frame:
            frames.append(frame.convert("RGB"))
    output = folder / ("station-animation-narrow.gif" if narrow else "station-animation.gif")
    frames[0].save(output, save_all=True, append_images=frames[1:], duration=20, loop=0)
    w, h = frames[0].size
    sheet = Image.new("RGB", (w * 2, h * 3), "#17191b")
    for slot, index in enumerate((0, 24, 54, 84, 144, 169)):
        sheet.paste(frames[index], ((slot % 2) * w, (slot // 2) * h))
    sheet.save(folder / ("station-animation-narrow-frames.png" if narrow else "station-animation-frames.png"))
    print(output)


def render_collapse_preview(folder):
    sheet = Image.new("RGB", (1480, 575), "#17191b")
    draw = ImageDraw.Draw(sheet)
    font = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 22)
    draw.text((20, 12), "侧面板收起 · 离线布局预览（盔甲架为模型示意，字体替代）", font=font, fill="#dedede")
    for name, x, label in [("1", 10, "展开：锻造台"), ("collapsed", 750, "收起：仅保留工作站按钮与主界面")]:
        draw.text((x + 20, 58), label, font=font, fill="#dedede")
        with Image.open(folder / f"station-panel-{name}.png") as panel:
            sheet.paste(panel, (x, 95), panel)
    output = folder / "station-collapse.png"
    sheet.save(output)
    print(output)


def render_smithing_hints(folder):
    """Composite vanilla sprite snapshots at the source's rendered slot positions.

    Minecraft runs CyclingSlotBackground itself; these static illustrations show
    representative animation frames for empty/template-selected slot states.
    """
    client = Path.home() / ".gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client.jar"
    positions = json.loads((folder / "smithing-slot-positions.json").read_text())
    font = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 20)
    small_font = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 17)
    sheet = Image.new("RGB", (1120, 415), "#17191b")
    draw = ImageDraw.Draw(sheet)
    draw.text((20, 12), "锻造台空槽提示 · 原版贴图静态帧 · 离线预览", font=font, fill="#dedede")
    states = [
        ("未放模板", ["empty_slot_smithing_template_armor_trim", None, None]),
        ("放入下界合金升级模板", ["netherite_upgrade_smithing_template", "empty_armor_slot_helmet", "empty_slot_ingot"]),
        ("放入盔甲纹饰模板", ["sentry_armor_trim_smithing_template", "empty_armor_slot_helmet", "empty_slot_redstone_dust"]),
    ]
    with Image.open(folder / "station-panel-1.png") as original, ZipFile(client) as jar:
        base = original.convert("RGBA")
        for index, (label, sprites) in enumerate(states):
            panel = base.copy()
            for slot, sprite in enumerate(sprites):
                if sprite is None:
                    continue
                with Image.open(BytesIO(jar.read(f"assets/minecraft/textures/item/{sprite}.png"))) as raw:
                    icon = raw.convert("RGBA").resize((32, 32), Image.Resampling.NEAREST)
                    panel.alpha_composite(icon, tuple(positions[slot]))
            x = 20 + index * 365
            draw.text((x, 54), label, font=font, fill="#dedede")
            cropped = panel.crop((20, 30, 340, 278))
            sheet.paste(cropped, (x, 87), cropped)
            if index == 0:
                # Keep the general panel sheet in sync with the empty screen.
                panel.save(folder / "station-panel-1.png")
    draw.text((20, 352), "模板图案会轮播；放入模板后，装备和材料提示随模板变化。", font=small_font, fill="#b0b0b0")
    draw.text((20, 382), "物品占用槽位后隐藏底图。字体替代；盔甲架为离线模型示意。", font=small_font, fill="#b0b0b0")
    output = folder / "smithing-slot-hints.png"
    sheet.save(output)
    print(output)


if __name__ == "__main__":
    main()
