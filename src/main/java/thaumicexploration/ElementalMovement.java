package thaumicexploration;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.phys.Vec3;

public final class ElementalMovement {
    private static final AttributeModifier STEP=new AttributeModifier(ThaumicExploration.id("elemental_step"),0.4,AttributeModifier.Operation.ADD_VALUE);
    public static void tick(Player player,boolean forward){
        var boots=player.getItemBySlot(EquipmentSlot.FEET);
        boolean elemental=boots.getItem() instanceof ElementalBoots b&&Config.enabled(b.kind);
        boolean flying=player.getAbilities().flying||player.isFallFlying();
        var step=player.getAttribute(Attributes.STEP_HEIGHT);
        if(step!=null){
            boolean active=step.hasModifier(STEP.id());
            if(elemental&&!flying&&!player.isShiftKeyDown()){if(!active)step.addTransientModifier(STEP);}
            else if(active)step.removeModifier(STEP.id());
        }
        // Remove the old port.1 speed modifier after a live equipment change.
        var speed=player.getAttribute(Attributes.MOVEMENT_SPEED);if(speed!=null)speed.removeModifier(ThaumicExploration.id("boots_speed"));
        if(!elemental)return;
        boolean comet=((ElementalBoots)boots.getItem()).kind.equals("comet_boots");
        var data=Data.get(boots);int run=data.getInt("runTicks");
        if(comet){run=(!player.onGround()||player.onClimbable()||Math.abs(player.getDeltaMovement().x)+Math.abs(player.getDeltaMovement().z)>0.1)?Math.min(100,run+1):0;data.putInt("runTicks",run);Data.put(boots,data);}
        if(!flying&&forward){
            double bonus=(player.onGround()||player.onClimbable())?(comet?0.110+(run/5)*0.003:0.055):0.03;
            if(player.isInWater())bonus/=4;
            player.moveRelative((float)bonus,new Vec3(0,0,1));
        }
        if(comet){if(forward&&!flying&&player.fallDistance>0.25F)player.fallDistance-=0.25F;}else player.fallDistance=0;
    }
}
