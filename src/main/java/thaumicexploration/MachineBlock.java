package thaumicexploration;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class MachineBlock extends BaseEntityBlock {
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
    public final String kind;
    public MachineBlock(Properties p,String kind){super(p);this.kind=kind;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return rotate(s,m.getRotation(s.getValue(FACING)));}
    @Override public void setPlacedBy(Level l,BlockPos pos,BlockState s,net.minecraft.world.entity.LivingEntity placer,ItemStack stack){
        super.setPlacedBy(l,pos,s,placer,stack);
        if(l.getBlockEntity(pos) instanceof MachineEntity m){
            if(kind.equals("soul_brazier")&&placer!=null)m.brazierOwner=placer.getUUID();
            var data=Data.get(stack);if((kind.equals("bound_chest")||kind.equals("bound_jar"))&&data.hasUUID("Channel")){m.channel=data.getUUID("Channel");m.color=data.getInt("Color");l.invalidateCapabilities(pos);}
            if(kind.equals("bound_jar")&&m.channel==null&&!l.isClientSide())m.channel=java.util.UUID.randomUUID();
            m.setChanged();if(!l.isClientSide())l.sendBlockUpdated(pos,s,s,3);
        }
    }
    @Override protected java.util.List<ItemStack> getDrops(BlockState s,net.minecraft.world.level.storage.loot.LootParams.Builder params){
        var drops=super.getDrops(s,params);
        if((kind.equals("bound_chest")||kind.equals("bound_jar"))&&params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY) instanceof MachineEntity m&&m.channel!=null){
            for(var drop:drops)if(drop.is(asItem())){var data=Data.get(drop);data.putUUID("Channel",m.channel);data.putInt("Color",m.color);Data.put(drop,data);drop.set(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE,1);}
        }
        return drops;
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(p->new MachineBlock(p,kind));}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState s){return new MachineEntity(pos,s);}
    @Override protected RenderShape getRenderShape(BlockState s){return kind.equals("bound_chest")?RenderShape.INVISIBLE:RenderShape.MODEL;}
    @Override public void animateTick(BlockState s,Level l,BlockPos p,net.minecraft.util.RandomSource r){
        if(kind.equals("everfull_urn")){for(int i=0;i<6;i++)l.addParticle(net.minecraft.core.particles.ParticleTypes.SPLASH,p.getX()+0.5,p.getY()+1,p.getZ()+0.5,0,1,0);LegacyVisuals.note("water_splash",6);}
    }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
        return switch(kind){case "bound_chest"->box(1,0,1,15,15,15);case "bound_jar","oblivion_jar"->box(3,0,3,13,12,13);case "think_tank","replicator"->Shapes.block();case "soul_brazier"->Shapes.or(box(3,0,3,13,12,13),box(6,8,1,10,16,3),box(6,8,13,10,16,15),box(1,8,6,3,16,10),box(13,8,6,15,16,10));case "everfull_urn","everburn_urn"->Shapes.or(box(2,0,2,14,9,14),box(5,9,5,11,13,11),box(4,13,4,12,16,12));case "crucible_of_souls"->Shapes.or(box(0,0,0,16,4,16),box(0,0,0,2,16,16),box(14,0,0,16,16,16),box(2,0,0,14,16,2),box(2,0,14,14,16,16));default->box(0,0,0,16,14,16);};
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return createTickerHelper(type,Content.MACHINE.get(),MachineEntity::tick);}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player p,BlockHitResult hit){if(!l.isClientSide() && l.getBlockEntity(pos) instanceof MachineEntity m)m.interact(p,InteractionHand.MAIN_HAND);return InteractionResult.sidedSuccess(l.isClientSide());}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){if(!l.isClientSide() && l.getBlockEntity(pos) instanceof MachineEntity m)m.interact(p,hand);return ItemInteractionResult.sidedSuccess(l.isClientSide());}
    @Override protected void onRemove(BlockState old,Level level,BlockPos pos,BlockState next,boolean moved){
        if(!level.isClientSide()&&!old.is(next.getBlock()) && level.getBlockEntity(pos) instanceof MachineEntity m){
            if(m.kind().equals("soul_brazier"))m.returnWarp();
            // Shared inventories belong to SavedData; dropping them here would duplicate every linked chest.
            if(!m.kind().equals("bound_chest")||m.channel==null)for(int i=0;i<m.local.getSlots();i++)Containers.dropItemStack(level,pos.getX(),pos.getY(),pos.getZ(),m.local.getStackInSlot(i));
            level.updateNeighbourForOutputSignal(pos,this);
        }
        super.onRemove(old,level,pos,next,moved);
    }
}
