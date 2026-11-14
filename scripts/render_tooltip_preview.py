"""Illustrate tooltip batch submission across the side-panel scissor boundary.

Offline Java2D rendering only: uses the actual panel begin/end methods with a
small deferred-text adapter. Does not emulate OpenGL or launch Minecraft.
"""

from pathlib import Path
import subprocess

from render_station_panels import ROOT, SOURCE, method


ADAPTER = r'''
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import javax.imageio.ImageIO;

public class TooltipBatchPreview {
    int leftPos = 174, topPos = 5, selectedStation = 1;
    static final int STATION_PANEL_X = -144, STATION_PANEL_Y = 10;
    int stationPanelLeft() { return leftPos + STATION_PANEL_X; }
    int stationPanelWidth() { return 140; }
    int stationPanelHeight() { return 124; }
    int stationPanelOffset() { return 0; }
    static class Animation {
        float switchProgress() { return 1; }
        int previousStation() { return -1; }
    }
    final Animation stationAnimation = new Animation();
    static class PoseStack {
        final Graphics2D graphics;
        final ArrayDeque<AffineTransform> stack = new ArrayDeque<>();
        PoseStack(Graphics2D graphics) { this.graphics = graphics; }
        void pushPose() { stack.push(graphics.getTransform()); }
        void popPose() { graphics.setTransform(stack.pop()); }
        void translate(double x, double y, double z) { graphics.translate(x, y); }
    }
    static class GuiGraphics {
        final Graphics2D graphics;
        final PoseStack pose;
        Shape previousClip;
        final java.util.List<Runnable> queuedText = new java.util.ArrayList<>();
        GuiGraphics(Graphics2D graphics) { this.graphics = graphics; pose = new PoseStack(graphics); }
        PoseStack pose() { return pose; }
        void enableScissor(int x1, int y1, int x2, int y2) {
            previousClip = graphics.getClip();
            graphics.clipRect(x1, y1, x2 - x1, y2 - y1);
        }
        void disableScissor() { graphics.setClip(previousClip); }
        void flush() {
            for (Runnable text : queuedText) text.run();
            queuedText.clear();
        }
        void queueText(String text, int x, int y, int color) {
            queuedText.add(() -> {
                graphics.setFont(new Font("SimSun", Font.PLAIN, 9));
                graphics.setColor(new Color(color));
                graphics.drawString(text, x, y + 8);
            });
        }
    }
    __BEGIN_METHOD__
    __END_METHOD__

    public static void main(String[] args) throws Exception {
        BufferedImage base = ImageIO.read(new File(args[0], "station-panel-1.png"));
        BufferedImage sheet = new BufferedImage(1440, 550, BufferedImage.TYPE_INT_RGB);
        Graphics2D header = sheet.createGraphics();
        header.setColor(new Color(0x17191b)); header.fillRect(0, 0, 1440, 550);
        header.setFont(new Font("Microsoft YaHei", Font.PLAIN, 21));
        header.setColor(new Color(0xdddddd));
        header.drawString("离线绘制顺序示意 · 非游戏截图，字体为替代字体", 20, 28);
        for (int corrected = 0; corrected < 2; corrected++) {
            int x = corrected * 720;
            header.drawString(corrected == 0 ? "问题顺序：文字在裁剪内提交" : "修正顺序：先提交文字，再开启裁剪", x + 20, 62);
            Graphics2D canvas = (Graphics2D) header.create(x, 80, 720, 454);
            canvas.drawImage(base, 0, 0, null);
            canvas.scale(2, 2);
            canvas.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            GuiGraphics graphics = new GuiGraphics(canvas);
            // The native tooltip background is submitted before its text batch.
            canvas.setColor(new Color(0xF0100010, true)); canvas.fillRect(220, 174, 115, 44);
            canvas.setColor(new Color(0x905000A0, true)); canvas.drawRect(220, 174, 114, 43);
            graphics.queueText("铁砧", 225, 179, 0xFFFFFF);
            graphics.queueText("EMC: 1,984", 225, 192, 0xAAAAAA);
            graphics.queueText("Minecraft", 225, 204, 0x5555FF);
            TooltipBatchPreview preview = new TooltipBatchPreview();
            if (corrected == 0) {
                // Illustrate the previous ordering without retaining old source.
                graphics.enableScissor(30, 15, 170, 139);
                graphics.pose().pushPose();
            } else {
                preview.beginStationPanelRender(graphics, false);
            }
            // Polymorph can render an empty widget after the main tooltip.
            preview.endStationPanelRender(graphics);
            canvas.dispose();
        }
        header.dispose();
        ImageIO.write(sheet, "png", new File(args[0], "tooltip-batching-preview.png"));
    }
}
'''


def main():
    folder = ROOT / "build/gui-preview"
    text = SOURCE.read_text(encoding="utf-8")
    source = ADAPTER.replace("__BEGIN_METHOD__", method(text, "    private void beginStationPanelRender("))
    source = source.replace("__END_METHOD__", method(text, "    public void endStationPanelRender("))
    generated = folder / "TooltipBatchPreview.java"
    generated.write_text(source, encoding="utf-8")
    java = Path("C:/Program Files/Java/jdk-21.0.11/bin")
    subprocess.run([str(java / "javac.exe"), "-encoding", "UTF-8", "-d", str(folder), str(generated)], check=True)
    subprocess.run([str(java / "java.exe"), "-Djava.awt.headless=true", "-cp", str(folder), "TooltipBatchPreview", str(folder)], check=True)
    print(folder / "tooltip-batching-preview.png")


if __name__ == "__main__":
    main()
