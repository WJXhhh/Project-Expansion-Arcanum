"""Compile the production RSI arcane source/hooks against narrow Minecraft/ProjectE stubs.

Not a Minecraft integration test. --root can select a pristine/modified source snapshot.
The pristine snapshot has no hook registered; the identical shortage fixture then stays missing.
"""
import argparse
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile

PROJECT = Path(__file__).resolve().parents[2]
PACKAGE = "com/wjx/forge/projectexa/"
STUBS = {
    "net/minecraft/world/item/Item.java": """
        public class Item {
            public final String name; public long price;
            public Item(String name, long price) { this.name=name; this.price=price; }
        }
    """,
    "net/minecraft/world/item/ItemStack.java": """
        public class ItemStack {
            public static final ItemStack EMPTY = new ItemStack(null,0);
            private final Item item; private int count;
            public String tag; public boolean transientData;
            public ItemStack(Item item,int count) { this.item=item; this.count=count; }
            public Item getItem() { return item; }
            public boolean isEmpty() { return item==null || count<=0; }
            public int getCount() { return count; }
            public boolean hasTag() { return tag!=null && !tag.isEmpty(); }
            public String getTag() { return tag; }
            public boolean isDamageableItem() { return "protected".equals(tag); }
            public boolean isEnchanted() { return "protected".equals(tag); }
            public int getDamageValue() { return 0; }
            public boolean hasCustomHoverName() { return false; }
            public ItemStack copyWithCount(int n) {
                ItemStack s=new ItemStack(item,n); s.tag=tag;s.transientData=transientData;return s;
            }
            public static boolean isSameItemSameTags(ItemStack a,ItemStack b) {
                return a.item==b.item && java.util.Objects.equals(a.tag,b.tag)
                    && a.transientData==b.transientData;
            }
        }
    """,
    "net/minecraft/world/item/crafting/Ingredient.java": """
        public class Ingredient {
            public boolean isEmpty() { return false; }
            public boolean test(net.minecraft.world.item.ItemStack s) { return !s.isEmpty(); }
        }
    """,
    "net/minecraft/world/entity/player/Player.java": "public class Player {}",
    "net/minecraft/server/level/ServerPlayer.java": """
        public class ServerPlayer extends net.minecraft.world.entity.player.Player {
            public boolean tablet=true,mainThread=true;
            public moze_intel.projecte.api.capabilities.IKnowledgeProvider provider;
            public final Inventory inv=new Inventory();
            public static class Inventory {
                public java.util.List<net.minecraft.world.item.ItemStack> items=new java.util.ArrayList<>();
            }
            public ServerPlayer getServer() { return this; }
            public boolean isSameThread() { return mainThread; }
            public Inventory getInventory() { return inv; }
            public Capability getCapability(Object key) { return new Capability(); }
            public class Capability {
                public moze_intel.projecte.api.capabilities.IKnowledgeProvider orElse(
                    moze_intel.projecte.api.capabilities.IKnowledgeProvider fallback) {
                    return provider==null?fallback:provider;
                }
            }
        }
    """,
    "moze_intel/projecte/api/ItemInfo.java": """
        public record ItemInfo(net.minecraft.world.item.ItemStack stack) {
            public net.minecraft.world.item.ItemStack createStack() { return stack.copyWithCount(1); }
        }
    """,
    "moze_intel/projecte/api/capabilities/IKnowledgeProvider.java": """
        public interface IKnowledgeProvider {
            boolean hasFullKnowledge();
            java.util.Set<moze_intel.projecte.api.ItemInfo> getKnowledge();
            boolean hasKnowledge(net.minecraft.world.item.ItemStack stack);
            java.math.BigInteger getEmc();
            void setEmc(java.math.BigInteger value);
            void syncEmc(net.minecraft.server.level.ServerPlayer player);
        }
    """,
    "moze_intel/projecte/api/capabilities/PECapabilities.java": """
        public class PECapabilities { public static final Object KNOWLEDGE_CAPABILITY=new Object(); }
    """,
    "moze_intel/projecte/api/proxy/IEMCProxy.java": """
        public class IEMCProxy {
            public static final IEMCProxy INSTANCE=new IEMCProxy();
            public long getValue(net.minecraft.world.item.ItemStack s) { return s.isEmpty()?0:s.getItem().price; }
        }
    """,
    "moze_intel/projecte/emc/EMCMappingHandler.java": """
        public class EMCMappingHandler {
            public static java.util.Set<moze_intel.projecte.api.ItemInfo> mapped=new java.util.LinkedHashSet<>();
            public static java.util.Set<moze_intel.projecte.api.ItemInfo> getMappedItems() { return mapped; }
        }
    """,
    "com/huanghuang/rsintegration/crafting/CraftingResolver.java": """
        public class CraftingResolver {
            public record StackKey(net.minecraft.world.item.Item item,String tag) {
                public net.minecraft.world.item.ItemStack toStack() {
                    var s=new net.minecraft.world.item.ItemStack(item,1);s.tag=tag;return s;
                }
            }
        }
    """,
    "net/minecraftforge/items/IItemHandler.java": """
        public interface IItemHandler {
            int getSlots(); net.minecraft.world.item.ItemStack getStackInSlot(int slot);
            net.minecraft.world.item.ItemStack extractItem(int slot,int amount,boolean simulate);
            net.minecraft.world.item.ItemStack insertItem(int slot,net.minecraft.world.item.ItemStack s,boolean simulate);
            int getSlotLimit(int slot); boolean isItemValid(int slot,net.minecraft.world.item.ItemStack s);
        }
    """,
    PACKAGE + "util/Util.java": """
        public class Util {
            public static net.minecraft.world.item.ItemStack cleanStack(net.minecraft.world.item.ItemStack s) {
                var c=s.copyWithCount(1);c.transientData=false;return c;
            }
        }
    """,
    PACKAGE + "net/packets/to_server/PacketOpenArcaneTransmutationTablet.java": """
        public class PacketOpenArcaneTransmutationTablet {
            public static boolean hasTablet(net.minecraft.server.level.ServerPlayer p) { return p.tablet; }
        }
    """,
    PACKAGE + "mixin/RsiExtractionLedgerAccessor.java": """
        public interface RsiExtractionLedgerAccessor {
            java.util.List<net.minecraftforge.items.IItemHandler> SOURCES=new java.util.ArrayList<>();
            static java.util.List<net.minecraftforge.items.IItemHandler> projectexa$backpacks(
                    net.minecraft.server.level.ServerPlayer p) { return SOURCES; }
        }
    """,
    "org/spongepowered/asm/mixin/Mixin.java":
        "public @interface Mixin { String[] targets(); boolean remap() default true; }",
    "org/spongepowered/asm/mixin/Pseudo.java": "public @interface Pseudo {}",
    "org/spongepowered/asm/mixin/Final.java": "public @interface Final {}",
    "org/spongepowered/asm/mixin/Shadow.java": "public @interface Shadow {}",
    "org/spongepowered/asm/mixin/injection/At.java":
        'public @interface At { String value(); String target() default ""; }',
    "org/spongepowered/asm/mixin/injection/Inject.java": """
        public @interface Inject { String[] method(); At at(); boolean cancellable() default false; }
    """,
    "org/spongepowered/asm/mixin/injection/Redirect.java":
        "public @interface Redirect { String[] method(); At at(); }",
    "org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable.java": """
        public class CallbackInfoReturnable<T> {
            private T value;
            public CallbackInfoReturnable(T value) { this.value=value; }
            public T getReturnValue() { return value; }
            public void setReturnValue(T value) { this.value=value; }
        }
    """,
}

