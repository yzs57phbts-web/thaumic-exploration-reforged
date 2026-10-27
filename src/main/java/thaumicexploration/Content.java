package thaumicexploration;

import java.util.*;
import java.util.function.Function;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import net.neoforged.neoforge.capabilities.*;
import thaumcraft.platform.*;

public final class Content {
    public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(ThaumicExploration.ID);
    public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(ThaumicExploration.ID);
    public static final DeferredBlock<MeltyIceBlock> MELTY_ICE=BLOCKS.register("melty_ice",()->new MeltyIceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.ICE).randomTicks().noLootTable()));
    public static final DeferredRegister<net.minecraft.world.effect.MobEffect> EFFECTS=DeferredRegister.create(Registries.MOB_EFFECT,ThaumicExploration.ID);
    public static final DeferredHolder<net.minecraft.world.effect.MobEffect,net.minecraft.world.effect.MobEffect> BINDING=EFFECTS.register("binding",()->new net.minecraft.world.effect.MobEffect(net.minecraft.world.effect.MobEffectCategory.HARMFUL,0x777777){});
    public static final DeferredHolder<net.minecraft.world.effect.MobEffect,net.minecraft.world.effect.MobEffect> WITHDRAWAL=EFFECTS.register("taint_withdrawal",()->new net.minecraft.world.effect.MobEffect(net.minecraft.world.effect.MobEffectCategory.HARMFUL,0x660077){
        @Override public boolean shouldApplyEffectTickThisTick(int ticks,int amp){return ticks%Math.max(1,40>>amp)==0;}
        @Override public boolean applyEffectTick(net.minecraft.world.entity.LivingEntity entity,int amp){if(TaintCharm.inTaint(entity)){entity.removeEffect(WITHDRAWAL);return false;}if(!entity.level().isClientSide())entity.hurt(entity.damageSources().magic(),1);return true;}
    });
    public static final DeferredRegister<BlockEntityType<?>> TILES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,ThaumicExploration.ID);
    public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,ThaumicExploration.ID);
    public static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,ThaumicExploration.ID);
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>,net.minecraft.world.inventory.MenuType<ThinkTankMenu>> THINK_MENU=MENUS.register("think_tank",()->new net.minecraft.world.inventory.MenuType<>(ThinkTankMenu::new,net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>,net.minecraft.world.inventory.MenuType<SorterMenu>> SORTER_MENU=MENUS.register("auto_sorter",()->new net.minecraft.world.inventory.MenuType<>(SorterMenu::new,net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,ThaumicExploration.ID);
    public static final DeferredHolder<net.minecraft.world.entity.EntityType<?>,net.minecraft.world.entity.EntityType<TentacleMinion>> MINION=ENTITIES.register("tentacle_minion",()->net.minecraft.world.entity.EntityType.Builder.of(TentacleMinion::new,net.minecraft.world.entity.MobCategory.MISC).sized(0.22F,1F).clientTrackingRange(8).build("thaumicexploration:tentacle_minion"));
    public static final Map<String, DeferredItem<? extends Item>> ALL=new LinkedHashMap<>();
    public static final Map<String, DeferredBlock<? extends Block>> MACHINES=new LinkedHashMap<>();
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<MachineEntity>> MACHINE = TILES.register("machine",()->BlockEntityType.Builder.of(MachineEntity::new,MACHINES.values().stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));
    public static final Map<String,DeferredBlock<? extends Block>> CANDLES=new LinkedHashMap<>();
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<CandleEntity>> CANDLE=TILES.register("floating_candle",()->BlockEntityType.Builder.of(CandleEntity::new,CANDLES.values().stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));
    public static final TcArmorMaterial BOOTS = new TcArmorMaterial(27, 0, 0, 0, 3, 25, SoundEvents.ARMOR_EQUIP_LEATHER, 0, 0, TagKey.create(Registries.ITEM,ThaumicExploration.id("boots_repair")),ThaumicExploration.id("elemental"));

    public static void init(IEventBus bus) {
        for(String name: List.of("think_tank","everfull_urn","crucible_of_souls","replicator","oblivion_jar","bound_chest","bound_jar","auto_sorter","soul_brazier","everburn_urn")) {
            var block=BLOCKS.register(name,()->new MachineBlock(BlockBehaviour.Properties.of().strength(2.5F).sound(LegacySounds.block(name)).noOcclusion().lightLevel(s->Set.of("bound_jar","oblivion_jar","think_tank").contains(name)?9:0),name));
            MACHINES.put(name,block); ALL.put(name,ITEMS.registerSimpleBlockItem(name,block));
        }
        for(DyeColor color:DyeColor.values()) {
            String name=color.getName()+"_floating_candle";
            var block=BLOCKS.register(name,()->new FloatingCandleBlock(BlockBehaviour.Properties.of().strength(0.1F).noCollission().noOcclusion().lightLevel(s->14).sound(SoundType.WOOL)));
            CANDLES.put(name,block);ALL.put(name,ITEMS.registerItem(name,p->new BlockItem(block.get(),p){
                @Override protected net.minecraft.world.level.block.state.BlockState getPlacementState(net.minecraft.world.item.context.BlockPlaceContext context){
                    return thaumcraft.modern.ModernContent.CANDLES.get(color.getName()).get().getStateForPlacement(context);
                }
            },new Item.Properties()));
            for(String kind: List.of("blank","chest","jar")) add(color.getName()+"_"+kind+"_seal",new Item.Properties().stacksTo(64),p->kind.equals("blank")?new Item(p):new SealItem(kind,color,p));
        }
        for(String name: List.of("purified_brain","amber_rod","amber_staff_core","transmutation_rod","transmutation_staff_core","necromancer_staff_core","sojourner_cap","inert_sojourner_cap","mechanist_cap","inert_mechanist_cap")) add(name,new Item.Properties().rarity(Rarity.UNCOMMON),Item::new);
        add("bread_rod",new Item.Properties().rarity(Rarity.UNCOMMON),Item::new);
        add("taint_charm",new Item.Properties().stacksTo(1).rarity(Rarity.RARE),p->new Item(p){@Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> text,TooltipFlag f){text.add(Component.translatable("tx.tooltip.taint_charm"));}});
        for(String name: List.of("dreamcatcher","tentacle_ring","stabilizer_belt","talisman_of_nourishment")) add(name,new Item.Properties().stacksTo(1).rarity(Rarity.RARE),p->new Trinket(name,p));
        for(String aspect:List.of("air","earth","fire","water","order","entropy")) add(aspect+"_ring",new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON),p->new Trinket(aspect+"_ring",p));
        for(String name:List.of("meteor_boots","comet_boots")) {
            Registration.armorMaterial(bootMaterial(name));
            add(name,Registration.armor(new Item.Properties().rarity(Rarity.RARE), bootMaterial(name),ArmorSlot.BOOTS).durability(350),p->new ElementalBoots(name,p));
        }
        TABS.register("main",()->CreativeModeTab.builder().title(Component.translatable("itemGroup.thaumicexploration")).icon(()->new ItemStack(item("replicator"))).displayItems((p,o)->CreativeOrder.items().forEach(n->o.accept(item(n)))).build());
        BLOCKS.register(bus); ITEMS.register(bus); TILES.register(bus); TABS.register(bus);ENTITIES.register(bus);MENUS.register(bus);EFFECTS.register(bus);
        bus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent e)->e.put(MINION.get(),TentacleMinion.attributes().build()));
    }
    public static void capabilities(RegisterCapabilitiesEvent e) {
        e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,MACHINE.get(),(be,side)->be.itemHandler(side));
        e.registerBlockEntity(Capabilities.FluidHandler.BLOCK,MACHINE.get(),(be,side)->be.kind().equals("everfull_urn")&&(side==null||side==net.minecraft.core.Direction.UP)?be.water:be.kind().equals("everburn_urn")&&(side==null||side==net.minecraft.core.Direction.UP)?be.lava:null);
    }
    private static void add(String n,Item.Properties p,Function<Item.Properties,? extends Item> f){ALL.put(n,ITEMS.registerItem(n,f,p));}
    public static Item item(String name){return ALL.get(name).get();}
    public static Block block(String name){return MACHINES.get(name).get();}
    public static Item tc(String name){return BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft",name));}
    public static TcArmorMaterial bootMaterial(String name){return new TcArmorMaterial(27,0,0,0,1,25,SoundEvents.ARMOR_EQUIP_LEATHER,0,0,TagKey.create(Registries.ITEM,ThaumicExploration.id("boots_repair")),ThaumicExploration.id(name));}
}

