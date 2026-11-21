package github.iri.indicate.data;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.*;
import github.iri.indicate.*;
import github.iri.indicate.config.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.*;
import net.minecraft.network.chat.*;
import net.minecraft.resources.*;
import net.minecraft.util.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import org.joml.Matrix4f;
import pro.komaru.tridot.api.render.text.*;
import pro.komaru.tridot.client.gfx.text.*;
import pro.komaru.tridot.util.*;
import pro.komaru.tridot.util.struct.data.*;

import javax.annotation.*;
import java.text.*;
import java.util.*;

public class DamageIndicatorInstance {
    public static final DecimalFormat FORMAT = new DecimalFormat("###.##", new DecimalFormatSymbols(Locale.ENGLISH));
    private static final Col DEFAULT_HEAL_COLOR = Col.fromHex("55ff55");
    private static final Col DEFAULT_HEAL_OUTLINE = Col.fromHex("0e2b0e");
    private static final Col DEFAULT_CRIT_COLOR = Col.fromHex("ffaa00");
    private static final Col DEFAULT_CRIT_OUTLINE = Col.fromHex("2b1600");

    private final Col defaultColor;
    private final Col renderColor = new Col();
    private final Col renderOutlineColor = new Col();
    @Nullable
    private final DotStyleEffects.OutlineFX outlineFx;

    private final String mergeKey;
    private final boolean isHeal;
    private final boolean isCrit;
    @Nullable
    private final String glyph;
    @Nullable
    private final ResourceLocation glyphFont;
    @Nullable
    private final Component baseLabel;

    private float amount;
    private int count = 1;

    private final int maxAge;
    private final float spawnAngle;
    private float spawnOffsetX;
    private float spawnOffsetY;
    private float zBias;

    private int age;
    private int deathAnimTicks;
    private int maxDeathTicks = 20;

    private MutableComponent component;
    private final Seq<DotStyle> styles = Seq.with();
    private int lastRgb = -1;
    private TextColor cachedTextColor;
    private TextColor cachedShadowColor;

    private DamageIndicatorInstance(LivingEntity entity, String mergeKey, boolean isHeal, boolean isCrit, float amount, @Nullable String glyph, @Nullable ResourceLocation glyphFont, @Nullable Component baseLabel, Col color, Col outline, int maxAge) {
        this.mergeKey = mergeKey;
        this.isHeal = isHeal;
        this.isCrit = isCrit;
        this.amount = amount;
        this.glyph = glyph;
        this.glyphFont = glyphFont;
        this.baseLabel = baseLabel;
        this.defaultColor = color.copy();
        this.maxAge = maxAge;

        this.renderOutlineColor.set(outline);
        this.outlineFx = ClientConfig.OUTLINE.get() ? DotStyleEffects.OutlineFX.of(this.renderOutlineColor, true) : null;
        rebuildComponent();

        RandomSource rand = entity.getRandom();
        float rawAngle = (rand.nextFloat() * 2.0F - 1.0F) * 32.0F;
        this.spawnAngle = Mth.clamp(rawAngle, -ClientConfig.MIN_ANGLE.get(), ClientConfig.MAX_ANGLE.get());
        this.spawnOffsetX = (rand.nextFloat() * 2.0F - 1.0F) * 0.30F;
        this.spawnOffsetY = rand.nextFloat() * 0.15F;
    }

