package thaumicexploration;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import thaumcraft.platform.*;
import thaumcraft.aspect.Aspect;
import thaumcraft.wand.VisDiscountGear;

public final class Trinket extends Item implements Bauble, VisDiscountGear {
    public final String kind;
    public Trinket(String kind,Properties p){super(p);this.kind=kind;}
    @Override public BaubleType getBaubleType(ItemStack s){return kind.equals("stabilizer_belt")?BaubleType.BELT:kind.equals("talisman_of_nourishment")||kind.equals("dreamcatcher")?BaubleType.AMULET:BaubleType.RING;}
    @Override public int getVisDiscount(ItemStack s,Player p,Aspect a){return Config.enabled("rings")&&kind.equals(a.name().toLowerCase(java.util.Locale.ROOT)+"_ring")?3:0;}
    @Override public boolean canEquip(ItemStack s,LivingEntity p){return !kind.equals("dreamcatcher")&&!kind.equals("talisman_of_nourishment");}
    @Override public void onWornTick(ItemStack s,LivingEntity wearer){if(wearer instanceof Player p)active(s,p);}
    @Override public void inventoryTick(ItemStack s,Level l,Entity e,int slot,boolean selected){if(e instanceof Player p && kind.equals("talisman_of_nourishment"))active(s,p);}
    public void active(ItemStack stack,Player p){
        if(!Config.enabled(kind)||p.level().isClientSide()||!kind.equals("talisman_of_nourishment")||p.tickCount%20!=0)return;
        var t=Data.get(stack);long now=p.level().getGameTime();if(t.getLong("LastMealTick")==now)return;t.putLong("LastMealTick",now);
        int stored=t.getInt("Food");float sat=t.getFloat("Saturation");
        for(int i=0;i<=9 && stored<100;i++){
            var food=p.getInventory().getItem(i);var properties=food.getFoodProperties(p);
            if(properties==null || !properties.effects().isEmpty() || properties.nutrition()<=0 || stored+properties.nutrition()>=100 || net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(food.getItem()).getPath().matches(".*(brain|bean).*"))continue;
            stored+=properties.nutrition();sat=Math.min(100,sat+properties.saturation()/properties.nutrition());
            var remainder=food.getCraftingRemainingItem();food.shrink(1);if(!remainder.isEmpty()&&!p.addItem(remainder))p.drop(remainder,false);
            p.level().playSound(null,p.getX(),p.getY(),p.getZ(),net.minecraft.sounds.SoundEvents.GENERIC_EAT,net.minecraft.sounds.SoundSource.PLAYERS,0.5F+0.5F*p.getRandom().nextInt(2),(p.getRandom().nextFloat()-p.getRandom().nextFloat())*0.2F+1);
        }
        int needed=Math.min(stored,20-p.getFoodData().getFoodLevel());
        if(needed>0){p.getFoodData().setFoodLevel(p.getFoodData().getFoodLevel()+needed);stored-=needed;}
        float added=Math.min(sat,p.getFoodData().getFoodLevel()-p.getFoodData().getSaturationLevel());
        if(added>0){p.getFoodData().setSaturation(p.getFoodData().getSaturationLevel()+added);sat-=added;}
        t.putInt("Food",stored);t.putFloat("Saturation",sat);Data.put(stack,t);
    }
    @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> text,TooltipFlag f){
        text.add(Component.translatable("tx.tooltip."+kind));
        if(kind.equals("talisman_of_nourishment"))text.add(Component.translatable("tx.food.stored",Data.get(s).getInt("Food"),String.format(java.util.Locale.ROOT,"%.1f",Data.get(s).getFloat("Saturation"))));
    }
}
