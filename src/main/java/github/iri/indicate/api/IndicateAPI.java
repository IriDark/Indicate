package github.iri.indicate.api;

import github.iri.indicate.*;
import github.iri.indicate.data.*;
import github.iri.indicate.network.*;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.neoforged.fml.loading.*;
import pro.komaru.tridot.util.*;

/**
 * Public API for spawning floating damage/status popups on living entities.
 */
public final class IndicateAPI {
    private IndicateAPI() {}

    public static void popup(LivingEntity target, Component label) {
        popup(target, label, extractColor(label), Col.black);
    }

    public static void popup(LivingEntity target, Component label, int rgb) {
        popup(target, label, fromRgb(rgb), Col.black);
    }

    public static void popup(LivingEntity target, Component label, int rgb, int outlineRgb) {
        popup(target, label, fromRgb(rgb), fromRgb(outlineRgb));
    }

    public static void popup(LivingEntity target, Component label, Col color) {
        popup(target, label, color, Col.black);
    }

    public static void popup(LivingEntity target, Component label, Col color, Col outline) {
        if (target == null || target.getType().is(IndicateTags.DAMAGE_INDICATOR_IGNORED)) return;
        if (target.level().isClientSide()) {
            if (FMLEnvironment.dist.isClient()) {
                ClientOnly.spawnLocal(target, label, color, outline);
            }

            return;
        }

        IndicateNetwork.sendTracking(target, IndicatorPacket.popup(target, label, color, outline));
    }

    public static void popup(ServerPlayer viewer, LivingEntity target, Component label) {
        popup(viewer, target, label, extractColor(label), Col.black);
    }

    public static void popup(ServerPlayer viewer, LivingEntity target, Component label, int rgb) {
        popup(viewer, target, label, fromRgb(rgb), Col.black);
    }

    public static void popup(ServerPlayer viewer, LivingEntity target, Component label, int rgb, int outlineRgb) {
        popup(viewer, target, label, fromRgb(rgb), fromRgb(outlineRgb));
    }

    public static void popup(ServerPlayer viewer, LivingEntity target, Component label, Col color, Col outline) {
        if (viewer == null || target == null || target.getType().is(IndicateTags.DAMAGE_INDICATOR_IGNORED)) return;
        IndicateNetwork.sendToPlayer(viewer, IndicatorPacket.popup(target, label, color, outline));
    }

    public static void miss(LivingEntity target) {
        if (target != null && !target.level().isClientSide() && !target.getType().is(IndicateTags.DAMAGE_INDICATOR_IGNORED)) {
            IndicateNetwork.sendTracking(target, IndicatorPacket.miss(target));
        }
    }

    public static void dodge(LivingEntity target) {
        if (target != null && !target.level().isClientSide() && !target.getType().is(IndicateTags.DAMAGE_INDICATOR_IGNORED)) {
            IndicateNetwork.sendTracking(target, IndicatorPacket.dodge(target));
        }
    }

    private static Col extractColor(Component label) {
        TextColor textColor = label.getStyle().getColor();
        return textColor != null ? fromRgb(textColor.getValue()) : Col.white;
    }

    private static Col fromRgb(int rgb) {
        return new Col(((rgb >> 16) & 0xFF) / 255.0F, ((rgb >> 8) & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F, 1.0F);
    }

    private static final class ClientOnly {
        private static void spawnLocal(LivingEntity target, Component label, Col color, Col outline) {
            if (target instanceof ILivingEntityData data) {
                DamageIndicatorInstance.addOrMerge(data.indicate$getIndicators(), DamageIndicatorInstance.forPopup(target, label, color, outline));
            }
        }
    }
}