package com.wjx.forge.projectexa.smoke;

import com.huanghuang.rsintegration.crafting.*;
import com.mojang.authlib.GameProfile;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.math.BigInteger;
import java.nio.file.*;
import java.util.*;

@Mod.EventBusSubscriber(modid="projectexa")
public class RsiArcaneNativeSmoke {
    static void check(boolean ok,String msg) { if(!ok) throw new AssertionError(msg); }
    @SubscribeEvent
    public static void started(ServerStartedEvent event) {
        var server=event.getServer();
        server.execute(() -> {
            String result;
            try {
                var p=FakePlayerFactory.get(server.overworld(),new GameProfile(
                    UUID.fromString("2e1e6b25-5cd0-4d52-842e-5717b1b46a71"),"ArcaneSmoke"));
                var k=p.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY).orElseThrow(()->new AssertionError("knowledge"));
                var iron=new ItemStack(Items.IRON_INGOT);
                var diamond=new ItemStack(Items.DIAMOND);
                long price=IEMCProxy.INSTANCE.getValue(iron);
                long diamondPrice=IEMCProxy.INSTANCE.getValue(diamond);
                check(price>0&&diamondPrice>0,"EMC mapping ready");
                var initial=BigInteger.valueOf(price).multiply(BigInteger.valueOf(10));
                k.clearKnowledge();k.addKnowledge(iron);k.addKnowledge(diamond);k.setEmc(initial);
                p.getInventory().clearContent();
                p.getInventory().setItem(0,new ItemStack(com.wjx.forge.projectexa.registries.Items.ARCANE_TRANSMUTATION_TABLET.get()));
                p.getInventory().setItem(1,iron.copyWithCount(3));
                var key=new CraftingResolver.StackKey(Items.IRON_INGOT,null);
                var counts=MaterialSources.countInventory(p);
                if (!Boolean.parseBoolean(System.getProperty("rsi.smoke.expect", "true"))) {
                    check(counts.getOrDefault(key,0)==3,"baseline physical-only count");
                    var baseline=new ExtractionLedger();
                    check(baseline.reserveExactAcrossNetworkAndInventory(iron,8,(CraftStorageEndpoint)null,p).isEmpty(),"baseline must stay short");
                    check(k.getEmc().equals(initial),"baseline EMC unchanged");
                    System.out.println("RSI_ARCANE_NATIVE PASS physical=3 request=8 supplemental=0 EMC="+initial+"->"+initial+" missing=5");
                    Files.writeString(Path.of("native-smoke-result.txt"),"PASS physical=3 request=8 supplemental=0 EMC="+initial+"->"+initial+" missing=5\n");
                    server.halt(false);
                    return;
                }
                check(counts.getOrDefault(key,0)==13,"snapshot: physical3+virtual10, got "+counts.get(key));
                check(k.getEmc().equals(initial),"preview charged EMC");
                var ledger=new ExtractionLedger();
                var reserved=ledger.reserveExactAcrossNetworkAndInventory(iron,8,(CraftStorageEndpoint)null,p);
                check(reserved.getCount()==8,"exact reserve8");
                check(k.getEmc().equals(initial),"reservation charged EMC");
                check(ledger.commit(null,p),"commit");
                var remaining=initial.subtract(BigInteger.valueOf(price).multiply(BigInteger.valueOf(5)));
                check(k.getEmc().equals(remaining),"charge only missing5");
                ledger.rollback(p);
                int returned=p.getInventory().items.stream().filter(s->s.is(Items.IRON_INGOT)).mapToInt(ItemStack::getCount).sum();
                check(returned==8&&k.getEmc().equals(remaining),"refund real material, no double EMC refund");
                p.getInventory().clearContent();
                p.getInventory().setItem(0,new ItemStack(com.wjx.forge.projectexa.registries.Items.ARCANE_TRANSMUTATION_TABLET.get()));
                p.getInventory().setItem(1,iron.copyWithCount(3));
                k.setEmc(initial);
                var shared=new ExtractionLedger();
                check(shared.reserveExactAcrossNetworkAndInventory(iron,11,(CraftStorageEndpoint)null,p).getCount()==11,"reserve shared iron");
                check(shared.reserveExactAcrossNetworkAndInventory(iron,3,(CraftStorageEndpoint)null,p).isEmpty(),"reject overbudget same item");
                shared.cancelReservationsSince(0);
                check(shared.reserveExactAcrossNetworkAndInventory(iron,13,(CraftStorageEndpoint)null,p).getCount()==13,"cancel frees budget");
                shared.rollback(p);
                var gold=new ItemStack(Items.GOLD_INGOT);
                k.addKnowledge(gold);
                var cross=new ExtractionLedger();
                check(cross.reserveExactAcrossNetworkAndInventory(iron,11,(CraftStorageEndpoint)null,p).getCount()==11,"cross-material iron reserve");
                check(cross.reserveExactAcrossNetworkAndInventory(gold,1,(CraftStorageEndpoint)null,p).isEmpty(),"shared EMC blocks gold oversubscription");
                cross.cancelReservationsSince(0);
                check(cross.reserveExactAcrossNetworkAndInventory(gold,1,(CraftStorageEndpoint)null,p).getCount()==1,"cross-material cancel frees EMC");
                cross.rollback(p);
                var generic=new ExtractionLedger();
                check(generic.reserveFromInventory(net.minecraft.world.item.crafting.Ingredient.of(iron),3,p).getCount()==3,"generic physical reserve");
                check(generic.reserveFromInventory(net.minecraft.world.item.crafting.Ingredient.of(iron),10,p).getCount()==10,"generic combine physical pending and virtual");
                check(generic.commit(null,p),"generic commit");
                check(k.getEmc().equals(BigInteger.ZERO),"generic only missing10 charged");
                generic.rollback(p);
                p.getInventory().clearContent();
                k.setEmc(initial);
                check(MaterialSources.countInventory(p).isEmpty(),"no tablet no virtual inventory");
                result="PASS native_RSI_snapshot exact_reserve commit missing5_only refund8_real generic_pending shared_budget cancel cross_material no_tablet physical=3 request=8 supplemental=5 price="+price;
            } catch(Throwable failure) {
                failure.printStackTrace();result="FAIL "+failure;
            }
            System.out.println("RSI_ARCANE_NATIVE "+result);
            try { Files.writeString(Path.of("native-smoke-result.txt"),result+"\n"); } catch(Exception ignored) {}
            server.halt(false);
        });
    }
}
