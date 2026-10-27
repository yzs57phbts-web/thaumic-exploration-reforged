package thaumicexploration;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import thaumcraft.aspect.*;
import thaumcraft.fx.*;

@EventBusSubscriber(modid=ThaumicExploration.ID,value=Dist.CLIENT)
public final class ActivityClientSmoke {
    static int ticks,fragments,sparkles,trails;static long changeTime=-1;
    static final BlockPos JAR_A=new BlockPos(2,65,0),JAR_B=new BlockPos(4,65,0),ACTIVE=new BlockPos(1,65,4),IDLE=new BlockPos(4,65,4);
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(!Boolean.getBoolean("tx.activitySmoke"))return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null||mc.getOverlay()!=null)return;var server=mc.getSingleplayerServer();
        if(ticks++==0){
            mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;mc.setScreen(null);
            FxProxy original=Fx.get();Fx.set(new FxProxy(){
                @Override public void infusionBlockParticles(net.minecraft.world.level.Level l,double x,double y,double z,BlockPos target,net.minecraft.world.level.block.state.BlockState state){fragments++;original.infusionBlockParticles(l,x,y,z,target,state);}
                @Override public void infusionSparkle(net.minecraft.world.level.Level l,double x,double y,double z,BlockPos target,boolean alternate){sparkles++;original.infusionSparkle(l,x,y,z,target,alternate);}
                @Override public void essentiaTrail(net.minecraft.world.level.Level l,BlockPos from,BlockPos to,int count,int color,float scale){trails++;original.essentiaTrail(l,from,to,count,color,scale);}
            });
            server.execute(()->{
                var l=server.overworld();l.setDayTime(1000);for(BlockPos pos:BlockPos.betweenClosed(-4,64,-2,9,71,10))l.setBlockAndUpdate(pos,pos.getY()==64?Blocks.STONE_BRICKS.defaultBlockState():Blocks.AIR.defaultBlockState());
                var channel=java.util.UUID.randomUUID();for(var pos:new BlockPos[]{JAR_A,JAR_B}){l.setBlockAndUpdate(pos,Content.block("bound_jar").defaultBlockState());var m=(MachineEntity)l.getBlockEntity(pos);m.channel=channel;m.color=DyeColor.BLUE.getId();m.getAspects();m.syncVisualState();}
                for(var pos:new BlockPos[]{ACTIVE,IDLE}){l.setBlockAndUpdate(pos,Content.block("replicator").defaultBlockState());var m=(MachineEntity)l.getBlockEntity(pos);m.template=new ItemStack(Items.STONE);m.syncVisualState();}
                var source=new BlockPos(-2,65,4);l.setBlockAndUpdate(source,Content.block("crucible_of_souls").defaultBlockState());((MachineEntity)l.getBlockEntity(source)).setAspects(AspectHelper.getBonusTags(new ItemStack(Items.STONE)));
                var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.getAbilities().flying=true;p.onUpdateAbilities();p.connection.teleport(3.5,66.9,3.5,180,17);
            });
        }
        if(ticks==30)shot("port6-jars-empty-seals.png");
        if(ticks==40)server.execute(()->{changeTime=server.overworld().getGameTime();((MachineEntity)server.overworld().getBlockEntity(JAR_A)).addToContainer(Aspect.WATER,8);});
        if(changeTime>=0&&ticks>=41&&ticks<=48&&mc.level.getBlockEntity(JAR_B) instanceof MachineEntity m&&m.containerContains(Aspect.WATER)==8){System.out.println("TX_PORT6_JAR_SYNC_TICKS: "+(mc.level.getGameTime()-changeTime));changeTime=-2;}
        if(ticks==55&&changeTime!=-2)throw new IllegalStateException("Shared jar client still stale after 15 ticks");
        if(ticks==65)shot("port6-jars-filled-no-label.png");
        if(ticks==80)server.execute(()->{((MachineEntity)server.overworld().getBlockEntity(ACTIVE)).startReplication();server.getPlayerList().getPlayers().getFirst().connection.teleport(2.5,67.3,8,180,18);});
        if(ticks==110){var m=(MachineEntity)mc.level.getBlockEntity(ACTIVE);if(m.replicationPhase()!=ReplicatorAnimation.Phase.COLLECTING)throw new IllegalStateException("Collecting phase not synchronized");shot("port6-replicator-collecting.png");}
        if(ticks==205){var m=(MachineEntity)mc.level.getBlockEntity(ACTIVE);if(m.replicationPhase()!=ReplicatorAnimation.Phase.ASSEMBLING)throw new IllegalStateException("Assembly phase not synchronized");shot("port6-replicator-assembly-a.png");}
        if(ticks==250)shot("port6-replicator-assembly-b.png");
        if(ticks==300){var m=(MachineEntity)mc.level.getBlockEntity(ACTIVE);if(m.replicationRunning||!m.local.getStackInSlot(1).is(Items.STONE))throw new IllegalStateException("Output/completion not synchronized");if(fragments<100||sparkles==0||trails==0)throw new IllegalStateException("Missing original assembly effects "+fragments+"/"+sparkles+"/"+trails);shot("port6-replicator-complete.png");}
        if(ticks>=315){System.out.println("TX_PORT6_ACTIVITY_SMOKE: jar seal/no label and immediate shared sync; complete replication cycle; fragment/sparkle/trail calls="+fragments+"/"+sparkles+"/"+trails);mc.stop();}
    }
    static void shot(String name){var mc=Minecraft.getInstance();Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),m->System.out.println("TX_PORT6_SCREENSHOT: "+name));}
}
