package github.iri.indicate;

import com.mojang.logging.LogUtils;
import github.iri.indicate.config.*;
import github.iri.indicate.network.*;
import net.minecraft.resources.*;
import net.neoforged.bus.api.*;
import net.neoforged.fml.*;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.*;
import net.neoforged.neoforge.common.*;
import org.slf4j.Logger;

@Mod(Indicate.ID)
public class Indicate{
    public static final String ID = "indicate";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Indicate(IEventBus modEventBus, ModContainer modContainer){
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        modEventBus.addListener(IndicateNetwork::register);

        NeoForge.EVENT_BUS.register(new Events());
    }

    public static ResourceLocation loc(String path) {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }
}
