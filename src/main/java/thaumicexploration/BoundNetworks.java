package thaumicexploration;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.items.ItemStackHandler;
import thaumcraft.aspect.*;

/** Each randomly generated seal channel owns its storage in the overworld SavedData. */
public final class BoundNetworks extends SavedData {
    public final Map<UUID, Network> networks=new HashMap<>();
    public final Map<String,UUID> names=new HashMap<>();
    public UUID named(String kind,String name){return names.computeIfAbsent(kind+":"+name,k->{setDirty();return UUID.randomUUID();});}
    public static BoundNetworks get(ServerLevel level){return level.getServer().overworld().getDataStorage().computeIfAbsent(new Factory<>(BoundNetworks::new,BoundNetworks::load,null),"thaumicexploration_networks");}
    public Network network(UUID id){return networks.computeIfAbsent(id,k->{setDirty();return new Network(this);});}
    public static final class Network {
        private final BoundNetworks owner;
        private final Set<MachineEntity> viewers=Collections.newSetFromMap(new WeakHashMap<>());
        public long revision;
        void watch(MachineEntity jar){viewers.add(jar);}
        private void changed(){
            owner.setDirty();revision++;
            for(var jar:new ArrayList<>(viewers)){
                if(owner.networks.get(jar.channel)!=this||jar.isRemoved()||!(jar.getLevel() instanceof ServerLevel level)||!level.hasChunkAt(jar.getBlockPos())||level.getBlockEntity(jar.getBlockPos())!=jar){viewers.remove(jar);continue;}
                if(jar.kind().equals("bound_chest"))jar.chestAccessTicks=80;else jar.jarAccessTicks=80;jar.setChanged();
                level.sendBlockUpdated(jar.getBlockPos(),jar.getBlockState(),jar.getBlockState(),2);
            }
        }
        public final ItemStackHandler items;
        public Aspect aspect;
        public int amount;
        Network(BoundNetworks owner){this.owner=owner;items=new ItemStackHandler(27){@Override protected void onContentsChanged(int slot){changed();}};}
        public int add(Aspect type,int count){if(type==null||count<=0 || (amount>0 && aspect!=type))return 0;int accepted=Math.min(64-amount,count);if(accepted>0){aspect=type;amount+=accepted;changed();}return accepted;}
        public int take(Aspect type,int count){if(count<=0 || aspect!=type)return 0;int taken=Math.min(amount,count);amount-=taken;if(amount==0)aspect=null;if(taken>0)changed();return taken;}
    }
    private static BoundNetworks load(CompoundTag tag,HolderLookup.Provider provider){
        var data=new BoundNetworks();
        var names=tag.getCompound("Names");for(String key:names.getAllKeys())if(names.hasUUID(key))data.names.put(key,names.getUUID(key));
        for(Tag raw:tag.getList("Networks",Tag.TAG_COMPOUND)){
            var t=(CompoundTag)raw;if(!t.hasUUID("Id"))continue;
            var n=new Network(data);n.items.deserializeNBT(provider,t.getCompound("Items"));
            n.amount=Math.clamp(t.getInt("Amount"),0,64);n.aspect=Aspect.getAspect(t.getString("Aspect"));if(n.aspect==null)n.amount=0;
            data.networks.put(t.getUUID("Id"),n);
        }
        return data;
    }
    static BoundNetworks restore(CompoundTag tag,HolderLookup.Provider provider){return load(tag,provider);}
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider provider){
        var named=new CompoundTag();names.forEach(named::putUUID);tag.put("Names",named);
        var list=new ListTag();networks.forEach((id,n)->{var t=new CompoundTag();t.putUUID("Id",id);t.put("Items",n.items.serializeNBT(provider));t.putInt("Amount",n.amount);if(n.aspect!=null)t.putString("Aspect",n.aspect.getTag());list.add(t);});tag.put("Networks",list);return tag;
    }
}