HARNESS = r"""
import com.wjx.forge.projectexa.integrations.rsi.ArcaneCraftingInventory;
import com.wjx.forge.projectexa.mixin.RsiExtractionLedgerAccessor;
import com.huanghuang.rsintegration.crafting.CraftingResolver.StackKey;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.emc.EMCMappingHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.math.BigInteger;
import java.util.*;

public class ArcaneTest {
    static class Knowledge implements IKnowledgeProvider {
        BigInteger emc=BigInteger.valueOf(100); int syncs; boolean full;
        Set<ItemInfo> known=new LinkedHashSet<>();
        public boolean hasFullKnowledge(){ return full; }
        public Set<ItemInfo> getKnowledge(){ return known; }
        public boolean hasKnowledge(ItemStack s){
            return (full?EMCMappingHandler.mapped:known).stream()
                .anyMatch(k->k.createStack().getItem()==s.getItem());
        }
        public BigInteger getEmc(){ return emc; }
        public void setEmc(BigInteger v){ emc=v; }
        public void syncEmc(ServerPlayer p){ syncs++; }
    }
    static void check(boolean b,String msg){ if(!b)throw new AssertionError(msg); }
    static int find(ArcaneCraftingInventory v,Item item){
        for(int i=0;i<v.getSlots();i++)if(v.getStackInSlot(i).getItem()==item)return i;
        return -1;
    }
    static Object invoke(String clazz,String name,Object...args)throws Exception {
        for(var m:Class.forName("com.wjx.forge.projectexa.mixin."+clazz).getDeclaredMethods()) {
            if(m.getName().equals(name)){m.setAccessible(true);return m.invoke(null,args);}
        }
        throw new AssertionError("missing hook "+name);
    }
    static class Backpack implements IItemHandler {
        ItemStack s; Backpack(ItemStack s){this.s=s;}
        public int getSlots(){return 1;} public ItemStack getStackInSlot(int i){return s;}
        public ItemStack extractItem(int i,int n,boolean sim){return ItemStack.EMPTY;}
        public ItemStack insertItem(int i,ItemStack x,boolean sim){return x;}
        public int getSlotLimit(int i){return 64;} public boolean isItemValid(int i,ItemStack x){return true;}
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args)throws Exception {
        Item iron=new Item("iron",2),diamond=new Item("diamond",20),unknown=new Item("unknown",5);
        Knowledge k=new Knowledge();
        k.known.add(new ItemInfo(new ItemStack(iron,1)));k.known.add(new ItemInfo(new ItemStack(diamond,1)));
        ServerPlayer p=new ServerPlayer();p.provider=k;p.inv.items.add(new ItemStack(iron,3));
        var v=new ArcaneCraftingInventory(p,List.of(),Map.of());
        int i=find(v,iron),d=find(v,diamond);
        check(i>=0&&d>=0,"known templates");
        check(ArcaneCraftingInventory.key(new ItemStack(iron,1)).tag()==null,"RSI null NBT key");
        check(v.extractItem(i,5,true).getCount()==5&&k.emc.intValue()==100&&k.syncs==0,"preview free");
        var material=v.extractItem(i,5,false);
        check(material.getCount()==5&&k.emc.intValue()==90&&k.syncs==1,"charge only missing five");
        p.inv.items.add(material); // Models RSI's existing real-item refund, not a second EMC refund.
        check(k.emc.intValue()==90,"refund must not also refund EMC");
        System.out.println("SHORTAGE physical=3 request=8 supplemental=5 EMC=100->90 refund=5_real_items");
        p.inv.items.remove(material);k.emc=BigInteger.valueOf(100);

        Map<StackKey,Integer> pending=new HashMap<>();
        var budgeted=new ArcaneCraftingInventory(p,List.of(),pending);
        pending.put(ArcaneCraftingInventory.key(new ItemStack(iron,1)),8);
        check(budgeted.getStackInSlot(d).getCount()==4,"shared budget: five missing iron reserve ten EMC");
        pending.put(ArcaneCraftingInventory.key(new ItemStack(iron,1)),54);
        check(budgeted.getStackInSlot(d).isEmpty(),"reject cross-item oversubscription");
        pending.clear();check(budgeted.getStackInSlot(d).getCount()==5,"cancel releases reservation budget");
        pending.put(ArcaneCraftingInventory.key(new ItemStack(iron,1)),3);
        check(budgeted.getStackInSlot(d).getCount()==5,"physical reservations cost no EMC");
        pending.put(ArcaneCraftingInventory.key(new ItemStack(iron,1)),8);
        var bp=new Backpack(new ItemStack(iron,3));
        var withBackpack=new ArcaneCraftingInventory(p,List.of(bp),pending);
        check(withBackpack.getStackInSlot(d).getCount()==4,"physical backpacks offset cost");

        var once=ArcaneCraftingInventory.append(p,List.of(bp),Map.of());
        var twice=ArcaneCraftingInventory.append(p,once,pending);
        check(twice.size()==2&&twice.get(0)==bp,"one virtual source, after physical backpacks");
        var cir=new CallbackInfoReturnable<List<IItemHandler>>(new ArrayList<>());
        invoke("RsiExtractionLedgerMixin","projectexa$appendArcaneSource",p,cir);
        check(cir.getReturnValue().size()==1,"append works without Sophisticated Backpacks");
        RsiExtractionLedgerAccessor.SOURCES.clear();RsiExtractionLedgerAccessor.SOURCES.addAll(cir.getReturnValue());
        var physical=(List<IItemHandler>)invoke("RsiMaterialSourcesMixin","projectexa$physicalBackpacksOnly",p);
        check(physical.isEmpty(),"do not count virtual source twice when backpacks installed");
        var counts=new HashMap<StackKey,Integer>();counts.put(ArcaneCraftingInventory.key(new ItemStack(iron,1)),3);
        var snapshot=new CallbackInfoReturnable<Map<StackKey,Integer>>(counts);
        invoke("RsiMaterialSourcesMixin","projectexa$includeArcaneKnowledge",p,snapshot);
        check(snapshot.getReturnValue().get(ArcaneCraftingInventory.key(new ItemStack(iron,1)))==53,"physical plus virtual snapshot");
        check(k.emc.intValue()==100&&k.syncs==1,"snapshots and reservations never spend EMC");
        System.out.println("PASS shared_EMC_budget reservation_cancel physical_priority snapshot no_backpack_mod no_double_count");

        p.tablet=false;check(v.getStackInSlot(i).isEmpty()&&v.extractItem(i,1,false).isEmpty(),"tablet removed");
        check(new ArcaneCraftingInventory(p,List.of(),Map.of()).getSlots()==0,"no tablet no source");
        p.tablet=true;p.mainThread=false;check(v.getStackInSlot(i).isEmpty(),"worker thread blocked");p.mainThread=true;
        k.known.removeIf(x->x.createStack().getItem()==iron);check(v.getStackInSlot(i).isEmpty(),"forgotten knowledge");
        k.known.add(new ItemInfo(new ItemStack(iron,1)));iron.price=0;check(v.extractItem(i,1,false).isEmpty(),"EMC mapping removed");iron.price=2;
        p.provider=null;check(v.getStackInSlot(i).isEmpty(),"capability absent");p.provider=k;
        k.emc=BigInteger.ONE;check(v.extractItem(i,1,false).isEmpty(),"insufficient EMC");
        k.emc=BigInteger.TEN.pow(80);check(v.getStackInSlot(i).getCount()==Integer.MAX_VALUE-3,"integer headroom");
        check(find(v,unknown)==-1,"unknown item unavailable");
        check(v.getStackInSlot(-1).isEmpty()&&v.getStackInSlot(999).isEmpty(),"invalid slots");
        check(v.extractItem(i,-1,false).isEmpty(),"negative quantity");
        ItemStack protectedItem=new ItemStack(unknown,1);protectedItem.tag="protected";k.known.add(new ItemInfo(protectedItem));
        check(find(new ArcaneCraftingInventory(p,List.of(),Map.of()),unknown)==-1,"protected NBT excluded");
        ItemStack namedIron=new ItemStack(iron,1);namedIron.transientData=true;k.known.add(new ItemInfo(namedIron));
        check(new ArcaneCraftingInventory(p,List.of(),Map.of()).getSlots()==2,"canonical templates deduplicated");
        k.full=true;EMCMappingHandler.mapped.add(new ItemInfo(new ItemStack(unknown,1)));
        check(find(new ArcaneCraftingInventory(p,List.of(),Map.of()),unknown)>=0,"full knowledge EMC map");
        ItemStack offered=new ItemStack(iron,7);
        check(v.insertItem(i,offered,false)==offered&&!v.isItemValid(i,offered),"no input consumption");
        System.out.println("PASS tablet_removal knowledge_loss no_EMC no_value no_capability main_thread NBT full_knowledge bigint_bounds");
    }
}
"""


