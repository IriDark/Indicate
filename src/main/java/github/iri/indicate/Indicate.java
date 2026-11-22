package github.iri.indicate;

import com.mojang.logging.LogUtils;
import github.iri.indicate.config.*;
import github.iri.indicate.network.*;
import net.minecraft.resources.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.*;
import org.slf4j.Logger;

@Mod(Indicate.ID)
public class Indicate{
    public static final String ID = "indicate";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Indicate(){
        IndicateNetwork.init();
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;
        ModLoadingContext.get().registerConfig(Type.CLIENT, ClientConfig.SPEC);

        forgeBus.register(new Events());
    }

    public static ResourceLocation loc(String path) {
        return new ResourceLocation(ID, path);
    }
}