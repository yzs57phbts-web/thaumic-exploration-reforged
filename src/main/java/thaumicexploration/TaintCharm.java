package thaumicexploration;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.*;
import thaumcraft.registry.ModEffects;

public final class TaintCharm {
    static boolean inTaint(net.minecraft.world.entity.LivingEntity p){return p.level().getBiome(p.blockPosition()).is(thaumcraft.taint.TaintBiomes.TAINTED_LAND);}
    static void tick(Player p){
        if(!Config.enabled("taintCharm"))return;
        var data=p.getPersistentData();boolean active=Events.hasHotbar(p,"taint_charm"),previous=data.getBoolean("tx.tainted");
        if(previous&&!active&&!p.getAbilities().instabuild)p.hurt(p.damageSources().genericKill(),999);
        if(active&&!previous)p.level().playSound(null,p.getX(),p.getY(),p.getZ(),thaumcraft.registry.ModSounds.ZAP.get(),net.minecraft.sounds.SoundSource.PLAYERS,1,1);
        data.putBoolean("tx.tainted",active);
        if(!active)return;
        p.removeEffect(ModEffects.TAINT_POISON);
        if(inTaint(p)){
            data.putInt("tx.taintGrace",0);p.removeEffect(Content.WITHDRAWAL);
            int food=data.getInt("tx.taintFood");if(p.getFoodData().getFoodLevel()<4){if(++food>80){p.getFoodData().eat(1,0);food=0;}}else food=0;data.putInt("tx.taintFood",food);
        }else{
            int grace=data.getInt("tx.taintGrace")+1;data.putInt("tx.taintGrace",grace);
            if(grace>100&&!p.hasEffect(Content.WITHDRAWAL)){
                for(int i=0;i<=9;i++){var s=p.getInventory().getItem(i);if(s.is(Content.tc("taint_tendril"))||s.is(Content.tc("tainted_goo"))){s.shrink(1);grace=0;data.putInt("tx.taintGrace",0);break;}}
                if(grace>100)p.addEffect(new MobEffectInstance(Content.WITHDRAWAL,100,1));
            }
        }
    }
}