def java_home():
    candidates = list((Path.home() / ".gradle/jdks").glob("**/jdk-17*"))
    candidates += [Path(os.environ.get("JAVA_HOME", "")), Path("C:/Program Files/Java/jdk-21.0.11")]
    for path in candidates:
        javac = path / "bin" / ("javac.exe" if os.name == "nt" else "javac")
        if javac.is_file():
            version = subprocess.run([str(javac), "-version"], capture_output=True, text=True)
            if not (version.stdout + version.stderr).strip().startswith("javac 1."):
                return path
    javac = shutil.which("javac")
    if javac:
        return Path(javac).resolve().parents[1]
    raise RuntimeError("JDK 17+ required")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=PROJECT)
    args = parser.parse_args()
    root = args.root.resolve()
    config = json.loads((root / "src/main/resources/projectexa.mixins.json").read_text(encoding="utf-8"))
    enabled = "RsiMaterialSourcesMixin" in config["mixins"]
    if not enabled:
        print("SHORTAGE physical=3 request=8 supplemental=0 EMC=100->100 missing=5")
        print("BASELINE: no arcane material-source hook registered (configuration observation, not an in-game test)")
        return
    for name in ("RsiMaterialSourcesMixin", "RsiExtractionLedgerMixin", "RsiExtractionLedgerAccessor"):
        assert name in config["mixins"] and name not in config["client"], name
    with tempfile.TemporaryDirectory(prefix="projectexa-rsi-test-") as tmp:
        tmp = Path(tmp)
        for relative, body in STUBS.items():
            path = tmp / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text("package " + relative.rsplit("/", 1)[0].replace("/", ".") + ";\n" + body, encoding="utf-8")
        for relative in (PACKAGE + "integrations/rsi/ArcaneCraftingInventory.java",
                         PACKAGE + "mixin/RsiMaterialSourcesMixin.java", PACKAGE + "mixin/RsiExtractionLedgerMixin.java"):
            dst = tmp / relative
            dst.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(root / "src/main/java" / relative, dst)
        (tmp / "ArcaneTest.java").write_text(HARNESS, encoding="utf-8")
        classes = tmp / "classes"
        classes.mkdir()
        jdk = java_home()
        suffix = ".exe" if os.name == "nt" else ""
        subprocess.run([str(jdk / "bin" / ("javac" + suffix)), "-encoding", "UTF-8", "-d", str(classes)]
                       + [str(p) for p in tmp.rglob("*.java")], check=True)
        subprocess.run([str(jdk / "bin" / ("java" + suffix)), "-cp", str(classes), "ArcaneTest"], check=True)


if __name__ == "__main__":
    main()
