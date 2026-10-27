package thaumicexploration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import thaumcraft.client.geom.QuadSink;
import thaumcraft.client.platform.ItemGeometry;

@EventBusSubscriber(modid=ThaumicExploration.ID,value=Dist.CLIENT)
public final class SorterOverlay {
    private static final ResourceLocation MARK=thaumcraft.Thaumcraft.id("textures/misc/mark.png"),HOME=thaumcraft.Thaumcraft.id("textures/misc/home.png"),SCRIPT=thaumcraft.Thaumcraft.id("textures/misc/script.png");
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
        var held=mc.player.getMainHandItem();var data=Data.get(held);
        boolean tool=held.is(Content.tc("golem_bell"))||net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(held.getItem()).getPath().endsWith("_golem");
        if(!tool||!data.contains("tx.sorter")||!mc.level.dimension().location().toString().equals(data.getString("tx.sorterDimension")))return;
        var origin=BlockPos.of(data.getLong("tx.sorter"));if(!mc.level.hasChunkAt(origin)||!(mc.level.getBlockEntity(origin) instanceof MachineEntity m)||!m.kind().equals("auto_sorter")||mc.player.distanceToSqr(Vec3.atCenterOf(origin))>4096)return;
        var pose=e.getPoseStack();var camera=e.getCamera().getPosition();var buffers=mc.renderBuffers().bufferSource();
        pose.pushPose();pose.translate(-camera.x,-camera.y,-camera.z);
        ItemGeometry.blended(buffers,pose,HOME,1,s->marker(s,origin,Direction.UP,0.65F));
        for(var link:m.sorter.links){
            if(!mc.level.hasChunkAt(link.pos))continue;
            ItemGeometry.blended(buffers,pose,MARK,1,s->marker(s,link.pos,link.side,0.4F));LegacyVisuals.note("sorter_mark",1);
            int quality=Config.EARLY_SPEC.isLoaded()?Config.SORTER_LINK_QUALITY.get():16;
            if(quality>=4){ItemGeometry.blended(buffers,pose,SCRIPT,1,s->line(s,Vec3.atCenterOf(origin),Vec3.atCenterOf(link.pos).add(Vec3.atLowerCornerOf(link.side.getNormal()).scale(0.51)),link.side,mc.level.getGameTime(),quality));LegacyVisuals.note("sorter_line",1);}
        }
        pose.popPose();
    }
    private static void marker(QuadSink s,BlockPos p,Direction side,float size){
        Vec3 center=Vec3.atCenterOf(p).add(Vec3.atLowerCornerOf(side.getNormal()).scale(0.51));float h=size/2;
        Vec3 a=side.getAxis()==Direction.Axis.X?new Vec3(0,h,0):new Vec3(h,0,0);
        Vec3 b=side.getAxis()==Direction.Axis.Z?new Vec3(0,h,0):new Vec3(0,0,h);
        quad(s,center.subtract(a).subtract(b),center.add(a).subtract(b),center.add(a).add(b),center.subtract(a).add(b),0,1,0xFFF0F0F0);
    }
    private static void line(QuadSink sink,Vec3 from,Vec3 to,Direction face,long ticks,int quality){
        double dist=from.distanceTo(to);int segments=Math.max(1,(int)Math.round(dist)*quality);Vec3 last=from;double time=ticks*50/30.0;
        for(int i=1;i<=segments;i++){
            float f=i/(float)segments,envelope=1-Math.abs(f*2-1);double phase=face.ordinal()*20+dist*(1-f)*quality-time/5;
            Vec3 point=from.lerp(to,f).add(Math.sin((phase+to.z%16)/4)*0.5*envelope,Math.sin((phase+to.x%16)/3)*0.5*envelope,Math.sin((phase+to.y%16)/2)*0.5*envelope);
            var width=point.subtract(last).cross(new Vec3(0,1,0)).normalize().scale(0.025);if(width.lengthSqr()<1.0E-8)width=new Vec3(0.025,0,0);
            quad(sink,last.subtract(width),last.add(width),point.add(width),point.subtract(width),(i-1)*1.5F/segments,i*1.5F/segments,0xBFF0F0F0);last=point;
        }
    }
    private static void quad(QuadSink s,Vec3 a,Vec3 b,Vec3 c,Vec3 d,float v0,float v1,int color){s.vertex((float)a.x,(float)a.y,(float)a.z,0,v0,color,0xF000F0);s.vertex((float)b.x,(float)b.y,(float)b.z,1,v0,color,0xF000F0);s.vertex((float)c.x,(float)c.y,(float)c.z,1,v1,color,0xF000F0);s.vertex((float)d.x,(float)d.y,(float)d.z,0,v1,color,0xF000F0);}
}
