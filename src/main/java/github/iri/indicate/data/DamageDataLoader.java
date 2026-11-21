package github.iri.indicate.data;

import com.google.gson.*;
import github.iri.indicate.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.*;
import net.minecraft.resources.*;
import net.minecraft.server.packs.resources.*;
import net.minecraft.tags.*;
import net.minecraft.util.*;
import net.minecraft.util.profiling.*;
import net.minecraft.world.damagesource.*;
import pro.komaru.tridot.util.*;

import java.util.*;
import java.util.List;
import java.util.function.*;

public class DamageDataLoader extends SimpleJsonResourceReloadListener{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public DamageDataLoader() {
        super(GSON, "damage_indicators");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager manager, ProfilerFiller profiler){
        DamageData.clear();
        List<Map.Entry<ResourceLocation, JsonElement>> sorted = new ArrayList<>(jsons.entrySet());
        sorted.sort((a, b) -> Integer.compare(getPriority(b), getPriority(a)));
        for(var entry : sorted){
            try{
                JsonObject json = entry.getValue().getAsJsonObject();
                if(!json.has("damage_type")) throw new IllegalArgumentException("Missing 'damage_type'");

                Predicate<DamageSource> predicate = parsePredicate(json.get("damage_type"));
                String hex = GsonHelper.getAsString(json, "color_hex", "b31111");
                Col color = Col.fromHex(hex);

                String outlineHex = GsonHelper.getAsString(json, "outline_color_hex", "000000");
                Col outline = Col.fromHex(outlineHex);
                String glyph = json.has("glyph") ? GsonHelper.getAsString(json, "glyph") : null;
                ResourceLocation font = json.has("font") ? ResourceLocation.parse(GsonHelper.getAsString(json, "font")) : null;
                DamageData.registerFromJson(new DamageData(predicate, glyph, font, color, outline));
            }catch(Exception e){
                Indicate.LOGGER.error("Failed to load damage indicator {}", entry.getKey(), e);
            }
        }
    }

    private int getPriority(Map.Entry<ResourceLocation, JsonElement> entry) {
        try {
            JsonElement element = entry.getValue();
            return element.isJsonObject() ? GsonHelper.getAsInt(element.getAsJsonObject(), "priority", 0) : 0;
        } catch (Exception e) {
            Indicate.LOGGER.warn("Failed to parse priority in {}, using 0", entry.getKey(), e);
            return 0;
        }
    }

    private Predicate<DamageSource> parsePredicate(JsonElement element) {
        if (element.isJsonArray()) {
            List<Predicate<DamageSource>> list = new ArrayList<>();
            for (JsonElement el : element.getAsJsonArray()) {
                list.add(singlePredicate(el.getAsString()));
            }

            return source -> {
                for (var p : list) if (p.test(source)) return true;
                return false;
            };
        }

        return singlePredicate(element.getAsString());
    }

    private Predicate<DamageSource> singlePredicate(String raw) {
        if (raw.startsWith("#")) {
            TagKey<DamageType> tag = TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse(raw.substring(1)));
            return source -> source.is(tag);
        } else {
            ResourceKey<DamageType> key = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse(raw));
            return source -> source.is(key);
        }
    }
}