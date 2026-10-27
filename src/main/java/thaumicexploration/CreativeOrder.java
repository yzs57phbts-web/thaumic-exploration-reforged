package thaumicexploration;
import java.util.*;
import net.minecraft.world.item.DyeColor;
public final class CreativeOrder {
    public static List<String> items(){
        var names=new ArrayList<String>();names.add("replicator");
        // Floating candles now belong to TC4; old IDs are migration-only.
        Collections.addAll(names,"auto_sorter","taint_charm","soul_brazier","think_tank","everfull_urn","everburn_urn","crucible_of_souls","oblivion_jar","bound_chest","bound_jar","meteor_boots","comet_boots","talisman_of_nourishment","amber_rod","amber_staff_core","transmutation_rod","transmutation_staff_core","necromancer_staff_core","inert_sojourner_cap","sojourner_cap","inert_mechanist_cap","mechanist_cap","purified_brain","dreamcatcher","tentacle_ring","stabilizer_belt");
        for(String a:List.of("air","earth","fire","water","order","entropy"))names.add(a+"_ring");
        for(String kind:List.of("blank","chest","jar"))for(var color:DyeColor.values())names.add(color.getName()+"_"+kind+"_seal");
        if(Config.EARLY_SPEC.isLoaded()&&Config.BREAD.get())names.add("bread_rod");return List.copyOf(names);
    }
}
