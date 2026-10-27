package thaumicexploration;

import java.time.LocalDate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.particles.ParticleTypes;

/** Original 1.1-53 timing, shared with server-side regression tests. */
public final class LegacyVisuals {
    private LegacyVisuals(){}
    public static final java.util.Map<String,Integer> observed=new java.util.concurrent.ConcurrentHashMap<>();
    public static void note(String key,int n){if(Boolean.getBoolean("tx.effectsSmoke"))observed.merge(key,n,Integer::sum);}
    public static java.util.function.Consumer<MachineEntity> clientTick=m->{};
    private static final int[] ROD_FRAMES={0,0,0,0,0,0,1,2,3,4,5,5,5,5,5,5,6,7,8,9,10,10,10,10,10,10,11,12,13,14,15,15,15,15,15,15,16,17,18,19,20,20,20,20,20,20,21,22,23,24,25,25,25,25,25,25,26,27,28,29};
    public static int rodFrame(long tick){return ROD_FRAMES[(int)Math.floorMod(tick,118)/2];}
    public static int accessFrame(int ticks){return Math.clamp((int)Math.ceil((ticks-1)/13.0001-0.5),0,5);}
    public static int books(int space){return space==0?6:8;}
    public static float bookAlpha(float warmup){return Math.clamp(warmup/40F,0F,1F);}
    public static float fluidHeight(float flux){float h=0.3F+0.7F*flux;return h>1?1.001F:h==1?0.9999F:h==0.3F?0.2F:h;}
    public static LocalDate chestDate(){return Boolean.getBoolean("tx.effectsSmoke")&&System.getProperty("tx.effectsDate")!=null?LocalDate.parse(System.getProperty("tx.effectsDate")):LocalDate.now();}
    public static boolean christmas(LocalDate date){return date.getMonthValue()==12&&date.getDayOfMonth()>=24&&date.getDayOfMonth()<=26;}
    public static boolean movingTrail(boolean comet,boolean wet,double x,double y,double z){return !wet&&Math.abs(x)+Math.abs(z)+Math.abs(y)*(comet?1:0.5)>0.1;}
    public static void boots(Player p,boolean smash){
        if(!(p.level() instanceof ServerLevel level))return;
        var stack=p.getItemBySlot(EquipmentSlot.FEET);if(!(stack.getItem() instanceof ElementalBoots b)||!Config.enabled(b.kind))return;
        boolean comet=b.kind.equals("comet_boots");var v=p.getDeltaMovement();
        if(!smash&&!movingTrail(comet,p.isInWater(),v.x,v.y,v.z))return;
        note(smash?"meteor_dive":comet?"comet_trail":"meteor_trail",2);
        for(int i=0;i<2;i++)level.sendParticles(comet?ParticleTypes.ITEM_SNOWBALL:ParticleTypes.FLAME,p.getX()+level.random.nextDouble()-0.5,smash?p.getY()+level.random.nextDouble()-0.5:p.getY()+0.25+(level.random.nextDouble()-0.5)*0.25,p.getZ()+level.random.nextDouble()-0.5,0,0,smash?0:0.025,0,1);
        if(smash)level.sendParticles(ParticleTypes.SMOKE,p.getX()+level.random.nextDouble()-0.5,p.getY()+level.random.nextDouble()-0.5,p.getZ()+level.random.nextDouble()-0.5,0,0,0,0,0);
    }
}
