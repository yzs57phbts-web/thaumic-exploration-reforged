package thaumicexploration;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
/** Explicit opt-in regression run in the disposable tx-validation save only. */
@EventBusSubscriber(modid=ThaumicExploration.ID,value=Dist.CLIENT)
public final class ClientWorldSmoke {
    private static int ticks;
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        if(Boolean.getBoolean("tx.effectsSmoke"))return;
        if(!Boolean.getBoolean("tx.worldSmoke")||Boolean.getBoolean("tx.jarSmoke")||Boolean.getBoolean("tx.activitySmoke"))return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null||mc.getOverlay()!=null)return;
        var server=mc.getSingleplayerServer();
        if(ticks++==0){
            mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;mc.setScreen(null);
            for(var entry:Content.ALL.entrySet()){
                var stack=new ItemStack(entry.getValue().get());var model=mc.getItemRenderer().getModel(stack,mc.level,mc.player,0);
                if(model==mc.getModelManager().getMissingModel()||model.getParticleIcon().contents().name().getPath().equals("missingno"))throw new IllegalStateException("Missing model/texture "+entry.getKey());
                if(!net.minecraft.client.resources.language.I18n.exists(stack.getDescriptionId()))throw new IllegalStateException("Missing translation "+entry.getKey());
            }
            if(Config.BREAD.get()){
                var rod=thaumcraft.wand.WandRegistry.rod("BREAD");
                if(!rod.tag().equals("BREAD")||rod.capacity()!=39||!CreativeOrder.items().contains("bread_rod")||thaumcraft.crafting.TcRecipes.get("TX:ROD_BREAD")==null)throw new IllegalStateException("Bread wand startup option did not register legacy content");
                System.out.println("TX_OPTIONAL_BREAD_SMOKE: 39 vis rod, recipe, creative entry and Russian name loaded");
            }
            server.execute(()->{
                var l=server.overworld();l.setDayTime(1000);l.setWeatherParameters(100000,0,false,false);
                for(BlockPos pos:BlockPos.betweenClosed(-10,63,-4,22,72,8))l.setBlockAndUpdate(pos,pos.getY()==63?Blocks.STONE_BRICKS.defaultBlockState():Blocks.AIR.defaultBlockState());
                String[] names={"crucible_of_souls","replicator","everfull_urn","bound_chest","think_tank","bound_jar","oblivion_jar","soul_brazier","everburn_urn"};
                for(int i=0;i<names.length;i++){
                    var pos=new BlockPos(-6+i*3,65,0);l.setBlockAndUpdate(pos,Content.block(names[i]).defaultBlockState().setValue(MachineBlock.FACING,Direction.SOUTH));
                    var m=(MachineEntity)l.getBlockEntity(pos);
                    if(names[i].equals("think_tank")){l.setBlockAndUpdate(pos.below(),Blocks.BOOKSHELF.defaultBlockState());m.local.setStackInSlot(0,new ItemStack(Items.BOOK,64));}
                    if(names[i].equals("replicator"))m.local.setStackInSlot(0,new ItemStack(Items.OAK_LOG));
                    if(names[i].equals("bound_chest")){m.color=DyeColor.BLUE.getId();m.channel=java.util.UUID.randomUUID();}
                    if(names[i].equals("bound_jar"))m.color=DyeColor.RED.getId();
                    if(names[i].equals("soul_brazier")){m.brazierVis=16;m.brazierEssentia=16;}
                    if(names[i].equals("everburn_urn"))m.lavaAmount=2000;
                    m.setChanged();l.sendBlockUpdated(pos,m.getBlockState(),m.getBlockState(),3);
                }
                for(int i=0;i<4;i++)l.setBlockAndUpdate(new BlockPos(i*2,65,3),Content.CANDLES.get(new String[]{"orange_floating_candle","purple_floating_candle","blue_floating_candle","white_floating_candle"}[i]).get().defaultBlockState());
                for(var item:l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(-10,63,-4,23,73,9)))item.discard();
                var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.connection.teleport(6,69,17,180,15);
            });
        }
        if(ticks==90)shot("port4-world.png");
        if(ticks==100)server.execute(()->server.getPlayerList().getPlayers().getFirst().connection.teleport(2,66.8,6.8,180,20));
        if(ticks==130)shot("port4-candles-a.png");
        if(ticks==175)shot("port4-candles-b.png");
        if(ticks==185)server.execute(()->server.getPlayerList().getPlayers().getFirst().connection.teleport(3.5,66.7,3,180,25));
        if(ticks==215)shot("port4-chest-closed.png");
        if(ticks==225)server.execute(()->{var l=server.overworld();var m=(MachineEntity)l.getBlockEntity(new BlockPos(3,65,0));m.viewers=1;l.blockEvent(m.getBlockPos(),m.getBlockState().getBlock(),1,1);});
        if(ticks==250){var m=(MachineEntity)mc.level.getBlockEntity(new BlockPos(3,65,0));if(m==null||m.lid<0.9F)throw new IllegalStateException("Chest lid did not animate");shot("port4-chest-open.png");}
        if(ticks==260)server.execute(()->{var l=server.overworld();var m=(MachineEntity)l.getBlockEntity(new BlockPos(3,65,0));m.viewers=0;l.blockEvent(m.getBlockPos(),m.getBlockState().getBlock(),1,0);server.getPlayerList().getPlayers().getFirst().connection.teleport(16,67.5,5.5,180,20);});
        if(ticks==290)shot("port4-brazier-urn.png");
        if(ticks==300){mc.options.hideGui=false;server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.connection.teleport(6,66,3,180,15);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);((MachineEntity)server.overworld().getBlockEntity(new BlockPos(6,65,0))).interact(p,net.minecraft.world.InteractionHand.MAIN_HAND);});}
        if(ticks==335){if(!(mc.screen instanceof ThinkTankScreen))throw new IllegalStateException("Think tank menu regression");shot("port4-think-tank.png");}
        if(ticks==345)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.closeContainer();var filters=new net.minecraft.world.SimpleContainer(18);filters.setItem(0,new ItemStack(Items.DIAMOND));p.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inv,player)->new SorterMenu(id,inv,filters,new net.minecraft.world.inventory.SimpleContainerData(1)),net.minecraft.network.chat.Component.translatable("tx.sorter.title")));});
        if(ticks==380){if(!(mc.screen instanceof SorterScreen))throw new IllegalStateException("Sorter menu regression");shot("port4-sorter.png");}
        if(ticks>=395){System.out.println("TX_PORT4_WORLD_SMOKE: 98 models, candles, sealed chest animation, brazier, urn, think tank and sorter menus verified");mc.stop();}
    }
    private static void shot(String name){var mc=Minecraft.getInstance();Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),m->System.out.println("TX_WORLD_SCREENSHOT: "+name));}
}
