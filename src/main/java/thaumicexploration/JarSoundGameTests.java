package thaumicexploration;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.aspect.*;
import thaumcraft.registry.ModSounds;

@GameTestHolder(ThaumicExploration.ID) @PrefixGameTestTemplate(false)
public final class JarSoundGameTests {
    private static final String T="test/platform";
    private static List<PlayLevelSoundEvent> hear(GameTestHelper h,Runnable action){
        var heard=new ArrayList<PlayLevelSoundEvent>();
        Consumer<PlayLevelSoundEvent> listener=e->{if(e.getLevel()==h.getLevel())heard.add(e);};
        NeoForge.EVENT_BUS.addListener(listener);
        try{action.run();}finally{NeoForge.EVENT_BUS.unregister(listener);}
        return heard;
    }
    private static long count(List<PlayLevelSoundEvent> heard,SoundEvent sound){return heard.stream().filter(e->e.getSound()!=null&&e.getSound().value()==sound).count();}
    @GameTest(template=T,timeoutTicks=150) public static void oblivionReceivesEssentiaFromAlembicThroughRealTubes(GameTestHelper h){
        var jar=PortGameTests.place(h,"oblivion_jar",4,2,2);h.setBlock(new BlockPos(2,2,2),thaumcraft.registry.ModBlocks.ALEMBIC.get());
        thaumcraft.alchemy.AlembicBlockEntity source=h.getBlockEntity(new BlockPos(2,2,2));source.addToContainer(Aspect.ORDER,8);
        for(int x=2;x<=4;x++)h.setBlock(new BlockPos(x,3,2),thaumcraft.registry.ModBlocks.ESSENTIA_TUBE.get());
        h.succeedWhen(()->{h.assertTrue(source.containerContains(Aspect.ORDER)<8,"alembic was not drained");h.assertTrue(jar.containerContains(Aspect.ORDER)>0,"real tubes did not fill the destructive jar");});
    }
    @GameTest(template=T) public static void urnExtinguishingPlaysLegacyLoudSwimOnce(GameTestHelper h){
        var m=PortGameTests.place(h,"everfull_urn",3,2,3);var p=thaumcraft.test.TestSupport.mockServerPlayer(h);p.setPos(h.absolutePos(new BlockPos(4,2,3)).getCenter());p.setNoGravity(true);h.getLevel().addNewPlayer(p);p.setRemainingFireTicks(200);
        try{
            var heard=hear(h,()->{for(int i=0;i<50;i++)MachineEntity.tick(h.getLevel(),m.getBlockPos(),m.getBlockState(),m);});
            h.assertTrue(!p.isOnFire(),"urn did not extinguish player");h.assertValueEqual(count(heard,SoundEvents.GENERIC_SWIM),1L,"extinguishing sound duplicated or missing");
            h.assertTrue(heard.stream().anyMatch(e->e.getSound().value()==SoundEvents.GENERIC_SWIM&&e.getOriginalVolume()==2),"legacy extinguishing volume lost");h.succeed();
        }finally{h.getLevel().removePlayerImmediately(p,net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);}
    }
    @GameTest(template=T) public static void oblivionAcceptsRealPhialsAndDestroysTheirEssentia(GameTestHelper h){
        var jar=PortGameTests.place(h,"oblivion_jar",2,2,2);var p=h.makeMockPlayer(GameType.SURVIVAL);p.getInventory().clearContent();
        var phial=BoundJarGameTests.phial(Aspect.WATER);
        var heard=hear(h,()->BoundJarGameTests.click(h,p,jar,phial));
        h.assertTrue(phial.isEmpty(),"filled phial was not consumed");h.assertValueEqual(jar.containerContains(Aspect.WATER),8,"real block use did not pour phial");
        h.assertValueEqual(p.getInventory().countItem(thaumcraft.registry.ModItems.GLASS_PHIAL.get()),1,"empty phial not returned");
        h.assertValueEqual(count(heard,SoundEvents.GENERIC_SWIM),1L,"pour sound missing or duplicated");
        h.runAfterDelay(7,()->{h.assertTrue(jar.containerContains(Aspect.WATER)<8&&jar.containerContains(Aspect.WATER)>=6,"jar did not gradually destroy poured essentia");h.succeed();});
    }
    @GameTest(template=T) public static void oblivionRejectsMixedAndNearlyFullPhialsWithoutLoss(GameTestHelper h){
        var jar=PortGameTests.place(h,"oblivion_jar",2,2,2);var p=h.makeMockPlayer(GameType.SURVIVAL);jar.addToContainer(Aspect.AIR,60);
        var full=BoundJarGameTests.phial(Aspect.AIR);var wrong=BoundJarGameTests.phial(Aspect.FIRE);
        var heard=hear(h,()->{BoundJarGameTests.click(h,p,jar,full);BoundJarGameTests.click(h,p,jar,wrong);});
        h.assertValueEqual(full.getCount(),1,"partly consumed full phial");h.assertValueEqual(wrong.getCount(),1,"mixed aspect consumed");
        h.assertValueEqual(jar.containerContains(Aspect.AIR),60,"existing contents changed");h.assertValueEqual(heard.size(),0,"failed pour played success sound");h.succeed();
    }
    @GameTest(template=T) public static void oblivionCannotReturnEssentiaLikeOriginalTrashJar(GameTestHelper h){
        var jar=PortGameTests.place(h,"oblivion_jar",2,2,2);var p=h.makeMockPlayer(GameType.SURVIVAL);jar.addToContainer(Aspect.FIRE,16);
        var empty=new ItemStack(thaumcraft.registry.ModItems.GLASS_PHIAL.get());BoundJarGameTests.click(h,p,jar,empty);
        h.assertValueEqual(empty.getCount(),1,"destructive jar returned a phial");h.assertValueEqual(jar.containerContains(Aspect.FIRE),16,"extraction removed essentia");
        h.assertValueEqual(jar.takeEssentia(Aspect.FIRE,1,Direction.UP),0,"destructive jar allows tube output");h.succeed();
    }
    @GameTest(template=T) public static void oblivionOutlineMatchesOriginalWardedJar(GameTestHelper h){
        var pos=new BlockPos(2,2,2);h.setBlock(pos,thaumcraft.registry.ModBlocks.WARDED_JAR.get());var normal=h.getBlockState(pos).getShape(h.getLevel(),h.absolutePos(pos)).bounds();
        h.setBlock(pos,Content.block("oblivion_jar"));h.assertTrue(normal.equals(h.getBlockState(pos).getShape(h.getLevel(),h.absolutePos(pos)).bounds()),"oblivion jar is wider than original ModelJar");h.succeed();
    }
    @GameTest(template=T) public static void replicatorSoundsOnlyOnSuccessfulActions(GameTestHelper h){
        var m=PortGameTests.place(h,"replicator",2,2,2);var p=h.makeMockPlayer(GameType.SURVIVAL);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STONE));
        var select=hear(h,()->m.interact(p,InteractionHand.MAIN_HAND));h.assertValueEqual(count(select,SoundEvents.ITEM_PICKUP),1L,"template selection pop missing");
        var start=hear(h,()->{m.startReplication();m.startReplication();});
        h.assertTrue(m.replicationRunning,"replication failed to start");h.assertValueEqual(count(start,ModSounds.CRAFTSTART.get()),1L,"craft start duplicated or missing");
        m.local.setStackInSlot(1,new ItemStack(Items.STONE));p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        var extract=hear(h,()->m.interact(p,InteractionHand.MAIN_HAND));h.assertValueEqual(count(extract,SoundEvents.ITEM_PICKUP),1L,"output pop missing");h.succeed();
    }
    @GameTest(template=T) public static void urnsPlaySwimOnlyWhenFillingContainers(GameTestHelper h){
        var water=PortGameTests.place(h,"everfull_urn",1,2,2);var lava=PortGameTests.place(h,"everburn_urn",4,2,2);var p=h.makeMockPlayer(GameType.SURVIVAL);
        var heard=hear(h,()->{
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET));water.interact(p,InteractionHand.MAIN_HAND);
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.GLASS_BOTTLE));water.interact(p,InteractionHand.MAIN_HAND);
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET));lava.interact(p,InteractionHand.MAIN_HAND);
            lava.lavaAmount=1000;lava.interact(p,InteractionHand.MAIN_HAND);
        });
        h.assertValueEqual(count(heard,SoundEvents.GENERIC_SWIM),3L,"urn fill sound missing, duplicated or played on empty lava tank");
        for(var e:heard)h.assertTrue(e.getOriginalVolume()==0.5F&&e.getOriginalPitch()==1,"urn sound parameters differ");h.succeed();
    }
    @GameTest(template=T) public static void talismanEatsAudiblyOnceAndCharmZapsOnlyOnActivation(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);p.tickCount=20;p.getInventory().clearContent();p.getInventory().setItem(0,new ItemStack(Items.BREAD,3));
        var talisman=new ItemStack(Content.item("talisman_of_nourishment"));
        var eat=hear(h,()->{((Trinket)talisman.getItem()).active(talisman,p);((Trinket)talisman.getItem()).active(talisman,p);});
        h.assertValueEqual(count(eat,SoundEvents.GENERIC_EAT),1L,"meal sound missing or duplicate");
        p.getInventory().setItem(0,new ItemStack(Content.item("taint_charm")));
        var zap=hear(h,()->{TaintCharm.tick(p);TaintCharm.tick(p);});h.assertValueEqual(count(zap,ModSounds.ZAP.get()),1L,"charm activation must zap exactly once");h.succeed();
    }
    @GameTest(template=T) public static void minionKeepsInheritedHurtDeathAndAmbientSounds(GameTestHelper h){
        var m=Content.MINION.get().create(h.getLevel());m.moveTo(h.absolutePos(new BlockPos(2,2,2)).getCenter());h.getLevel().addFreshEntity(m);
        h.assertTrue(m.getAmbientSound()==ModSounds.ROOTS.get(),"legacy ambient roots lost");
        var heard=hear(h,()->{m.hurt(h.getLevel().damageSources().magic(),1);m.invulnerableTime=0;m.hurt(h.getLevel().damageSources().magic(),100);});
        h.assertValueEqual(count(heard,ModSounds.TENTACLE.get()),2L,"minion hurt/death audio missing");h.succeed();
    }
    @GameTest(template=T) public static void machinesUseLegacyMaterialSounds(GameTestHelper h){
        for(var name:List.of("think_tank","bound_jar","oblivion_jar"))h.assertTrue(Content.block(name).defaultBlockState().getSoundType().getBreakSound()==ModSounds.JAR.get(),name+" has stone breaking sound");
        h.assertTrue(Content.block("bound_chest").defaultBlockState().getSoundType()==net.minecraft.world.level.block.SoundType.WOOD,"chest lost wood sound");
        h.assertTrue(Content.block("crucible_of_souls").defaultBlockState().getSoundType()==net.minecraft.world.level.block.SoundType.METAL,"crucible lost metal sound");h.succeed();
    }
}
