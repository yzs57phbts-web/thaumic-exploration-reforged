package thaumicexploration;

import java.util.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.aspect.*;
import thaumcraft.wand.*;
import thaumcraft.node.NodeBlockEntity;

public final class Magic {
    public static void init(){
        rod("AMBER",10,"amber_rod",8,false,Magic::amber,"rodamber");
        rod("AMBER",25,"amber_staff_core",18,true,Magic::amber,"rodamber");
        rod("TRANSMUTATION",75,"transmutation_rod",6,false,Magic::transmute,"0");
        rod("TRANSMUTATION",175,"transmutation_staff_core",18,true,Magic::transmute,"0");
        rod("NECROMANCER",200,"necromancer_staff_core",15,true,Magic::necro,"rodnecro");
        WandRegistry.registerCap("SOJOURNER",0.95F,List.of(),0F,ThaumicExploration.id("sojourner_cap"),6,null,ThaumicExploration.id("textures/models/capsojourner.png"));
        WandRegistry.registerCap("MECHANIST",0.95F,List.of(),0F,ThaumicExploration.id("mechanist_cap"),6,null,ThaumicExploration.id("textures/models/capmechanist.png"));
    }
    public static void optional(){if(Config.BREAD.get()){rod("BREAD",39,"bread_rod",8,false,Magic::bread,"rodbread");Recipes.bread();}}
    private static void rod(String tag,int cap,String item,int cost,boolean staff,RodUpdate update,String texture){WandRegistry.registerRod(tag,cap,ThaumicExploration.id(item),cost,update,false,staff,false,ThaumicExploration.id("textures/models/"+texture+".png"));}
    public static void amber(ItemStack s,Player p){if(Config.enabled("wands")&&!p.level().isClientSide()&&p.tickCount%100==0)for(Aspect a:Aspect.getPrimalAspects())Wands.addVis(s,a,1,true);}
    public static void transmute(ItemStack s,Player p){
        if(p.level().isClientSide()||!Config.enabled("wands"))return;
        if(!Config.enabled("wands"))return;
        int max=Wands.getMaxVis(s),cutoff=max/10*9;
        var low=Aspect.getPrimalAspects().stream().filter(a->Wands.getVis(s,a)<=cutoff).toList();if(low.isEmpty())return;
        double total=0;
        for(Aspect a:Aspect.getPrimalAspects()){double excess=Wands.getVis(s,a)-cutoff-0.1;if(excess>0){Wands.storeVis(s,a,Wands.getVis(s,a)-(int)excess);total+=excess;}}
        if(total<10)total=0;
        int each=(int)(total/(low.size()*4));double wasted=0;
        for(Aspect a:low){int amount=Wands.getVis(s,a)+each;wasted+=Math.max(0,amount-max);Wands.storeVis(s,a,Math.min(max,amount));}
        for(Aspect a:Aspect.getPrimalAspects())Wands.storeVis(s,a,Math.min(max,(int)(Wands.getVis(s,a)+wasted/6)));
    }
    public static void bread(ItemStack s,Player p){if(!p.level().isClientSide()&&Config.BREAD.get()&&p.getRandom().nextInt(1000)==0)p.displayClientMessage(net.minecraft.network.chat.Component.literal("<").append(s.getHoverName()).append("> ").append(net.minecraft.network.chat.Component.translatable("tx.bread."+p.getRandom().nextInt(5))),false);}
    public static void necro(ItemStack s,Player p){
        if(p.level().isClientSide()||!Config.enabled("wands"))return;
        var tag=Data.get(s);
        if(tag.contains("PreviousVis")){
            var last=tag.getCompound("PreviousVis");
            for(Aspect a:Aspect.getPrimalAspects()){
                int current=Wands.getVis(s,a),previous=last.getInt(a.getTag());
                if(current>previous)Wands.storeVis(s,a,previous+(current-previous)*3/4);
            }
        }
        // TC's pickup method supplies hotbar selection, orb cooldown and the player's two-tick cooldown.
        for(var orb:p.level().getEntitiesOfClass(thaumcraft.entity.AspectOrb.class,p.getBoundingBox().inflate(3)))if(orb.isAlive())orb.playerTouch(p);
        tag.put("PreviousVis",MachineEntity.saveAspects(Wands.getAllVis(s)));Data.put(s,tag);
    }
    public static void caps(Player p){
        var s=p.getMainHandItem();if(!Wands.isWand(s))return;
        String cap=Wands.getCap(s).tag();
        var data=Data.get(s);int ticks=data.getInt("nodeTicks")+1;
        if(cap.equals("SOJOURNER")&&ticks>=20){
            var nodes=new ArrayList<net.minecraft.core.BlockPos>();
            for(var pos:net.minecraft.core.BlockPos.betweenClosed(p.blockPosition().offset(-8,-8,-8),p.blockPosition().offset(8,8,8))){
                if(!p.level().hasChunkAt(pos)||!(p.level().getBlockEntity(pos) instanceof NodeBlockEntity node))continue;
                if(Aspect.getPrimalAspects().stream().anyMatch(a->Wands.getVis(s,a)<Wands.getMaxVis(s)&&node.containerContains(a)>1))nodes.add(pos.immutable());
            }
            if(!nodes.isEmpty()){
                var target=net.minecraft.core.BlockPos.of(data.getLong("drainPos"));if(!nodes.contains(target))target=nodes.get(p.getRandom().nextInt(nodes.size()));data.putLong("drainPos",target.asLong());
                var node=(NodeBlockEntity)p.level().getBlockEntity(target);var possible=Aspect.getPrimalAspects().stream().filter(a->Wands.getVis(s,a)<Wands.getMaxVis(s)&&node.containerContains(a)>1).toList();
                var aspect=possible.get(p.getRandom().nextInt(possible.size()));if(node.takeFromContainer(aspect,1)){Wands.addVis(s,aspect,1,true);node.setChanged();p.level().sendBlockUpdated(target,node.getBlockState(),node.getBlockState(),2);}
            }ticks=0;
        }
        if(cap.equals("MECHANIST")&&p.isUsingItem()&&WandKeys.TARGET.has(s)){
            var pos=WandKeys.TARGET.get(s);
            if(p.level().hasChunkAt(pos)&&p.level().getBlockEntity(pos) instanceof NodeBlockEntity node){node.onUsingWandTick(s,p,ticks);if(ticks%3==0)p.level().playSound(null,pos,p.getRandom().nextBoolean()?net.minecraft.sounds.SoundEvents.PISTON_EXTEND:net.minecraft.sounds.SoundEvents.PISTON_CONTRACT,net.minecraft.sounds.SoundSource.BLOCKS,0.1F+p.getRandom().nextFloat()*0.5F,0.75F);}
        }
        data.putInt("nodeTicks",ticks);Data.put(s,data);
    }
}
