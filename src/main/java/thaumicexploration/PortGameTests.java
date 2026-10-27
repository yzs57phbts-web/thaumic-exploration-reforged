package thaumicexploration;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import thaumcraft.aspect.*;
import thaumcraft.crafting.*;
import thaumcraft.research.*;
import thaumcraft.wand.*;

@GameTestHolder(ThaumicExploration.ID)
@PrefixGameTestTemplate(false)
public final class PortGameTests {
    static MachineEntity place(GameTestHelper h,String kind,int x,int y,int z){var pos=new BlockPos(x,y,z);h.setBlock(pos,Content.block(kind));return h.getBlockEntity(pos);}
    static void useSeal(GameTestHelper h,net.minecraft.world.entity.player.Player player,ItemStack seal,BlockPos local){
        BlockPos absolute=h.absolutePos(local);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,seal);player.setPos(absolute.getX(),absolute.getY()+1,absolute.getZ());
        ((SealItem)seal.getItem()).useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(absolute),Direction.UP,absolute,false)));
    }
    @GameTest(template="test/platform") public static void sealingPreservesExistingChestContents(GameTestHelper h){
        var p=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var seal=new ItemStack(Content.item("red_chest_seal"));BlockPos first=new BlockPos(1,2,1),second=new BlockPos(4,2,1);
        h.setBlock(first,Blocks.CHEST);h.setBlock(second,Blocks.CHEST);net.minecraft.world.level.block.entity.ChestBlockEntity chest=h.getBlockEntity(first);chest.setItem(0,new ItemStack(Items.DIAMOND,8));
        h.getLevel().setBlock(h.absolutePos(first),Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,Direction.WEST),3);
        chest=h.getBlockEntity(first);chest.setItem(0,new ItemStack(Items.DIAMOND,8));
        useSeal(h,p,seal,first);h.assertTrue(seal.isEmpty(),"initial seal not consumed");
        MachineEntity origin=h.getBlockEntity(first);h.assertTrue(origin.getBlockState().getValue(MachineBlock.FACING)==Direction.WEST,"binding rotated chest");
        var copy=new ItemStack(Content.item("red_chest_seal"));useSeal(h,p,copy,first);h.assertTrue(copy.isEmpty(),"copying link did not consume seal");
        var linked=p.getInventory().items.stream().filter(s->s.is(Content.item("red_chest_seal"))&&Data.get(s).getBoolean("LinkedSeal")).findFirst().orElseThrow();
        useSeal(h,p,linked,second);h.assertTrue(linked.isEmpty(),"linked seal not consumed");
        h.assertTrue(h.getLevel().getBlockEntity(h.absolutePos(first)) instanceof MachineEntity,"first seal did not convert chest");
        MachineEntity a=h.getBlockEntity(first),b=h.getBlockEntity(second);h.assertTrue(a.channel!=null && a.channel.equals(b.channel),"seal channels differ");h.assertValueEqual(b.inventory().getStackInSlot(0).getCount(),8,"existing chest items lost");h.succeed();
    }
    @GameTest(template="test/platform") public static void sealingJarsConservesEssentia(GameTestHelper h){
        var p=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var seal=new ItemStack(Content.item("blue_jar_seal"));var pos=new BlockPos(2,2,2);h.setBlock(pos,thaumcraft.registry.ModBlocks.WARDED_JAR.get());thaumcraft.essentia.JarBlockEntity jar=h.getBlockEntity(pos);jar.addToContainer(Aspect.WATER,32);useSeal(h,p,seal,pos);h.assertTrue(h.getLevel().getBlockEntity(h.absolutePos(pos)) instanceof thaumcraft.essentia.JarBlockEntity,"filled jar must not convert");h.assertValueEqual(jar.getAmount(),32,"rejected conversion lost essentia");jar.takeFromContainer(Aspect.WATER,32);useSeal(h,p,seal,pos);
        h.assertTrue(h.getLevel().getBlockEntity(h.absolutePos(pos)) instanceof MachineEntity,"jar not converted");MachineEntity m=h.getBlockEntity(pos);h.assertValueEqual(m.containerContains(Aspect.WATER),0,"empty jar gained essentia");h.succeed();
    }
    @GameTest(template="test/platform") public static void networksWorkAcrossDimensions(GameTestHelper h){var overworld=BoundNetworks.get(h.getLevel());var nether=BoundNetworks.get(h.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER));UUID id=UUID.randomUUID();overworld.network(id).items.setStackInSlot(0,new ItemStack(Items.GOLD_INGOT,3));h.assertValueEqual(nether.network(id).items.getStackInSlot(0).getCount(),3,"cross dimension storage");h.succeed();}
    @GameTest(template="test/platform") public static void talismanFeedsAndRingDiscountStacks(GameTestHelper h){
        var p=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);p.tickCount=20;p.getFoodData().setFoodLevel(10);p.getInventory().setItem(0,new ItemStack(Items.BREAD));var talisman=new ItemStack(Content.item("talisman_of_nourishment"));((Trinket)talisman.getItem()).active(talisman,p);
        h.assertValueEqual(p.getFoodData().getFoodLevel(),15,"talisman did not feed");h.assertTrue(p.getInventory().getItem(0).isEmpty(),"food not consumed");var ring=new ItemStack(Content.item("air_ring"));h.assertValueEqual(((Trinket)ring.getItem()).getVisDiscount(ring,p,Aspect.AIR),3,"ring discount");h.assertValueEqual(((Trinket)ring.getItem()).getVisDiscount(ring,p,Aspect.FIRE),0,"wrong aspect discount");h.succeed();
    }
    @GameTest(template="test/platform") public static void recipesAndResearchResolve(GameTestHelper h){
        var category=ResearchCategories.get("TX");h.assertTrue(category!=null,"research category missing");
        h.assertValueEqual(category.research().stream().filter(r->!(r instanceof ProxyResearchItem)).count(),29L,"active researches after candle transfer");
        h.assertValueEqual(Research.PROXIES.size(),11,"source research markers");
        for(var entry:category.research()){
            if(entry.iconItem()!=null)h.assertTrue(entry.iconItem().exists(),"missing icon "+entry.key());
            for(String p:entry.parents())h.assertTrue(ResearchCategories.getResearch(p)!=null,"missing parent "+p+" in "+entry.key());
            for(String p:entry.parentsHidden())h.assertTrue(ResearchCategories.getResearch(p)!=null,"missing hidden parent "+p+" in "+entry.key());
            for(var page:entry.pages()){
                List<String> recipes=page instanceof ResearchPage.Infusion v?v.recipes():page instanceof ResearchPage.Arcane v?v.recipes():page instanceof ResearchPage.Crucible v?v.recipes():page instanceof ResearchPage.Crafting v?v.recipes():page instanceof ResearchPage.InfusionEnchantment v?List.of(v.recipe()):List.of();
                for(String key:recipes)h.assertTrue(TcRecipes.get(key)!=null,"missing recipe page "+key);
            }
        }
        int count=0;
        for(var recipe:TcRecipes.all())if(recipe.key().startsWith("TX:")){
            count++;h.assertTrue(ResearchCategories.getResearch(recipe.research())!=null,"recipe research "+recipe.key());
            List<TcIngredient> ingredients=recipe instanceof InfusionRecipe r?java.util.stream.Stream.concat(java.util.stream.Stream.of(r.central()),r.components().stream()).toList():recipe instanceof ShapedArcaneRecipe r?r.grid():recipe instanceof ShapelessArcaneRecipe r?r.ingredients():recipe instanceof CrucibleRecipe r?List.of(r.catalyst()):recipe instanceof InfusionEnchantmentRecipe r?r.components():List.of();
            for(var i:ingredients)h.assertTrue(i.exists(),"missing ingredient in "+recipe.key()+": "+i);
            TcStack output=recipe instanceof InfusionRecipe r?r.result():recipe instanceof ShapedArcaneRecipe r?r.result():recipe instanceof ShapelessArcaneRecipe r?r.result():recipe instanceof CrucibleRecipe r?r.result():recipe instanceof CraftingRecipeRef r?r.result():null;
            if(output!=null)h.assertTrue(output.exists(),"missing output "+recipe.key());
        }
        h.assertTrue(count>=80&&count<=81,"recipe collection after moving 16 candle recipes to TC: "+count);h.succeed();
    }
    @GameTest(template="test/platform") public static void boundChestsShareWithoutDuplication(GameTestHelper h){
        var a=place(h,"bound_chest",1,2,1);var b=place(h,"bound_chest",3,2,1);UUID id=UUID.randomUUID();a.channel=id;b.channel=id;
        a.inventory().setStackInSlot(0,new ItemStack(Items.DIAMOND,12));
        h.assertValueEqual(b.inventory().getStackInSlot(0).getCount(),12,"shared inventory");
        b.inventory().extractItem(0,5,false);h.assertValueEqual(a.inventory().getStackInSlot(0).getCount(),7,"single storage");
        h.getLevel().destroyBlock(a.getBlockPos(),false);
        h.assertValueEqual(b.inventory().getStackInSlot(0).getCount(),7,"destroying a node must preserve shared contents");
        var isolated=place(h,"bound_chest",5,2,1);isolated.channel=UUID.randomUUID();h.assertTrue(isolated.inventory().getStackInSlot(0).isEmpty(),"channels leaked");h.succeed();
    }
    @GameTest(template="test/platform") public static void boundJarLimitsAndConsumption(GameTestHelper h){
        var a=place(h,"bound_jar",1,2,1);var b=place(h,"bound_jar",3,2,1);a.channel=b.channel=UUID.randomUUID();
        h.assertValueEqual(a.addToContainer(Aspect.FIRE,70),6,"64 unit limit");h.assertValueEqual(b.containerContains(Aspect.FIRE),64,"shared essentia");
        h.assertValueEqual(b.addToContainer(Aspect.WATER,1),1,"different aspect rejected");
        h.assertTrue(b.takeFromContainer(Aspect.FIRE,10),"take shared essentia");h.assertValueEqual(a.containerContains(Aspect.FIRE),54,"take visible to first jar");
        h.assertValueEqual(a.takeEssentia(Aspect.FIRE,100,Direction.UP),54,"bounded extraction");h.assertValueEqual(b.addToContainer(Aspect.WATER,3),0,"empty jar changes aspect");h.succeed();
    }
    @GameTest(template="test/platform") public static void networksSurviveSerialization(GameTestHelper h){
        var data=BoundNetworks.get(h.getLevel());UUID id=UUID.randomUUID();var n=data.network(id);n.items.setStackInSlot(2,new ItemStack(Items.EMERALD,7));n.add(Aspect.AIR,22);
        var serialized=data.save(new net.minecraft.nbt.CompoundTag(),h.getLevel().registryAccess());
        var restored=BoundNetworks.restore(serialized,h.getLevel().registryAccess()).network(id);
        h.assertValueEqual(restored.items.getStackInSlot(2).getCount(),7,"saved items");h.assertValueEqual(restored.amount,22,"saved essentia");h.assertTrue(restored.aspect==Aspect.AIR,"saved aspect");h.succeed();
    }
    @GameTest(template="test/platform") public static void oblivionJarBuffersAndDestroysGradually(GameTestHelper h){var jar=place(h,"oblivion_jar",1,2,1);h.assertValueEqual(jar.addToContainer(Aspect.AIR,128),64,"capacity 64");h.assertValueEqual(jar.containerContains(Aspect.AIR),64,"buffer missing");h.assertValueEqual(jar.addToContainer(Aspect.FIRE,1),1,"mixed aspect accepted");h.assertValueEqual(jar.takeEssentia(Aspect.AIR,10,Direction.UP),0,"output forbidden");h.runAfterDelay(7,()->{h.assertTrue(jar.containerContains(Aspect.AIR)<64&&jar.containerContains(Aspect.AIR)>=62,"one unit per six ticks");h.succeed();});}
    @GameTest(template="test/platform") public static void urnProvidesWaterAndFillsCauldron(GameTestHelper h){
        var urn=place(h,"everfull_urn",2,2,2);h.setBlock(new BlockPos(3,2,2),Blocks.CAULDRON);
        for(int i=0;i<100;i++)h.assertValueEqual(urn.water.drain(1000,IFluidHandler.FluidAction.EXECUTE).getAmount(),1000,"unlimited water");
        h.succeedWhen(()->h.assertTrue(h.getBlockState(new BlockPos(3,2,2)).is(Blocks.WATER_CAULDRON),"cauldron not filled"));
    }
    @GameTest(template="test/platform",timeoutTicks=240) public static void thinkTankRequiresSpaceAndMakesFragments(GameTestHelper h){
        for(BlockPos pos:BlockPos.betweenClosed(new BlockPos(2,1,2),new BlockPos(4,3,4)))h.setBlock(pos,Blocks.AIR);
        h.setBlock(new BlockPos(3,1,3),Blocks.BOOKSHELF);var tank=place(h,"think_tank",3,2,3);
        h.assertTrue(tank.thinkSpace(),"valid tank placement");h.setBlock(new BlockPos(2,2,3),Blocks.STONE);h.assertTrue(!tank.thinkSpace(),"obstructed tank must stop");h.setBlock(new BlockPos(2,2,3),Blocks.AIR);
        h.getLevel().random.setSeed(1);tank.local.setStackInSlot(0,new ItemStack(Items.ENCHANTED_BOOK,1));tank.progress=Config.THINK_TICKS.get()-1;
        h.runAfterDelay(2,()->{h.assertTrue(tank.local.getStackInSlot(0).isEmpty(),"book consumed");h.assertTrue(tank.progress==0,"cycle reset");h.succeed();});
    }
    @GameTest(template="test/platform",timeoutTicks=500) public static void replicatorConsumesEssentiaAndPreservesSample(GameTestHelper h){
        var m=place(h,"replicator",3,2,3);var source=place(h,"crucible_of_souls",5,2,3);
        m.local.setStackInSlot(0,new ItemStack(Items.STONE));var tags=AspectHelper.getBonusTags(new ItemStack(Items.STONE));h.assertTrue(!tags.isEmpty(),"stone aspects missing");source.setAspects(tags);m.template=new ItemStack(Items.STONE);m.startReplication();
        h.succeedWhen(()->{h.assertTrue(m.local.getStackInSlot(1).is(Items.STONE),"no replica produced");h.assertValueEqual(m.local.getStackInSlot(0).getCount(),1,"sample consumed");h.assertTrue(source.getAspects().visSize()<tags.visSize(),"no essentia spent");});
    }
    @GameTest(template="test/platform") public static void replicationRejectsValuablesAndCustomData(GameTestHelper h){
        h.assertTrue(!MachineEntity.canReplicate(new ItemStack(Items.DIAMOND_BLOCK)),"valuable block replicated");h.assertTrue(!MachineEntity.canReplicate(new ItemStack(Items.SHULKER_BOX)),"container replicated");var custom=new ItemStack(Items.OAK_LOG);Data.integer(custom,"secret",1);h.assertTrue(!MachineEntity.canReplicate(custom),"custom stack replicated");h.succeed();
    }
    @GameTest(template="test/platform") public static void weaknessPotionVariantsMatch(GameTestHelper h){
        var ingredient=Recipes.weakness();for(var item:List.of(Items.POTION,Items.SPLASH_POTION,Items.LINGERING_POTION))h.assertTrue(ingredient.test(PotionContents.createItemStack(item,Potions.WEAKNESS)),"weakness variant rejected");h.assertTrue(!ingredient.test(PotionContents.createItemStack(Items.POTION,Potions.WATER)),"water accepted as weakness");h.succeed();
    }
    @GameTest(template="test/platform") public static void wandPartsAndRecharge(GameTestHelper h){
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);player.tickCount=100;var amber=Wands.create(WandRegistry.cap("gold"),WandRegistry.rod("AMBER"),false);Magic.amber(amber,player);h.assertValueEqual(Wands.getVis(amber,Aspect.AIR),100,"amber regen");h.assertValueEqual(Wands.getMaxVis(amber),1000,"amber capacity");
        var trans=Wands.create(WandRegistry.cap("SOJOURNER"),WandRegistry.rod("TRANSMUTATION"),false);Wands.storeVis(trans,Aspect.FIRE,7500);Magic.transmute(trans,player);h.assertValueEqual(Wands.getVis(trans,Aspect.FIRE),6751,"excess removed");h.assertTrue(Wands.getVis(trans,Aspect.AIR)>0,"missing aspect not replenished");h.assertTrue(Wands.getAllVis(trans).visSize()<7500,"conversion created energy");h.assertValueEqual(Wands.getMaxVis(Wands.create(WandRegistry.cap("gold"),WandRegistry.rod("NECROMANCER_staff"),false)),20000,"necro capacity");h.succeed();
    }
    @GameTest(template="test/platform") public static void machineStatePersists(GameTestHelper h){
        var m=place(h,"replicator",3,2,3);m.local.setStackInSlot(0,new ItemStack(Items.OAK_LOG,2));m.progress=42;var tag=m.saveWithFullMetadata(h.getLevel().registryAccess());var other=place(h,"replicator",5,2,3);other.loadWithComponents(tag,h.getLevel().registryAccess());h.assertValueEqual(other.local.getStackInSlot(0).getCount(),2,"sample saved");h.assertValueEqual(other.progress,42,"progress saved");h.succeed();
    }
    @GameTest(template="test/platform") public static void enchantmentsAndTinkererLoad(GameTestHelper h){
        for(String name:List.of("binding","disarming","night_vision"))h.assertTrue(h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).containsKey(ThaumicExploration.id(name)),"enchantment missing: "+name);
        h.assertTrue(net.neoforged.fml.ModList.get().isLoaded("thaumictinkerer"),"Thaumic Tinkerer not loaded in integration run");
        h.assertTrue(thaumictinkerer.enchanting.EnchantmentManager.data().keySet().stream().anyMatch(k->k.location().equals(ThaumicExploration.id("binding"))),"osmotic enchanter integration missing");
        h.succeed();
    }
    @GameTest(template="test/platform") public static void sourceResearchMarkersFollowKnowledge(GameTestHelper h){
        var p=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var knowledge=Knowledge.get(p);knowledge.clearResearch();
        h.assertTrue(!Research.syncProxies(knowledge),"fresh player gained source markers");
        h.assertTrue(!ResearchManager.hasRequisites(p,"ENCHBINDING"),"enchantments visible before prerequisite");
        knowledge.addResearch("INFUSIONENCHANTMENT");h.assertTrue(Research.syncProxies(knowledge),"existing research not migrated");
        h.assertTrue(knowledge.isResearchComplete("TXINFUSIONENCHANTMENT"),"source marker incomplete");
        h.assertTrue(ResearchManager.hasRequisites(p,"ENCHBINDING"),"enchantments did not unlock");
        h.assertTrue(ResearchCategories.getResearch("ENCHBINDING").parents().contains("TXINFUSIONENCHANTMENT"),"thread to source marker missing");
        var alias=ResearchCategories.getResearch("TXINFUSIONENCHANTMENT");var source=ResearchCategories.getResearch("INFUSIONENCHANTMENT");
        h.assertTrue(alias.nameKey().equals(source.nameKey())&&alias.pages().equals(source.pages()),"source marker doesn't open the original knowledge");
        h.succeed();
    }
    @GameTest(template="test/platform") public static void elementalBootsStepAndMovement(GameTestHelper h){
        var p=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var step=p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT);double original=step.getValue();
        for(String name:List.of("meteor_boots","comet_boots")){
            p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,new ItemStack(Content.item(name)));p.setOnGround(true);p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);ElementalMovement.tick(p,true);
            h.assertTrue(step.getValue()>=1,"boots cannot step over a full block");h.assertTrue(p.getDeltaMovement().horizontalDistance()>0.05,"boots do not accelerate forward");
        }
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,ItemStack.EMPTY);ElementalMovement.tick(p,false);h.assertTrue(Math.abs(step.getValue()-original)<0.0001,"step height left behind after removal");h.succeed();
    }
    @GameTest(template="test/platform") public static void thinkTankMenuAndCreativeOrder(GameTestHelper h){
        var p=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var menu=new ThinkTankMenu(1,p.getInventory());
        h.assertValueEqual(menu.slots.size(),38,"tank has generic chest slots");h.assertTrue(!menu.getSlot(1).mayPlace(new ItemStack(Items.DIAMOND)),"output accepts items");
        var order=CreativeOrder.items();h.assertValueEqual(new HashSet<>(order).size(),Content.ALL.size()-17,"creative tab duplicated or omitted entries");h.assertTrue(order.get(0).equals("replicator")&&order.get(1).equals("auto_sorter")&&order.stream().noneMatch(n->n.endsWith("_floating_candle")),"creative groups out of order");h.succeed();
    }
    @GameTest(template="test/platform") public static void replicatorUsesWandAndWorldInteraction(GameTestHelper h){
        var m=place(h,"replicator",2,2,2);var p=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.STONE,2));m.interact(p,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(m.template.is(Items.STONE)&&m.local.getStackInSlot(0).isEmpty(),"ghost template missing");h.assertValueEqual(p.getMainHandItem().getCount(),2,"template consumed held item");h.assertTrue(!m.replicationRunning,"started without wand");
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,Wands.create(WandRegistry.cap("gold"),WandRegistry.rod("greatwood"),false));m.interact(p,net.minecraft.world.InteractionHand.MAIN_HAND);h.assertTrue(m.replicationRunning,"wand did not start work");
        m.local.setStackInSlot(1,new ItemStack(Items.STONE,3));p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);m.interact(p,net.minecraft.world.InteractionHand.MAIN_HAND);h.assertTrue(!m.replicationRunning&&m.local.getStackInSlot(1).isEmpty(),"empty hand did not collect output and stop work");h.assertTrue(m.template.is(Items.STONE),"collecting output lost template");h.succeed();
    }

    @GameTest(template="test/platform") public static void legacySealsKeepChannels(GameTestHelper h){
        var p=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var seal=new ItemStack(Content.item("blue_chest_seal"));var tag=new net.minecraft.nbt.CompoundTag();var id=UUID.randomUUID();tag.putUUID("Channel",id);Data.put(seal,tag);
        h.setBlock(new BlockPos(1,2,1),Blocks.CHEST);h.setBlock(new BlockPos(4,2,1),Blocks.CHEST);useSeal(h,p,seal,new BlockPos(1,2,1));useSeal(h,p,seal,new BlockPos(4,2,1));MachineEntity a=h.getBlockEntity(new BlockPos(1,2,1)),b=h.getBlockEntity(new BlockPos(4,2,1));h.assertTrue(id.equals(a.channel)&&id.equals(b.channel)&&!seal.isEmpty(),"legacy seal channel changed");h.succeed();
    }
    @GameTest(template="test/platform") public static void candleHasAnimatedEntityAndOriginalBob(GameTestHelper h){
        var pos=new BlockPos(2,2,2);h.setBlock(pos,thaumcraft.modern.ModernContent.CANDLES.get("orange").get());h.assertTrue(h.getBlockEntity(pos) instanceof thaumcraft.modern.FloatingCandleEntity,"candle lacks animated block entity");
        h.assertTrue(Math.abs(FloatingCandleBlock.bob(BlockPos.ZERO,0))<0.001&&Math.abs(FloatingCandleBlock.bob(BlockPos.ZERO,Math.PI*15)-0.2)<0.001,"original levitation amplitude or period changed");h.succeed();
    }
    @GameTest(template="test/platform") public static void brazierAndUrnPowerPersists(GameTestHelper h){
        var m=place(h,"soul_brazier",2,2,2);h.assertValueEqual(m.addEssentia(Aspect.FIRE,5,Direction.DOWN),0,"wrong essentia accepted");h.assertValueEqual(m.addEssentia(Aspect.DEATH,40,Direction.DOWN),16,"essentia capacity");h.assertValueEqual(m.addEssentia(Aspect.DEATH,1,Direction.UP),0,"input above accepted");
        m.brazierOwner=UUID.randomUUID();m.storedWarp=17;m.brazierVis=12;m.brazierActive=true;
        var tag=m.saveWithFullMetadata(h.getLevel().registryAccess());var other=place(h,"soul_brazier",4,2,2);other.loadWithComponents(tag,h.getLevel().registryAccess());h.assertTrue(other.brazierOwner.equals(m.brazierOwner)&&other.brazierActive&&other.storedWarp==17&&other.brazierVis==12&&other.brazierEssentia==16,"brazier lost saved state");
        other.storedWarp=0;other.brazierActive=false;m.storedWarp=0;m.brazierActive=false;
        var urn=place(h,"everburn_urn",2,2,4);urn.lavaAmount=1250;h.assertValueEqual(urn.lava.drain(1000,IFluidHandler.FluidAction.SIMULATE).getAmount(),1000,"simulated lava drain");h.assertValueEqual(urn.lavaAmount,1250,"simulation drained lava");urn.lava.drain(1000,IFluidHandler.FluidAction.EXECUTE);h.assertValueEqual(urn.lavaAmount,250,"lava drain creates energy");h.succeed();
    }
    @GameTest(template="test/platform") public static void offlineWarpReturnSurvivesSave(GameTestHelper h){
        var id=UUID.randomUUID();var d=new WarpReturns();d.add(id,7);d.add(id,4);var tag=d.save(new net.minecraft.nbt.CompoundTag(),h.getLevel().registryAccess());var restored=WarpReturns.load(tag,h.getLevel().registryAccess());var saved=restored.save(new net.minecraft.nbt.CompoundTag(),h.getLevel().registryAccess());h.assertValueEqual(saved.getList("Pending",10).getCompound(0).getInt("Warp"),11,"pending return lost");h.succeed();
    }
    @GameTest(template="test/platform") public static void brazierStoresAndReturnsPermanentWarpOnce(GameTestHelper h){
        var p=thaumcraft.test.TestSupport.mockServerPlayer(h);var other=thaumcraft.test.TestSupport.mockServerPlayer(h);var m=place(h,"soul_brazier",3,2,3);m.brazierOwner=p.getUUID();m.brazierVis=16;m.brazierEssentia=16;
        Warp.set(p,19,WarpType.PERMANENT);Warp.set(p,5,WarpType.STICKY);m.activateBrazier(other);h.assertTrue(!m.brazierActive,"non-owner activated brazier");m.activateBrazier(p);
        h.assertTrue(m.brazierActive&&m.storedWarp==19,"warp not stored");h.assertValueEqual(Warp.get(p,WarpType.PERMANENT),0,"permanent warp not suppressed");h.assertValueEqual(Warp.get(p,WarpType.STICKY),5,"sticky warp changed");
        m.returnWarp();WarpReturns.get(h.getLevel()).deliver(p);h.assertValueEqual(Warp.get(p,WarpType.PERMANENT),19,"permanent warp not returned");m.returnWarp();WarpReturns.get(h.getLevel()).deliver(p);h.assertValueEqual(Warp.get(p,WarpType.PERMANENT),19,"warp returned twice");h.succeed();
    }
    @GameTest(template="test/platform") public static void energizedNodeFeedsUrnAndBrazier(GameTestHelper h){
        var nodePos=new BlockPos(2,3,2);h.setBlock(nodePos,thaumcraft.registry.ModBlocks.ENERGIZED_AURA_NODE.get());thaumcraft.node.EnergizedNodeBlockEntity node=h.getBlockEntity(nodePos);node.setupNode();thaumcraft.vis.VisNet.addSource(h.getLevel(),node);
        var urn=place(h,"everburn_urn",4,2,2);var brazier=place(h,"soul_brazier",2,2,4);
        h.succeedWhen(()->{h.assertTrue(urn.lavaAmount>=250,"urn did not draw Ignis from energized node");h.assertTrue(brazier.brazierVis>0,"brazier did not draw Ignis from energized node");});
    }
    @GameTest(template="test/platform") public static void boundChestOpensAndClosesLid(GameTestHelper h){
        var m=place(h,"bound_chest",3,2,3);var p=thaumcraft.test.TestSupport.mockServerPlayer(h);p.setPos(m.getBlockPos().getX()+0.5,m.getBlockPos().getY(),m.getBlockPos().getZ()+0.5);m.interact(p,net.minecraft.world.InteractionHand.MAIN_HAND);h.assertValueEqual(m.viewers,1,"chest did not notify opening");p.closeContainer();h.assertValueEqual(m.viewers,0,"chest did not notify closing");h.succeed();
    }
    @GameTest(template="test/platform") public static void oldCandlesAcquireRenderEntityAndStabilize(GameTestHelper h){
        var local=new BlockPos(2,2,2);var block=Content.CANDLES.get("white_floating_candle").get();h.setBlock(local,block);var pos=h.absolutePos(local);var chunk=h.getLevel().getChunkAt(pos);chunk.removeBlockEntity(pos);h.assertTrue(!chunk.getBlockEntities().containsKey(pos),"legacy candle setup failed");CandleMigration.repair(chunk);h.assertTrue(chunk.getBlockEntities().get(pos) instanceof thaumcraft.modern.FloatingCandleEntity,"old candle invisible after upgrade");h.assertTrue(h.getLevel().getBlockState(pos).getBlock() instanceof thaumcraft.block.InfusionStabiliser stabiliser&&stabiliser.canStabiliseInfusion(h.getLevel(),pos),"candle does not stabilize infusion");h.succeed();
    }
    @GameTest(template="test/platform") public static void brokenBoundContainersRetainTheirNetwork(GameTestHelper h){
        for(String kind:List.of("bound_chest","bound_jar")){
            var m=place(h,kind,2,2,2);m.channel=UUID.randomUUID();m.color=DyeColor.CYAN.getId();var id=m.channel;
            if(kind.equals("bound_chest"))m.inventory().setStackInSlot(0,new ItemStack(Items.EMERALD,13));else m.addEssentia(Aspect.WATER,37,Direction.UP);
            var drops=Block.getDrops(m.getBlockState(),h.getLevel(),m.getBlockPos(),m);var item=drops.stream().filter(s->s.is(Content.item(kind))).findFirst().orElseThrow();h.assertTrue(Data.get(item).getUUID("Channel").equals(id),"network missing from dropped container");h.getLevel().destroyBlock(m.getBlockPos(),false);
            var placed=place(h,kind,4,2,2);((MachineBlock)placed.getBlockState().getBlock()).setPlacedBy(h.getLevel(),placed.getBlockPos(),placed.getBlockState(),h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL),item);
            h.assertTrue(id.equals(placed.channel)&&placed.color==DyeColor.CYAN.getId(),"placing lost network or seal color");
            h.assertValueEqual(kind.equals("bound_chest")?placed.inventory().getStackInSlot(0).getCount():placed.containerContains(Aspect.WATER),kind.equals("bound_chest")?13:37,"stored contents lost after replacing container");h.getLevel().destroyBlock(placed.getBlockPos(),false);
        }h.succeed();
    }
}
