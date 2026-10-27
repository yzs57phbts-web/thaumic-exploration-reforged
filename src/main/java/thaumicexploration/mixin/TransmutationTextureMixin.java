package thaumicexploration.mixin;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import thaumicexploration.*;

/** Restrict the compatibility hook to TX's transmutation model; no TC JAR replacement. */
@Mixin(value=thaumcraft.client.render.WandSpecialRenderer.class,remap=false)
public abstract class TransmutationTextureMixin {
    @ModifyArg(method="render*",at=@At(value="INVOKE",target="Lthaumcraft/client/platform/ItemGeometry;solid(Lnet/minecraft/client/renderer/MultiBufferSource;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/resources/ResourceLocation;ZLjava/util/function/Consumer;)V",ordinal=0),index=2,remap=false,require=1,allow=1)
    private static ResourceLocation tx$animatedRod(ResourceLocation texture){
        if(!texture.equals(ThaumicExploration.id("textures/models/0.png")))return texture;
        var mc=Minecraft.getInstance();long ticks=mc.level==null?net.minecraft.Util.getMillis()/50:mc.level.getGameTime();
        int frame=LegacyVisuals.rodFrame(ticks);LegacyVisuals.note("wand_frame_"+frame,1);
        return ThaumicExploration.id("textures/models/"+frame+".png");
    }
}
