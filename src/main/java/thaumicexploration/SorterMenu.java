package thaumicexploration;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
public final class SorterMenu extends AbstractContainerMenu {
    private final Container filters;private final ContainerData data;
    public SorterMenu(int id,Inventory inv){this(id,inv,new SimpleContainer(18),new SimpleContainerData(1));}
    public SorterMenu(int id,Inventory inv,Container filters,ContainerData data){
        super(Content.SORTER_MENU.get(),id);this.filters=filters;this.data=data;
        for(int y=0;y<2;y++)for(int x=0;x<9;x++)addSlot(new Slot(filters,y*9+x,8+18*x,36+18*y){@Override public boolean mayPickup(Player p){return false;}@Override public boolean mayPlace(ItemStack s){return false;}});
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inv,x+y*9+9,8+x*18,84+y*18));
        for(int x=0;x<9;x++)addSlot(new Slot(inv,x,8+x*18,142));addDataSlots(data);
    }
    public int mode(){return data.get(0);}
    @Override public boolean clickMenuButton(Player p,int button){if(button<0||button>2||!stillValid(p))return false;data.set(0,button);broadcastChanges();return true;}
    @Override public void clicked(int slot,int button,ClickType type,Player p){if(slot>=0&&slot<18){if(type==ClickType.PICKUP||type==ClickType.QUICK_MOVE){filters.setItem(slot,button==1?ItemStack.EMPTY:getCarried().copyWithCount(1));filters.setChanged();broadcastChanges();}return;}super.clicked(slot,button,type,p);}
    @Override public ItemStack quickMoveStack(Player p,int index){return ItemStack.EMPTY;}
    @Override public boolean stillValid(Player p){return filters.stillValid(p);}
}
