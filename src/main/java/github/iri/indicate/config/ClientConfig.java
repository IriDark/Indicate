package github.iri.indicate.config;

import net.minecraftforge.common.*;
import org.apache.commons.lang3.tuple.*;

public class ClientConfig{
    public static ForgeConfigSpec.ConfigValue<Integer>
    MAX_LIFETIME, MERGE_TICKS, MAX_INDICATORS;

    public static ForgeConfigSpec.ConfigValue<Float>
    MIN_ANGLE, MAX_ANGLE, SCALE, Y_OFFSET;

    public static ForgeConfigSpec.ConfigValue<Boolean>
    ENABLED, HEALING_RENDERING, USE_CUSTOM_FONT, CRIT_OVERRIDE_PRESET_COLOR, HAS_CRIT_SKULL_GLYPH, CRIT_OVERRIDE_PRESET_GLYPH, SHOW_COMBO_HITS, ADDITIVE, OUTLINE, SHADOW;

    public static ForgeConfigSpec.ConfigValue<String>
    CUSTOM_FONT, DAMAGE_COLOR, DAMAGE_OUTLINE_COLOR, HEAL_COLOR, HEAL_OUTLINE_COLOR, CRIT_COLOR, CRIT_OUTLINE_COLOR;

    static{
        final Pair<ClientConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(ClientConfig::new);
        SPEC = specPair.getRight();
        INSTANCE = specPair.getLeft();
    }

    public static final ClientConfig INSTANCE;
    public static final ForgeConfigSpec SPEC;

    public ClientConfig(ForgeConfigSpec.Builder builder){
        ENABLED = builder.comment("Specifies if popups will be showcased").define("enabled", true);
        HEALING_RENDERING = builder.comment("Specifies if healing popups will be showcased").define("healingEnabled", true);

        USE_CUSTOM_FONT = builder.comment("Use custom font for damage/heal numbers instead of vanilla default font").define("useCustomFont", true);
        CUSTOM_FONT = builder.comment("ResourceLocation of the font used for damage/heal numbers").define("customFont", "indicate:numbers");

        DAMAGE_COLOR = builder.comment("Default hex color for damage popups without a damage type preset").define("damageColor", "ff0000");
        DAMAGE_OUTLINE_COLOR = builder.comment("Default hex outline color for damage popups without a damage type preset").define("damageOutlineColor", "000000");

        HEAL_COLOR = builder.comment("Hex color for healing popups").define("healColor", "55ff55");
        HEAL_OUTLINE_COLOR = builder.comment("Hex outline color for healing popups").define("healOutlineColor", "0e2b0e");

        CRIT_COLOR = builder.comment("Hex color for critical hit popups").define("critColor", "f3ff4d");
        CRIT_OUTLINE_COLOR = builder.comment("Hex outline color for critical hit popups").define("critOutlineColor", "2b1600");
        CRIT_OVERRIDE_PRESET_COLOR = builder.comment("If true, critical hits override damage type preset colors with critColor").define("critOverridePresetColor", false);
        HAS_CRIT_SKULL_GLYPH = builder.comment("Show skull glyph before critical hits; if false, shows as 12! instead").define("hasCritSkullGlyph", true);
        CRIT_OVERRIDE_PRESET_GLYPH = builder.comment("Will override other preset glyphs if hasCritSkullGlyph is true").define("critOverridePresetGlyph", false);

        MIN_ANGLE = builder.comment("Min angle of popup").define("minAngle", 15.0f);
        MAX_ANGLE = builder.comment("Max angle of popup").define("maxAngle", 15.0f);

        MAX_LIFETIME = builder.comment("Max lifetime of popup in ticks").define("maxLifetime", 20);
        MERGE_TICKS = builder.comment("Time window in ticks to merge rapid identical popups (0 disables merging)").defineInRange("mergeWindowTicks", 12, 0, 100);
        MAX_INDICATORS = builder.comment("Maximum simultaneous popups per entity (0 = unlimited)").defineInRange("maxIndicatorsPerEntity", 8, 0, 100);
        SHOW_COMBO_HITS = builder.comment("Show (xN) multiplier suffix when rapid hits are merged").define("showComboHits", true);

        ADDITIVE = builder.comment("Render popups at max brightness ignoring world lighting").define("additive", true);
        OUTLINE = builder.comment("Render outline around popups").define("outline", true);
        SHADOW = builder.comment("Render drop shadow under popups (only used when outline is disabled)").define("shadow", true);
        SCALE = builder.comment("Scale multiplier for popup text size").define("scale", 1.0f);
        Y_OFFSET = builder.comment("Vertical spawn offset above the entity head in blocks").define("yOffset", 0.5f);
    }
}