package thaumicexploration;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.*;
import net.minecraft.world.level.saveddata.SavedData;
/** Returns are persisted even when the owner is offline or the brazier is broken. */
public final class WarpReturns extends SavedData {
    private final Map<UUID,Integer> pending=new HashMap<>();
    public static WarpReturns get(ServerLevel l){return l.getServer().overworld().getDataStorage().computeIfAbsent(new Factory<>(WarpReturns::new,WarpReturns::load,null),"thaumicexploration_warp_returns");}
    public void add(UUID owner,int n){if(n>0){pending.merge(owner,n,Integer::sum);setDirty();}}
    public void deliver(ServerPlayer p){if(p==null)return;Integer n=pending.remove(p.getUUID());if(n!=null){thaumcraft.research.Warp.set(p,thaumcraft.research.Warp.get(p,thaumcraft.research.WarpType.PERMANENT)+n,thaumcraft.research.WarpType.PERMANENT);setDirty();p.displayClientMessage(net.minecraft.network.chat.Component.translatable("tx.brazier.returned",n),false);}}
    public static WarpReturns load(CompoundTag t,HolderLookup.Provider p){var d=new WarpReturns();for(Tag raw:t.getList("Pending",Tag.TAG_COMPOUND)){var v=(CompoundTag)raw;if(v.hasUUID("Owner")&&v.getInt("Warp")>0)d.pending.put(v.getUUID("Owner"),v.getInt("Warp"));}return d;}
    @Override public CompoundTag save(CompoundTag t,HolderLookup.Provider p){var list=new ListTag();pending.forEach((id,n)->{var v=new CompoundTag();v.putUUID("Owner",id);v.putInt("Warp",n);list.add(v);});t.put("Pending",list);return t;}
}
