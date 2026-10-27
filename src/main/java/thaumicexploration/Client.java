package thaumicexploration;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
@EventBusSubscriber(modid=ThaumicExploration.ID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class Client {
    @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e){e.enqueueWork(()->{LegacyVisuals.clientTick=LegacyMachineEffects::tick;
        for(var color:net.minecraft.world.item.DyeColor.values())for(String kind:java.util.List.of("chest","jar"))net.minecraft.client.renderer.item.ItemProperties.register(Content.item(color.getName()+"_"+kind+"_seal"),ThaumicExploration.id("linked"),(s,l,p,seed)->Data.get(s).hasUUID("Channel")?1:0);
    });}
    @SubscribeEvent public static void menus(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent e){e.register(Content.THINK_MENU.get(),ThinkTankScreen::new);e.register(Content.SORTER_MENU.get(),SorterScreen::new);}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(Content.MINION.get(),thaumcraft.client.render.entity.taint.TaintMobRenderer.provider(thaumcraft.client.render.entity.taint.TaintEntityModel.Kind.TENTACLE_TINY,0.2F));e.registerBlockEntityRenderer(Content.MACHINE.get(),MachineRenderer::new);e.registerBlockEntityRenderer(Content.CANDLE.get(),CandleRenderer::new);}
    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Item e){
        e.register((s,t)->t==0?0xFF000000|net.minecraft.world.item.DyeColor.byId(Data.get(s).getInt("Color")).getTextureDiffuseColor():-1,Content.item("bound_chest"),Content.item("bound_jar"));
        for(var entry:Content.ALL.entrySet()){
            var item=entry.getValue().get();String key=entry.getKey();
            for(var color:net.minecraft.world.item.DyeColor.values())if(key.startsWith(color.getName()+"_")&&(key.endsWith("_seal")||key.endsWith("_floating_candle"))){e.register((s,t)->t==0?0xFF000000|color.getTextureDiffuseColor():-1,item);break;}
        }
    }
    @SubscribeEvent public static void blockColors(RegisterColorHandlersEvent.Block e){
        for(var color:net.minecraft.world.item.DyeColor.values()){
            var item=Content.item(color.getName()+"_floating_candle");
            e.register((s,l,p,t)->0xFF000000|color.getTextureDiffuseColor(),((net.minecraft.world.item.BlockItem)item).getBlock());
        }
        e.register((s,l,p,t)->0xFF3F76E4,Content.block("everfull_urn"));
        e.register((s,l,p,t)->l!=null&&p!=null&&l.getBlockEntity(p) instanceof MachineEntity m?0xFF000000|net.minecraft.world.item.DyeColor.byId(m.color).getTextureDiffuseColor():-1,Content.block("bound_jar"));
    }
}

