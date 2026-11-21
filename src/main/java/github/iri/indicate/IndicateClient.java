package github.iri.indicate;

import github.iri.indicate.data.*;
import net.neoforged.api.distmarker.*;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.*;
import net.neoforged.neoforge.client.event.*;

public class IndicateClient{

    @EventBusSubscriber(modid = Indicate.ID, value = Dist.CLIENT)
    public static class RegistryEvents{

        @SubscribeEvent
        public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(new DamageDataLoader());
        }
    }
}
