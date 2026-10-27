package thaumicexploration;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import thaumcraft.aspect.Aspect;
import thaumcraft.client.fx.*;
import thaumcraft.client.geom.LightningBolt;
import thaumcraft.client.platform.*;

/** Uses the original TC particle implementations at the original world-space endpoints. */
public final class LegacyMachineEffects {
    private LegacyMachineEffects(){}
    public static void bolt(MachineEntity m,Vec3 from,Vec3 to){
        BoltRenderer.add(new LightningBolt(from.x,from.y,from.z,to.x,to.y,to.z,m.getLevel().random.nextLong(),6,0.5F,5).setType(5).setWidth(0.068F).defaultFractal());
    }
    private static void water(MachineEntity m,Vec3 to){
        var p=m.getBlockPos();WorldFxRenderer.add(new EssentiaTrailFx(m.getLevel().random,p.getX()+0.5,p.getY()+1.1,p.getZ()+0.5,to.x,to.y,to.z,5,Aspect.TOOL.getColor(),1));
    }
    public static void tick(MachineEntity m){
        var l=m.getLevel();if(l==null||!l.isClientSide()||!Config.enabled(m.kind()))return;
        var p=m.getBlockPos();var r=l.random;int t=m.visualTicks;
        switch(m.kind()){
            case "oblivion_jar"->{
                for(int i=0;i<3;i++){var a=new Vec3(p.getX()+r.nextDouble(),p.getY()+r.nextDouble(),p.getZ()+r.nextDouble());var b=a.add(r.nextDouble()/2-0.25,r.nextDouble()/2-0.25,r.nextDouble()/2-0.25);bolt(m,a,Vec3.atLowerCornerOf(p).add(0.5,0.4,0.5));bolt(m,b,a);LegacyVisuals.note("oblivion_bolts",2);}
            }
            case "think_tank"->{if(m.warmup>0)for(int i=0;i<LegacyVisuals.books(m.space);i++){double a=-2*m.rotationTicks+360.0*i/LegacyVisuals.books(m.space);l.addParticle(ParticleTypes.ENCHANT,p.getX()+0.5+2*Math.sin(a),p.getY(),p.getZ()+0.5+2*Math.cos(a),0,0.1,0);LegacyVisuals.note("think_enchant",1);}}
            case "everfull_urn"->{
                if(!l.getBlockState(p.above()).isAir()&&!l.getBlockState(p.above()).canBeReplaced())return;
                if(m.waterDestination!=null){if(t%3==0){water(m,m.waterDestination);LegacyVisuals.note(m.waterOnPlayer?"water_player":"water_directed",1);}}
                else if(t%2==0){water(m,new Vec3(p.getX()+0.5+r.nextDouble()-0.5,p.getY()+2.1,p.getZ()+0.5+r.nextDouble()-0.5));LegacyVisuals.note("water_fountain",1);}
            }
            case "crucible_of_souls"->{
                if(m.drainTicks<=0||m.soulTarget==null)return;
                var targets=l.getEntitiesOfClass(LivingEntity.class,new net.minecraft.world.phys.AABB(p).inflate(Config.SOUL_RANGE.get()+1),e->e.isAlive()&&e.getUUID().equals(m.soulTarget));
                if(targets.isEmpty())return;var victim=targets.getFirst();
                double x=victim.getX(),y=victim.getBoundingBox().maxY-0.5,z=victim.getZ();
                if(r.nextBoolean()){
                    var sprite=ParticleSprites.block(Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());
                    if(sprite!=null){var fx=new BoreParticleFx(r,x,y,z,p.getX()+0.5,p.getY()+0.5,p.getZ()+0.5,sprite);fx.motion(r.nextGaussian()*0.03,r.nextGaussian()*0.03,r.nextGaussian()*0.03);WorldFxRenderer.add(fx);}
                }else {var fx=new BoreSparkleFx(r,x,y,z,p.getX()+0.5,p.getY()+0.5,p.getZ()+0.5);fx.color(0.4F+r.nextFloat()*0.2F,0.2F,0.6F+r.nextFloat()*0.3F);WorldFxRenderer.add(fx);}
                LegacyVisuals.note("soul_drain",1);
            }
            case "soul_brazier"->{
                var center=Vec3.atLowerCornerOf(p).add(0.55+Math.cos(Math.toRadians(t*3))/4,1.5+Math.sin(Math.toRadians(t))/4,0.55+Math.sin(Math.toRadians(t*3))/4);
                if(m.brazierActive&&t%10==0){
                    var purple=new WispFx(r,center.x,center.y,center.z,r.nextFloat()/1.125F,0,null);purple.color(0.69803923F,0,1).gravity(0);purple.shrink(false);WorldFxRenderer.add(purple);
                    var dark=new WispFx(r,center.x,center.y,center.z,r.nextFloat()/1.5F,5,null){@Override public int blendMode(){return 770;}};dark.color(0.1F,0.1F,0.1F).gravity(0);dark.shrink(false);WorldFxRenderer.add(dark);LegacyVisuals.note("brazier_wisps",2);
                }
                if(m.taintDestination!=null&&m.taintVisualTick!=m.clientTaintTick&&l.getGameTime()-m.taintVisualTick<40){bolt(m,center,Vec3.atLowerCornerOf(m.taintDestination));m.clientTaintTick=m.taintVisualTick;LegacyVisuals.note("brazier_bolts",1);}
            }
        }
    }
}
