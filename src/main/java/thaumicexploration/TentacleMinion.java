package thaumicexploration;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
public final class TentacleMinion extends PathfinderMob {
    private UUID owner,target;
    private int lifetime=200;
    public TentacleMinion(EntityType<? extends TentacleMinion> t,Level l){super(t,l);xpReward=0;}
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound(){return thaumcraft.registry.ModSounds.ROOTS.get();}
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source){return thaumcraft.registry.ModSounds.TENTACLE.get();}
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound(){return thaumcraft.registry.ModSounds.TENTACLE.get();}
    @Override protected float getSoundVolume(){return getBbHeight()/8;}
    @Override public float getVoicePitch(){return 1.3F-getBbHeight()/10;}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,8).add(Attributes.ATTACK_DAMAGE,2).add(Attributes.MOVEMENT_SPEED,0);}
    public static void spawn(Player p,LivingEntity enemy){
        var spots=new java.util.ArrayList<net.minecraft.core.BlockPos>();
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){var pos=p.blockPosition().offset(x,0,z);if(p.level().isEmptyBlock(pos)&&p.level().getBlockState(pos.below()).isFaceSturdy(p.level(),pos.below(),net.minecraft.core.Direction.UP))spots.add(pos);}
        if(spots.isEmpty())return;var pos=spots.get(p.getRandom().nextInt(spots.size()));
        var minion=Content.MINION.get().create(p.level());if(minion==null)return;
        minion.owner=p.getUUID();minion.moveTo(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5,0,0);p.level().addFreshEntity(minion);minion.playSound(thaumcraft.registry.ModSounds.TENTACLE.get(),0.125F,1.2F);
    }
    @Override public void tick(){
        super.tick();setDeltaMovement(0,getDeltaMovement().y,0);
        if(level() instanceof ServerLevel s){
            if(--lifetime<=0){hurt(damageSources().magic(),10);return;}
            if(tickCount%20==0&&!TaintCharm.inTaint(this))hurt(damageSources().starve(),1);
            var master=owner==null?null:s.getEntity(owner);var victim=target==null?null:s.getEntity(target);
            if(!(victim instanceof LivingEntity living)||!living.isAlive()||distanceToSqr(living)>49){
                var candidates=s.getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class,getBoundingBox().inflate(16,8,16),m->m.isAlive()&&m!=master);
                candidates.sort(java.util.Comparator.comparingDouble(this::distanceToSqr));victim=candidates.isEmpty()?null:candidates.getFirst();target=victim==null?null:victim.getUUID();
            }
            if(!(victim instanceof LivingEntity enemy)||!enemy.isAlive()||enemy==master)return;
            if(master instanceof LivingEntity m&&m.isAlliedTo(enemy))return;
            getLookControl().setLookAt(enemy,30,30);
            if(tickCount>10&&tickCount%20==0&&distanceToSqr(enemy)<=1&&hasLineOfSight(enemy)&&getBoundingBox().maxY>enemy.getBoundingBox().minY&&getBoundingBox().minY<enemy.getBoundingBox().maxY)enemy.hurt(damageSources().mobAttack(this),2);
        }
    }
    @Override public void addAdditionalSaveData(CompoundTag t){super.addAdditionalSaveData(t);if(owner!=null)t.putUUID("Owner",owner);if(target!=null)t.putUUID("Target",target);t.putInt("Life",lifetime);}
    @Override public void readAdditionalSaveData(CompoundTag t){super.readAdditionalSaveData(t);owner=t.hasUUID("Owner")?t.getUUID("Owner"):null;target=t.hasUUID("Target")?t.getUUID("Target"):null;lifetime=t.getInt("Life");}
    @Override protected void dropCustomDeathLoot(ServerLevel l,net.minecraft.world.damagesource.DamageSource d,boolean hit){}
}
