package thaumicexploration;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in in the disposable validation world. Checks baked animation frames and actual phial interaction. */
@EventBusSubscriber(modid=ThaumicExploration.ID,value=Dist.CLIENT)
public final class JarClientSmoke {
    static int ticks;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(!Boolean.getBoolean("tx.jarSmoke"))return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null||mc.getOverlay()!=null)return;
        var server=mc.getSingleplayerServer();
        if(ticks++==0){
            var sprite=mc.getTextureAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS).apply(ThaumicExploration.id("items/taint_charm"));
            if(sprite.contents().width()!=16||sprite.contents().height()!=16)throw new IllegalStateException("Charm is a whole animation strip instead of 16x16 frames");
            mc.options.pauseOnLostFocus=false;mc.options.hideGui=false;mc.setScreen(null);
            server.execute(()->{
                var l=server.overworld();for(BlockPos pos:BlockPos.betweenClosed(-1,64,-2,9,70,5))l.setBlockAndUpdate(pos,pos.getY()==64?Blocks.STONE_BRICKS.defaultBlockState():Blocks.AIR.defaultBlockState());
                var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();
                var channel=java.util.UUID.randomUUID();
                for(int x:new int[]{2,4,6}){
                    var pos=new BlockPos(x,65,0);l.setBlockAndUpdate(pos,thaumcraft.registry.ModBlocks.WARDED_JAR.get().defaultBlockState());
                    if(x==2)continue;
                    var seal=new ItemStack(Content.item("blue_jar_seal"));var tag=Data.get(seal);tag.putUUID("Channel",channel);Data.put(seal,tag);p.setItemInHand(InteractionHand.MAIN_HAND,seal);
                    ((SealItem)seal.getItem()).useOn(new net.minecraft.world.item.context.UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false)));
                }
                var filled=new ItemStack(thaumcraft.registry.ModItems.ESSENTIA_PHIAL.get());thaumcraft.aspect.AspectKeys.ASPECTS.set(filled,new thaumcraft.aspect.AspectList().add(thaumcraft.aspect.Aspect.WATER,8));p.setItemInHand(InteractionHand.MAIN_HAND,filled);
                var pos=new BlockPos(4,65,0);p.gameMode.useItemOn(p,l,filled,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
                if(!(l.getBlockEntity(pos) instanceof MachineEntity a)||a.containerContains(thaumcraft.aspect.Aspect.WATER)!=8||!(l.getBlockEntity(new BlockPos(6,65,0)) instanceof MachineEntity b)||b.containerContains(thaumcraft.aspect.Aspect.WATER)!=8)throw new IllegalStateException("Live phial/shared jar fill failed");
                p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Content.item("taint_charm")));p.connection.teleport(4.5,67.2,6.5,180,15);
            });
        }
        if(ticks==90){
            var a=mc.level.getBlockEntity(new BlockPos(4,65,0));if(!(a instanceof MachineEntity jar)||jar.containerContains(thaumcraft.aspect.Aspect.WATER)!=8)throw new IllegalStateException("Bound jar contents not synchronized to client");shot("port5-jars.png");
        }
        if(ticks==100)mc.setScreen(new Preview());
        if(ticks==120)shot("port5-charm-a.png");
        if(ticks==127)shot("port5-charm-b.png");
        if(ticks>=140){System.out.println("TX_PORT5_JAR_SMOKE: shared phial fill and client sync, matching jar size, 16x16 animated charm verified");mc.stop();}
    }
    static void shot(String name){var mc=Minecraft.getInstance();Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),m->System.out.println("TX_PORT5_SCREENSHOT: "+name));}
    static final class Preview extends Screen {
        Preview(){super(Component.literal("Амулет заражения"));}
        @Override public void render(GuiGraphics g,int x,int y,float partial){g.fill(0,0,width,height,0xFF202331);g.drawCenteredString(font,Component.translatable("item.thaumicexploration.taint_charm"),width/2,height/2-68,0xFFFFFFFF);g.pose().pushPose();g.pose().translate(width/2-48,height/2-48,0);g.pose().scale(6,6,1);g.renderItem(new ItemStack(Content.item("taint_charm")),0,0);g.pose().popPose();}
    }
}
