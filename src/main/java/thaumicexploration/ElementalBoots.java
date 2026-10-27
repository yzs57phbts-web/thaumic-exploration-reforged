package thaumicexploration;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import thaumcraft.platform.*;
import thaumcraft.item.gear.Repairable;
public final class ElementalBoots extends ArmorItem implements Repairable {
    public final String kind;
    public ElementalBoots(String kind,Properties p){super(Registration.armorMaterial(Content.bootMaterial(kind)),Type.BOOTS,p);this.kind=kind;}
    @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> text,TooltipFlag f){text.add(Component.translatable("tx.tooltip."+kind));}
}
