package thaumicexploration;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.SoundType;
import thaumcraft.registry.ModSounds;

/** Sound triggers from TE 1.1-53; the original addon contains no audio files of its own. */
public final class LegacySounds {
    private LegacySounds(){}
    public static SoundType block(String kind){
        return switch(kind){
            case "think_tank","bound_jar","oblivion_jar"->new SoundType(1,1,ModSounds.JAR.get(),ModSounds.JAR.get(),ModSounds.JAR.get(),ModSounds.JAR.get(),ModSounds.JAR.get());
            case "bound_chest"->SoundType.WOOD;
            case "crucible_of_souls"->SoundType.METAL;
            default->SoundType.STONE;
        };
    }
    public static void swim(Player player,float volume){
        player.level().playSound(null,player.getX(),player.getY(),player.getZ(),SoundEvents.GENERIC_SWIM,SoundSource.PLAYERS,volume,1);
    }
    public static void pop(MachineEntity machine,float pitch){
        var level=machine.getLevel();
        level.playSound(null,machine.getBlockPos(),SoundEvents.ITEM_PICKUP,SoundSource.BLOCKS,0.2F,((level.random.nextFloat()-level.random.nextFloat())*0.7F+1)*pitch);
    }
}
