package thaumicexploration;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import java.util.function.BiPredicate;
public final class HandlerContainer implements Container {
    private final IItemHandlerModifiable handler;
    private final BiPredicate<Integer,ItemStack> accepts;
    private final java.util.function.BooleanSupplier valid;
    private final Runnable changed;
    public java.util.function.Consumer<Boolean> viewers=b->{};
    @Override public void startOpen(Player p){viewers.accept(true);}
    @Override public void stopOpen(Player p){viewers.accept(false);}
    public HandlerContainer(IItemHandlerModifiable h,BiPredicate<Integer,ItemStack> accepts,java.util.function.BooleanSupplier valid,Runnable changed){handler=h;this.accepts=accepts;this.valid=valid;this.changed=changed;}
    @Override public int getContainerSize(){return handler.getSlots();}
    @Override public boolean isEmpty(){for(int i=0;i<getContainerSize();i++)if(!getItem(i).isEmpty())return false;return true;}
    @Override public ItemStack getItem(int i){return handler.getStackInSlot(i);}
    @Override public ItemStack removeItem(int i,int n){return handler.extractItem(i,n,false);}
    @Override public ItemStack removeItemNoUpdate(int i){var s=getItem(i);handler.setStackInSlot(i,ItemStack.EMPTY);return s;}
    @Override public void setItem(int i,ItemStack stack){handler.setStackInSlot(i,stack);}
    @Override public void setChanged(){changed.run();}
    @Override public boolean stillValid(Player p){return valid.getAsBoolean();}
    @Override public boolean canPlaceItem(int i,ItemStack s){return accepts.test(i,s);}
    @Override public void clearContent(){for(int i=0;i<getContainerSize();i++)handler.setStackInSlot(i,ItemStack.EMPTY);}
}
