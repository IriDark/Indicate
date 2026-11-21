package github.iri.indicate;

import github.iri.indicate.api.*;
import github.iri.indicate.network.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.*;
import net.minecraft.tags.*;
import net.minecraft.world.entity.*;
import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.*;
import pro.komaru.tridot.util.*;

public class Events{

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onCrit(CriticalHitEvent event) {
        if (!event.getEntity().level().isClientSide() && event.getTarget() instanceof LivingEntity target) {
            boolean isCrit = event.getResult() == Event.Result.ALLOW || (event.isVanillaCritical() && event.getResult() == Event.Result.DEFAULT);
            if (isCrit) {
                target.getPersistentData().putBoolean("indicate:crit", true);
            } else {
                target.getPersistentData().remove("indicate:crit");
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDamage(LivingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        if (entity.getType().is(IndicateTags.DAMAGE_INDICATOR_IGNORED)) return;

        if (!event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            CompoundTag data = entity.getPersistentData();
            boolean crit = data.getBoolean("indicate:crit");
            data.remove("indicate:crit");
            float amount = Math.max(0.0F, event.getAmount());
            if (amount > 0.0F) {
                IndicateNetwork.sendTracking(entity, IndicatorPacket.damage(entity, event.getSource(), amount, crit));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || entity.getHealth() <= 0.0F) return;
        if (entity.getType().is(IndicateTags.DAMAGE_INDICATOR_IGNORED)) return;

        float healed = Math.min(event.getAmount(), entity.getMaxHealth() - entity.getHealth());
        if (healed > 0.01F) {
            IndicateNetwork.sendTracking(entity, IndicatorPacket.heal(entity, healed));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onAttack(LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        if (entity.getType().is(IndicateTags.DAMAGE_INDICATOR_IGNORED)) return;

        CompoundTag data = entity.getPersistentData();
        if (data.getBoolean("indicate:dodge")) {
            data.remove("indicate:dodge");
            data.remove("indicate:crit");
            IndicateAPI.dodge(entity);
        } else if (data.getBoolean("indicate:miss")) {
            data.remove("indicate:miss");
            data.remove("indicate:crit");
            IndicateAPI.miss(entity);
        } else if (data.contains("indicate:popup", Tag.TAG_COMPOUND)) {
            CompoundTag popup = data.getCompound("indicate:popup");
            data.remove("indicate:popup");
            data.remove("indicate:crit");
            String text = popup.getString("text");
            if (!text.isEmpty()) {
                Col color = popup.contains("color", Tag.TAG_STRING) ? Col.fromHex(popup.getString("color")) : Col.white;
                Col outline = popup.contains("outline", Tag.TAG_STRING) ? Col.fromHex(popup.getString("outline")) : Col.black;
                IndicateAPI.popup(entity, Component.translatable(text), color, outline);
            }
        } else if (data.contains("indicate:popup", Tag.TAG_STRING)) {
            String text = data.getString("indicate:popup");
            data.remove("indicate:popup");
            data.remove("indicate:crit");
            if (!text.isEmpty()) {
                IndicateAPI.popup(entity, Component.translatable(text));
            }
        }
    }
}