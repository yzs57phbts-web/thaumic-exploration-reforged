package thaumicexploration;
import net.neoforged.neoforge.common.world.chunk.*;
public final class BrazierTickets {
    public static final TicketController CONTROLLER=new TicketController(ThaumicExploration.id("soul_brazier"),(level,helper)->{
        for(var pos:helper.getBlockTickets().keySet())if(!(level.getBlockEntity(pos) instanceof MachineEntity m)||!m.kind().equals("soul_brazier")||!m.brazierActive)helper.removeAllTickets(pos);
    });
    public static void register(RegisterTicketControllersEvent e){e.register(CONTROLLER);}
    public static void force(MachineEntity m,boolean active){if(m.getLevel() instanceof net.minecraft.server.level.ServerLevel l)CONTROLLER.forceChunk(l,m.getBlockPos(),m.getBlockPos().getX()>>4,m.getBlockPos().getZ()>>4,active,true);}
}
