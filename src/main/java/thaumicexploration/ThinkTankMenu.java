package thaumicexploration;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

public final class ThinkTankMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;
    public ThinkTankMenu(int id,Inventory inventory){this(id,inventory,new SimpleContainer(2),new SimpleContainerData(2));}
    public ThinkTankMenu(int id,Inventory inventory,Container container,ContainerData data){
        super(Content.THINK_MENU.get(),id);this.container=container;this.data=data;
        addSlot(new Slot(container,0,64,16){@Override public boolean mayPlace(ItemStack s){return s.is(Items.BOOK)||s.is(Items.ENCHANTED_BOOK);}});
        addSlot(new Slot(container,1,64,48){@Override public boolean mayPlace(ItemStack s){return false;}});
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inventory,x+y*9+9,8+x*18,84+y*18));
        for(int x=0;x<9;x++)addSlot(new Slot(inventory,x,8+x*18,142));
        addDataSlots(data);
    }
    public int progress(){return data.get(0)*28/Math.max(1,data.get(1));}
    @Override public boolean stillValid(Player p){return container.stillValid(p);}
    @Override public ItemStack quickMoveStack(Player p,int index){
        var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var source=slot.getItem();var copy=source.copy();
        if(index<2){if(!moveItemStackTo(source,2,38,true))return ItemStack.EMPTY;}
        else if(source.is(Items.BOOK)||source.is(Items.ENCHANTED_BOOK)){if(!moveItemStackTo(source,0,1,false))return ItemStack.EMPTY;}
        else if(index<29){if(!moveItemStackTo(source,29,38,false))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(source,2,29,false))return ItemStack.EMPTY;
        if(source.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,source);return copy;
    }
}
