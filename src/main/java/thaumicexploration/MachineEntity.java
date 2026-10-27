package thaumicexploration;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.items.*;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import thaumcraft.aspect.*;
import thaumcraft.essentia.*;

public final class MachineEntity extends thaumcraft.vis.VisRelayBlockEntity implements AspectSource, EssentiaTransport {
    public UUID channel;
    public ItemStack template=ItemStack.EMPTY;
    private boolean redstone;
    private int essentiaTicks,urnTicks,soulTicks;
    public int drainTicks,space,warmup,rotationTicks;
    public float flux;
    public UUID soulTarget;
    public net.minecraft.world.phys.Vec3 waterDestination;
    public boolean waterOnPlayer;
    public BlockPos taintDestination;
    public long taintVisualTick=Long.MIN_VALUE,clientTaintTick=Long.MIN_VALUE;
    public int chestAccessTicks,visualTicks,oldRotationTicks,oldWarmup;
    public boolean thinkActive;
    private Aspect transportAspect;
    private long transportAspectTick=Long.MIN_VALUE;
    private BlockPos urnTarget;
    private UUID fireTarget;
    public final SorterLinks sorter=new SorterLinks(this);
    @Override public int getRange(){return kind().equals("everburn_urn")?8:0;}
    public int color,progress,jarAccessTicks;
    public BlockPos lastEssentiaSource;
    public Aspect lastEssentiaAspect;
    public long lastEssentiaTick=Long.MIN_VALUE,clientEffectTick=Long.MIN_VALUE;
    public boolean replicationRunning;
    public UUID brazierOwner;
    public int storedWarp,brazierVis,brazierEssentia,lavaAmount,viewers;
    public boolean brazierActive;
    public float lid,oldLid;
    public final IFluidHandler lava=new IFluidHandler(){
        public int getTanks(){return 1;}
        public FluidStack getFluidInTank(int t){return lavaAmount==0?FluidStack.EMPTY:new FluidStack(Fluids.LAVA,lavaAmount);}
        public int getTankCapacity(int t){return 4000;}
        public boolean isFluidValid(int t,FluidStack f){return f.is(Fluids.LAVA);}
        public int fill(FluidStack f,FluidAction a){return 0;}
        public FluidStack drain(FluidStack f,FluidAction a){return f.is(Fluids.LAVA)?drain(f.getAmount(),a):FluidStack.EMPTY;}
        public FluidStack drain(int n,FluidAction a){int taken=Math.min(lavaAmount,Math.max(0,n));if(taken==0)return FluidStack.EMPTY;if(a.execute()){lavaAmount-=taken;setChanged();}return new FluidStack(Fluids.LAVA,taken);}
    };
    private AspectList stored=new AspectList(), needed=new AspectList(),recipeCost=new AspectList();
    public final ItemStackHandler local=new ItemStackHandler(27){
        @Override protected void onContentsChanged(int slot){setChanged();if(slot==0){needed=new AspectList();progress=0;}}
        @Override public boolean isItemValid(int slot,ItemStack stack){return accepts(slot,stack);}
    };
    public final IFluidHandler water=new IFluidHandler(){
        @Override public int getTanks(){return 1;}
        @Override public FluidStack getFluidInTank(int tank){return new FluidStack(Fluids.WATER,999999);}
        @Override public int getTankCapacity(int tank){return 999999;}
        @Override public boolean isFluidValid(int tank,FluidStack fluid){return fluid.is(Fluids.WATER);}
        @Override public int fill(FluidStack fluid,FluidAction a){return 0;}
        @Override public FluidStack drain(FluidStack fluid,FluidAction a){return fluid.is(Fluids.WATER)?drain(fluid.getAmount(),a):FluidStack.EMPTY;}
        @Override public FluidStack drain(int amount,FluidAction a){return amount>0?new FluidStack(Fluids.WATER,Math.min(999999,amount)):FluidStack.EMPTY;}
    };
    public MachineEntity(BlockPos pos,BlockState state){super(Content.MACHINE.get(),pos,state);}
    public String kind(){return ((MachineBlock)getBlockState().getBlock()).kind;}
    private BoundNetworks.Network shared(){if(channel==null||!(level instanceof ServerLevel server))return null;var network=BoundNetworks.get(server).network(channel);if(kind().equals("bound_jar")||kind().equals("bound_chest"))network.watch(this);return network;}
    @Override public void onLoad(){super.onLoad();if(level instanceof ServerLevel server){if(kind().equals("bound_jar")||kind().equals("bound_chest"))shared();if(Set.of("bound_jar","oblivion_jar","think_tank").contains(kind()))server.getChunkSource().getLightEngine().checkBlock(worldPosition);}}
    public AspectList replicationCost(){return recipeCost.copy();}
    public AspectList replicationRemaining(){return needed.copy();}
    public ReplicatorAnimation.Phase replicationPhase(){return !replicationRunning?ReplicatorAnimation.Phase.IDLE:needed.isEmpty()?ReplicatorAnimation.Phase.ASSEMBLING:ReplicatorAnimation.Phase.COLLECTING;}
    public void syncVisualState(){setChanged();if(level!=null&&!level.isClientSide())level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    public IItemHandlerModifiable inventory(){var n=shared();return kind().equals("bound_chest") && n!=null?n.items:local;}
    public IItemHandler itemHandler(Direction side){
        if(kind().equals("bound_chest"))return inventory();
        if(!Set.of("think_tank","replicator").contains(kind()))return null;
        boolean tank=kind().equals("think_tank");
        return new IItemHandler(){
            public int getSlots(){return tank?2:1;}
            public ItemStack getStackInSlot(int slot){return local.getStackInSlot(tank?slot:1);}
            public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return tank&&slot==0&&(side==null||side==Direction.UP)?local.insertItem(slot,stack,simulate):stack;}
            public ItemStack extractItem(int slot,int n,boolean simulate){return (!tank||slot==1)&&(side==null||side!=Direction.UP||!tank)?local.extractItem(tank?slot:1,n,simulate):ItemStack.EMPTY;}
            public int getSlotLimit(int slot){return 64;}
            public boolean isItemValid(int slot,ItemStack stack){return tank&&slot==0&&(side==null||side==Direction.UP)&&accepts(slot,stack);}
        };
    }
    public boolean accepts(int slot,ItemStack stack){
        return switch(kind()){
            case "think_tank"->slot==0 && (stack.is(Items.BOOK)||stack.is(Items.ENCHANTED_BOOK));
            case "replicator"->slot==0 && canReplicate(stack);
            case "auto_sorter","bound_chest"->true;
            default->false;
        };
    }
    public static boolean canReplicate(ItemStack stack){
        if(!(stack.getItem() instanceof BlockItem) || !stack.getComponentsPatch().isEmpty())return false;
        var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        String name=id.getPath();boolean vanilla=id.getNamespace().equals("minecraft"),tc=id.getNamespace().equals("thaumcraft");
        if(tc&&(name.endsWith("_log")||name.endsWith("_leaves")))return false;
        if(stack.is(net.minecraft.tags.ItemTags.LOGS)||stack.is(net.minecraft.tags.ItemTags.LEAVES))return !AspectHelper.getBonusTags(stack).isEmpty();
        boolean wood=stack.is(net.minecraft.tags.ItemTags.PLANKS)||stack.is(net.minecraft.tags.ItemTags.WOODEN_SLABS)||stack.is(net.minecraft.tags.ItemTags.WOODEN_STAIRS);
        if(wood)return (vanilla||!Config.SPEC.isLoaded()||Config.MOD_WOOD.get())&&(!tc||!Config.SPEC.isLoaded()||Config.MAGIC_PLANKS.get())&&!AspectHelper.getBonusTags(stack).isEmpty();
        boolean stone=stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,net.minecraft.resources.ResourceLocation.parse("c:stones")))||stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,net.minecraft.resources.ResourceLocation.parse("c:cobblestones")));
        if(stone&&(vanilla||!Config.SPEC.isLoaded()||Config.MOD_STONE.get()))return !AspectHelper.getBonusTags(stack).isEmpty();
        return vanilla&&Set.of("stone","cobblestone","mossy_cobblestone","stone_slab","smooth_stone_slab","cobblestone_slab","cobblestone_stairs","sand","red_sand","sandstone","chiseled_sandstone","cut_sandstone","sandstone_stairs","sandstone_slab","bricks","brick_stairs","brick_slab","stone_bricks","mossy_stone_bricks","cracked_stone_bricks","chiseled_stone_bricks","stone_brick_stairs","stone_brick_slab","nether_bricks","nether_brick_stairs","nether_brick_slab","soul_sand","gravel","glass","grass_block","dirt","coarse_dirt","podzol","snow_block","clay","terracotta").contains(name);

    }
    public void interact(Player p,InteractionHand hand){
        String kind=kind();var held=p.getItemInHand(hand);
        if(kind.equals("soul_brazier")){activateBrazier(p);return;}
        if(kind.equals("everburn_urn")){
            if(held.is(Items.BUCKET)&&lavaAmount>=1000){lava.drain(1000,IFluidHandler.FluidAction.EXECUTE);if(!p.hasInfiniteMaterials())held.shrink(1);give(p,new ItemStack(Items.LAVA_BUCKET));LegacySounds.swim(p,0.5F);}return;
        }
        if(kind.equals("everfull_urn")){
            if(held.is(Items.BUCKET)){held.shrink(1);give(p,new ItemStack(Items.WATER_BUCKET));LegacySounds.swim(p,0.5F);}
            else if(held.is(Items.GLASS_BOTTLE)){held.shrink(1);give(p,net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION,net.minecraft.world.item.alchemy.Potions.WATER));LegacySounds.swim(p,0.5F);}
            return;
        }
        if(kind.equals("oblivion_jar")||kind.equals("bound_jar")||kind.equals("crucible_of_souls")){
            if(BoundJarPhials.interact(this,p,hand))return;
            p.displayClientMessage(Component.translatable("tx.essentia.stored",getAspects().toString()),true);return;
        }
        if(kind.equals("think_tank")){
            var view=new HandlerContainer(local,this::accepts,()->!isRemoved()&&p.distanceToSqr(Vec3.atCenterOf(worldPosition))<=64,this::setChanged);
            var data=new net.minecraft.world.inventory.ContainerData(){public int get(int i){return i==0?progress:Config.THINK_TICKS.get();}public void set(int i,int n){}public int getCount(){return 2;}};
            p.openMenu(new SimpleMenuProvider((id,inv,player)->new ThinkTankMenu(id,inv,view,data),Component.translatable("block.thaumicexploration.think_tank")));return;
        }
        if(kind.equals("replicator")){
            if(thaumcraft.wand.Wands.isWand(held)){startReplication();return;}
            replicationRunning=false;needed=new AspectList();recipeCost=new AspectList();progress=0;
            if(!local.getStackInSlot(1).isEmpty()){give(p,local.extractItem(1,64,false));LegacySounds.pop(this,1.5F);setChanged();return;}
            // A pre-port.4 physical sample is refunded exactly once; the template is never an item slot.
            if(!local.getStackInSlot(0).isEmpty())give(p,local.extractItem(0,64,false));
            boolean hadTemplate=!template.isEmpty();template=canReplicate(held)?held.copyWithCount(1):ItemStack.EMPTY;if(hadTemplate||!template.isEmpty())LegacySounds.pop(this,template.isEmpty()?1.5F:1.6F);syncVisualState();
            p.displayClientMessage(Component.translatable("tx.replicator.use_wand"),true);setChanged();return;
        }
        if(kind.equals("auto_sorter")){sorter.interact(p,held);return;}
        if(kind.equals("bound_chest")&&!level.getBlockState(worldPosition.above()).isRedstoneConductor(level,worldPosition.above())){
            var view=new HandlerContainer(inventory(),this::accepts,()->!isRemoved()&&p.distanceToSqr(Vec3.atCenterOf(worldPosition))<=64,()->{setChanged();if(channel!=null&&level instanceof ServerLevel s)BoundNetworks.get(s).setDirty();});
            view.viewers=open->{if(!p.isSpectator()){viewers=Math.max(0,viewers+(open?1:-1));level.blockEvent(worldPosition,getBlockState().getBlock(),1,viewers);if(viewers==(open?1:0))level.playSound(null,worldPosition,open?net.minecraft.sounds.SoundEvents.CHEST_OPEN:net.minecraft.sounds.SoundEvents.CHEST_CLOSE,net.minecraft.sounds.SoundSource.BLOCKS,0.5F,0.9F+level.random.nextFloat()*0.1F);}};
            p.openMenu(new SimpleMenuProvider((id,inv,player)->ChestMenu.threeRows(id,inv,view),Component.translatable("block.thaumicexploration.bound_chest")));
        }
    }
    private void give(Player p,ItemStack s){if(!p.addItem(s))p.drop(s,false);}
    @Override public boolean triggerEvent(int id,int n){if(id==1){viewers=n;return true;}return super.triggerEvent(id,n);}
    public static void tick(Level level,BlockPos pos,BlockState state,MachineEntity m){if(m.kind().equals("everburn_urn"))thaumcraft.vis.VisRelayBlockEntity.tick(level,pos,state,m);if(level.isClientSide()){m.visualTicks++;m.oldWarmup=m.warmup;m.oldRotationTicks=m.rotationTicks;if(m.kind().equals("think_tank")){m.warmup=Math.clamp(m.warmup+(m.thinkActive?1:-1),0,40);if(m.warmup>0)m.rotationTicks++;}if(m.chestAccessTicks>0)m.chestAccessTicks--;LegacyVisuals.clientTick.accept(m);m.oldLid=m.lid;m.lid=net.minecraft.util.Mth.clamp(m.lid+(m.viewers>0?0.1F:-0.1F),0,1);if(m.jarAccessTicks>0)m.jarAccessTicks--;if(m.replicationPhase()==ReplicatorAnimation.Phase.ASSEMBLING)m.progress=Math.min(Config.REPLICATION_TICKS.get()-1,m.progress+1);ReplicatorAnimation.clientTick(m);}else m.serverTick();}
    private void serverTick(){
        if(level==null)return;
        visualTicks++;if(chestAccessTicks>0)chestAccessTicks--;
        if(kind().equals("bound_chest"))shared();
        if(level.getGameTime()%20==0&&!kind().equals("bound_jar"))level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);
        if(!Config.enabled(kind()))return;
        if(kind().equals("soul_brazier")){tickBrazier();return;}
        if(kind().equals("everburn_urn")){if(level.getGameTime()%10==0&&lavaAmount<=3750){lavaAmount+=250*thaumcraft.vis.VisNet.drainVis(level,worldPosition,Aspect.FIRE,1);setChanged();}return;}
        long time=level.getGameTime();
        switch(kind()){
            case "think_tank"->think();
            case "replicator"->{if(template.isEmpty()&&!local.getStackInSlot(0).isEmpty())template=local.getStackInSlot(0).copyWithCount(1);boolean powered=level.hasNeighborSignal(worldPosition);if(powered&&!redstone)startReplication();redstone=powered;if(Config.REPLICATION.get()&&replicationRunning)replicate();}
            case "everfull_urn"->tickWater();
            case "oblivion_jar","bound_jar"->{if(kind().equals("bound_jar")){shared();if(jarAccessTicks>0)jarAccessTicks--;pull();}else if(time%5==0)pull();if(kind().equals("oblivion_jar")&&time%6==0&&!stored.isEmpty()){var a=stored.getAspects().getFirst();stored.remove(a,1);setChanged();}}
            case "crucible_of_souls"->{if(Config.SOULS.get())souls();}
            case "auto_sorter"->{if(time%10==0)sorter.tick();}
        }
    }
    public boolean thinkSpace(){
        if(level==null || !level.getBlockState(worldPosition.below()).is(Blocks.BOOKSHELF))return false;
        for(BlockPos p:BlockPos.betweenClosed(worldPosition.offset(-1,-1,-1),worldPosition.offset(1,1,1)))if(!p.equals(worldPosition) && !p.equals(worldPosition.below()) && !clear(p))return false;
        space=1;for(BlockPos p:BlockPos.betweenClosed(worldPosition.offset(-2,-1,-2),worldPosition.offset(2,1,2)))if((Math.abs(p.getX()-worldPosition.getX())>1||Math.abs(p.getZ()-worldPosition.getZ())>1)&&!clear(p))space=0;
        return true;
    }
    private boolean clear(BlockPos p){var state=level.getBlockState(p);return state.isAir()||state.canBeReplaced()||net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().contains("airy");}
    private void think(){
        boolean wasActive=thinkActive;int oldSpace=space;
        var input=local.getStackInSlot(0);var out=local.getStackInSlot(1);
        if(input.isEmpty() || !thinkSpace() || (!out.isEmpty() && !out.is(Content.tc("knowledge_fragment"))) || out.getCount()>(input.is(Items.ENCHANTED_BOOK)?62:63)){thinkActive=false;progress=0;warmup=Math.max(0,warmup-1);if(warmup>0)rotationTicks++;if(wasActive)syncVisualState();return;}
        thinkActive=true;warmup=Math.min(40,warmup+1);rotationTicks++;if(!wasActive||oldSpace!=space)syncVisualState();
        if(++progress<Config.THINK_TICKS.get()){setChanged();return;}
        boolean enchanted=input.is(Items.ENCHANTED_BOOK);
        float roll=level.random.nextFloat();int fragments=enchanted?(roll>0.66F?1:roll>0.33F?2:0):(roll<=0.1F?1:0);
        input.shrink(1);local.setStackInSlot(0,input);if(fragments>0){if(out.isEmpty())local.setStackInSlot(1,new ItemStack(Content.tc("knowledge_fragment"),fragments));else{out.grow(fragments);local.setStackInSlot(1,out);}}
        progress=0;setChanged();
    }
    public void startReplication(){
        if(replicationRunning||!canReplicate(template)||!local.getStackInSlot(1).isEmpty()||!clear(worldPosition.above()))return;
        needed=AspectHelper.getBonusTags(template).copy();if(needed.isEmpty())return;recipeCost=needed.copy();lastEssentiaSource=null;lastEssentiaAspect=null;
        progress=0;essentiaTicks=0;replicationRunning=true;level.playSound(null,worldPosition,thaumcraft.registry.ModSounds.CRAFTSTART.get(),net.minecraft.sounds.SoundSource.BLOCKS,0.5F,1);syncVisualState();
    }
    private void replicate(){
        if(!clear(worldPosition.above())||!canReplicate(template)){replicationRunning=false;syncVisualState();return;}
        if(!needed.isEmpty()){
            if(++essentiaTicks<50)return;essentiaTicks=0;
            for(BlockPos pos:BlockPos.betweenClosed(worldPosition.offset(-12,-10,-12),worldPosition.offset(12,5,12))){
                if((pos.getX()==worldPosition.getX()&&pos.getZ()==worldPosition.getZ())||!level.hasChunkAt(pos))continue;
                if(level.getBlockEntity(pos) instanceof AspectSource source)for(Aspect a:new ArrayList<>(needed.getAspects()))if(source.takeFromContainer(a,1)){needed.remove(a,1);lastEssentiaSource=pos.immutable();lastEssentiaAspect=a;lastEssentiaTick=level.getGameTime();syncVisualState();return;}
            }return;
        }
        if(++progress>=Config.REPLICATION_TICKS.get()){
            if(local.getStackInSlot(1).isEmpty())local.setStackInSlot(1,template.copyWithCount(1));
            replicationRunning=false;progress=0;syncVisualState();
        }else if(progress%5==0)syncVisualState();setChanged();
    }
    private boolean soulEligible(LivingEntity mob){
        String id=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
        return mob.isAlive()&&!(mob instanceof Player)&&!id.equals("thaumcraft:thaumic_slime")&&!id.startsWith("thaumcraft:golem");
    }
    private void souls(){
        var above=level.getBlockState(worldPosition.above());boolean fluxBlock=above.is(thaumcraft.registry.ModBlocks.FLUX_GAS.get())||above.is(thaumcraft.registry.ModBlocks.FLUX_GOO.get());
        if(!above.isAir()&&!fluxBlock){boolean hadTarget=drainTicks>0||soulTarget!=null;drainTicks=0;soulTarget=null;if(hadTarget)syncVisualState();return;}
        if(flux>1){spillFlux();flux--;setChanged();}
        if(++soulTicks%10!=0)return;
        var area=new AABB(worldPosition).inflate(Config.SOUL_RANGE.get());
        var mob=soulTarget!=null&&level instanceof ServerLevel sl&&sl.getEntity(soulTarget) instanceof LivingEntity entity?entity:null;
        if(mob==null||!soulEligible(mob)||!area.intersects(mob.getBoundingBox())||drainTicks<=0){
            var mobs=level.getEntitiesOfClass(LivingEntity.class,area,this::soulEligible);if(mobs.isEmpty()){soulTarget=null;drainTicks=0;syncVisualState();return;}
            mob=mobs.get(level.random.nextInt(mobs.size()));soulTarget=mob.getUUID();drainTicks=(int)(mob.getMaxHealth()*10);
        }
        drainTicks=Math.max(0,drainTicks-10);
        if(mob.getHealth()<=1){
            var aspects=AspectHelper.getEntityTags(mob);mob.getPersistentData().putLong("tx.soulDeath",level.getGameTime());
            if(mob.hurt(level.damageSources().magic(),100)&&!mob.isAlive()&&aspects!=null){
                for(Aspect a:aspects.getAspects())for(int i=0;i<aspects.getAmount(a);i++){
                    if(level.random.nextInt(3)==0)stored.add(a,1);
                    else if(level.random.nextBoolean())stored.add(a.isPrimal()?a:a.getComponents()[level.random.nextInt(2)],1);
                    else flux+=0.4F;
                }soulTarget=null;drainTicks=0;
            }
            mob.getPersistentData().remove("tx.soulDeath");
        }else mob.hurt(level.damageSources().magic(),1);
        syncVisualState();
    }
    private void spillFlux(){
        BlockPos pos=worldPosition.above();var state=level.getBlockState(pos);
        if(state.is(thaumcraft.registry.ModBlocks.FLUX_GAS.get())||state.is(thaumcraft.registry.ModBlocks.FLUX_GOO.get())){
            var property=thaumcraft.alchemy.FluxBlock.LEVEL;int n=state.getValue(property);if(n<8){level.setBlockAndUpdate(pos,state.setValue(property,n+1));return;}
            pos=worldPosition.offset(level.random.nextInt(3)-1,level.random.nextInt(3)-1,level.random.nextInt(3)-1);
        }
        if(level.isEmptyBlock(pos))level.setBlockAndUpdate(pos,(level.random.nextBoolean()?thaumcraft.registry.ModBlocks.FLUX_GAS.get():thaumcraft.registry.ModBlocks.FLUX_GOO.get()).defaultBlockState());
    }
    private boolean waterTarget(BlockPos pos){
        var tile=level.getBlockEntity(pos);
        if(tile instanceof thaumcraft.alchemy.CrucibleBlockEntity c)return c.getWaterAmount()<1000;
        if(tile instanceof thaumcraft.device.ArcaneSpaBlockEntity spa)return spa.getTank().getAmount()<5000;
        var id=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
        if(id.getNamespace().equals("botania")&&id.getPath().contains("apothecary")){var handler=level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,pos,Direction.UP);return handler!=null&&handler.fill(new FluidStack(Fluids.WATER,10),IFluidHandler.FluidAction.SIMULATE)>0;}
        return level.getBlockState(pos).is(Blocks.CAULDRON);
    }
    private void waterVisual(Vec3 target){waterVisual(target,false);}
    private void waterVisual(Vec3 target,boolean player){if(!java.util.Objects.equals(waterDestination,target)||waterOnPlayer!=player){waterDestination=target;waterOnPlayer=player;syncVisualState();}}
    private void tickWater(){
        if(!clear(worldPosition.above())){urnTarget=null;fireTarget=null;urnTicks=0;waterVisual(null);return;}
        var burning=fireTarget!=null&&level instanceof ServerLevel sl?sl.getPlayerByUUID(fireTarget):null;
        if(burning==null||!burning.isOnFire()){
            fireTarget=null;
            if(level.getGameTime()%5==0)for(var player:level.getEntitiesOfClass(Player.class,new AABB(worldPosition).inflate(3,2,3)))if(player.isOnFire()){burning=player;fireTarget=player.getUUID();urnTicks=0;break;}
        }
        if(burning!=null&&burning.isOnFire()&&burning.distanceToSqr(Vec3.atCenterOf(worldPosition))<25){
            waterVisual(burning.position(),true);
            if(++urnTicks>=15*Math.sqrt(burning.distanceToSqr(Vec3.atCenterOf(worldPosition)))){
                burning.clearFire();LegacySounds.swim(burning,2);for(BlockPos pos:BlockPos.betweenClosed(burning.blockPosition().offset(-1,-1,-1),burning.blockPosition().offset(1,1,1)))if(level.getBlockState(pos).is(net.minecraft.tags.BlockTags.FIRE))level.removeBlock(pos,false);urnTicks=0;fireTarget=null;waterVisual(null);
            }return;
        }
        if(urnTarget!=null&&level.hasChunkAt(urnTarget)&&waterTarget(urnTarget)){
            waterVisual(Vec3.atCenterOf(urnTarget).add(0,0.6,0));
            if(++urnTicks>=20*Math.sqrt(worldPosition.distSqr(urnTarget))){
                var tile=level.getBlockEntity(urnTarget);
                if(tile instanceof thaumcraft.alchemy.CrucibleBlockEntity c)c.fillWater(10);
                else if(tile instanceof thaumcraft.device.ArcaneSpaBlockEntity spa){spa.fillFrom(new FluidStack(Fluids.WATER,10));spa.setChanged();}
                else if(level.getBlockState(urnTarget).is(Blocks.CAULDRON))level.setBlockAndUpdate(urnTarget,Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL,3));
                else{var cap=level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,urnTarget,Direction.UP);if(cap!=null)cap.fill(new FluidStack(Fluids.WATER,10),IFluidHandler.FluidAction.EXECUTE);}
            }return;
        }
        urnTicks=0;urnTarget=null;fireTarget=null;waterVisual(null);
        for(var player:level.getEntitiesOfClass(Player.class,new AABB(worldPosition).inflate(3,2,3)))if(player.isOnFire()){fireTarget=player.getUUID();return;}
        var targets=new ArrayList<BlockPos>();for(BlockPos pos:BlockPos.betweenClosed(worldPosition.offset(-3,-2,-3),worldPosition.offset(3,2,3)))if(!pos.equals(worldPosition)&&level.hasChunkAt(pos)&&waterTarget(pos))targets.add(pos.immutable());
        if(!targets.isEmpty())urnTarget=targets.get(level.random.nextInt(targets.size()));
    }
    private void pull(){
        for(Direction d:Direction.values())if(canInputFrom(d)&&level.getBlockEntity(worldPosition.relative(d)) instanceof EssentiaTransport t && t.canOutputTo(d.getOpposite()) && t.getSuctionAmount(d.getOpposite())<getSuctionAmount(d)){
            var a=t.getEssentiaType(d.getOpposite());if(a!=null && doesContainerAccept(a) && t.getEssentiaAmount(d.getOpposite())>0){int n=t.takeEssentia(a,1,d.getOpposite());if(n>0)addEssentia(a,n,d);}
        }
    }
    @Override public AspectList getAspects(){var n=shared();if(kind().equals("bound_jar") && n!=null)return n.aspect==null?new AspectList():new AspectList().add(n.aspect,n.amount);return stored.copy();}
    @Override public void setAspects(AspectList a){stored=a.copy();setChanged();}
    @Override public boolean doesContainerAccept(Aspect a){if(kind().equals("oblivion_jar"))return stored.visSize()<64&&(stored.isEmpty()||stored.getAmount(a)>0);var n=shared();return kind().equals("bound_jar") && n!=null && n.amount<64 && (n.amount==0||n.aspect==a);}
    @Override public int addToContainer(Aspect a,int n){return n-addEssentia(a,n,Direction.UP);}
    @Override public boolean takeFromContainer(Aspect a,int n){if(n<0 || containerContains(a)<n)return false;if(kind().equals("crucible_of_souls")){stored.remove(a,n);setChanged();return true;}return takeEssentia(a,n,Direction.UP)==n;}
    @Override public boolean doesContainerContainAmount(Aspect a,int n){return containerContains(a)>=n;}
    @Override public int containerContains(Aspect a){return getAspects().getAmount(a);}
    @Override public boolean isConnectable(Direction d){return (kind().equals("soul_brazier")&&d==Direction.DOWN)||(kind().equals("crucible_of_souls")&&d!=Direction.UP)||((kind().equals("bound_jar")||kind().equals("oblivion_jar"))&&d==Direction.UP);}
    @Override public boolean canInputFrom(Direction d){return isConnectable(d)&&!kind().equals("crucible_of_souls");}
    @Override public boolean canOutputTo(Direction d){return isConnectable(d)&&!kind().equals("oblivion_jar")&&!kind().equals("soul_brazier");}
    @Override public void setSuction(Aspect a,int n){}
    @Override public Aspect getSuctionType(Direction d){if(kind().equals("soul_brazier"))return d==Direction.DOWN?Aspect.DEATH:null;return (kind().equals("bound_jar")||kind().equals("oblivion_jar"))?getAspects().getAspects().stream().findFirst().orElse(null):null;}
    @Override public int getSuctionAmount(Direction d){if(!isConnectable(d))return 0;if(kind().equals("soul_brazier"))return brazierEssentia<16?128:0;return kind().equals("oblivion_jar")?(stored.visSize()<64?8:0):kind().equals("bound_jar") && getAspects().visSize()<64?40:0;}
    @Override public int takeEssentia(Aspect a,int n,Direction d){if(!canOutputTo(d)||n<=0)return 0;var net=shared();if(kind().equals("bound_jar") && net!=null)return net.take(a,n);int count=Math.min(n,stored.getAmount(a));stored.remove(a,count);setChanged();return count;}
    @Override public int addEssentia(Aspect a,int n,Direction d){if(!canInputFrom(d)||a==null||n<=0)return 0;if(kind().equals("soul_brazier")){if(a!=Aspect.DEATH)return 0;int accepted=Math.min(n,16-brazierEssentia);brazierEssentia+=accepted;setChanged();return accepted;}if(kind().equals("oblivion_jar")){if(!doesContainerAccept(a))return 0;int accepted=Math.min(n,64-stored.visSize());stored.add(a,accepted);setChanged();return accepted;}var net=shared();return net!=null?net.add(a,n):0;}
    @Override public Aspect getEssentiaType(Direction d){var types=getAspects().getAspects();if(types.isEmpty())return null;if(kind().equals("crucible_of_souls")&&level!=null){if(transportAspectTick!=level.getGameTime()||!types.contains(transportAspect)){transportAspect=types.get(level.random.nextInt(types.size()));transportAspectTick=level.getGameTime();}return transportAspect;}return types.getFirst();}
    @Override public int getEssentiaAmount(Direction d){var a=getEssentiaType(d);return a==null?0:containerContains(a);}
    @Override public int getMinimumSuction(){return kind().equals("oblivion_jar")?8:kind().equals("bound_jar")?40:0;}
    @Override public boolean renderExtendedTube(){return false;}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider provider){return saveWithoutMetadata(provider);}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider provider){super.saveAdditional(tag,provider);tag.put("Items",local.serializeNBT(provider));if(channel!=null)tag.putUUID("Channel",channel);tag.putInt("Color",color);tag.putInt("Progress",progress);tag.putBoolean("Running",replicationRunning);if(brazierOwner!=null)tag.putUUID("BrazierOwner",brazierOwner);tag.putInt("Warp",storedWarp);tag.putInt("BrazierVis",brazierVis);tag.putInt("BrazierEssentia",brazierEssentia);tag.putBoolean("BrazierActive",brazierActive);tag.putInt("Lava",lavaAmount);tag.putInt("Viewers",viewers);tag.put("Stored",saveAspects(getAspects()));tag.put("Needed",saveAspects(needed));tag.put("RecipeCost",saveAspects(recipeCost));tag.putInt("JarAccessTicks",jarAccessTicks);if(lastEssentiaSource!=null)tag.putLong("LastEssentiaSource",lastEssentiaSource.asLong());if(lastEssentiaAspect!=null)tag.putString("LastEssentiaAspect",lastEssentiaAspect.getTag());tag.putLong("LastEssentiaTick",lastEssentiaTick);if(!template.isEmpty())tag.put("Template",template.save(provider));tag.putBoolean("Redstone",redstone);tag.putInt("EssentiaTicks",essentiaTicks);tag.putFloat("Flux",flux);tag.putInt("Warmup",warmup);tag.putInt("RotationTicks",rotationTicks);tag.putInt("Space",space);tag.putInt("DrainTicks",drainTicks);if(soulTarget!=null)tag.putUUID("SoulTarget",soulTarget);sorter.save(tag,provider);tag.putInt("ChestAccessTicks",chestAccessTicks);tag.putInt("VisualTicks",visualTicks);tag.putBoolean("ThinkActive",thinkActive);tag.putBoolean("WaterOnPlayer",waterOnPlayer);if(waterDestination!=null){tag.putDouble("WaterX",waterDestination.x);tag.putDouble("WaterY",waterDestination.y);tag.putDouble("WaterZ",waterDestination.z);}if(taintDestination!=null)tag.putLong("TaintDestination",taintDestination.asLong());tag.putLong("TaintVisualTick",taintVisualTick);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider provider){super.loadAdditional(tag,provider);local.deserializeNBT(provider,tag.getCompound("Items"));channel=tag.hasUUID("Channel")?tag.getUUID("Channel"):null;color=tag.getInt("Color");brazierOwner=tag.hasUUID("BrazierOwner")?tag.getUUID("BrazierOwner"):null;storedWarp=tag.getInt("Warp");brazierVis=Math.clamp(tag.getInt("BrazierVis"),0,16);brazierEssentia=Math.clamp(tag.getInt("BrazierEssentia"),0,16);brazierActive=tag.getBoolean("BrazierActive");lavaAmount=Math.clamp(tag.getInt("Lava"),0,4000);if(level!=null&&level.isClientSide())viewers=tag.getInt("Viewers");progress=tag.getInt("Progress");stored=loadAspects(tag.getCompound("Stored"));needed=loadAspects(tag.getCompound("Needed"));replicationRunning=tag.getBoolean("Running");template=tag.contains("Template")?ItemStack.parseOptional(provider,tag.getCompound("Template")):local.getStackInSlot(0).copy();redstone=tag.getBoolean("Redstone");essentiaTicks=tag.getInt("EssentiaTicks");flux=tag.getFloat("Flux");warmup=tag.getInt("Warmup");rotationTicks=tag.getInt("RotationTicks");space=tag.getInt("Space");drainTicks=tag.getInt("DrainTicks");soulTarget=tag.hasUUID("SoulTarget")?tag.getUUID("SoulTarget"):null;sorter.load(tag,provider);recipeCost=loadAspects(tag.getCompound("RecipeCost"));if(recipeCost.isEmpty()&&replicationRunning&&!template.isEmpty())recipeCost=AspectHelper.getBonusTags(template).copy();jarAccessTicks=tag.getInt("JarAccessTicks");lastEssentiaSource=tag.contains("LastEssentiaSource")?BlockPos.of(tag.getLong("LastEssentiaSource")):null;lastEssentiaAspect=Aspect.getAspect(tag.getString("LastEssentiaAspect"));lastEssentiaTick=tag.contains("LastEssentiaTick")?tag.getLong("LastEssentiaTick"):Long.MIN_VALUE;chestAccessTicks=tag.getInt("ChestAccessTicks");visualTicks=tag.getInt("VisualTicks");thinkActive=tag.getBoolean("ThinkActive");waterOnPlayer=tag.getBoolean("WaterOnPlayer");oldWarmup=warmup;oldRotationTicks=rotationTicks;waterDestination=tag.contains("WaterX")?new Vec3(tag.getDouble("WaterX"),tag.getDouble("WaterY"),tag.getDouble("WaterZ")):null;taintDestination=tag.contains("TaintDestination")?BlockPos.of(tag.getLong("TaintDestination")):null;taintVisualTick=tag.contains("TaintVisualTick")?tag.getLong("TaintVisualTick"):Long.MIN_VALUE;}
    void activateBrazier(Player player){
        if(!(player instanceof net.minecraft.server.level.ServerPlayer p))return;
        if(brazierOwner==null){brazierOwner=p.getUUID();setChanged();}
        if(!brazierOwner.equals(p.getUUID())){p.displayClientMessage(Component.translatable("tx.brazier.owner"),true);return;}
        if(p.isShiftKeyDown()&&brazierActive){returnWarp();return;}
        if(brazierVis<=0||brazierEssentia<=0){p.displayClientMessage(Component.translatable("tx.brazier.power",brazierVis,brazierEssentia),true);return;}
        int warp=thaumcraft.research.Warp.get(p,thaumcraft.research.WarpType.PERMANENT);
        if(warp<=0){p.displayClientMessage(Component.translatable(brazierActive?"tx.brazier.active":"tx.brazier.empty",storedWarp),true);return;}
        storedWarp+=warp;brazierActive=true;setChanged();thaumcraft.research.Warp.set(p,0,thaumcraft.research.WarpType.PERMANENT);BrazierTickets.force(this,true);
        p.displayClientMessage(Component.translatable("tx.brazier.stored",storedWarp),true);
    }
    void tickBrazier(){
        long t=level.getGameTime();
        if(t%5==0){if(brazierVis<16)brazierVis+=thaumcraft.vis.VisNet.drainVis(level,worldPosition,Aspect.FIRE,1);if(brazierEssentia<16&&level.getBlockEntity(worldPosition.below()) instanceof EssentiaTransport e&&e.canOutputTo(Direction.UP)&&e.getSuctionAmount(Direction.UP)<128)addEssentia(Aspect.DEATH,e.takeEssentia(Aspect.DEATH,1,Direction.UP),Direction.DOWN);setChanged();}
        if(!brazierActive)return;
        if(t%60==0){brazierVis=Math.max(0,brazierVis-3);brazierEssentia=Math.max(0,brazierEssentia-1);setChanged();}
        if(brazierVis<=0||brazierEssentia<=0){returnWarp();return;}
        if(t%50==0&&level instanceof ServerLevel s){var pos=worldPosition.offset(level.random.nextInt(16)-level.random.nextInt(16),level.random.nextInt(16)-level.random.nextInt(16),level.random.nextInt(16)-level.random.nextInt(16));if(s.hasChunkAt(pos)){if(!thaumcraft.taint.TaintBiomes.isTainted(s,pos)){taintDestination=new BlockPos(pos.getX(),worldPosition.getY()-1,pos.getZ());taintVisualTick=t;syncVisualState();}thaumcraft.taint.TaintBiomes.set(s,pos,thaumcraft.taint.TaintBiomes.TAINTED_LAND);if(Config.HARD_BRAZIER.get())thaumcraft.taint.TaintSpread.spreadFibres(s,pos);}}

    }
    public void returnWarp(){
        if(!(level instanceof ServerLevel s))return;
        if(brazierOwner!=null&&storedWarp>0){WarpReturns.get(s).add(brazierOwner,storedWarp);storedWarp=0;WarpReturns.get(s).deliver(s.getServer().getPlayerList().getPlayer(brazierOwner));}
        brazierActive=false;BrazierTickets.force(this,false);setChanged();
    }
    static CompoundTag saveAspects(AspectList a){var tag=new CompoundTag();for(Aspect aspect:a.getAspects())tag.putInt(aspect.getTag(),a.getAmount(aspect));return tag;}
    static AspectList loadAspects(CompoundTag tag){var a=new AspectList();for(String key:tag.getAllKeys()){var aspect=Aspect.getAspect(key);if(aspect!=null && tag.getInt(key)>0)a.add(aspect,tag.getInt(key));}return a;}
}

