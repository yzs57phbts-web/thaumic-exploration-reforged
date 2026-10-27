package thaumicexploration;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import thaumcraft.client.render.EssentiaJarRendering;
public final class MachineRenderer implements BlockEntityRenderer<MachineEntity> {
    private final net.minecraft.client.model.BookModel book;
    public MachineRenderer(BlockEntityRendererProvider.Context context){book=new net.minecraft.client.model.BookModel(context.bakeLayer(net.minecraft.client.model.geom.ModelLayers.BOOK));}
    @Override public void render(MachineEntity m,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(m.kind().equals("soul_brazier")&&m.brazierVis>0&&m.brazierEssentia>0){
            float time=m.visualTicks+partial;
            pose.pushPose();pose.translate(0.5,1.5,0.5);pose.mulPose(net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
            int alpha=(int)(Math.min(1,(0.5F+Math.sin(time/8F)*0.1F)*4*((m.visualTicks%60==0||m.visualTicks%60==1)?7/37F:1))*255);
            int color=(alpha<<24)|0xB202FF;
            thaumcraft.client.platform.ItemGeometry.blended(buffers,pose,thaumcraft.Thaumcraft.id("textures/misc/node_bubble.png"),1,s->{s.vertex(-0.35F,-0.35F,0,0,1,color,15728880);s.vertex(0.35F,-0.35F,0,1,1,color,15728880);s.vertex(0.35F,0.35F,0,1,0,color,15728880);s.vertex(-0.35F,0.35F,0,0,0,color,15728880);});pose.popPose();
        }
        if(m.kind().equals("bound_chest")){BoundChestRendering.render(m,partial,pose,buffers,light);}
        if(m.kind().equals("think_tank"))ThinkTankRendering.render(m,partial,pose,buffers,light,overlay,book);
        if(m.kind().equals("crucible_of_souls"))fluid(m,pose,buffers,light);
        if((m.kind().equals("bound_jar")||m.kind().equals("oblivion_jar"))){
            if(m.kind().equals("bound_jar"))BoundJarRendering.pulse(m,pose,buffers,light);
            var tags=m.getAspects();var aspect=tags.getAspects().stream().findFirst().orElse(null);
            EssentiaJarRendering.submit(buffers,pose,light,m.getBlockPos().getX(),aspect,tags.visSize(),64,null,Direction.NORTH);
        }
        if(m.kind().equals("replicator"))ReplicatorRendering.render(m,partial,pose,buffers,light,overlay);
    }
    private static void fluid(MachineEntity m,PoseStack pose,MultiBufferSource buffers,int light){
        if(m.drainTicks>0)thaumcraft.client.platform.ItemGeometry.solid(buffers,pose,ThaumicExploration.id("textures/blocks/crucible7.png"),false,s->{
            int c=-1;float e=0.001F;
            s.vertex(1,0,-e,0,1,c,light);s.vertex(0,0,-e,1,1,c,light);s.vertex(0,1,-e,1,0,c,light);s.vertex(1,1,-e,0,0,c,light);
            s.vertex(0,0,1+e,0,1,c,light);s.vertex(1,0,1+e,1,1,c,light);s.vertex(1,1,1+e,1,0,c,light);s.vertex(0,1,1+e,0,0,c,light);
            s.vertex(-e,0,0,0,1,c,light);s.vertex(-e,0,1,1,1,c,light);s.vertex(-e,1,1,1,0,c,light);s.vertex(-e,1,0,0,0,c,light);
            s.vertex(1+e,0,1,0,1,c,light);s.vertex(1+e,0,0,1,1,c,light);s.vertex(1+e,1,0,1,0,c,light);s.vertex(1+e,1,1,0,0,c,light);
        });
        var sprite=net.minecraft.client.Minecraft.getInstance().getTextureAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS).apply(net.minecraft.resources.ResourceLocation.withDefaultNamespace("block/water_still"));
        float y=LegacyVisuals.fluidHeight(m.flux);
        thaumcraft.client.platform.ItemGeometry.solid(buffers,pose,net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS,true,s->{
            s.vertex(0,y,0,sprite.getU0(),sprite.getV0(),0xFFAA0080,light);s.vertex(0,y,1,sprite.getU0(),sprite.getV1(),0xFFAA0080,light);s.vertex(1,y,1,sprite.getU1(),sprite.getV1(),0xFFAA0080,light);s.vertex(1,y,0,sprite.getU1(),sprite.getV0(),0xFFAA0080,light);
        });LegacyVisuals.note("soul_fluid",1);
    }
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(MachineEntity m){return new net.minecraft.world.phys.AABB(m.getBlockPos()).inflate(m.kind().equals("think_tank")?2:0,1, m.kind().equals("think_tank")?2:0);}

}
