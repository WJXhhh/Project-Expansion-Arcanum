"""Headless regression for the real JEI context mixin and crafting handler.

Uses narrow Minecraft/JEI stubs, not a running game. Optionally test a source
overlay (including a pristine snapshot) with --overlay PATH.
"""
import argparse
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile

PROJECT = Path(__file__).resolve().parents[2]
PACKAGE = "com/wjx/forge/projectexa/"
STUBS = {
    "javax/annotation/Nullable.java": "public @interface Nullable {}",
    "org/spongepowered/asm/mixin/Mixin.java":
        "public @interface Mixin { String[] targets(); boolean remap() default true; }",
    "org/spongepowered/asm/mixin/Pseudo.java": "public @interface Pseudo {}",
    "org/spongepowered/asm/mixin/Unique.java": "public @interface Unique {}",
    "org/spongepowered/asm/mixin/injection/At.java":
        'public @interface At { String value(); }',
    "org/spongepowered/asm/mixin/injection/Inject.java": """
        public @interface Inject { String[] method(); At at();
        boolean cancellable() default false; boolean remap() default true;
        int require() default -1; }
    """,
    "org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable.java": """
        public class CallbackInfoReturnable<T> {
            private T value;
            public CallbackInfoReturnable(T value) { this.value = value; }
            public T getReturnValue() { return value; }
            public void setReturnValue(T value) { this.value = value; }
        }
    """,
    "net/minecraft/client/Minecraft.java": """
        public class Minecraft {
            private static final Minecraft INSTANCE = new Minecraft();
            public net.minecraft.world.entity.player.Player player;
            public Object screen;
            public Object font;
            public static Minecraft getInstance() { return INSTANCE; }
        }
    """,
    "net/minecraft/client/gui/GuiGraphics.java": """
        public class GuiGraphics {
            public void renderTooltip(Object font,
                net.minecraft.network.chat.Component text, int x, int y) {}
        }
    """,
    "net/minecraft/network/chat/Component.java": """
        public class Component {
            public static Component empty() { return new Component(); }
            public static Component translatable(String key) { return new Component(); }
        }
    """,
    "net/minecraft/world/entity/player/Inventory.java": "public class Inventory {}",
    "net/minecraft/world/entity/player/Player.java": """
        public class Player {
            public boolean tablet = true;
            public final net.minecraft.world.inventory.InventoryMenu inventoryMenu =
                new net.minecraft.world.inventory.InventoryMenu();
            public net.minecraft.world.inventory.AbstractContainerMenu containerMenu = inventoryMenu;
            private final Inventory inventory = new Inventory();
            public Inventory getInventory() { return inventory; }
        }
    """,
    "net/minecraft/world/inventory/MenuType.java": "public class MenuType<T> {}",
    "net/minecraft/world/inventory/AbstractContainerMenu.java": """
        public abstract class AbstractContainerMenu {
            public AbstractContainerMenu(MenuType<?> type, int id) {}
            public abstract net.minecraft.world.item.ItemStack quickMoveStack(
                net.minecraft.world.entity.player.Player player, int slot);
            public abstract boolean stillValid(net.minecraft.world.entity.player.Player player);
        }
    """,
    "net/minecraft/world/inventory/InventoryMenu.java": """
        public class InventoryMenu extends AbstractContainerMenu {
            public InventoryMenu() { super(null, 0); }
            public net.minecraft.world.item.ItemStack quickMoveStack(
                net.minecraft.world.entity.player.Player p, int s) {
                return net.minecraft.world.item.ItemStack.EMPTY;
            }
            public boolean stillValid(net.minecraft.world.entity.player.Player p) { return true; }
        }
    """,
    "net/minecraft/client/gui/screens/inventory/AbstractContainerScreen.java": """
        public abstract class AbstractContainerScreen<T extends
            net.minecraft.world.inventory.AbstractContainerMenu> {
            private final T menu;
            protected AbstractContainerScreen(T menu,
                net.minecraft.world.entity.player.Inventory inventory,
                net.minecraft.network.chat.Component title) { this.menu = menu; }
            public T getMenu() { return menu; }
            protected abstract void renderBg(net.minecraft.client.gui.GuiGraphics g,
                float tick, int x, int y);
        }
    """,
    "net/minecraft/world/item/ItemStack.java": """
        public record ItemStack(String id) {
            public static final ItemStack EMPTY = new ItemStack("empty");
        }
    """,
    "net/minecraft/world/item/crafting/CraftingRecipe.java": "public interface CraftingRecipe {}",
    "mezz/jei/api/recipe/RecipeType.java": "public class RecipeType<T> {}",
    "mezz/jei/api/constants/RecipeTypes.java": """
        public class RecipeTypes {
            public static final mezz.jei.api.recipe.RecipeType<
                net.minecraft.world.item.crafting.CraftingRecipe> CRAFTING =
                new mezz.jei.api.recipe.RecipeType<>();
        }
    """,
    "mezz/jei/api/recipe/RecipeIngredientRole.java": "public enum RecipeIngredientRole { INPUT }",
    "mezz/jei/api/gui/ingredient/IRecipeSlotView.java": """
        public interface IRecipeSlotView {
            java.util.stream.Stream<net.minecraft.world.item.ItemStack> getItemStacks();
        }
    """,
    "mezz/jei/api/gui/ingredient/IRecipeSlotsView.java": """
        public interface IRecipeSlotsView {
            java.util.List<IRecipeSlotView> getSlotViews(mezz.jei.api.recipe.RecipeIngredientRole role);
        }
    """,
    "mezz/jei/api/recipe/transfer/IRecipeTransferError.java": """
        public interface IRecipeTransferError {
            enum Type { INTERNAL, COSMETIC }
            Type getType();
            default void showError(net.minecraft.client.gui.GuiGraphics g, int x, int y,
                mezz.jei.api.gui.ingredient.IRecipeSlotsView slots, int rx, int ry) {}
        }
    """,
    "mezz/jei/api/recipe/transfer/IRecipeTransferHandler.java": """
        public interface IRecipeTransferHandler<C, R> {
            Class<? extends C> getContainerClass();
            java.util.Optional<net.minecraft.world.inventory.MenuType<C>> getMenuType();
            mezz.jei.api.recipe.RecipeType<R> getRecipeType();
            IRecipeTransferError transferRecipe(C container, R recipe,
                mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,
                net.minecraft.world.entity.player.Player player, boolean all, boolean transfer);
        }
    """,
    PACKAGE + "net/PacketHandler.java": """
        public class PacketHandler {
            public static Object lastPacket;
            public static void sendToServer(Object packet) { lastPacket = packet; }
        }
    """,
    PACKAGE + "net/packets/to_server/PacketOpenArcaneTransmutationTablet.java": """
        public class PacketOpenArcaneTransmutationTablet {
            public static boolean hasTablet(net.minecraft.world.entity.player.Player p) {
                return p != null && p.tablet;
            }
        }
    """,
    PACKAGE + "net/packets/to_server/PacketOpenArcaneTransmutationTabletWithRecipe.java": """
        public record PacketOpenArcaneTransmutationTabletWithRecipe(
            java.util.List<java.util.List<net.minecraft.world.item.ItemStack>> recipe,
            boolean transferAll) {}
    """,
}

