package thaumicexploration;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import thaumcraft.aspect.*;
import thaumcraft.essentia.PhialItem;
import thaumcraft.registry.ModItems;

/** TC's phial item only accepts its own JarBlockEntity; bridge the addon's jars at block interaction. */
public final class BoundJarPhials {
    private BoundJarPhials(){}
    public static boolean interact(MachineEntity jar,Player player,InteractionHand hand){
        if(!jar.kind().equals("bound_jar")&&!jar.kind().equals("oblivion_jar"))return false;
        ItemStack held=player.getItemInHand(hand);
        if(!(held.getItem() instanceof PhialItem phial))return false;
        if(player.level().isClientSide())return true;
        ItemStack result;
        if(phial.isFilled()){
            AspectList contents=AspectKeys.ASPECTS.get(held);
            if(contents==null||contents.size()!=1)return true;
            Aspect aspect=contents.getAspects().getFirst();
            // A phial is indivisible: never consume it or partly fill a nearly full channel.
            if(contents.getAmount(aspect)!=PhialItem.CAPACITY||!jar.doesContainerAccept(aspect)||jar.getAspects().visSize()>64-PhialItem.CAPACITY)return true;
            if(jar.addToContainer(aspect,PhialItem.CAPACITY)!=0)return true;
            result=new ItemStack(ModItems.GLASS_PHIAL.get());
        }else{
            AspectList contents=jar.getAspects();
            if(contents.size()!=1)return true;
            Aspect aspect=contents.getAspects().getFirst();
            if(contents.getAmount(aspect)<PhialItem.CAPACITY||!jar.takeFromContainer(aspect,PhialItem.CAPACITY))return true;
            result=new ItemStack(ModItems.ESSENTIA_PHIAL.get());
            AspectKeys.ASPECTS.set(result,new AspectList().add(aspect,PhialItem.CAPACITY));
        }
        held.consume(1,player);
        if(!player.addItem(result))player.drop(result,false);
        jar.setChanged();
        player.level().sendBlockUpdated(jar.getBlockPos(),jar.getBlockState(),jar.getBlockState(),3);
        player.level().playSound(null,jar.getBlockPos(),SoundEvents.GENERIC_SWIM,SoundSource.PLAYERS,0.25F,1);
        player.containerMenu.broadcastChanges();
        return true;
    }
}
