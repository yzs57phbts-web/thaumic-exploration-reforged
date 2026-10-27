package thaumicexploration;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
@EventBusSubscriber(modid=ThaumicExploration.ID,value=Dist.CLIENT)
public final class ClientMovement {
    private static net.minecraft.client.player.LocalPlayer lastPlayer;
    private static boolean forward;
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        var p=net.minecraft.client.Minecraft.getInstance().player;if(p==null){lastPlayer=null;return;}
        boolean next=p.input.forwardImpulse>0;
        if(p!=lastPlayer||next!=forward){lastPlayer=p;forward=next;net.neoforged.neoforge.network.PacketDistributor.sendToServer(new MovementPayload(next));}
    }
}
