package github.iri.indicate;

import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.tags.*;
import net.minecraft.world.entity.*;

public class IndicateTags{

    public static TagKey<EntityType<?>> entity(final ResourceLocation name){
        return TagKey.create(Registries.ENTITY_TYPE, name);
    }

    public static final TagKey<EntityType<?>> DAMAGE_INDICATOR_IGNORED = entity(Indicate.loc("damage_indicator_ignored"));
}