    private void rebuildComponent() {
        this.styles.clear();
        this.lastRgb = -1;

        boolean showMultiplier = this.count > 1 && ClientConfig.SHOW_COMBO_HITS.get();
        if (this.baseLabel != null) {
            DotStyle labelStyle = addStyle(this.baseLabel.getStyle().getFont());
            if (showMultiplier) {
                this.component = Component.empty().append(this.baseLabel.copy().setStyle(labelStyle));
                appendMultiplier();
            } else {
                this.component = this.baseLabel.copy().setStyle(labelStyle);
            }
            return;
        }

        String text = (this.isHeal ? "+" : "") + FORMAT.format(this.amount);
        if (this.isHeal) {
            DotStyle numberStyle = addStyle(getNumberFont());
            if (showMultiplier) {
                this.component = Component.empty().append(Component.literal(text).setStyle(numberStyle));
                appendMultiplier();
            } else {
                this.component = Component.literal(text).setStyle(numberStyle);
            }
            return;
        }

        boolean useCritSkull = this.isCrit && ClientConfig.HAS_CRIT_SKULL_GLYPH.get();
        boolean hidePresetGlyph = useCritSkull && ClientConfig.CRIT_OVERRIDE_PRESET_GLYPH.get();

        this.component = Component.empty();
        if (useCritSkull) {
            this.component.append(Component.literal("☠ ").setStyle(addStyle(Style.DEFAULT_FONT)));
        }
        if (!hidePresetGlyph && this.glyph != null && !this.glyph.isEmpty()) {
            this.component.append(Component.literal(this.glyph + " ").setStyle(addStyle(this.glyphFont != null ? this.glyphFont : Style.DEFAULT_FONT)));
        }
        if (this.isCrit && !useCritSkull) {
            text += "!";
        }
        this.component.append(Component.literal(text).setStyle(addStyle(getNumberFont())));
        if (showMultiplier) {
            appendMultiplier();
        }
    }

    private DotStyle addStyle(ResourceLocation font) {
        DotStyle style = new DotStyle().font(font);
        if (this.outlineFx != null) {
            style.effect(this.outlineFx);
        }
        this.styles.add(style);
        return style;
    }

    private void applyTextColor(TextColor color) {
        for (int i = 0; i < this.styles.size; i++) {
            this.styles.get(i).color(color);
        }
    }

    private void appendMultiplier() {
        this.component.append(Component.literal(" (x" + this.count + ")").setStyle(addStyle(getNumberFont())));
    }

    public static void addOrMerge(Seq<DamageIndicatorInstance> indicators, DamageIndicatorInstance incoming) {
        int mergeWindow = ClientConfig.MERGE_TICKS.get();
        if (mergeWindow > 0) {
            for (int i = indicators.size - 1; i >= 0; i--) {
                DamageIndicatorInstance existing = indicators.get(i);
                if (existing.canMerge(incoming, mergeWindow)) {
                    existing.merge(incoming);
                    return;
                }
            }
        }

        int maxPerEntity = ClientConfig.MAX_INDICATORS.get();
        if (maxPerEntity > 0) {
            while (indicators.size >= maxPerEntity) {
                indicators.remove(0);
            }
        }

        for (int i = 0; i < indicators.size; i++) {
            DamageIndicatorInstance existing = indicators.get(i);
            if (existing.age == 0) {
                existing.zBias += 0.004F;
            }
        }
        incoming.separateFromNeighbors(indicators);
        indicators.add(incoming);
    }

    private void separateFromNeighbors(Seq<DamageIndicatorInstance> indicators) {
        if (!hasCloseNeighbor(indicators, this.spawnOffsetX, this.spawnOffsetY)) return;

        float[] candidates = {0.38F, -0.38F, 0.72F, -0.72F};
        for (float offset : candidates) {
            if (!hasCloseNeighbor(indicators, offset, this.spawnOffsetY)) {
                this.spawnOffsetX = offset;
                return;
            }
        }
        this.spawnOffsetY += 0.24F;
    }

    private boolean hasCloseNeighbor(Seq<DamageIndicatorInstance> indicators, float x, float y) {
        for (int i = 0; i < indicators.size; i++) {
            DamageIndicatorInstance other = indicators.get(i);
            if (other.age <= 5 && Math.abs(x - other.spawnOffsetX) < 0.32F && Math.abs(y - other.spawnOffsetY) < 0.22F) {
                return true;
            }
        }
        return false;
    }

    private boolean canMerge(DamageIndicatorInstance other, int mergeWindow) {
        return this.age <= mergeWindow && this.mergeKey.equals(other.mergeKey);
    }

