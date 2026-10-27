package thaumicexploration;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.aspect.*;

@GameTestHolder(ThaumicExploration.ID) @PrefixGameTestTemplate(false)
public final class ActivityGameTests {
    static final String T="test/platform";
    @GameTest(template=T) public static void sharedJarUpdatesEveryLoadedPeerImmediately(GameTestHelper h){
        var a=PortGameTests.place(h,"bound_jar",1,2,2);var b=PortGameTests.place(h,"bound_jar",5,2,2);a.channel=UUID.randomUUID();b.channel=a.channel;
        a.getAspects();b.getAspects();a.addToContainer(Aspect.WATER,8);
        h.assertValueEqual(b.jarAccessTicks,80,"peer waited for periodic refresh");
        h.assertValueEqual(MachineEntity.loadAspects(b.getUpdateTag(h.getLevel().registryAccess()).getCompound("Stored")).getAmount(Aspect.WATER),8,"peer packet has stale amount");
        b.takeFromContainer(Aspect.WATER,8);h.assertValueEqual(a.jarAccessTicks,80,"extraction did not notify origin");h.assertTrue(a.getAspects().isEmpty(),"network not empty");h.succeed();
    }
    @GameTest(template=T) public static void boundJarPullsAvailableTubeWithoutFiveTickDelay(GameTestHelper h){
        var m=PortGameTests.place(h,"bound_jar",3,2,3);m.channel=UUID.randomUUID();h.setBlock(new BlockPos(3,3,3),thaumcraft.registry.ModBlocks.ESSENTIA_TUBE.get());
        Runnable check=()->{thaumcraft.essentia.TubeBlockEntity tube=h.getBlockEntity(new BlockPos(3,3,3));tube.addEssentia(Aspect.AIR,1,Direction.UP);MachineEntity.tick(h.getLevel(),m.getBlockPos(),m.getBlockState(),m);h.assertValueEqual(m.containerContains(Aspect.AIR),1,"available essentia waited for polling interval");h.succeed();};
        if(h.getLevel().getGameTime()%5==0)h.runAfterDelay(1,check);else check.run();
    }
    @GameTest(template=T) public static void activeReplicatorPublishesAndRestoresRecipeVisualState(GameTestHelper h){
        var m=PortGameTests.place(h,"replicator",3,2,3);m.template=new ItemStack(Items.STONE);m.startReplication();
        h.assertTrue(m.replicationPhase()==ReplicatorAnimation.Phase.COLLECTING,"wrong initial phase");h.assertTrue(!m.replicationCost().isEmpty(),"cost not available to renderer");
        var packet=m.getUpdateTag(h.getLevel().registryAccess());h.assertTrue(packet.getBoolean("Running")&&!packet.getCompound("RecipeCost").isEmpty(),"incomplete client packet");packet.remove("RecipeCost");
        var restored=PortGameTests.place(h,"replicator",5,2,3);restored.loadWithComponents(packet,h.getLevel().registryAccess());h.assertTrue(restored.replicationPhase()==ReplicatorAnimation.Phase.COLLECTING&&!restored.replicationCost().isEmpty(),"old active save not migrated");h.succeed();
    }
    @GameTest(template=T) public static void paidEssentiaFillsRunesAndPublishesTrailSource(GameTestHelper h){
        var m=PortGameTests.place(h,"replicator",3,2,3);m.template=new ItemStack(Items.STONE);var source=PortGameTests.place(h,"crucible_of_souls",5,2,3);source.setAspects(AspectHelper.getBonusTags(m.template));m.startReplication();
        h.runAfterDelay(55,()->{h.assertTrue(m.lastEssentiaSource!=null&&m.lastEssentiaSource.equals(source.getBlockPos()),"trail source missing");h.assertTrue(m.lastEssentiaAspect!=null&&ReplicatorAnimation.runeFill(m,m.lastEssentiaAspect)>0,"paid aspect did not light runes");h.assertTrue(m.getUpdateTag(h.getLevel().registryAccess()).contains("LastEssentiaSource"),"trail not synchronized");h.succeed();});
    }
    @GameTest(template=T) public static void constructionGrowsThenClearsActivityOnCompletion(GameTestHelper h){
        var m=PortGameTests.place(h,"replicator",3,2,3);m.template=new ItemStack(Items.STONE);m.startReplication();h.assertTrue(ReplicatorAnimation.itemScale(m,0)==0,"collecting phase renders finished block");
        var tag=m.saveWithFullMetadata(h.getLevel().registryAccess());tag.put("Needed",new CompoundTag());tag.putInt("Progress",25);m.loadWithComponents(tag,h.getLevel().registryAccess());float first=ReplicatorAnimation.itemScale(m,0);
        tag.putInt("Progress",75);m.loadWithComponents(tag,h.getLevel().registryAccess());h.assertTrue(ReplicatorAnimation.itemScale(m,0)>first,"block does not grow during assembly");
        tag.putInt("Progress",Config.REPLICATION_TICKS.get()-1);m.loadWithComponents(tag,h.getLevel().registryAccess());MachineEntity.tick(h.getLevel(),m.getBlockPos(),m.getBlockState(),m);
        h.assertTrue(m.replicationPhase()==ReplicatorAnimation.Phase.IDLE&&!m.getUpdateTag(h.getLevel().registryAccess()).getBoolean("Running"),"completed machine retains active effects");h.assertValueEqual(m.local.getStackInSlot(1).getCount(),1,"completion output");h.succeed();
    }
    @GameTest(template=T) public static void runeFacesUseOriginalThreeAspectDistribution(GameTestHelper h){
        var costs=new AspectList().add(Aspect.AIR,2).add(Aspect.EARTH,8).add(Aspect.FIRE,3);
        h.assertTrue(ReplicatorAnimation.faceAspect(costs,0)==Aspect.EARTH&&ReplicatorAnimation.faceAspect(costs,2)==Aspect.EARTH,"largest aspect must occupy opposite sides");
        h.assertTrue(ReplicatorAnimation.faceAspect(costs,1)!=ReplicatorAnimation.faceAspect(costs,3),"secondary aspects lost");h.assertTrue(ReplicatorAnimation.faceAspect(new AspectList(),0)==null,"empty aspect list unsafe");h.succeed();
    }
}
