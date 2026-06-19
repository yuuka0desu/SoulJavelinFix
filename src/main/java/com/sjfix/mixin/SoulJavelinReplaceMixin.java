package com.sjfix.mixin;

import com.sjfix.entity.FixedSoulJavelinEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that replaces original SoulJavelinEntity instances with our safe,
 * optimized FixedSoulJavelinEntity on their first server tick.
 *
 * We cannot prevent the original from being spawned (boss AI creates it via
 * "new SoulJavelinEntity()"), but we can discard it immediately on its first
 * tick and spawn a replacement in the same position with the same kinematics.
 * This is transparent to both the spawning code and the player.
 */
@Mixin(
    targets = "net.miauczel.legendary_monsters.entity.AnimatedMonster.Projectile.SoulJavelinEntity",
    remap = false
)
public abstract class SoulJavelinReplaceMixin extends AbstractArrow {

    private SoulJavelinReplaceMixin() {
        super(null, null);
    }

    @Unique
    private boolean sjfix$replaced = false;

    /**
     * On the first server-side tick, copy all relevant state from the original
     * javelin, spawn a FixedSoulJavelinEntity in its place, then discard self.
     */
    @Inject(method = "m_8119_", at = @At("HEAD"), cancellable = true, remap = true)
    private void sjfix$replaceOnFirstTick(CallbackInfo ci) {
        if (this.level().isClientSide() || this.sjfix$replaced) {
            return;
        }
        this.sjfix$replaced = true;

        Entity owner = this.getOwner();
        if (!(owner instanceof LivingEntity livingOwner)) {
            // Owner is invalid – discard the original without replacement
            this.discard();
            ci.cancel();
            return;
        }

        Vec3 pos = this.position();
        Vec3 vel = this.getDeltaMovement();

        // Build a non-empty reference item so the renderer shows a trident
        ItemStack ref = new ItemStack(Items.TRIDENT);

        FixedSoulJavelinEntity replacement = new FixedSoulJavelinEntity(
            this.level(), livingOwner, ref
        );
        replacement.absMoveTo(pos.x, pos.y, pos.z, this.getYRot(), this.getXRot());
        replacement.setDeltaMovement(vel.x, vel.y, vel.z);
        replacement.setNoGravity(this.isNoGravity());

        this.level().addFreshEntity(replacement);

        this.discard();
        ci.cancel();
    }
}
