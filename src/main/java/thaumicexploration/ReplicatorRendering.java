package thaumicexploration;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import thaumcraft.client.platform.ItemGeometry;

public final class ReplicatorRendering {
    private ReplicatorRendering(){}
    public static void render(MachineEntity m,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(m.template.isEmpty())return;
        var above=m.getLevel().getBlockState(m.getBlockPos().above());if(!above.isAir()&&!above.canBeReplaced())return;
        float time=m.getLevel().getGameTime()+partial;
        float scale=ReplicatorAnimation.itemScale(m,partial);
        if(scale>0){
            var shown=!m.replicationRunning&&!m.local.getStackInSlot(1).isEmpty()?m.local.getStackInSlot(1):m.template;
            pose.pushPose();pose.translate(0.5,1.15+Math.sin(time/16F)*0.05,0.5);pose.scale(scale,scale,scale);pose.mulPose(Axis.YP.rotationDegrees(time%360));
            net.minecraft.client.Minecraft.getInstance().getItemRenderer().renderStatic(shown,ItemDisplayContext.FIXED,light,overlay,pose,!m.replicationRunning&&m.local.getStackInSlot(1).isEmpty()?GhostRendering.buffers(buffers):buffers,m.getLevel(),0);
            if(!m.replicationRunning&&m.local.getStackInSlot(1).isEmpty())LegacyVisuals.note("replicator_ghost",1);pose.popPose();
        }
        if(!m.replicationRunning)return;
        var cost=m.replicationCost();
        pose.pushPose();pose.translate(0.5,0,0.5);
        for(int i=0;i<4;i++){
            var aspect=ReplicatorAnimation.faceAspect(cost,i);if(aspect==null)continue;
            int rgb=aspect.getColor();
            // A faint full outline makes the collecting phase visible even before its first aspect arrives.
            rune(pose,buffers,1,0x38000000|rgb);
            float fill=ReplicatorAnimation.runeFill(m,aspect);
            if(fill>0)rune(pose,buffers,fill,0xFF000000|rgb);
            pose.mulPose(Axis.YP.rotationDegrees(90));
        }
        pose.popPose();
    }
    private static void rune(PoseStack pose,MultiBufferSource buffers,float height,int color){
        ItemGeometry.blended(buffers,pose,ThaumicExploration.id("textures/blocks/replicatorrunes.png"),1,s->{
            s.vertex(-0.5F,0,-0.502F,1,1,color,15728880);s.vertex(0.5F,0,-0.502F,0,1,color,15728880);s.vertex(0.5F,height,-0.502F,0,1-height,color,15728880);s.vertex(-0.5F,height,-0.502F,1,1-height,color,15728880);
        });
    }
}
