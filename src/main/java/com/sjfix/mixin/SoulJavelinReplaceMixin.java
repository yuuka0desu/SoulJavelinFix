package com.sjfix.mixin;

import com.sjfix.entity.FixedSoulJavelinEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces original SoulJavelinEntity with FixedSoulJavelinEntity on the
 * first server tick. Position and velocity are copied from the original;
 * rotation is deliberately not set, letting Projectile.updateRotation()
 * align the entity with its velocity on the next tick so that there is
 * no spinning artifact.
 */
@Mixin(
    targets = "net.miauczel.legendary_monsters.entity.AnimatedMonster.Projectile.SoulJavelinEntity",
    remap = false
)
public abstract class SoulJavelinReplaceMixin extends AbstractArrow {

    private SoulJavelinReplaceMixin() {
        super(null, null);
    }

    @Shadow(remap = false)
    public abstract boolean isFoil();

    @Unique
    private boolean sjfix$replaced = false;

    @Inject(method = "m_8119_", at = @At("HEAD"), cancellable = true, remap = true)
    private void sjfix$replaceOnFirstTick(CallbackInfo ci) {
        if (this.level().isClientSide() || this.sjfix$replaced) {
            return;
        }
        this.sjfix$replaced = true;

        Entity owner = this.getOwner();
        if (!(owner instanceof LivingEntity livingOwner)) {
            this.discard();
            ci.cancel();
            return;
        }

        Vec3 pos = this.position();
        Vec3 vel = this.getDeltaMovement();
        boolean foil = this.isFoil();

        FixedSoulJavelinEntity replacement = new FixedSoulJavelinEntity(
            this.level(), livingOwner
        );
        // Only copy position and velocity; rotation will be derived from
        // velocity by Projectile.updateRotation() on next tick.
        replacement.setPos(pos.x, pos.y, pos.z);
        replacement.setDeltaMovement(vel.x, vel.y, vel.z);
        replacement.setNoGravity(this.isNoGravity());
        replacement.setHasFoil(foil);

        this.level().addFreshEntity(replacement);
        this.discard();
        ci.cancel();
    }
}
