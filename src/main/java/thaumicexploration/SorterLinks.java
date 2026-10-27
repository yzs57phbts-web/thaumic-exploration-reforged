package thaumicexploration;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ContainerData;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.*;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Reconstructs the unfinished legacy sorter without its destructive inventory-clearing loop. */
public final class SorterLinks {
    public final MachineEntity owner;
    public final List<Link> links=new ArrayList<>();
    public static final class Link {
        public final BlockPos pos;public final Direction side;
        public int mode=1;
        public final ItemStackHandler filter=new ItemStackHandler(18);
        Link(BlockPos pos,Direction side){this.pos=pos.immutable();this.side=side;}
        boolean matches(ItemStack stack){boolean empty=true;for(int i=0;i<18;i++){var f=filter.getStackInSlot(i);if(!f.isEmpty()){empty=false;if(ItemStack.isSameItemSameComponents(f,stack))return true;}}return empty;}
    }
    SorterLinks(MachineEntity owner){this.owner=owner;}
    public Link link(BlockPos pos,Direction side){return links.stream().filter(l->l.pos.equals(pos)&&l.side==side).findFirst().orElseGet(()->{var l=new Link(pos,side);links.add(l);owner.syncVisualState();return l;});}
    public void interact(Player p,ItemStack held){
        if(held.is(Content.tc("golem_bell"))){var data=Data.get(held);data.putLong("tx.sorter",owner.getBlockPos().asLong());data.putString("tx.sorterDimension",p.level().dimension().location().toString());Data.put(held,data);owner.syncVisualState();p.displayClientMessage(Component.translatable("tx.sorter.selected"),true);}
        else p.displayClientMessage(Component.translatable("tx.sorter.help"),true);
    }
    public static void click(PlayerInteractEvent.RightClickBlock event){
        var p=event.getEntity();var stack=event.getItemStack();if(!stack.is(Content.tc("golem_bell")))return;
        var data=Data.get(stack);var level=p.level();var pos=event.getPos();
        if(level.getBlockEntity(pos) instanceof MachineEntity m&&m.kind().equals("auto_sorter")){
            if(!level.isClientSide())m.sorter.interact(p,stack);event.setCanceled(true);event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);return;
        }
        if(!data.contains("tx.sorter")||!data.getString("tx.sorterDimension").equals(level.dimension().location().toString()))return;
        var origin=BlockPos.of(data.getLong("tx.sorter"));if(!level.hasChunkAt(origin)||origin.distSqr(pos)>4096||!(level.getBlockEntity(origin) instanceof MachineEntity m)||!m.kind().equals("auto_sorter"))return;
        var side=event.getFace();if(side==null||level.getCapability(Capabilities.ItemHandler.BLOCK,pos,side)==null)return;
        event.setCanceled(true);event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);if(level.isClientSide())return;
        var link=m.sorter.link(pos,side);
        if(p.isShiftKeyDown()){
            var view=new HandlerContainer(link.filter,(i,s)->true,()->p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos))<=64&&!m.isRemoved(),m::setChanged);
            var sync=new ContainerData(){public int get(int i){return link.mode;}public void set(int i,int n){link.mode=Math.clamp(n,0,2);m.syncVisualState();}public int getCount(){return 1;}};
            p.openMenu(new SimpleMenuProvider((id,inv,player)->new SorterMenu(id,inv,view,sync),Component.translatable("tx.sorter.title")));
        }else p.displayClientMessage(Component.translatable("tx.sorter.linked",pos.getX(),pos.getY(),pos.getZ()),true);
    }
    public void tick(){
        var level=owner.getLevel();if(level==null)return;
        for(var from:links){if(from.mode!=1||!level.hasChunkAt(from.pos))continue;var input=level.getCapability(Capabilities.ItemHandler.BLOCK,from.pos,from.side);if(input==null)continue;
            for(int i=0;i<input.getSlots();i++){
                var probe=input.extractItem(i,1,true);if(probe.isEmpty()||!from.matches(probe))continue;
                for(var to:links){if(to.mode!=2||to.pos.equals(from.pos)||!to.matches(probe)||!level.hasChunkAt(to.pos))continue;var output=level.getCapability(Capabilities.ItemHandler.BLOCK,to.pos,to.side);if(output==null||!ItemHandlerHelper.insertItemStacked(output,probe,true).isEmpty())continue;
                    var moved=input.extractItem(i,1,false);if(moved.isEmpty())continue;var remainder=ItemHandlerHelper.insertItemStacked(output,moved,false);
                    if(!remainder.isEmpty()){remainder=input.insertItem(i,remainder,false);if(!remainder.isEmpty())net.minecraft.world.Containers.dropItemStack(level,from.pos.getX(),from.pos.getY(),from.pos.getZ(),remainder);}return;
                }
            }
        }
        // Recover the old port's real buffer; never treat it as ghost filters or silently discard it.
        for(int i=0;i<owner.local.getSlots();i++){var s=owner.local.getStackInSlot(i);if(s.isEmpty())continue;for(var to:links){if(to.mode!=2||!to.matches(s)||!level.hasChunkAt(to.pos))continue;var out=level.getCapability(Capabilities.ItemHandler.BLOCK,to.pos,to.side);if(out!=null){var rest=ItemHandlerHelper.insertItemStacked(out,s.copy(),false);if(rest.getCount()!=s.getCount()){owner.local.setStackInSlot(i,rest);return;}}}}
    }
    public void save(CompoundTag tag,HolderLookup.Provider provider){var list=new ListTag();for(var link:links){var t=new CompoundTag();t.putLong("Pos",link.pos.asLong());t.putInt("Side",link.side.ordinal());t.putInt("Mode",link.mode);t.put("Filter",link.filter.serializeNBT(provider));list.add(t);}tag.put("SorterLinks",list);}
    public void load(CompoundTag tag,HolderLookup.Provider provider){links.clear();for(Tag raw:tag.getList("SorterLinks",Tag.TAG_COMPOUND)){var t=(CompoundTag)raw;var link=new Link(BlockPos.of(t.getLong("Pos")),Direction.from3DDataValue(t.getInt("Side")));link.mode=Math.clamp(t.getInt("Mode"),0,2);link.filter.deserializeNBT(provider,t.getCompound("Filter"));links.add(link);}}
}
