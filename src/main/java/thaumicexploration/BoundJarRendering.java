package thaumicexploration;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.DyeColor;
import thaumcraft.client.geom.ModelBoxGeometry;
import thaumcraft.client.platform.ItemGeometry;

public final class BoundJarRendering {
    private BoundJarRendering(){}
    public static void pulse(MachineEntity m,PoseStack pose,MultiBufferSource buffers,int light){
        int frame=Math.clamp((int)Math.ceil((m.jarAccessTicks-1)/13.0001-0.5),0,5);
        if(frame==0)return;
        pose.pushPose();pose.translate(0.5,0,0.5);pose.scale(10.02F/12,1.001F,10.02F/12);
        int color=0xBB000000|DyeColor.byId(m.color).getTextureDiffuseColor();
        ItemGeometry.solid(buffers,pose,ThaumicExploration.id("textures/blocks/boundjaroverlay"+(frame-1)+".png"),true,s->ModelBoxGeometry.box(s,-6,0,-6,12,12,12,0,0,64,32,false,color,light));
        pose.popPose();
    }
}
