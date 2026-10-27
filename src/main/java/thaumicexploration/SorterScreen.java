package thaumicexploration;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public final class SorterScreen extends AbstractContainerScreen<SorterMenu> {
    public SorterScreen(SorterMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=176;imageHeight=166;}
    @Override protected void init(){super.init();for(int i=0;i<3;i++){final int mode=i;addRenderableWidget(Button.builder(Component.translatable("tx.sorter.mode."+i),button->{minecraft.gameMode.handleInventoryButtonClick(menu.containerId,mode);}).bounds(leftPos+8+i*54,topPos+9,52,20).build());}}
    @Override protected void renderBg(GuiGraphics g,float partial,int x,int y){g.blit(ThaumicExploration.id("textures/gui/brains.png"),leftPos,topPos,0,0,176,166,256,256);g.fill(leftPos+8+menu.mode()*54,topPos+30,leftPos+60+menu.mode()*54,topPos+32,0xFFB4FF65);}
    @Override protected void renderLabels(GuiGraphics g,int x,int y){g.drawCenteredString(font,title,88,-12,0xFFF1D5);}
    @Override public void render(GuiGraphics g,int x,int y,float partial){super.render(g,x,y,partial);renderTooltip(g,x,y);}
}
