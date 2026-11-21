package github.iri.indicate;

import github.iri.indicate.config.*;
import github.iri.indicate.data.*;
import net.minecraft.client.*;
import net.minecraft.world.entity.*;
import net.neoforged.bus.api.*;
import net.neoforged.neoforge.client.event.*;
import pro.komaru.tridot.util.struct.data.*;

public class ClientEvents{

    @SubscribeEvent
    public static void onEntityRender(RenderLivingEvent.Post<?, ?> event) {
        if (!ClientConfig.ENABLED.get()) return;
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof ILivingEntityData data)) return;
        if (entity.getType().is(IndicateTags.DAMAGE_INDICATOR_IGNORED) || entity.isInvisibleTo(Minecraft.getInstance().player)) return;

        Seq<DamageIndicatorInstance> indicators = data.indicate$getIndicators();
        if (indicators.isEmpty()) return;

        float partialTicks = event.getPartialTick();
        for (DamageIndicatorInstance instance : indicators) {
            instance.render(entity, event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), partialTicks);
        }
    }
}