    private void merge(DamageIndicatorInstance other) {
        this.amount += other.amount;
        this.count += other.count;
        this.age = 0;
        this.zBias = 0.0F;
        rebuildComponent();
    }

    public static DamageIndicatorInstance forDamage(LivingEntity entity, @Nullable DamageSource source, float amount, boolean isCrit) {
        Col baseColor = Col.red;
        Col outlineColor = Col.black;
        String glyph = null;
        ResourceLocation glyphFont = null;
        boolean matchedPreset = false;
        if (source != null) {
            for (DamageData damageData : DamageData.dataTypes) {
                if (damageData.predicate().test(source)) {
                    baseColor = damageData.color();
                    outlineColor = damageData.outline();
                    glyph = damageData.glyph();
                    glyphFont = damageData.font();
                    matchedPreset = true;
                    break;
                }
            }
        }

        if (isCrit && (!matchedPreset || ClientConfig.CRIT_OVERRIDE_PRESET_COLOR.get())) {
            baseColor = parseColor(ClientConfig.CRIT_COLOR.get(), DEFAULT_CRIT_COLOR);
            outlineColor = parseColor(ClientConfig.CRIT_OUTLINE_COLOR.get(), DEFAULT_CRIT_OUTLINE);
        } else if (!matchedPreset) {
            baseColor = parseColor(ClientConfig.DAMAGE_COLOR.get(), Col.red);
            outlineColor = parseColor(ClientConfig.DAMAGE_OUTLINE_COLOR.get(), Col.black);
        }

        String typeKey = source != null ? source.typeHolder().unwrapKey().map(k -> k.location().toString()).orElse("unknown") : "unknown";
        String mergeKey = "dmg:" + typeKey + ":" + isCrit;
        return new DamageIndicatorInstance(entity, mergeKey, false, isCrit, amount, glyph, glyphFont, null, baseColor, outlineColor, ClientConfig.MAX_LIFETIME.get());
    }

    public static DamageIndicatorInstance forHeal(LivingEntity entity, float amount) {
        Col healColor = parseColor(ClientConfig.HEAL_COLOR.get(), DEFAULT_HEAL_COLOR);
        Col healOutline = parseColor(ClientConfig.HEAL_OUTLINE_COLOR.get(), DEFAULT_HEAL_OUTLINE);
        return new DamageIndicatorInstance(entity, "heal", true, false, amount, null, null, null, healColor, healOutline, ClientConfig.MAX_LIFETIME.get());
    }

    private static ResourceLocation getNumberFont() {
        if (!ClientConfig.USE_CUSTOM_FONT.get()) return Style.DEFAULT_FONT;
        ResourceLocation loc = ResourceLocation.tryParse(ClientConfig.CUSTOM_FONT.get());
        return loc != null ? loc : Style.DEFAULT_FONT;
    }

    private static Col parseColor(String hex, Col fallback) {
        try {
            return Col.fromHex(hex);
        } catch (Exception e) {
            return fallback;
        }
    }

    public static DamageIndicatorInstance forPopup(LivingEntity entity, Component label, Col color) {
        return forPopup(entity, label, color, Col.black);
    }

    public static DamageIndicatorInstance forPopup(LivingEntity entity, Component label, Col color, Col outline) {
        String mergeKey = "popup:" + label.getString() + ":" + color.toARGB() + ":" + outline.toARGB();
        return new DamageIndicatorInstance(entity, mergeKey, false, false, 0.0F, null, null, label, color, outline, ClientConfig.MAX_LIFETIME.get());
    }

    public boolean deathTicks() {
        return ++deathAnimTicks >= maxDeathTicks;
    }

    public boolean tick() {
        return ++this.age >= this.maxAge;
    }

