package github.iri.indicate.data;

import net.minecraft.resources.*;
import net.minecraft.world.damagesource.*;
import pro.komaru.tridot.util.struct.data.*;

import javax.annotation.*;

public interface ILivingEntityData {
    Seq<DamageIndicatorInstance> indicate$getIndicators();

    @Nullable
    DamageSource indicate$resolveDamageSource(@Nullable ResourceLocation damageTypeId);
}
