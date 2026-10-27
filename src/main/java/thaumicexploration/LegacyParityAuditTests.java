package thaumicexploration;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import thaumcraft.aspect.*;
import thaumcraft.wand.*;
/** Required parity expectations read from the official 1.1-53 JAR. */
@GameTestHolder(ThaumicExploration.ID)
@PrefixGameTestTemplate(false)
public final class LegacyParityAuditTests {
    static final String T="test/platform";
    static net.minecraft.world.entity.player.Player fullPlayer(GameTestHelper h){var p=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);p.tickCount=20;p.getFoodData().setFoodLevel(20);p.getFoodData().setSaturation(20);return p;}
    static MachineEntity place(GameTestHelper h,String name){if(name.equals("think_tank"))for(BlockPos pos:BlockPos.betweenClosed(new BlockPos(1,1,1),new BlockPos(5,3,5)))h.setBlock(pos,Blocks.AIR);return PortGameTests.place(h,name,3,2,3);}
    @GameTest(template=T) public static void legacyRingDiscountIsThree(GameTestHelper h){
        var p=fullPlayer(h);var s=new ItemStack(Content.item("air_ring"));h.assertValueEqual(((Trinket)s.getItem()).getVisDiscount(s,p,Aspect.AIR),3,"1.1-53 ring discount must be 3 percent");h.succeed();
    }
    @GameTest(template=T) public static void legacyTalismanReadsSlotNine(GameTestHelper h){
        var p=fullPlayer(h);p.getInventory().setItem(9,new ItemStack(Items.BREAD));var s=new ItemStack(Content.item("talisman_of_nourishment"));((Trinket)s.getItem()).active(s,p);h.assertTrue(p.getInventory().getItem(9).isEmpty(),"1.1-53 iterates slots 0 through 9");h.succeed();
    }
    @GameTest(template=T) public static void legacyTalismanSaturationUsesFoodModifier(GameTestHelper h){
        var p=fullPlayer(h);p.getInventory().setItem(0,new ItemStack(Items.BREAD));var s=new ItemStack(Content.item("talisman_of_nourishment"));((Trinket)s.getItem()).active(s,p);h.assertTrue(Math.abs(Data.get(s).getFloat("Saturation")-1.2F)<0.001,"1.1-53 bread stores saturation modifier x2 = 1.2, not actual food saturation 6");h.succeed();
    }
    @GameTest(template=T) public static void legacyTalismanAcceptsFoodAboveNinety(GameTestHelper h){
        var p=fullPlayer(h);p.getInventory().setItem(0,new ItemStack(Items.BREAD));var s=new ItemStack(Content.item("talisman_of_nourishment"));var t=Data.get(s);t.putInt("Food",90);Data.put(s,t);((Trinket)s.getItem()).active(s,p);h.assertValueEqual(Data.get(s).getInt("Food"),95,"original accepts when stored+nutrition <100");h.succeed();
    }
    @GameTest(template=T) public static void legacyOblivionSuctionIsEight(GameTestHelper h){var m=place(h,"oblivion_jar");h.assertValueEqual(m.getSuctionAmount(Direction.UP),8,"1.1-53 suction=8");h.succeed();}
    @GameTest(template=T) public static void legacyOblivionBuffersEssentia(GameTestHelper h){var m=place(h,"oblivion_jar");m.addEssentia(Aspect.FIRE,5,Direction.UP);h.assertValueEqual(m.containerContains(Aspect.FIRE),5,"1.1-53 buffers essentia and destroys one every six ticks");h.succeed();}
    @GameTest(template=T) public static void legacyBoundJarSuctionIsForty(GameTestHelper h){var m=place(h,"bound_jar");m.channel=java.util.UUID.randomUUID();h.assertValueEqual(m.getSuctionAmount(Direction.UP),40,"1.1-53 bound jar suction=40");h.succeed();}
    @GameTest(template=T) public static void legacyWaterUrnOnlyDrainsFromTop(GameTestHelper h){var m=place(h,"everfull_urn");var cap=h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,m.getBlockPos(),Direction.DOWN);h.assertTrue(cap==null||cap.drain(1000,net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE).isEmpty(),"water pipes must drain only from above");h.succeed();}
    @GameTest(template=T) public static void legacyWaterUrnCanSupplySixBuckets(GameTestHelper h){var m=place(h,"everfull_urn");h.assertValueEqual(m.water.drain(6000,net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE).getAmount(),6000,"original drain cap is 999999 mB per request, not 1000");h.succeed();}
    @GameTest(template=T) public static void legacyReplicatorAcceptsSand(GameTestHelper h){h.assertTrue(MachineEntity.canReplicate(new ItemStack(Items.SAND)),"sand is in the original default whitelist");h.succeed();}
    @GameTest(template=T) public static void legacyReplicatorTemplateDoesNotConsumeBlock(GameTestHelper h){var p=fullPlayer(h);var m=place(h,"replicator");p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.STONE,2));m.interact(p,net.minecraft.world.InteractionHand.MAIN_HAND);h.assertValueEqual(p.getMainHandItem().getCount(),2,"original stores a zero-count ghost template without shrinking held stack");h.succeed();}
    @GameTest(template=T) public static void legacyReplicatorRejectsHopperInsertion(GameTestHelper h){var m=place(h,"replicator");var cap=m.itemHandler(Direction.UP);h.assertTrue(!cap.insertItem(0,new ItemStack(Items.STONE),true).isEmpty(),"original ISidedInventory.canInsertItem returns false");h.succeed();}
    @GameTest(template=T) public static void legacyReplicatorRedstoneStartsCycle(GameTestHelper h){var m=place(h,"replicator");m.template=new ItemStack(Items.STONE);h.setBlock(new BlockPos(4,2,3),Blocks.REDSTONE_BLOCK);h.runAfterDelay(2,()->{h.assertTrue(m.replicationRunning,"original rising redstone edge starts one craft");h.succeed();});}
    @GameTest(template=T) public static void legacySoulCrucibleHasNoTopTube(GameTestHelper h){var m=place(h,"crucible_of_souls");h.assertTrue(!m.isConnectable(Direction.UP)&&!m.canOutputTo(Direction.UP),"original excludes UP from essentia connections");h.succeed();}
    @GameTest(template=T) public static void legacySoulCrucibleRangeIsFive(GameTestHelper h){h.assertValueEqual(Config.SOUL_RANGE.get(),5,"original horizontal and vertical targeting radius=5");h.succeed();}
    @GameTest(template=T) public static void legacyThinkTankAllowsReplaceablePlants(GameTestHelper h){var m=place(h,"think_tank");h.setBlock(new BlockPos(3,1,3),Blocks.BOOKSHELF);h.setBlock(new BlockPos(2,2,3),Blocks.SHORT_GRASS);h.assertTrue(m.thinkSpace(),"original permits replaceable materials in the clearance volume");h.succeed();}
    @GameTest(template=T) public static void legacyThinkTankCanProcessBookAtOutputSixtyThree(GameTestHelper h){var m=place(h,"think_tank");h.setBlock(new BlockPos(3,1,3),Blocks.BOOKSHELF);m.local.setStackInSlot(0,new ItemStack(Items.BOOK));m.local.setStackInSlot(1,new ItemStack(Content.tc("knowledge_fragment"),63));m.progress=Config.THINK_TICKS.get()-1;h.runAfterDelay(2,()->{h.assertTrue(m.local.getStackInSlot(0).isEmpty(),"normal book needs one output slot, so a stack of 63 must not block processing");h.succeed();});}
    @GameTest(template=T) public static void legacyDisarmingNeverDisarmsPlayers(GameTestHelper h){var victim=fullPlayer(h);var attacker=h.spawn(EntityType.ZOMBIE,new BlockPos(3,2,3));var sword=new ItemStack(Items.DIAMOND_SWORD);sword.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT,ThaumicExploration.id("disarming"))),3);attacker.setItemSlot(EquipmentSlot.MAINHAND,sword);victim.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_AXE));victim.getRandom().setSeed(0);for(int i=0;i<40;i++){victim.invulnerableTime=0;victim.setHealth(20);h.assertTrue(victim.hurt(h.getLevel().damageSources().mobAttack(attacker),1),"audit setup: damage was blocked");h.assertTrue(!victim.getMainHandItem().isEmpty(),"original explicitly excludes EntityPlayer from disarming");}h.succeed();}
    @GameTest(template=T) public static void legacyNightVisionDurationAndAmplifier(GameTestHelper h){var p=fullPlayer(h);var helmet=new ItemStack(Items.DIAMOND_HELMET);helmet.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT,ThaumicExploration.id("night_vision"))),1);p.setItemSlot(EquipmentSlot.HEAD,helmet);net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));var effect=p.getEffect(MobEffects.NIGHT_VISION);h.assertTrue(effect!=null&&effect.getDuration()==202&&effect.getAmplifier()==1,"original applies 202 ticks, amplifier 1");h.succeed();}
    @GameTest(template=T) public static void legacyBindingDurationAndAmplifier(GameTestHelper h){var attacker=thaumcraft.test.TestSupport.mockServerPlayer(h);var sword=new ItemStack(Items.DIAMOND_SWORD);sword.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT,ThaumicExploration.id("binding"))),1);attacker.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,sword);var victim=h.spawn(EntityType.ZOMBIE,new BlockPos(3,2,3));victim.hurt(h.getLevel().damageSources().playerAttack(attacker),1);var effect=victim.getEffect(MobEffects.MOVEMENT_SLOWDOWN);h.assertTrue(effect!=null&&effect.getDuration()==50&&effect.getAmplifier()==1,"original applies 50 ticks, amplifier 1");h.succeed();}
    @GameTest(template=T) public static void matchingAmberRegeneration(GameTestHelper h){var p=fullPlayer(h);p.tickCount=100;var wand=Wands.create(WandRegistry.cap("gold"),WandRegistry.rod("AMBER"),false);Magic.amber(wand,p);for(var a:Aspect.getPrimalAspects())h.assertValueEqual(Wands.getVis(wand,a),100,"one vis per aspect every 100 ticks");h.succeed();}
    @GameTest(template=T) public static void matchingCandleBobbingAndStabilization(GameTestHelper h){var b=Content.CANDLES.get("orange_floating_candle").get();h.assertTrue(b instanceof thaumcraft.block.InfusionStabiliser&&Math.abs(FloatingCandleBlock.bob(BlockPos.ZERO,Math.PI*15)-0.2)<0.001,"original amplitude and stabilizer");h.succeed();}
    @GameTest(template=T) public static void matchingBrazierEssentiaCapacity(GameTestHelper h){var m=place(h,"soul_brazier");h.assertValueEqual(m.addEssentia(Aspect.DEATH,99,Direction.DOWN),16,"original buffer16");h.succeed();}
}

