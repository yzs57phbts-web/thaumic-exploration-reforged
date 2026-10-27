package thaumicexploration;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import thaumcraft.client.geom.ModelBoxGeometry;
import thaumcraft.client.platform.ItemGeometry;

public final class ThinkTankRendering {
    private ThinkTankRendering(){}
    public static void render(MachineEntity m,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay,BookModel book){
        float ticks=m.getLevel().getGameTime()+partial;
        float warmup=Mth.lerp(partial,m.oldWarmup,m.warmup),rotation=Mth.lerp(partial,m.oldRotationTicks,m.rotationTicks);
        for(int i=0;i<3;i++){
            float bob=Mth.sin((ticks+(i==1?9.32F:i==2?4.66F:0))/14F)*0.03F+0.03F;
            pose.pushPose();pose.translate(0.5,0.55-bob+warmup/160F,0.5);pose.mulPose(Axis.YP.rotationDegrees(2*rotation+i*120));pose.translate(0,0,-0.266);pose.mulPose(Axis.XP.rotationDegrees(180));pose.scale(0.3F,0.3F,0.3F);
            ItemGeometry.solid(buffers,pose,thaumcraft.Thaumcraft.id("textures/model/brain2.png"),false,s->{ModelBoxGeometry.box(s,-6,8,-8,12,10,16,0,0,128,64,true,-1,light);ModelBoxGeometry.box(s,-4,18,0,8,3,7,64,0,128,64,true,-1,light);});pose.popPose();
        }
        float alpha=LegacyVisuals.bookAlpha(warmup);if(alpha<=0)return;
        int count=LegacyVisuals.books(m.space);
        for(int i=0;i<count;i++){
            pose.pushPose();pose.translate(0.5,0.5+((i%2!=0)?-1:1)*Math.sin(ticks/28)*0.24,0.5);pose.mulPose(Axis.YP.rotationDegrees(-2*rotation+360F/count*i));pose.translate(m.space==0?-1.4F:-2,0,0);
            pose.mulPose(Axis.XP.rotationDegrees(70));
            float flip=((rotation+i)%7)/7F;int direction=Math.floorMod(java.util.Objects.hash(m.getBlockPos(),i,(m.rotationTicks+i)/7),4);
            book.setupAnim(rotation,direction==1?1-flip:direction==2?flip:0,0,0.75F);
            book.renderToBuffer(pose,buffers.getBuffer(RenderType.entityTranslucent(ResourceLocation.withDefaultNamespace("textures/entity/enchanting_table_book.png"))),light,overlay,((int)(alpha*255)<<24)|0xFFFFFF);pose.popPose();
        }
        LegacyVisuals.note("think_books",count);LegacyVisuals.note("think_books_"+count,1);
    }
}
