package github.iri.indicate.mixin;

import github.iri.indicate.data.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import pro.komaru.tridot.util.struct.data.*;

import javax.annotation.*;

@Mixin(LivingEntity.class)
public class LivingEntityMixin implements ILivingEntityData {
    @Unique
    private final Seq<DamageIndicatorInstance> indicate$indicators = Seq.with();

    @Shadow
    @Nullable
    private DamageSource lastDamageSource;

    @Inject(method = "tickDeath", at = @At("TAIL"))
    protected void tickDeath(CallbackInfo ci){
        if (indicate$entity().level().isClientSide()) {
            this.indicate$indicators.removeAll(DamageIndicatorInstance::deathTicks);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    public void tick(CallbackInfo ci) {
        if (indicate$entity().level().isClientSide()) {
            this.indicate$indicators.removeAll(DamageIndicatorInstance::tick);
        }
    }

    @Inject(method = "handleDamageEvent", at = @At("HEAD"))
    public void handleDamageEvent(DamageSource pSource, CallbackInfo ci) {
        this.lastDamageSource = pSource;
    }

    @Override
    public Seq<DamageIndicatorInstance> indicate$getIndicators() {
        return this.indicate$indicators;
    }

    @Override
    @Nullable
    public DamageSource indicate$resolveDamageSource(@Nullable ResourceLocation damageTypeId) {
        if (damageTypeId == null) return this.lastDamageSource;

        ResourceKey<DamageType> key = ResourceKey.create(Registries.DAMAGE_TYPE, damageTypeId);
        if (this.lastDamageSource != null && this.lastDamageSource.is(key)) {
            return this.lastDamageSource;
        }

        return indicate$entity().level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolder(key)
                .map(DamageSource::new)
                .orElse(this.lastDamageSource);
    }

    @Unique
    private Entity indicate$entity() {
        return (Entity) (Object) this;
    }
}