    public void render(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight, float partialTicks) {
        float maxAge = this.deathAnimTicks > 0 ? this.maxDeathTicks : this.maxAge;
        float age = this.deathAnimTicks > 0 ? this.deathAnimTicks : this.age;
        float elapsed = Math.min(maxAge, age + partialTicks);

        float progress = Mth.clamp(elapsed / maxAge, 0.0F, 1.0F);
        float flashT = Mth.clamp(elapsed / 10F, 0.0F, 1.0F);
        float alpha = progress > 0.65F ? Mth.clamp(1.0F - (progress - 0.65F) / 0.35F, 0.05F, 1.0F) : 1.0F;

        this.renderColor.set(Col.white).lerp(this.defaultColor, flashT);
        this.renderOutlineColor.a = alpha;
        int rgb = this.renderColor.toARGB() & 0xFFFFFF;
        if (rgb != this.lastRgb) {
            this.lastRgb = rgb;
            this.cachedTextColor = TextColor.fromRgb(rgb);
            int sr = ((rgb >> 16) & 0xFF) / 4;
            int sg = ((rgb >> 8) & 0xFF) / 4;
            int sb = (rgb & 0xFF) / 4;
            this.cachedShadowColor = TextColor.fromRgb((sr << 16) | (sg << 8) | sb);
            applyTextColor(this.cachedTextColor);
        }

        float inEnd = 2.0F;
        float settleEnd = 5.5F;
        float outStart = maxAge * 0.58F;
        float scaleFactor;
        if (elapsed < inEnd) {
            float t = elapsed / inEnd;
            float easeOut = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
            scaleFactor = Mth.lerp(easeOut, 0.4F, 2.0F);
        } else if (elapsed < settleEnd) {
            float t = (elapsed - inEnd) / (settleEnd - inEnd);
            float smooth = t * t * (3.0F - 2.0F * t);
            scaleFactor = Mth.lerp(smooth, 1.5F, 1.0F);
        } else if (elapsed < outStart) {
            scaleFactor = 1.0F;
        } else {
            float t = Mth.clamp((elapsed - outStart) / Math.max(1.0F, maxAge - outStart), 0.0F, 1.0F);
            scaleFactor = Mth.lerp(t * t, 1.0F, 0.25F);
        }

        float popT = Mth.clamp(elapsed / outStart, 0.0F, 1.0F);
        float popEase = 1.0F - (1.0F - popT) * (1.0F - popT);
        float flyT = Mth.clamp((elapsed - outStart) / Math.max(1.0F, maxAge - outStart), 0.0F, 1.0F);
        float yRise = 0.25F + this.spawnOffsetY + popEase * 0.25F + (flyT * flyT) * 0.65F;
        float xDrift = this.spawnOffsetX * (0.4F + 0.6F * popEase);
        float rollDeg = this.spawnAngle * (1.0F - progress * 0.35F);
        float worldScale = 0.04F * Math.max(0.1F, ClientConfig.SCALE.get()) * scaleFactor;
        float zDepth = this.zBias + progress * 0.08F;

        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() + ClientConfig.Y_OFFSET.get() + yRise, 0.0D);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.translate(xDrift, 0.0F, zDepth);
        poseStack.mulPose(Axis.ZP.rotationDegrees(rollDeg));
        poseStack.scale(-worldScale, -worldScale, worldScale);
        Matrix4f matrix4f = poseStack.last().pose();

        Font font = Minecraft.getInstance().font;
        float xPos = -font.width(component) / 2.0F;
        float yPos = -font.lineHeight / 2.0F;
        int textColor = ((int) (alpha * 255.0F) << 24) | 0xFFFFFF;
        int light = ClientConfig.ADDITIVE.get() ? LightTexture.FULL_BRIGHT : packedLight;

        if (this.outlineFx == null && ClientConfig.SHADOW.get()) {
            applyTextColor(this.cachedShadowColor);
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.0F, 0.03F);
            font.drawInBatch(component, xPos + 1.0F, yPos + 1.0F, textColor, false, poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, light);
            poseStack.popPose();
            applyTextColor(this.cachedTextColor);
        }

        font.drawInBatch(component, xPos, yPos, textColor, false, matrix4f, buffer, Font.DisplayMode.POLYGON_OFFSET, 0, light);
        poseStack.popPose();
    }
}