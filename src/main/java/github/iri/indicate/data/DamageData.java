package github.iri.indicate.data;

import net.minecraft.resources.*;
import net.minecraft.world.damagesource.*;
import pro.komaru.tridot.util.*;
import pro.komaru.tridot.util.struct.data.*;

import java.util.function.*;

public record DamageData(Predicate<DamageSource> predicate, Col color, Col outline, String glyph, ResourceLocation font){
    public static final Seq<DamageData> dataTypes = Seq.with();

    public DamageData(Predicate<DamageSource> predicate, String glyph, Col color){
        this(predicate, color, Col.black, glyph, null);
    }

    public DamageData(Predicate<DamageSource> predicate, String glyph, ResourceLocation font, Col color, Col outline){
        this(predicate, color, outline, glyph, font);
    }

    public static void clear(){
        dataTypes.clear();
    }

    public static void registerFromJson(DamageData data){
        dataTypes.add(data);
    }
}