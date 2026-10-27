package thaumicexploration;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.DyeColor;
import thaumcraft.client.geom.ModelBoxGeometry;
import thaumcraft.client.platform.ItemGeometry;
public final class BoundChestRendering {
    public static void render(MachineEntity m,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        float open=net.minecraft.util.Mth.lerp(partial,m.oldLid,m.lid);open=1-(float)Math.pow(1-open,3);final float angle=-open*(float)Math.PI/2;
        pose.pushPose();pose.translate(0,1,1);pose.scale(1,-1,-1);pose.translate(0.5,0.5,0.5);
        float rotation=switch(m.getBlockState().getValue(MachineBlock.FACING)){case NORTH->180;case WEST->90;case EAST->-90;default->0;};pose.mulPose(Axis.YP.rotationDegrees(rotation));pose.translate(-0.5,-0.5,-0.5);
        boolean christmas=LegacyVisuals.christmas(LegacyVisuals.chestDate());
        var tex=ThaumicExploration.id(christmas?"textures/legacy/chest_christmas.png":"textures/legacy/chest.png");
        var seal=ThaumicExploration.id("textures/blocks/boundchestoverlayseal"+(christmas?"christmas":"")+".png");
        int frame=LegacyVisuals.accessFrame(m.chestAccessTicks);
        var pulse=ThaumicExploration.id("textures/blocks/boundchestoverlay"+(frame==0?"none":frame-1)+".png");
        int color=0xFF000000|DyeColor.byId(m.color).getTextureDiffuseColor();int translucent=(color&0xFFFFFF)|0xE5000000;
        pose.pushPose();pose.translate(1/16F,6/16F,1/16F);
        ItemGeometry.solid(buffers,pose,tex,false,sink->ModelBoxGeometry.box(sink,0,0,0,14,10,14,0,19,64,64,false,-1,light));
        ItemGeometry.solid(buffers,pose,pulse,true,sink->ModelBoxGeometry.box(sink,-1,0,-1,16,10,16,0,21,64,64,false,translucent,light));
        ItemGeometry.solid(buffers,pose,seal,true,sink->ModelBoxGeometry.box(sink,-1,0,-1,16,10,16,0,21,64,64,false,color,light));pose.popPose();
        pose.pushPose();pose.translate(1/16F,7/16F,15/16F);pose.mulPose(Axis.XP.rotation(angle));
        ItemGeometry.solid(buffers,pose,tex,false,sink->ModelBoxGeometry.box(sink,0,-5,-14,14,5,14,0,0,64,64,false,-1,light));
        ItemGeometry.solid(buffers,pose,pulse,true,sink->ModelBoxGeometry.box(sink,0,-5.1F,-14,14,5,14,0,0,64,64,false,translucent,light));
        ItemGeometry.solid(buffers,pose,seal,false,sink->ModelBoxGeometry.box(sink,0,-5.1F,-14,14,5,14,0,0,64,64,false,color,light));pose.popPose();
        pose.pushPose();pose.translate(8/16F,7/16F,15/16F);pose.mulPose(Axis.XP.rotation(angle));
        ItemGeometry.solid(buffers,pose,tex,false,sink->ModelBoxGeometry.box(sink,-1,-2,-15,2,4,1,0,0,64,64,false,-1,light));pose.popPose();pose.popPose();if(frame>0)LegacyVisuals.note("chest_pulse",1);
    }
}
