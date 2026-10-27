package thaumicexploration;
import java.time.LocalDate;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(ThaumicExploration.ID) @PrefixGameTestTemplate(false)
public final class VisualEffectsGameTests {
    static final String T="test/platform";
    @GameTest(template=T) public static void coveringSoulCrucibleImmediatelyClearsVisualTarget(GameTestHelper h){
        var m=PortGameTests.place(h,"crucible_of_souls",3,2,3);m.soulTarget=UUID.randomUUID();m.drainTicks=20;h.setBlock(new BlockPos(3,3,3),Blocks.STONE);
        MachineEntity.tick(h.getLevel(),m.getBlockPos(),m.getBlockState(),m);var packet=m.getUpdateTag(h.getLevel().registryAccess());h.assertTrue(m.soulTarget==null&&packet.getInt("DrainTicks")==0&&!packet.hasUUID("SoulTarget"),"covered crucible retains target particles");h.succeed();
    }
    @GameTest(template=T) public static void burningPlayerEffectUsesPlayerEndpointAndClearsAfterExtinguishing(GameTestHelper h){
        var m=PortGameTests.place(h,"everfull_urn",3,2,3);var p=thaumcraft.test.TestSupport.mockServerPlayer(h);p.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(4,2,3))));p.setNoGravity(true);h.getLevel().addNewPlayer(p);p.setRemainingFireTicks(200);
        h.runAfterDelay(10,()->{try{h.assertTrue(m.waterOnPlayer&&m.waterDestination!=null&&m.getUpdateTag(h.getLevel().registryAccess()).getBoolean("WaterOnPlayer"),"player water stream missing");h.assertTrue(m.waterDestination.distanceTo(p.position())<0.1,"stream points away from burning player");p.clearFire();MachineEntity.tick(h.getLevel(),m.getBlockPos(),m.getBlockState(),m);h.assertTrue(!m.waterOnPlayer&&m.waterDestination==null,"extinguished player still has stream");h.succeed();}finally{h.getLevel().removePlayerImmediately(p,Entity.RemovalReason.DISCARDED);}});
    }
    @GameTest(template=T) public static void legacyGlassMachinesIlluminateNeighbour(GameTestHelper h){
        for(var entry:new String[]{"think_tank","oblivion_jar","bound_jar"})h.assertValueEqual(Content.block(entry).defaultBlockState().getLightEmission(),9,entry+" lost legacy light");
        var m=PortGameTests.place(h,"oblivion_jar",3,2,3);m.onLoad();h.runAfterDelay(5,()->{h.assertTrue(h.getLevel().getBrightness(net.minecraft.world.level.LightLayer.BLOCK,m.getBlockPos().east())>=8,"jar does not illuminate world");h.succeed();});
    }
    @GameTest(template=T) public static void chestMutationNotifiesPeersButSimulationDoesNot(GameTestHelper h){
        var a=PortGameTests.place(h,"bound_chest",1,2,2);var b=PortGameTests.place(h,"bound_chest",5,2,2);a.channel=UUID.randomUUID();b.channel=a.channel;a.inventory();b.inventory();
        a.inventory().insertItem(0,new ItemStack(Items.DIAMOND),true);h.assertValueEqual(b.chestAccessTicks,0,"simulated insertion started pulse");
        a.inventory().insertItem(0,new ItemStack(Items.DIAMOND),false);h.assertValueEqual(a.chestAccessTicks,80,"origin pulse missing");h.assertValueEqual(b.chestAccessTicks,80,"peer pulse missing");
        var packet=b.getUpdateTag(h.getLevel().registryAccess());h.assertValueEqual(packet.getInt("ChestAccessTicks"),80,"pulse not sent to client");
        b.inventory().extractItem(0,1,false);h.assertTrue(a.inventory().getStackInSlot(0).isEmpty(),"shared item extraction failed");h.succeed();
    }
    @GameTest(template=T) public static void pulseUsesOriginalFramesAndEnds(GameTestHelper h){
        h.assertValueEqual(LegacyVisuals.accessFrame(80),5,"initial pulse frame");h.assertValueEqual(LegacyVisuals.accessFrame(40),3,"middle pulse frame");h.assertValueEqual(LegacyVisuals.accessFrame(0),0,"pulse never ends");
        var m=PortGameTests.place(h,"bound_chest",3,2,3);m.chestAccessTicks=1;MachineEntity.tick(h.getLevel(),m.getBlockPos(),m.getBlockState(),m);h.assertValueEqual(m.chestAccessTicks,0,"access countdown stuck");h.succeed();
    }
    @GameTest(template=T) public static void tankPublishesWarmupAndSixBookOrbit(GameTestHelper h){
        var m=ParityGameTests.machine(h,"think_tank");h.setBlock(new BlockPos(3,1,3),Blocks.BOOKSHELF);h.setBlock(new BlockPos(1,2,3),Blocks.STONE);m.local.setStackInSlot(0,new ItemStack(Items.BOOK,2));
        h.runAfterDelay(20,()->{var tag=m.getUpdateTag(h.getLevel().registryAccess());h.assertTrue(tag.getBoolean("ThinkActive"),"working state not sent");h.assertValueEqual(LegacyVisuals.books(tag.getInt("Space")),6,"tight orbit must have six books");h.assertTrue(LegacyVisuals.bookAlpha(m.warmup)>0&&LegacyVisuals.bookAlpha(m.warmup)<1,"warmup not gradual");m.local.setStackInSlot(0,ItemStack.EMPTY);MachineEntity.tick(h.getLevel(),m.getBlockPos(),m.getBlockState(),m);h.assertTrue(!m.thinkActive&&m.warmup>0,"cooldown disappeared instead of fading");h.succeed();});
    }
    @GameTest(template=T) public static void waterEffectPublishesActualTargetAndStopsWhenCovered(GameTestHelper h){
        var m=PortGameTests.place(h,"everfull_urn",3,2,3);h.setBlock(new BlockPos(5,2,3),Blocks.CAULDRON);
        h.runAfterDelay(4,()->{h.assertTrue(m.waterDestination!=null,"working urn has no visual destination");h.assertTrue(m.getUpdateTag(h.getLevel().registryAccess()).contains("WaterX"),"water destination not synchronized");h.setBlock(new BlockPos(3,3,3),Blocks.STONE);MachineEntity.tick(h.getLevel(),m.getBlockPos(),m.getBlockState(),m);h.assertTrue(m.waterDestination==null&&!m.getUpdateTag(h.getLevel().registryAccess()).contains("WaterX"),"covered urn still emits directed stream");h.succeed();});
    }
    @GameTest(template=T) public static void soulCruciblePublishesLivingTarget(GameTestHelper h){
        var m=PortGameTests.place(h,"crucible_of_souls",3,2,3);var mob=h.spawn(EntityType.ZOMBIE,new BlockPos(4,2,3));mob.setNoAi(true);mob.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.CARVED_PUMPKIN));
        h.runAfterDelay(12,()->{var tag=m.getUpdateTag(h.getLevel().registryAccess());h.assertTrue(tag.hasUUID("SoulTarget")&&tag.getUUID("SoulTarget").equals(mob.getUUID())&&tag.getInt("DrainTicks")>0,"soul visual target missing");h.succeed();});
    }
    @GameTest(template=T) public static void fluxSurfaceHasOriginalEmptyAndFullHeights(GameTestHelper h){
        h.assertTrue(Math.abs(LegacyVisuals.fluidHeight(0)-0.2F)<0.00001,"empty fluid height");h.assertTrue(Math.abs(LegacyVisuals.fluidHeight(0.5F)-0.65F)<0.00001,"flux does not raise surface");h.assertTrue(LegacyVisuals.fluidHeight(1)<1&&LegacyVisuals.fluidHeight(2)<1.01,"full surface z fighting or overflow");h.succeed();
    }
    @GameTest(template=T) public static void visualDestinationsAndBrazierEventSurvivePacketRoundTrip(GameTestHelper h){
        var m=PortGameTests.place(h,"soul_brazier",3,2,3);m.waterDestination=new Vec3(1.25,7.5,3.75);m.taintDestination=m.getBlockPos().east(5);m.taintVisualTick=h.getLevel().getGameTime();m.visualTicks=61;
        var tag=m.getUpdateTag(h.getLevel().registryAccess());var copy=PortGameTests.place(h,"soul_brazier",5,2,3);copy.loadWithComponents(tag,h.getLevel().registryAccess());h.assertTrue(copy.waterDestination.equals(m.waterDestination)&&copy.taintDestination.equals(m.taintDestination)&&copy.taintVisualTick==m.taintVisualTick&&copy.visualTicks==61,"visual event round trip lost destination/time");h.succeed();
    }
    @GameTest(template=T) public static void bootTrailsRespectMotionWaterAndVerticalWeight(GameTestHelper h){
        h.assertTrue(LegacyVisuals.movingTrail(false,false,0.2,0,0),"walking meteor fire missing");h.assertTrue(!LegacyVisuals.movingTrail(false,true,1,1,1),"fire trail underwater");h.assertTrue(!LegacyVisuals.movingTrail(false,false,0,0.15,0)&&LegacyVisuals.movingTrail(true,false,0,0.15,0),"original meteor/comet vertical thresholds changed");h.assertTrue(!LegacyVisuals.movingTrail(true,false,0,0,0),"stationary comet emits movement trail");h.succeed();
    }
    @GameTest(template=T) public static void rodFramesAndSeasonalWindowMatchOriginal(GameTestHelper h){
        h.assertValueEqual(LegacyVisuals.rodFrame(0),0,"rod initial frame");h.assertValueEqual(LegacyVisuals.rodFrame(12),1,"rod first animated step");h.assertValueEqual(LegacyVisuals.rodFrame(116),28,"original final reachable frame");h.assertValueEqual(LegacyVisuals.rodFrame(118),0,"original 118 tick cycle");
        h.assertTrue(LegacyVisuals.christmas(LocalDate.of(2026,12,24))&&LegacyVisuals.christmas(LocalDate.of(2026,12,26))&&!LegacyVisuals.christmas(LocalDate.of(2026,12,23))&&!LegacyVisuals.christmas(LocalDate.of(2026,12,27)),"holiday boundaries changed");h.succeed();
    }
    @GameTest(template=T) public static void sorterLinksPublishPositionsFacesAndModes(GameTestHelper h){
        var m=PortGameTests.place(h,"auto_sorter",3,2,3);var target=h.absolutePos(new BlockPos(5,2,3));m.sorter.link(target,Direction.WEST).mode=2;
        var copy=PortGameTests.place(h,"auto_sorter",1,2,3);copy.loadWithComponents(m.getUpdateTag(h.getLevel().registryAccess()),h.getLevel().registryAccess());h.assertTrue(copy.sorter.links.size()==1&&copy.sorter.links.getFirst().pos.equals(target)&&copy.sorter.links.getFirst().side==Direction.WEST&&copy.sorter.links.getFirst().mode==2,"client marker link incomplete");h.succeed();
    }
}
