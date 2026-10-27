package thaumicexploration;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Legacy boots ice melts on its first random tick, independently of light. */
public final class MeltyIceBlock extends IceBlock {
    public MeltyIceBlock(Properties p){super(p);}
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){melt(state,level,pos);}
}
