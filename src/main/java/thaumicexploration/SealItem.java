package thaumicexploration;

import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import thaumcraft.essentia.JarBlockEntity;

public final class SealItem extends Item {
    public final String kind;
    public final DyeColor color;
    public SealItem(String kind,DyeColor color,Properties p){super(p);this.kind=kind;this.color=color;}
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context){return useOn(context);}
    @Override public InteractionResult useOn(UseOnContext c){
        var p=c.getPlayer();if(p==null)return InteractionResult.PASS;
        var l=c.getLevel();var pos=c.getClickedPos();var tile=l.getBlockEntity(pos);var stack=c.getItemInHand();
        if(!p.mayUseItemAt(pos,c.getClickedFace(),stack) || !l.mayInteract(p,pos))return InteractionResult.FAIL;
        var tag=Data.get(stack);
        if(tile instanceof MachineEntity m&&m.kind().equals(kind.equals("chest")?"bound_chest":"bound_jar")&&m.channel!=null&&!tag.hasUUID("Channel")){
            if(l instanceof ServerLevel){var linked=new ItemStack(Content.item(DyeColor.byId(m.color).getName()+"_"+kind+"_seal"));var data=new net.minecraft.nbt.CompoundTag();data.putUUID("Channel",m.channel);data.putBoolean("LinkedSeal",true);data.putInt("X",pos.getX());data.putInt("Y",pos.getY());data.putInt("Z",pos.getZ());data.putString("Dimension",l.dimension().location().toString());Data.put(linked,data);linked.set(DataComponents.MAX_STACK_SIZE,1);if(!p.hasInfiniteMaterials())stack.shrink(1);if(!p.addItem(linked))p.drop(linked,false);p.displayClientMessage(Component.translatable("tx.seal.copied"),true);}
            return InteractionResult.sidedSuccess(l.isClientSide());
        }
        boolean valid=kind.equals("chest")?tile instanceof ChestBlockEntity:tile instanceof JarBlockEntity;
        if(!valid)return InteractionResult.PASS;
        if(tile instanceof JarBlockEntity jar&&(jar.getAmount()!=0||jar.getAspectFilter()!=null))return InteractionResult.FAIL;
        if(tile instanceof ChestBlockEntity chest && (!chest.canOpen(p) || l.getBlockState(pos).getValue(ChestBlock.TYPE)!=ChestType.SINGLE))return InteractionResult.FAIL;
        if(l instanceof ServerLevel server){
            var customName=stack.get(DataComponents.CUSTOM_NAME);
            UUID id=customName!=null?BoundNetworks.get(server).named(kind,customName.getString()):tag.hasUUID("Channel")?tag.getUUID("Channel"):UUID.randomUUID();
            var network=BoundNetworks.get(server).network(id);
            if(tile instanceof ChestBlockEntity chest){
                // Capacity is checked before changing either inventory.
                var simulation=new net.neoforged.neoforge.items.ItemStackHandler(27);
                simulation.deserializeNBT(server.registryAccess(),network.items.serializeNBT(server.registryAccess()));
                for(int i=0;i<chest.getContainerSize();i++)if(!ItemHandlerHelper.insertItemStacked(simulation,chest.getItem(i).copy(),false).isEmpty()){p.displayClientMessage(Component.translatable("tx.network.full"),true);return InteractionResult.FAIL;}
                for(int i=0;i<chest.getContainerSize();i++){ItemHandlerHelper.insertItemStacked(network.items,chest.getItem(i).copy(),false);chest.setItem(i,ItemStack.EMPTY);}
            }
            if(tile instanceof JarBlockEntity jar){
                if(jar.getAmount()>0 && ((network.amount>0 && network.aspect!=jar.getAspect()) || network.amount+jar.getAmount()>64)){p.displayClientMessage(Component.translatable("tx.network.full"),true);return InteractionResult.FAIL;}
                if(jar.getAspect()!=null)network.add(jar.getAspect(),jar.getAmount());
            }
            var state=Content.block(kind.equals("chest")?"bound_chest":"bound_jar").defaultBlockState();
            if(tile instanceof ChestBlockEntity)state=state.setValue(MachineBlock.FACING,l.getBlockState(pos).getValue(ChestBlock.FACING));
            l.setBlock(pos,state,3);
            if(l.getBlockEntity(pos) instanceof MachineEntity m){m.channel=id;m.color=color.getId();m.setChanged();l.sendBlockUpdated(pos,state,state,3);l.invalidateCapabilities(pos);}
            // Previously created port.1/2 seals keep their reusable channels. New seals follow the original consume/copy/apply flow.
            if(tag.hasUUID("Channel")&&!tag.getBoolean("LinkedSeal")){tag.putUUID("Channel",id);Data.put(stack,tag);}
            else if(!p.hasInfiniteMaterials())stack.shrink(1);
            p.displayClientMessage(Component.translatable("tx.network.linked",id.toString().substring(0,8)),true);
        }
        return InteractionResult.sidedSuccess(l.isClientSide());
    }
    @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> text,TooltipFlag f){
        var tag=Data.get(s);if(tag.getBoolean("LinkedSeal"))text.add(Component.translatable("tx.seal.location",tag.getInt("X"),tag.getInt("Y"),tag.getInt("Z"),tag.getString("Dimension")));text.add(Component.translatable(tag.hasUUID("Channel")?"tx.seal.linked":"tx.seal.unlinked",tag.hasUUID("Channel")?tag.getUUID("Channel").toString().substring(0,8):""));
    }
}
