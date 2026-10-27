package thaumicexploration;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public record MovementPayload(boolean forward) implements CustomPacketPayload {
    public static final Type<MovementPayload> TYPE=new Type<>(ThaumicExploration.id("movement"));
    public static final StreamCodec<RegistryFriendlyByteBuf,MovementPayload> CODEC=StreamCodec.composite(ByteBufCodecs.BOOL,MovementPayload::forward,MovementPayload::new);
    @Override public Type<MovementPayload> type(){return TYPE;}
    public static void register(RegisterPayloadHandlersEvent e){e.registrar("2").playToServer(TYPE,CODEC,(message,context)->context.enqueueWork(()->context.player().getPersistentData().putBoolean("tx.forward",message.forward())));}
}
