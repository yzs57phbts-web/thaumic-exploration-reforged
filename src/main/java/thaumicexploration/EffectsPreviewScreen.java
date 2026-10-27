package thaumicexploration;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import thaumcraft.wand.*;

/** Enlarged real GUI item rendering for the opt-in visual test only. */
final class EffectsPreviewScreen extends Screen {
    EffectsPreviewScreen(){super(Component.literal("Проверка анимации"));}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void render(GuiGraphics g,int x,int y,float partial){
        g.fill(0,0,width,height,0xE6101020);
        var wand=Wands.create(WandRegistry.cap("gold"),WandRegistry.rod("TRANSMUTATION"),false);
        var staff=Wands.create(WandRegistry.cap("gold"),WandRegistry.rod("TRANSMUTATION_staff"),false);
        g.drawCenteredString(font,wand.getHoverName(),width/4,45,0xFFFFFF);g.drawCenteredString(font,staff.getHoverName(),width*3/4,45,0xFFFFFF);
        g.pose().pushPose();g.pose().scale(9,9,1);g.renderItem(wand,width/36-8,height/18-8);g.renderItem(staff,width/12-8,height/18-8);g.pose().popPose();
    }
}
