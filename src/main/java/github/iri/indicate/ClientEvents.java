package github.iri.indicate;

import com.mojang.blaze3d.vertex.PoseStack;
import github.iri.indicate.config.*;
import github.iri.indicate.data.*;
import net.minecraft.client.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.*;
import pro.komaru.tridot.util.struct.data.*;

public class ClientEvents {

    public static void renderIndicators(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight, float partialTicks) {
        if (!ClientConfig.ENABLED.get()) return;
        if (!(entity instanceof ILivingEntityData data)) return;
        var player = Minecraft.getInstance().player;
        if (player == null || entity.getType().is(IndicateTags.DAMAGE_INDICATOR_IGNORED) || entity.isInvisibleTo(player)) return;

        Seq<DamageIndicatorInstance> indicators = data.indicate$getIndicators();
        if (indicators.isEmpty()) return;

        for (DamageIndicatorInstance instance : indicators) {
            instance.render(entity, poseStack, buffer, packedLight, partialTicks);
        }
    }
}
