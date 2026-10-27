package thaumicexploration;

import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent;
import thaumcraft.aspect.*;

/** Explicit opt-in visual/audio integration test, in a separate disposable save. */
@EventBusSubscriber(modid=ThaumicExploration.ID,value=Dist.CLIENT)
public final class JarSoundClientSmoke {
    private static int tick;
    private static final Set<String> channels=java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final BlockPos JAR=new BlockPos(2,65,0),REP=new BlockPos(1,65,-3),URN=new BlockPos(3,65,-3),LAVA=new BlockPos(4,65,-3),CHEST=new BlockPos(5,65,-3);
    @SubscribeEvent public static void sound(PlaySoundSourceEvent e){if(Boolean.getBoolean("tx.jarSoundSmoke")){channels.add(e.getSound().getLocation().toString());System.out.println("TX_PORT9_AUDIO_CHANNEL: "+e.getSound().getLocation());}}
    private static MachineEntity put(net.minecraft.server.level.ServerLevel l,BlockPos pos,String name){l.setBlockAndUpdate(pos,Content.block(name).defaultBlockState());return (MachineEntity)l.getBlockEntity(pos);}
    private static void use(net.minecraft.server.level.ServerPlayer p,BlockPos pos,ItemStack item){p.setItemInHand(InteractionHand.MAIN_HAND,item);p.gameMode.useItemOn(p,p.serverLevel(),item,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));}
    private static void shot(String name){var mc=Minecraft.getInstance();Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),c->System.out.println("TX_PORT9_SCREENSHOT: "+name));}
    @SubscribeEvent public static void run(ClientTickEvent.Post event){
        if(!Boolean.getBoolean("tx.jarSoundSmoke"))return;
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null||mc.getOverlay()!=null)return;
        var server=mc.getSingleplayerServer();int now=tick++;
        if(now==0){
            mc.options.pauseOnLostFocus=false;mc.options.hideGui=false;mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.particles().set(ParticleStatus.ALL);
            for(var source:net.minecraft.sounds.SoundSource.values())mc.options.getSoundSourceOptionInstance(source).set(source==net.minecraft.sounds.SoundSource.MUSIC?0D:1D);
            mc.setScreen(null);
            var model=mc.getItemRenderer().getModel(new ItemStack(Content.item("oblivion_jar")),mc.level,mc.player,0);
            if(!model.isGui3d()||model.getTransforms().gui.rotation.x()==0||model.getTransforms().firstPersonRightHand.scale.x()!=model.getTransforms().firstPersonRightHand.scale.y())throw new IllegalStateException("Jar item is flat or has no GUI rotation");
            server.execute(()->{
                var l=server.overworld();l.setDayTime(1000);l.setWeatherParameters(100000,0,false,false);l.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false,server);
                for(BlockPos pos:BlockPos.betweenClosed(-4,64,-6,10,72,9))l.setBlockAndUpdate(pos,pos.getY()==64?Blocks.STONE_BRICKS.defaultBlockState():Blocks.AIR.defaultBlockState());
                put(l,new BlockPos(0,65,0),"oblivion_jar");put(l,JAR,"oblivion_jar");l.setBlockAndUpdate(new BlockPos(4,65,0),thaumcraft.registry.ModBlocks.WARDED_JAR.get().defaultBlockState());
                put(l,REP,"replicator");put(l,URN,"everfull_urn");put(l,LAVA,"everburn_urn");put(l,CHEST,"bound_chest");
                var p=server.getPlayerList().getPlayers().getFirst();p.getInventory().clearContent();p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.connection.teleport(2.5,66.6,5.5,180,12);
            });
        }
        if(now==20)server.execute(()->{
            var p=server.getPlayerList().getPlayers().getFirst();
            for(int i=0;i<8;i++)use(p,JAR,BoundJarGameTests.phial(Aspect.WATER));
            var m=(MachineEntity)p.serverLevel().getBlockEntity(JAR);if(m.containerContains(Aspect.WATER)<60)throw new IllegalStateException("Live phial filling failed");
            m.syncVisualState();p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        });
        if(now==35){var m=(MachineEntity)mc.level.getBlockEntity(JAR);if(m.containerContains(Aspect.WATER)==0)throw new IllegalStateException("Filled jar liquid never reached client");shot("port9-jars-world-and-liquid.png");}
        if(now==45)server.execute(()->server.getPlayerList().getPlayers().getFirst().setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Content.item("oblivion_jar"))));
        if(now==65)shot("port9-jar-first-person.png");
        if(now==75)mc.setScreen(new Preview());
        if(now==90)shot("port9-jar-gui.png");
        if(now==100){mc.setScreen(null);mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);}
        if(now==120)shot("port9-jar-third-person.png");
        if(now==130){mc.options.setCameraType(CameraType.FIRST_PERSON);server.execute(()->{
            var p=server.getPlayerList().getPlayers().getFirst();
            use(p,URN,new ItemStack(Items.BUCKET));use(p,URN,new ItemStack(Items.GLASS_BOTTLE));
            ((MachineEntity)p.serverLevel().getBlockEntity(LAVA)).lavaAmount=1000;use(p,LAVA,new ItemStack(Items.BUCKET));
            p.getInventory().clearContent();p.getInventory().setItem(0,new ItemStack(Content.item("taint_charm")));TaintCharm.tick(p);
            p.getInventory().setItem(0,new ItemStack(Items.BREAD));p.tickCount=20;var food=new ItemStack(Content.item("talisman_of_nourishment"));((Trinket)food.getItem()).active(food,p);
            p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        });}
        if(now==160)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();use(p,REP,new ItemStack(Items.STONE));((MachineEntity)p.serverLevel().getBlockEntity(REP)).startReplication();});
        if(now==190)server.execute(()->{
            var l=server.overworld();var m=Content.MINION.get().create(l);m.setPos(2.5,65,3.5);l.addFreshEntity(m);m.playAmbientSound();m.hurt(l.damageSources().magic(),1);m.invulnerableTime=0;m.hurt(l.damageSources().magic(),100);
            var p=server.getPlayerList().getPlayers().getFirst();p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);use(p,CHEST,ItemStack.EMPTY);
        });
        if(now==205)server.execute(()->server.getPlayerList().getPlayers().getFirst().closeContainer());
        if(now==215)server.execute(()->server.overworld().destroyBlock(new BlockPos(0,65,0),true));
        if(now==240){
            for(String key:List.of("minecraft:entity.generic.swim","minecraft:entity.generic.eat","minecraft:entity.item.pickup","minecraft:block.chest.open","minecraft:block.chest.close","thaumcraft:craftstart","thaumcraft:zap","thaumcraft:roots","thaumcraft:tentacle","thaumcraft:jar"))if(!channels.contains(key))throw new IllegalStateException("Audio never reached an OpenAL channel: "+key+" heard="+channels);
            System.out.println("TX_PORT9_CLIENT_OK: world liquid, GUI, both hand perspectives, uniform transforms and real audio channels verified: "+channels);mc.stop();
        }
    }
    private static final class Preview extends Screen {
        Preview(){super(Component.literal("Банка забвения"));}
        @Override public void render(GuiGraphics g,int x,int y,float partial){g.fill(0,0,width,height,0xFF303444);g.drawCenteredString(font,Component.translatable("block.thaumicexploration.oblivion_jar"),width/2,height/2-80,-1);g.pose().pushPose();g.pose().translate(width/2-56,height/2-56,0);g.pose().scale(7,7,1);g.renderItem(new ItemStack(Content.item("oblivion_jar")),0,0);g.pose().popPose();}
    }
}
