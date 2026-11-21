package github.iri.indicate.network;

import github.iri.indicate.*;
import github.iri.indicate.config.*;
import github.iri.indicate.data.*;
import net.minecraft.client.*;
import net.minecraft.network.*;
import net.minecraft.network.chat.*;
import net.minecraft.resources.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraftforge.api.distmarker.*;
import net.minecraftforge.network.*;
import pro.komaru.tridot.common.networking.packets.*;
import pro.komaru.tridot.util.*;

import javax.annotation.*;
import java.util.function.*;

public class IndicatorPacket extends ClientPacket {
    public enum Kind {
        DAMAGE,
        HEAL,
        MISS,
        DODGE,
        CUSTOM
    }

    private final int entityId;
    private final Kind kind;
    private final float amount;
    private final boolean crit;
    @Nullable
    private final ResourceLocation damageType;
    @Nullable
    private final Component label;
    private final int colorRgba;
    private final int outlineRgba;

    private IndicatorPacket(int entityId, Kind kind, float amount, boolean crit, @Nullable ResourceLocation damageType, @Nullable Component label, int colorRgba, int outlineRgba) {
        this.entityId = entityId;
        this.kind = kind;
        this.amount = amount;
        this.crit = crit;
        this.damageType = damageType;
        this.label = label;
        this.colorRgba = colorRgba;
        this.outlineRgba = outlineRgba;
    }

    public static IndicatorPacket damage(LivingEntity entity, DamageSource source, float amount, boolean crit) {
        ResourceLocation typeId = source != null
                ? source.typeHolder().unwrapKey().map(ResourceKey::location).orElse(null)
                : null;
        return new IndicatorPacket(entity.getId(), Kind.DAMAGE, amount, crit, typeId, null, 0, 0);
    }

    public static IndicatorPacket heal(LivingEntity entity, float amount) {
        return new IndicatorPacket(entity.getId(), Kind.HEAL, amount, false, null, null, 0, 0);
    }

    public static IndicatorPacket miss(LivingEntity entity) {
        return new IndicatorPacket(entity.getId(), Kind.MISS, 0.0F, false, null, null, 0, 0);
    }

    public static IndicatorPacket dodge(LivingEntity entity) {
        return new IndicatorPacket(entity.getId(), Kind.DODGE, 0.0F, false, null, null, 0, 0);
    }

    public static IndicatorPacket popup(LivingEntity entity, Component label, Col color, Col outline) {
        return new IndicatorPacket(entity.getId(), Kind.CUSTOM, 0.0F, false, null, label, color.rgba8888(), outline.rgba8888());
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entityId);
        buf.writeEnum(this.kind);
        if (this.kind == Kind.DAMAGE || this.kind == Kind.HEAL) {
            buf.writeFloat(this.amount);
        }
        if (this.kind == Kind.DAMAGE) {
            buf.writeBoolean(this.crit);
            buf.writeBoolean(this.damageType != null);
            if (this.damageType != null) {
                buf.writeResourceLocation(this.damageType);
            }
        }
        if (this.kind == Kind.CUSTOM) {
            buf.writeComponent(this.label != null ? this.label : Component.empty());
            buf.writeInt(this.colorRgba);
            buf.writeInt(this.outlineRgba);
        }
    }

    public static IndicatorPacket decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        Kind kind = buf.readEnum(Kind.class);
        float amount = (kind == Kind.DAMAGE || kind == Kind.HEAL) ? buf.readFloat() : 0.0F;
        boolean crit = false;
        ResourceLocation damageType = null;
        if (kind == Kind.DAMAGE) {
            crit = buf.readBoolean();
            if (buf.readBoolean()) {
                damageType = buf.readResourceLocation();
            }
        }
        Component label = null;
        int colorRgba = 0;
        int outlineRgba = 0;
        if (kind == Kind.CUSTOM) {
            label = buf.readComponent();
            colorRgba = buf.readInt();
            outlineRgba = buf.readInt();
        }
        return new IndicatorPacket(entityId, kind, amount, crit, damageType, label, colorRgba, outlineRgba);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void execute(Supplier<NetworkEvent.Context> context) {
        if (!ClientConfig.ENABLED.get()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Entity entity = mc.level.getEntity(this.entityId);
        if (!(entity instanceof LivingEntity living) || !(living instanceof ILivingEntityData data)) return;
        if (living.getType().is(IndicateTags.DAMAGE_INDICATOR_IGNORED)) return;

        switch (this.kind) {
            case DAMAGE -> {
                if (this.amount > 0.0F) {
                    DamageSource source = data.indicate$resolveDamageSource(this.damageType);
                    DamageIndicatorInstance.addOrMerge(data.indicate$getIndicators(), DamageIndicatorInstance.forDamage(living, source, this.amount, this.crit));
                }
            }
            case HEAL -> {
                if (this.amount > 0.0F && ClientConfig.HEALING_RENDERING.get()) {
                    DamageIndicatorInstance.addOrMerge(data.indicate$getIndicators(), DamageIndicatorInstance.forHeal(living, this.amount));
                }
            }
            case MISS -> DamageIndicatorInstance.addOrMerge(data.indicate$getIndicators(), DamageIndicatorInstance.forPopup(living, Component.translatable("popup.indicate.miss"), Col.lightGray));
            case DODGE -> DamageIndicatorInstance.addOrMerge(data.indicate$getIndicators(), DamageIndicatorInstance.forPopup(living, Component.translatable("popup.indicate.dodge"), Col.white));
            case CUSTOM -> {
                if (this.label != null) {
                    DamageIndicatorInstance.addOrMerge(data.indicate$getIndicators(), DamageIndicatorInstance.forPopup(living, this.label, new Col(this.colorRgba), new Col(this.outlineRgba)));
                }
            }
        }
    }
}
