package thaumicexploration;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.aspect.*;
import thaumcraft.registry.*;

@GameTestHolder(ThaumicExploration.ID) @PrefixGameTestTemplate(false)
public final class BoundJarGameTests {
    static final String T="test/platform";
    @GameTest(template=T) public static void bindingPreservesJarLightAndIlluminatesNeighbours(GameTestHelper h){
        var pos=new BlockPos(3,2,3);h.setBlock(pos,ModBlocks.WARDED_JAR.get());
        int normal=h.getBlockState(pos).getLightEmission();h.assertValueEqual(normal,9,"ordinary jar light changed");
        h.runAfterDelay(5,()->{
            int before=h.getLevel().getBrightness(net.minecraft.world.level.LightLayer.BLOCK,h.absolutePos(pos.east()));
            h.assertTrue(before>=normal-1,"ordinary jar does not illuminate neighbour");
            var p=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            PortGameTests.useSeal(h,p,new ItemStack(Content.item("blue_jar_seal")),pos);
            h.assertTrue(h.getBlockEntity(pos) instanceof MachineEntity,"seal did not bind jar");
            h.assertValueEqual(h.getBlockState(pos).getLightEmission(),normal,"binding removed light emission");
            MachineEntity bound=h.getBlockEntity(pos);var saved=bound.saveWithFullMetadata(h.getLevel().registryAccess());
            bound.loadWithComponents(saved,h.getLevel().registryAccess());bound.onLoad();
            h.runAfterDelay(5,()->{
                h.assertValueEqual(h.getLevel().getBrightness(net.minecraft.world.level.LightLayer.BLOCK,h.absolutePos(pos.east())),before,"binding darkened neighbour");
                h.succeed();
            });
        });
    }
    @GameTest(template=T,timeoutTicks=250) public static void sealedJarAcceptsEssentiaThroughRealTubes(GameTestHelper h){
        var p=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var target=seal(h,p,new BlockPos(4,2,2),new ItemStack(Content.item("blue_jar_seal")));
        h.setBlock(new BlockPos(2,2,2),ModBlocks.WARDED_JAR.get());thaumcraft.essentia.JarBlockEntity source=h.getBlockEntity(new BlockPos(2,2,2));source.addToContainer(Aspect.ORDER,8);
        for(int x=2;x<=4;x++)h.setBlock(new BlockPos(x,3,2),ModBlocks.ESSENTIA_TUBE.get());
        h.succeedWhen(()->{h.assertValueEqual(target.containerContains(Aspect.ORDER),8,"sealed jar not supplied through tubes");h.assertValueEqual(source.getAmount(),0,"source not drained");});
    }
    static MachineEntity seal(GameTestHelper h,Player p,BlockPos pos,ItemStack seal){h.setBlock(pos,ModBlocks.WARDED_JAR.get());PortGameTests.useSeal(h,p,seal,pos);return h.getBlockEntity(pos);}
    static ItemStack phial(Aspect a){var s=new ItemStack(ModItems.ESSENTIA_PHIAL.get());AspectKeys.ASPECTS.set(s,new AspectList().add(a,8));return s;}
    static void click(GameTestHelper h,Player p,MachineEntity jar,ItemStack held){
        p.setItemInHand(InteractionHand.MAIN_HAND,held);
        var hit=new BlockHitResult(Vec3.atCenterOf(jar.getBlockPos()),Direction.UP,jar.getBlockPos(),false);
        // The actual item-first then block-use route, rather than a direct addEssentia call.
        var result=held.getItem().onItemUseFirst(held,new UseOnContext(p,InteractionHand.MAIN_HAND,hit));
        if(!result.consumesAction())jar.getBlockState().useItemOn(held,h.getLevel(),p,InteractionHand.MAIN_HAND,hit);
    }
    @GameTest(template=T) public static void sealedJarAcceptsAndReturnsRealThaumcraftPhials(GameTestHelper h){
        var p=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var m=seal(h,p,new BlockPos(2,2,2),new ItemStack(Content.item("blue_jar_seal")));p.getInventory().clearContent();
        var filled=phial(Aspect.WATER);click(h,p,m,filled);h.assertValueEqual(m.containerContains(Aspect.WATER),8,"phial not poured into bound jar");h.assertTrue(filled.isEmpty(),"filled phial not consumed");h.assertValueEqual(p.getInventory().countItem(ModItems.GLASS_PHIAL.get()),1,"empty phial not returned");
        p.getInventory().clearContent();var empty=new ItemStack(ModItems.GLASS_PHIAL.get());click(h,p,m,empty);h.assertTrue(empty.isEmpty(),"empty phial not consumed");h.assertValueEqual(m.containerContains(Aspect.WATER),0,"essentia not removed");var result=p.getInventory().items.stream().filter(s->s.is(ModItems.ESSENTIA_PHIAL.get())).findFirst().orElseThrow();h.assertValueEqual(AspectKeys.ASPECTS.get(result).getAmount(Aspect.WATER),8,"wrong phial contents");h.succeed();
    }
    @GameTest(template=T) public static void phialsUseSameChannelThroughBothJarsAfterReload(GameTestHelper h){
        var p=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var a=seal(h,p,new BlockPos(1,2,2),new ItemStack(Content.item("red_jar_seal")));var linked=new ItemStack(Content.item("red_jar_seal"));var data=Data.get(linked);data.putUUID("Channel",a.channel);Data.put(linked,data);var b=seal(h,p,new BlockPos(5,2,2),linked);
        click(h,p,a,phial(Aspect.FIRE));var nbt=b.saveWithFullMetadata(h.getLevel().registryAccess());b.loadWithComponents(nbt,h.getLevel().registryAccess());h.assertValueEqual(b.containerContains(Aspect.FIRE),8,"shared/reloaded channel lost fill");click(h,p,b,new ItemStack(ModItems.GLASS_PHIAL.get()));h.assertValueEqual(a.containerContains(Aspect.FIRE),0,"extraction not shared");h.succeed();
    }
    @GameTest(template=T) public static void fullOrDifferentAspectJarDoesNotConsumePhial(GameTestHelper h){
        var p=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var m=seal(h,p,new BlockPos(2,2,2),new ItemStack(Content.item("green_jar_seal")));m.addToContainer(Aspect.AIR,60);var full=phial(Aspect.AIR);click(h,p,m,full);h.assertValueEqual(full.getCount(),1,"nearly full jar consumed phial");h.assertValueEqual(m.containerContains(Aspect.AIR),60,"partial phial transfer");var wrong=phial(Aspect.FIRE);click(h,p,m,wrong);h.assertValueEqual(wrong.getCount(),1,"incompatible phial consumed");h.assertValueEqual(m.containerContains(Aspect.AIR),60,"existing essentia changed");h.succeed();
    }
    @GameTest(template=T) public static void boundJarOutlineMatchesOrdinaryJar(GameTestHelper h){var pos=new BlockPos(3,2,3);h.setBlock(pos,ModBlocks.WARDED_JAR.get());var normal=h.getBlockState(pos).getShape(h.getLevel(),h.absolutePos(pos)).bounds();h.setBlock(pos,Content.block("bound_jar"));var bound=h.getBlockState(pos).getShape(h.getLevel(),h.absolutePos(pos)).bounds();h.assertTrue(normal.equals(bound),"binding changed jar outline dimensions");h.succeed();}
    @GameTest(template=T) public static void newlyPlacedBoundJarHasUsableChannel(GameTestHelper h){var pos=new BlockPos(3,2,3);h.setBlock(pos,Content.block("bound_jar"));var p=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var block=(MachineBlock)Content.block("bound_jar");block.setPlacedBy(h.getLevel(),h.absolutePos(pos),h.getBlockState(pos),p,new ItemStack(block));MachineEntity m=h.getBlockEntity(pos);click(h,p,m,phial(Aspect.EARTH));h.assertTrue(m.channel!=null,"creative block has no channel");h.assertValueEqual(m.containerContains(Aspect.EARTH),8,"new channel cannot be filled");h.succeed();}
}
