package thaumicexploration;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(ThaumicExploration.ID)
public final class ThaumicExploration {
    public static final String ID = "thaumicexploration";
    public ThaumicExploration(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        container.registerConfig(ModConfig.Type.COMMON, Config.EARLY_SPEC);
        Content.init(bus);
        Magic.init();
        Recipes.init();
        Research.init();
        Events.init();
        bus.addListener(Content::capabilities);
        bus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(()->{Magic.optional();Research.bindProxies();if(net.neoforged.fml.ModList.get().isLoaded("thaumictinkerer"))TinkererCompat.register();}));
        bus.addListener(MovementPayload::register);bus.addListener(BrazierTickets::register);
    }
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(ID, path); }
}
