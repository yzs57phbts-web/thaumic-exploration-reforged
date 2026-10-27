package thaumicexploration;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import thaumcraft.aspect.*;
import thaumcraft.fx.Fx;

/** Shared phase/progress calculations and TC's original inward fragment, trail and sparkle effects. */
public final class ReplicatorAnimation {
    public enum Phase { IDLE, COLLECTING, ASSEMBLING }
    private ReplicatorAnimation(){}
    public static Aspect faceAspect(AspectList costs,int face){
        var types=costs.getAspects();if(types.isEmpty())return null;
        if(types.size()==3){var largest=types.stream().max(java.util.Comparator.comparingInt(costs::getAmount)).orElseThrow();if(face%2==0)return largest;return types.stream().filter(a->a!=largest).toList().get(face/2);}
        return types.get(face%types.size());
    }
    public static float runeFill(MachineEntity m,Aspect a){int total=m.replicationCost().getAmount(a);return total==0?0:net.minecraft.util.Mth.clamp(1-m.replicationRemaining().getAmount(a)/(float)total,0,1);}
    public static float itemScale(MachineEntity m,float partial){return switch(m.replicationPhase()){case IDLE->0.5F;case COLLECTING->0;case ASSEMBLING->0.5F*net.minecraft.util.Mth.clamp((m.progress+partial)/Math.max(1,Config.REPLICATION_TICKS.get()),0,1);};}
    public static void clientTick(MachineEntity m){
        if(!m.kind().equals("replicator")||!m.replicationRunning||m.template.isEmpty())return;
        var level=m.getLevel();if(level==null||!level.isClientSide())return;
        var pos=m.getBlockPos();var random=level.random;var fx=Fx.get();
        if(m.replicationPhase()==Phase.ASSEMBLING&&m.template.getItem() instanceof BlockItem item){
            for(int i=0;i<5;i++)fx.infusionBlockParticles(level,pos.getX()+0.5+random.nextDouble()*2-1,pos.getY()+1.5+random.nextDouble()*2-1,pos.getZ()+0.5+random.nextDouble()*2-1,pos.above(2),item.getBlock().defaultBlockState());
            if(random.nextInt(3)==0)fx.infusionSparkle(level,pos.getX()+0.5+random.nextDouble()*2-1,pos.getY()+1.5+random.nextDouble()*2-1,pos.getZ()+0.5+random.nextDouble()*2-1,pos.above(2),false);
        }
        // A newly consumed aspect has an authoritative source; show the trail during collection too.
        if(m.lastEssentiaSource!=null&&m.lastEssentiaAspect!=null&&m.lastEssentiaTick!=m.clientEffectTick&&level.getGameTime()-m.lastEssentiaTick<20){
            fx.essentiaTrail(level,m.lastEssentiaSource,pos.above(),5,m.lastEssentiaAspect.getColor(),1);m.clientEffectTick=m.lastEssentiaTick;
        }
        if(m.replicationPhase()==Phase.ASSEMBLING&&m.progress<Config.REPLICATION_TICKS.get()*0.6F&&random.nextInt(4)==0){
            var types=m.replicationCost().getAspects();if(!types.isEmpty())fx.essentiaTrail(level,pos.offset(random.nextInt(3)-1,1+random.nextInt(2),random.nextInt(3)-1),pos.above(),5,types.get(random.nextInt(types.size())).getColor(),1);
        }
    }
}
