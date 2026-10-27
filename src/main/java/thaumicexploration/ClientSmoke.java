package thaumicexploration;

import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Activated only by -PclientSmoke=true. Checks the real baked models and saves a Russian preview. */
@EventBusSubscriber(modid=ThaumicExploration.ID,value=Dist.CLIENT)
public final class ClientSmoke {
    private static int ticks;
    private static boolean started;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(!Boolean.getBoolean("tx.clientSmoke"))return;
        var mc=Minecraft.getInstance();
        if(!started){
            if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;
            for(var entry:Content.ALL.entrySet()){
                var item=entry.getValue().get();var stack=new ItemStack(item);
                if(mc.getItemRenderer().getModel(stack,null,null,0)==mc.getModelManager().getMissingModel())throw new IllegalStateException("Missing item model "+entry.getKey());
                if(mc.getItemRenderer().getModel(stack,null,null,0).getParticleIcon().contents().name().getPath().equals("missingno"))throw new IllegalStateException("Missing item texture "+entry.getKey());
                if(!net.minecraft.client.resources.language.I18n.exists(item.getDescriptionId()))throw new IllegalStateException("Missing translation "+item.getDescriptionId());
            }
            started=true;mc.setScreen(new Preview());System.out.println("TX_CLIENT_SMOKE: baked models and item translations resolved for "+Content.ALL.size()+" entries");
        }
        if(++ticks==60)Screenshot.grab(mc.gameDirectory,mc.getMainRenderTarget(),message->System.out.println("TX_CLIENT_SCREENSHOT: "+message.getString()));
        if(ticks>=85)mc.stop();
    }
    private static final class Preview extends Screen {
        Preview(){super(Component.literal("Thaumic Exploration — 1.21.1"));}
        @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
            g.fill(0,0,width,height,0xFF111827);g.drawCenteredString(font,"Thaumic Exploration — 1.21.1",width/2,8,0xFFF5E6C8);
            g.drawCenteredString(font,Component.translatable("tc.research_category.TX"),width/2,22,0xFFB49BDC);
            String[] names={"purified_brain","amber_rod","amber_staff_core","transmutation_rod","transmutation_staff_core","necromancer_staff_core","sojourner_cap","mechanist_cap","dreamcatcher","tentacle_ring","stabilizer_belt","talisman_of_nourishment","meteor_boots","comet_boots","think_tank","everfull_urn","replicator","crucible_of_souls","oblivion_jar","bound_chest","bound_jar","white_chest_seal","purple_jar_seal","red_floating_candle"};
            int cols=4,cellWidth=width/cols,cellHeight=Math.max(32,(height-42)/6);
            for(int i=0;i<names.length;i++){
                int x=(i%cols)*cellWidth,y=40+(i/cols)*cellHeight;var stack=new ItemStack(Content.item(names[i]));
                g.fill(x+2,y,x+cellWidth-2,y+cellHeight-2,0xFF202A3E);g.renderItem(stack,x+5,y+6);
                var label=font.split(stack.getHoverName(),cellWidth-31);int line=0;for(var text:label)g.drawString(font,text,x+27,y+5+line++*10,0xFFE9E1D4,false);
            }
        }
    }
}
