package thaumicexploration;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import thaumcraft.aspect.*;
import thaumictinkerer.enchanting.EnchantmentManager;
final class TinkererCompat {
    static void register(){
        add("binding","binding","ENCHBINDING",new AspectList().add(Aspect.ENTROPY,15).add(Aspect.ORDER,15));
        add("night_vision","nightvision","ENCHNIGHTVISION",new AspectList().add(Aspect.ENTROPY,20).add(Aspect.FIRE,10).add(Aspect.ORDER,20));
        add("disarming","disarm","ENCHDISARM",new AspectList().add(Aspect.AIR,12).add(Aspect.ORDER,7).add(Aspect.ENTROPY,7));
    }
    private static void add(String name,String texture,String research,AspectList cost){
        var key=ResourceKey.create(Registries.ENCHANTMENT,ThaumicExploration.id(name));
        EnchantmentManager.data().put(key,new EnchantmentManager.Data(key,ThaumicExploration.id("textures/tabs/"+texture+".png"),false,cost,research));
    }
}
