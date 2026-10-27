package thaumicexploration;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public final class ThinkTankScreen extends AbstractContainerScreen<ThinkTankMenu> {
    public ThinkTankScreen(ThinkTankMenu m,Inventory i,Component title){super(m,i,title);imageWidth=176;imageHeight=166;}
    @Override protected void renderBg(GuiGraphics g,float partial,int x,int y){
        var texture=ThaumicExploration.id("textures/gui/thinktank.png");
        g.blit(texture,leftPos,topPos,0,0,176,166,256,256);
        int progress=menu.progress();if(progress>0)g.blit(texture,leftPos+91,topPos+24,198,11,32,progress,256,256);
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y){g.drawCenteredString(font,title,imageWidth/2,-12,0xFFF1D5);}
    @Override public void render(GuiGraphics g,int x,int y,float partial){super.render(g,x,y,partial);renderTooltip(g,x,y);}
}