HARNESS = r"""
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import mezz.jei.api.gui.ingredient.*;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.wjx.forge.projectexa.integrations.jei.UniversalArcaneCraftingRecipeTransferHandler;
import com.wjx.forge.projectexa.net.PacketHandler;
import com.wjx.forge.projectexa.net.packets.to_server.PacketOpenArcaneTransmutationTabletWithRecipe;

public class Regression {
    static Object mixin;
    static Object hook(String name, Object original) throws Exception {
        if (mixin == null) return original;
        Method method = mixin.getClass().getDeclaredMethod(name, CallbackInfoReturnable.class);
        method.setAccessible(true);
        CallbackInfoReturnable<Object> callback = new CallbackInfoReturnable<>(original);
        method.invoke(mixin, callback);
        return callback.getReturnValue();
    }
    static Object screen(Object original) throws Exception {
        return hook("projectexa$provideTransferScreen", original);
    }
    static AbstractContainerMenu menu(Object original) throws Exception {
        return (AbstractContainerMenu) hook("projectexa$provideTransferMenu", original);
    }
    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        boolean enabled = Boolean.parseBoolean(args[0]);
        if (enabled) mixin = Class.forName(
            "com.wjx.forge.projectexa.mixin.JeiRecipesGuiMixin").getConstructor().newInstance();
        Player player = new Player();
        Minecraft.getInstance().player = player;
        Object quest = new Object();
        Minecraft.getInstance().screen = quest;
        Object fallback = screen(null);
        AbstractContainerMenu fallbackMenu = menu(null);
        check((fallback != null) == enabled, "modern fallback availability");
        check((fallbackMenu != null) == enabled, "legacy fallback availability");
        System.out.println("modern_quest=" + (enabled ? "ARCANE_CONTEXT" : "HIDDEN"));
        System.out.println("legacy_quest=" + (enabled ? "ARCANE_CONTEXT" : "HIDDEN"));
        if (enabled) {
            check(((AbstractContainerScreen<?>) fallback).getMenu() == fallbackMenu,
                "modern screen and legacy menu must share context");
            check(fallbackMenu.getClass() != player.inventoryMenu.getClass(),
                "must not select vanilla 2x2 transfer");
            check(fallback == screen(null), "cache context across ticks");
            check(!fallbackMenu.stillValid(player), "context must not open as a real menu");
            check(fallbackMenu.quickMoveStack(player, 0) == ItemStack.EMPTY, "no slot movement");
            var handler = UniversalArcaneCraftingRecipeTransferHandler.INSTANCE;
            for (int count : new int[] {4, 9}) {
                IRecipeSlotsView slots = role -> java.util.stream.IntStream.range(0, count)
                    .mapToObj(i -> (IRecipeSlotView) () -> Stream.of(new ItemStack("input" + i)))
                    .toList();
                PacketHandler.lastPacket = null;
                var error = handler.transferRecipe(fallbackMenu, null, slots, player, false, false);
                check(error.getType() == IRecipeTransferError.Type.COSMETIC, "enabled tooltip");
                check(PacketHandler.lastPacket == null, "hover must not send packet");
                for (boolean all : new boolean[] {false, true}) {
                    check(handler.transferRecipe(fallbackMenu, null, slots, player, all, true) == null,
                        "actual transfer");
                    var packet = (PacketOpenArcaneTransmutationTabletWithRecipe) PacketHandler.lastPacket;
                    check(packet.transferAll() == all && packet.recipe().size() == 9,
                        "shift and nine-slot packet");
                    for (int i = 0; i < 9; i++) check(packet.recipe().get(i).get(0).equals(
                        i < count ? new ItemStack("input" + i) : ItemStack.EMPTY), "slot " + i);
                }
            }
            System.out.println("crafting_2x2_and_3x3=ARCANE_PACKET; shift=PASS; preview=NO_PACKET");
        }
        var realMenu = player.inventoryMenu;
        var furnace = new AbstractContainerScreen<AbstractContainerMenu>(
            realMenu, player.getInventory(), Component.empty()) {
            protected void renderBg(GuiGraphics g, float tick, int x, int y) {}
        };
        check(screen(furnace) == furnace && menu(realMenu) == realMenu, "real parent preserved");
        check(Minecraft.getInstance().screen == quest && player.containerMenu == realMenu,
            "must not replace quest screen or player menu");
        System.out.println("real_container=UNCHANGED; quest_screen=UNCHANGED; player_menu=UNCHANGED");
        player.tablet = false;
        check(screen(null) == null && menu(null) == null, "no tablet hides transfer");
        if (enabled) {
            PacketHandler.lastPacket = null;
            var error = UniversalArcaneCraftingRecipeTransferHandler.INSTANCE.transferRecipe(
                fallbackMenu, null, role -> List.of(), player, false, true);
            check(error.getType() == IRecipeTransferError.Type.INTERNAL &&
                PacketHandler.lastPacket == null, "removed tablet must block stale-button transfer");
        }
        System.out.println("no_tablet=HIDDEN");
        Minecraft.getInstance().player = null;
        check(screen(null) == null && menu(null) == null, "no player");
        System.out.println("no_player=HIDDEN");
        Minecraft.getInstance().player = new Player();
        if (enabled) check(screen(null) != fallback, "player replacement rebuilds context");
        System.out.println("PASS (headless stubs; not an in-game GUI test)");
    }
}
"""


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--overlay", type=Path, default=PROJECT)
    args = parser.parse_args()
    config = json.loads((args.overlay / "src/main/resources/projectexa.mixins.json").read_text())
    enabled = "JeiRecipesGuiMixin" in config["client"]
    suffix = ".exe" if os.name == "nt" else ""
    candidates = [Path(os.environ.get("JAVA_HOME", ""))]
    candidates += list((Path.home() / ".gradle/jdks").glob("*/jdk-17*"))
    java_home = None
    for candidate in candidates:
        compiler = candidate / f"bin/javac{suffix}"
        if not compiler.is_file():
            continue
        version = subprocess.run([str(compiler), "-version"], capture_output=True, text=True)
        match = re.search(r"javac (\d+)", version.stdout + version.stderr)
        if match and int(match[1]) >= 17:
            java_home = candidate
            break
    if java_home is None:
        parser.error("Set JAVA_HOME to JDK 17 or newer.")
    with tempfile.TemporaryDirectory(prefix="projectexa-jei-regression-") as directory:
        root = Path(directory)
        for name, body in STUBS.items():
            path = root / name
            path.parent.mkdir(parents=True, exist_ok=True)
            package = name.rsplit("/", 1)[0].replace("/", ".")
            path.write_text(f"package {package};\n{body}", encoding="utf-8")
        real_sources = [(PROJECT, "integrations/jei/UniversalArcaneCraftingRecipeTransferHandler.java")]
        if enabled:
            real_sources += [(args.overlay, "integrations/jei/JeiArcaneTransferScreen.java"),
                             (args.overlay, "mixin/JeiRecipesGuiMixin.java")]
        for source_root, name in real_sources:
            source = source_root / "src/main/java" / PACKAGE / name
            target = root / PACKAGE / name
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source, target)
        (root / "Regression.java").write_text(HARNESS, encoding="utf-8")
        sources = sorted(str(path) for path in root.rglob("*.java"))
        (root / "classes").mkdir()
        subprocess.run([str(java_home / f"bin/javac{suffix}"), "--release", "17", "-encoding", "UTF-8", "-d",
                        str(root / "classes"), *sources], check=True)
        subprocess.run([str(java_home / f"bin/java{suffix}"), "-cp", str(root / "classes"),
                        "Regression", str(enabled).lower()], check=True)


if __name__ == "__main__":
    main()
