package thaumicexploration;

import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.*;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import thaumcraft.platform.Baubles;

public final class Events {
    public static void init(){
        NeoForge.EVENT_BUS.addListener(CandleMigration::loaded);NeoForge.EVENT_BUS.addListener(CandleMigration::inventory);NeoForge.EVENT_BUS.addListener(SorterLinks::click);
        NeoForge.EVENT_BUS.addListener(Events::playerTick);NeoForge.EVENT_BUS.addListener(Events::jump);NeoForge.EVENT_BUS.addListener(Events::fall);
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Pre e)->ElementalMovement.tick(e.getEntity(),e.getEntity() instanceof net.minecraft.server.level.ServerPlayer?e.getEntity().getPersistentData().getBoolean("tx.forward"):e.getEntity().zza>0));
        NeoForge.EVENT_BUS.addListener(Events::incoming);NeoForge.EVENT_BUS.addListener(Events::drops);NeoForge.EVENT_BUS.addListener(Events::target);NeoForge.EVENT_BUS.addListener(Events::livingTick);NeoForge.EVENT_BUS.addListener(Events::teleport);
    }
    static boolean wearing(Player p,String name){return Config.enabled(name)&&Baubles.isEquipped(p,Content.item(name));}
    static int enchant(LivingEntity e,ItemStack stack,String key){var holder=e.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT,ThaumicExploration.id(key)));return holder.isPresent()?stack.getEnchantmentLevel(holder.get()):0;}
    private static void playerTick(PlayerTickEvent.Post event){
        Player p=event.getEntity();if(p.level().isClientSide())return;
        if(p.tickCount%20==0 && Research.syncProxies(thaumcraft.research.Knowledge.get(p)))thaumcraft.research.Knowledge.sync(p);
        if(p instanceof net.minecraft.server.level.ServerPlayer sp&&p.tickCount%20==0)WarpReturns.get(sp.serverLevel()).deliver(sp);
        Magic.caps(p);
        var boots=p.getItemBySlot(EquipmentSlot.FEET);String type=boots.getItem() instanceof ElementalBoots b?b.kind:"";
        var resistance=p.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        var beltId=ThaumicExploration.id("stabilizer_belt");
        if(resistance!=null){if(wearing(p,"stabilizer_belt")){if(!resistance.hasModifier(beltId))resistance.addTransientModifier(new AttributeModifier(beltId,1,AttributeModifier.Operation.ADD_VALUE));}else resistance.removeModifier(beltId);}
        if(type.equals("meteor_boots")){
            var data=Data.get(boots);int ticks=data.getInt("smashTicks"),air=data.getInt("airTicks");boolean smash=data.getBoolean("IsSmashing");
            if(p.onGround()||p.onClimbable()){
                int size=ticks>15?3:ticks>10?2:ticks>5?1:0;
                if(size>0)p.level().explode(p,p.getX(),p.getY(),p.getZ(),size,net.minecraft.world.level.Level.ExplosionInteraction.NONE);
                ticks=0;air=0;smash=false;
            }else{air++;if(p.isShiftKeyDown()&&air>5)smash=true;}
            if(p.getAbilities().flying){ticks=0;air=0;smash=false;}
            if(smash){ticks++;p.setDeltaMovement(p.getDeltaMovement().add(0,-0.1,0));p.hurtMarked=true;}
            data.putInt("smashTicks",ticks);data.putInt("airTicks",air);data.putBoolean("IsSmashing",smash);Data.put(boots,data);
        }
        if(type.equals("comet_boots")&&!p.isInWater())for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)if(Math.abs(x)+Math.abs(z)<8){
            BlockPos pos=p.blockPosition().offset(x,-1,z);var state=p.level().getBlockState(pos);
            if(state.is(Blocks.WATER)&&state.getValue(BlockStateProperties.LEVEL)==0&&p.level().isEmptyBlock(pos.above())&&p.level().mayInteract(p,pos)){LegacyVisuals.note("comet_freeze",1);p.level().setBlockAndUpdate(pos,Content.MELTY_ICE.get().defaultBlockState());((net.minecraft.server.level.ServerLevel)p.level()).sendParticles(net.minecraft.core.particles.ParticleTypes.ITEM_SNOWBALL,pos.getX(),pos.getY()+1,pos.getZ(),0,0,0.025,0,1);}
        }
        LegacyVisuals.boots(p,type.equals("meteor_boots")&&Data.get(boots).getBoolean("IsSmashing"));
        nightVision(p);TaintCharm.tick(p);
    }
    static void nightVision(LivingEntity living){
        if(!living.level().isClientSide()&&enchant(living,living.getItemBySlot(EquipmentSlot.HEAD),"night_vision")>0){var effect=living.getEffect(MobEffects.NIGHT_VISION);if(effect==null||effect.getDuration()<202)living.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,202,1,false,false));}
    }

    private static void jump(LivingEvent.LivingJumpEvent event){
        if(event.getEntity() instanceof Player p&&p.getItemBySlot(EquipmentSlot.FEET).is(Content.item("meteor_boots"))&&p.isShiftKeyDown()){
            var look=p.getLookAngle();double ascent=enchantId(p,p.getItemBySlot(EquipmentSlot.FEET),"thaumictinkerer:ascent_boost");double factor=1+(ascent>=1?(ascent+2)/4:ascent);double up=factor*Math.max(look.y,Math.abs(look.z+look.x))/1.5;p.setDeltaMovement(p.getDeltaMovement().add(factor*look.x*4,up,factor*look.z*4));p.hurtMarked=true;p.getPersistentData().putBoolean("tx.launched",true);
        }
        else if(event.getEntity() instanceof Player p&&p.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof ElementalBoots)p.setDeltaMovement(p.getDeltaMovement().add(0,0.275,0));
    }
    static int enchantId(LivingEntity e,ItemStack stack,String id){var holder=e.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT,net.minecraft.resources.ResourceLocation.parse(id)));return holder.isPresent()?stack.getEnchantmentLevel(holder.get()):0;}
    private static void fall(LivingFallEvent event){if(event.getEntity() instanceof Player p&&p.getItemBySlot(EquipmentSlot.FEET).is(Content.item("meteor_boots")))event.setDistance(0);}
    static boolean hasHotbar(Player p,String item){for(int i=0;i<=9;i++)if(p.getInventory().getItem(i).is(Content.item(item)))return true;return false;}
    private static void incoming(LivingIncomingDamageEvent event){
        attack(event);
        if(event.getEntity() instanceof Player p){var source=event.getSource();boolean taint=source.is(thaumcraft.damage.ModDamageTypes.TAINT)||source.is(thaumcraft.damage.ModDamageTypes.TENTACLE)||source.is(thaumcraft.damage.ModDamageTypes.SWARM)||(source.getDirectEntity()==source.getEntity()&&source.getEntity() instanceof thaumcraft.entity.TaintedMob);
            if(Config.enabled("dreamcatcher")&&taint&&hasHotbar(p,"dreamcatcher")&&p.getRandom().nextInt(4)<3)event.setCanceled(true);
        }
    }
    private static void drops(LivingDropsEvent event){if(event.getEntity().getPersistentData().contains("tx.soulDeath")&&event.getEntity().getPersistentData().getLong("tx.soulDeath")==event.getEntity().level().getGameTime())event.setCanceled(true);}
    private static void target(LivingChangeTargetEvent event){if(event.getEntity() instanceof thaumcraft.entity.TaintedMob&&event.getNewAboutToBeSetTarget() instanceof Player p&&hasHotbar(p,"taint_charm"))event.setNewAboutToBeSetTarget(null);}
    private static void attack(LivingIncomingDamageEvent event){
        var victim=event.getEntity();if(victim.level().isClientSide())return;
        if(event.getSource().getEntity() instanceof LivingEntity attacker){
            if(thaumcraft.wand.Wands.isWand(attacker.getMainHandItem())&&thaumcraft.wand.Wands.getRod(attacker.getMainHandItem()).tag().startsWith("NECROMANCER"))victim.addEffect(new MobEffectInstance(MobEffects.WITHER,100,0));
            int binding=enchant(attacker,attacker.getMainHandItem(),"binding");
            if(binding>0){victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,50,1));if(binding>1&&(victim instanceof Player||victim instanceof net.minecraft.world.entity.monster.EnderMan||victim instanceof net.minecraft.world.entity.monster.Creeper))victim.addEffect(new MobEffectInstance(Content.BINDING,50,1));}
            int disarm=enchant(attacker,attacker.getMainHandItem(),"disarming");
            if(disarm>0 && !(victim instanceof Player) && !victim.getMainHandItem().isEmpty()&&victim.getRandom().nextInt(Math.max(1,10-2*disarm))==0){var weapon=victim.getMainHandItem();victim.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);victim.spawnAtLocation(weapon);}
        }
        if(victim instanceof Player p&&wearing(p,"tentacle_ring")&&p.onGround())TentacleMinion.spawn(p,null);
    }
    private static void livingTick(EntityTickEvent.Post e){
        if(e.getEntity() instanceof LivingEntity living)nightVision(living);
        if(e.getEntity() instanceof net.minecraft.world.entity.monster.Creeper c && bound(c))c.setSwellDir(-1);
    }
    static boolean bound(Entity entity){return (entity instanceof LivingEntity living&&living.hasEffect(Content.BINDING))||entity.getPersistentData().getLong("tx.boundUntil")>entity.level().getGameTime();}
    private static void teleport(EntityTeleportEvent event){if((event instanceof EntityTeleportEvent.EnderEntity||event instanceof EntityTeleportEvent.EnderPearl)&&bound(event.getEntity()))event.setCanceled(true);}
}

