package thaumicexploration;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in integration scenes in the disposable tx-validation save, never the user's worlds. */
@EventBusSubscriber(modid=ThaumicExploration.ID,value=Dist.CLIENT)
public final class EffectsClientSmoke {
    private static int tick;private static boolean done;
    private static net.minecraft.server.level.ServerPlayer remote;
    private static int beforeRemoteMeteor,beforeRemoteComet;
    private static void equipment(net.minecraft.server.MinecraftServer server,net.minecraft.world.item.ItemStack boots){remote.setItemSlot(EquipmentSlot.FEET,boots);server.getPlayerList().getPlayers().getFirst().connection.send(new net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket(remote.getId(),java.util.List.of(com.mojang.datafixers.util.Pair.of(EquipmentSlot.FEET,boots))));}
    private static final BlockPos TANK=new BlockPos(-5,65,0),JAR=new BlockPos(-1,65,0),URN=new BlockPos(2,65,0),SOUL=new BlockPos(8,65,0),BRAZIER=new BlockPos(13,65,0),CHEST=new BlockPos(17,65,0),GHOST=new BlockPos(22,65,0),SORTER=new BlockPos(26,65,0);
    private static MachineEntity put(net.minecraft.server.level.ServerLevel l,BlockPos p,String kind){l.setBlockAndUpdate(p,Content.block(kind).defaultBlockState());return (MachineEntity)l.getBlockEntity(p);}
    private static void camera(double x,double y,double z,float yaw,float pitch){var s=Minecraft.getInstance().getSingleplayerServer();s.execute(()->s.getPlayerList().getPlayers().getFirst().connection.teleport(x,y,z,yaw,pitch));}
    @SubscribeEvent public static void run(ClientTickEvent.Post event){
        if(!Boolean.getBoolean("tx.effectsSmoke")||done)return;
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null||mc.getOverlay()!=null)return;
        var server=mc.getSingleplayerServer();
        if(tick++==0){
            LegacyVisuals.observed.clear();mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.particles().set(ParticleStatus.ALL);mc.setScreen(null);
            server.execute(()->{
                var l=server.overworld();l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);l.setDayTime(18000);l.setWeatherParameters(100000,0,false,false);
                for(int x=-2;x<=3;x++)for(int z=-2;z<=2;z++)l.setChunkForced(x,z,true);
                for(BlockPos p:BlockPos.betweenClosed(-14,64,-8,32,74,18))l.setBlockAndUpdate(p,p.getY()==64?Blocks.STONE_BRICKS.defaultBlockState():Blocks.AIR.defaultBlockState());
                for(var entity:l.getEntitiesOfClass(LivingEntity.class,new net.minecraft.world.phys.AABB(-14,64,-8,33,75,19)))if(!(entity instanceof net.minecraft.world.entity.player.Player))entity.discard();
                for(BlockPos p:BlockPos.betweenClosed(-7,64,-2,-3,64,2))l.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());
                l.setBlockAndUpdate(TANK.below(),Blocks.BOOKSHELF.defaultBlockState());var tank=put(l,TANK,"think_tank");tank.local.setStackInSlot(0,new ItemStack(Items.BOOK,64));
                put(l,JAR,"oblivion_jar");put(l,URN,"everfull_urn");l.setBlockAndUpdate(URN.east(2),thaumcraft.registry.ModBlocks.CRUCIBLE.get().defaultBlockState());
                var souls=put(l,SOUL,"crucible_of_souls");souls.flux=0.5F;
                var mob=EntityType.ZOMBIE.create(l);mob.setPos(9.5,65,0.5);mob.setNoAi(true);mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);mob.setHealth(200);mob.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.CARVED_PUMPKIN));l.addFreshEntity(mob);
                var brazier=put(l,BRAZIER,"soul_brazier");brazier.brazierVis=16;brazier.brazierEssentia=16;brazier.brazierActive=true;brazier.syncVisualState();
                var a=put(l,CHEST,"bound_chest");var b=put(l,CHEST.east(2),"bound_chest");a.channel=UUID.randomUUID();b.channel=a.channel;a.color=b.color=DyeColor.BLUE.getId();a.inventory();b.inventory();a.syncVisualState();b.syncVisualState();
                var ghost=put(l,GHOST,"replicator");ghost.template=new ItemStack(Items.STONE);ghost.syncVisualState();
                var sorter=put(l,SORTER,"auto_sorter");l.setBlockAndUpdate(SORTER.east(3),Blocks.CHEST.defaultBlockState());sorter.sorter.link(SORTER.east(3),Direction.SOUTH).mode=2;sorter.syncVisualState();
                for(var item:l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(-14,64,-8,33,75,19)))item.discard();
                var p=server.getPlayerList().getPlayers().getFirst();p.getInventory().clearContent();p.removeAllEffects();p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.connection.teleport(-3,67.5,6,180,18);
            });
        }
        if(tick==20)shot("port8-tank-warming.png");
        if(tick==45)shot("port8-light-tank-and-oblivion.png");
        if(tick==50)camera(3.5,67,5,180,20);
        if(tick==75){var m=(MachineEntity)mc.level.getBlockEntity(URN);if(m.waterDestination==null)throw new IllegalStateException("Urn target not synchronized");shot("port8-water-directed.png");}
        if(tick==80)camera(9,67,5,180,20);
        if(tick==105){var m=(MachineEntity)mc.level.getBlockEntity(SOUL);if(m.soulTarget==null)throw new IllegalStateException("Soul target not synchronized");shot("port8-soul-drain-and-fluid.png");}
        if(tick==110)camera(13.5,67.5,5,180,15);
        if(tick==140)shot("port8-brazier-wisps.png");
        if(tick==145){camera(18.5,67,5,180,18);server.execute(()->((MachineEntity)server.overworld().getBlockEntity(CHEST)).inventory().insertItem(0,new ItemStack(Items.DIAMOND),false));}
        if(tick==170){if(((MachineEntity)mc.level.getBlockEntity(CHEST.east(2))).chestAccessTicks<=0)throw new IllegalStateException("Shared chest pulse missing");shot("port8-chest-access.png");}
        if(tick==175)camera(22.5,67,4,180,20);
        if(tick==200)shot("port8-replicator-ghost.png");
        if(tick==205){camera(27.5,67.5,6,180,20);server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Content.tc("golem_bell")));((MachineEntity)server.overworld().getBlockEntity(SORTER)).sorter.interact(p,p.getMainHandItem());});}
        if(tick==235)shot("port8-sorter-markers.png");
        if(tick==240){mc.options.hideGui=false;server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.setItemInHand(InteractionHand.MAIN_HAND,thaumcraft.wand.Wands.create(thaumcraft.wand.WandRegistry.cap("gold"),thaumcraft.wand.WandRegistry.rod("TRANSMUTATION"),false));});}
        if(tick==250)mc.setScreen(new EffectsPreviewScreen());
        if(tick==270)shot("port8-wand-frame-a.png");
        if(tick==286)shot("port8-wand-frame-b.png");
        if(tick==290)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.setItemInHand(InteractionHand.MAIN_HAND,thaumcraft.wand.Wands.create(thaumcraft.wand.WandRegistry.cap("gold"),thaumcraft.wand.WandRegistry.rod("TRANSMUTATION_staff"),false));});
        if(tick==315)shot("port8-staff-animation.png");
        if(tick==320){mc.setScreen(null);mc.options.hideGui=true;System.setProperty("tx.effectsDate","2026-12-25");camera(18.5,67,5,180,18);}
        if(tick==345)shot("port8-christmas-chest.png");
        if(tick==350){System.clearProperty("tx.effectsDate");mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.FEET,new ItemStack(Content.item("meteor_boots")));p.getAbilities().flying=false;p.onUpdateAbilities();p.connection.teleport(-6,65,10,90,12);});}
        if(tick>=358&&tick<390)mc.options.keyUp.setDown(true);
        if(tick==380)shot("port8-meteor-trail-third-person.png");
        if(tick==390){mc.options.keyUp.setDown(false);server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.setItemSlot(EquipmentSlot.FEET,new ItemStack(Content.item("comet_boots")));p.connection.teleport(2,65,10,-90,12);for(int x=3;x<17;x++)for(int z=8;z<13;z++)server.overworld().setBlockAndUpdate(new BlockPos(x,64,z),Blocks.WATER.defaultBlockState());});}
        if(tick>=400&&tick<430)mc.options.keyUp.setDown(true);
        if(tick==420)shot("port8-comet-trail-and-freeze.png");
        if(tick==430){mc.options.keyUp.setDown(false);server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.setItemSlot(EquipmentSlot.FEET,new ItemStack(Content.item("meteor_boots")));p.connection.teleport(-2,71,10,90,20);});}
        if(tick>=435&&tick<470)mc.options.keyShift.setDown(true);
        if(tick==450)shot("port8-meteor-dive-smoke.png");
        if(tick==475){
            beforeRemoteMeteor=LegacyVisuals.observed.getOrDefault("meteor_trail",0);beforeRemoteComet=LegacyVisuals.observed.getOrDefault("comet_trail",0);
            mc.options.keyShift.setDown(false);mc.options.keyUp.setDown(false);mc.options.setCameraType(CameraType.FIRST_PERSON);camera(3,67,17,180,18);
            server.execute(()->{
                var l=server.overworld();l.setDayTime(1000);var cameraPlayer=server.getPlayerList().getPlayers().getFirst();cameraPlayer.setItemSlot(EquipmentSlot.FEET,ItemStack.EMPTY);cameraPlayer.getAbilities().flying=true;cameraPlayer.onUpdateAbilities();cameraPlayer.setDeltaMovement(0,0,0);cameraPlayer.clearFire();
                var profile=new com.mojang.authlib.GameProfile(UUID.randomUUID(),"TX-Visual-Check");remote=new net.minecraft.server.level.ServerPlayer(server,l,profile,net.minecraft.server.level.ClientInformation.createDefault());
                var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);new io.netty.channel.embedded.EmbeddedChannel(connection);
                new net.minecraft.server.network.ServerGamePacketListenerImpl(server,connection,remote,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)){@Override public void send(net.minecraft.network.protocol.Packet<?> packet){}};
                remote.setPos(0.5,65,12);remote.setNoGravity(true);remote.setItemSlot(EquipmentSlot.FEET,new ItemStack(Content.item("meteor_boots")));
                server.getPlayerList().getPlayers().getFirst().connection.send(new net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket(java.util.EnumSet.of(net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER),java.util.List.of(remote)));
                l.addNewPlayer(remote);
            });
        }
        if(tick>=480&&tick<540)server.execute(()->{if(remote!=null){remote.setDeltaMovement(0.14,0,0);remote.move(MoverType.SELF,new net.minecraft.world.phys.Vec3(0.14,0,0));net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(remote));}});
        if(tick==500){if(remote==null||mc.level.getPlayerByUUID(remote.getUUID())==null)throw new IllegalStateException("Remote boot wearer not tracked by observing client");shot("port8-remote-meteor-observer.png");}
        if(tick==510)server.execute(()->{equipment(server,new ItemStack(Content.item("comet_boots")));remote.setPos(0.5,65,12);});
        if(tick==530){var actor=mc.level.getPlayerByUUID(remote.getUUID());if(actor==null||!actor.getItemBySlot(EquipmentSlot.FEET).is(Content.item("comet_boots")))throw new IllegalStateException("Observer did not receive comet equipment");if(LegacyVisuals.observed.getOrDefault("meteor_trail",0)<=beforeRemoteMeteor||LegacyVisuals.observed.getOrDefault("comet_trail",0)<=beforeRemoteComet)throw new IllegalStateException("Remote player tick did not emit both boot trails");shot("port8-remote-comet-observer.png");System.out.println("TX_PORT8_OBSERVER: tracked remote actor, received equipment update, server tick emitted meteor/comet trails");}
        if(tick==540){server.execute(()->{remote.setDeltaMovement(0,0,0);equipment(server,ItemStack.EMPTY);remote.setPos(3.5,65,0.5);remote.clearFire();});camera(8.5,67,2.5,180,45);}
        if(tick==545)server.execute(()->{var m=(MachineEntity)server.overworld().getBlockEntity(SOUL);m.flux=0.8F;m.syncVisualState();});
        if(tick==550)shot("port8-soul-fluid-close.png");
        if(tick==552)camera(2.5,67,4,180,30);
        if(tick==555)server.execute(()->remote.setRemainingFireTicks(200));
        if(tick==567)shot("port8-water-extinguishing-player.png");
        if(tick==585)server.execute(()->{server.overworld().removePlayerImmediately(remote,Entity.RemovalReason.DISCARDED);remote=null;});
        if(tick==590)shot("port8-water-idle-fountain.png");
        if(tick==595){camera(-4.5,67.5,5,180,25);server.execute(()->server.overworld().setBlockAndUpdate(TANK.west(2),Blocks.STONE.defaultBlockState()));}
        if(tick==615){if(((MachineEntity)mc.level.getBlockEntity(TANK)).space!=0)throw new IllegalStateException("Tight book orbit not synchronized");shot("port8-tank-six-books.png");}
        if(tick==620)server.execute(()->((MachineEntity)server.overworld().getBlockEntity(TANK)).local.setStackInSlot(0,ItemStack.EMPTY));
        if(tick==640)shot("port8-tank-cooling.png");
        if(tick==660){
            mc.options.keyShift.setDown(false);mc.options.keyUp.setDown(false);mc.options.setCameraType(CameraType.FIRST_PERSON);
            for(String key:new String[]{"oblivion_bolts","think_enchant","think_books","think_books_6","think_books_8","water_directed","water_player","water_fountain","water_splash","soul_drain","soul_fluid","brazier_wisps","brazier_bolts","chest_pulse","replicator_ghost","sorter_mark","sorter_line","meteor_trail","meteor_dive","comet_trail","comet_freeze"})if(LegacyVisuals.observed.getOrDefault(key,0)==0)throw new IllegalStateException("Effect not exercised: "+key+" "+LegacyVisuals.observed);
            if(LegacyVisuals.observed.keySet().stream().filter(k->k.startsWith("wand_frame_")).count()<3)throw new IllegalStateException("Assembled wand texture remained static "+LegacyVisuals.observed);
            System.out.println("TX_PORT8_EFFECTS_SMOKE: "+new TreeMap<>(LegacyVisuals.observed));done=true;mc.stop();
        }
    }
    static void shot(String name){var mc=Minecraft.getInstance();Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),m->System.out.println("TX_PORT8_SCREENSHOT: "+name));}
}
