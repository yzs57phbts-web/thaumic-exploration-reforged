package thaumicexploration;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class Config {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec EARLY_SPEC;
    public static final ModConfigSpec.IntValue THINK_TICKS, REPLICATION_TICKS, SOUL_RANGE, SORTER_LINK_QUALITY;
    public static final ModConfigSpec.BooleanValue SOULS, REPLICATION;
    public static final java.util.Map<String,ModConfigSpec.BooleanValue> FEATURES=new java.util.LinkedHashMap<>();
    public static final ModConfigSpec.BooleanValue HARD_BRAZIER, BREAD, MAGIC_PLANKS, MOD_WOOD, MOD_STONE;
    public static boolean enabled(String name){var value=FEATURES.get(name);return !SPEC.isLoaded()||value==null||value.get();}
    static {
        var b = new ModConfigSpec.Builder();
        THINK_TICKS = b.translation("config.thaumicexploration.thinkTicks").defineInRange("thinkTankTicks", 200, 20, 72000);
        REPLICATION_TICKS = b.translation("config.thaumicexploration.replicationTicks").defineInRange("replicationTicks", 100, 20, 72000);
        SOUL_RANGE = b.translation("config.thaumicexploration.soulRange").defineInRange("soulCrucibleRange", 5, 1, 16);
        SOULS = b.translation("config.thaumicexploration.souls").define("enableSoulCrucible", true);
        REPLICATION = b.translation("config.thaumicexploration.replication").define("enableReplication", true);
        HARD_BRAZIER=b.define("hardSoulBrazier",false);
        MAGIC_PLANKS=b.define("allowMagicPlankReplication",true);
        MOD_WOOD=b.define("allowModWoodReplication",true);
        MOD_STONE=b.define("allowModStoneReplication",true);
        b.push("features");
        for(String key:java.util.List.of("think_tank","everfull_urn","everburn_urn","oblivion_jar","bound_jar","bound_chest","soul_brazier","auto_sorter","taintCharm","dreamcatcher","meteor_boots","comet_boots","talisman_of_nourishment","tentacle_ring","stabilizer_belt","rings","wands"))FEATURES.put(key,b.define(key,true));
        b.pop();
        SPEC = b.build();
        var startup=new ModConfigSpec.Builder();BREAD=startup.define("enableBreadWand",false);SORTER_LINK_QUALITY=startup.defineInRange("sorterLinkQuality",16,0,64);EARLY_SPEC=startup.build();
    }
}
