package thaumicexploration;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.world.item.ItemStack;
/** Registry aliases preserve old saves; loaded blocks and acquired items become core candles. */
public final class CandleMigration {
    public static void loaded(ChunkEvent.Load e){if(e.getChunk() instanceof LevelChunk c&&e.getLevel() instanceof ServerLevel l)l.getServer().execute(()->repair(c));}
    static void repair(LevelChunk chunk){
        if(!(chunk.getLevel() instanceof ServerLevel level))return;
        for(int index=0;index<chunk.getSections().length;index++){
            var section=chunk.getSections()[index];if(!section.maybeHas(s->s.getBlock() instanceof FloatingCandleBlock))continue;
            int bottom=level.getSectionYFromSectionIndex(index)<<4;
            for(int y=0;y<16;y++)for(int x=0;x<16;x++)for(int z=0;z<16;z++)if(section.getBlockState(x,y,z).getBlock() instanceof FloatingCandleBlock){
                var pos=new BlockPos(chunk.getPos().getMinBlockX()+x,bottom+y,chunk.getPos().getMinBlockZ()+z);
                var id=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(section.getBlockState(x,y,z).getBlock());
                var color=id.getPath().replace("_floating_candle","");
                level.setBlockAndUpdate(pos,thaumcraft.modern.ModernContent.CANDLES.get(color).get().defaultBlockState());
            }
        }
    }
    public static void inventory(PlayerTickEvent.Post e){
        var p=e.getEntity();if(p.level().isClientSide()||p.tickCount%20!=0)return;
        boolean changed=false;
        for(int i=0;i<p.getInventory().getContainerSize();i++){
            var old=p.getInventory().getItem(i);var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(old.getItem());
            if(id.getNamespace().equals(ThaumicExploration.ID)&&id.getPath().endsWith("_floating_candle")){
                var item=thaumcraft.modern.ModernContent.CANDLES.get(id.getPath().replace("_floating_candle","")).get().asItem();
                p.getInventory().setItem(i,old.transmuteCopy(item));changed=true;
            }
        }
        if(changed)p.getInventory().setChanged();
    }
}
