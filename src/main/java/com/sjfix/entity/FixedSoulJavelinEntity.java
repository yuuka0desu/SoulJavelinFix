package com.sjfix.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Optimised replacement for Legendary Monsters SoulJavelinEntity.
 *
 * - Safe owner handling (no ClassCastException)
 * - Throttled per-tick collision scans
 * - Lifetime cap (600 ticks)
 * - Single hit-damage (does NOT call super.onHit)
 * - Never drops a pickup item
 */
public class FixedSoulJavelinEntity extends AbstractArrow {

    private static final int MAX_LIFETIME = 600;
    private static final int SCAN_INTERVAL = 4;
    private boolean dealtDamage;
    private boolean hasFoil;

    public FixedSoulJavelinEntity(EntityType<? extends FixedSoulJavelinEntity> type, Level level) {
        super(type, level);
    }

    public FixedSoulJavelinEntity(Level level, LivingEntity shooter) {
        super(SoulJavelinFixMod.FIXED_SOUL_JAVELIN.get(), shooter, level);
    }

    // ---- tick ---------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        this.pickup = AbstractArrow.Pickup.DISALLOWED; // never droppable
        if (!this.level().isClientSide() && this.tickCount > MAX_LIFETIME) {
            this.discard();
        }
    }

    // ---- collision ----------------------------------------------------

    @Override
    protected EntityHitResult findHitEntity(Vec3 start, Vec3 end) {
        if (this.level().isClientSide()) return super.findHitEntity(start, end);
        if (this.dealtDamage) return null;
        if (this.tickCount % SCAN_INTERVAL != 0) return null;
        return super.findHitEntity(start, end);
    }

    // ---- hit (single damage, no super call) ---------------------------

    @Override
    protected void onHit(HitResult result) {
        // Fully self-contained hit – do NOT call super, which would apply
        // AbstractArrow.onHitEntity's own damage as a second hit.
        HitResult.Type type = result.getType();

        if (type == HitResult.Type.ENTITY && !this.dealtDamage) {
            EntityHitResult entityResult = (EntityHitResult) result;
            Entity target = entityResult.getEntity();
            Entity owner = this.getOwner();

            if (target != owner) {
                float damage = 8.0F;
                if (target instanceof LivingEntity livingTarget) {
                    damage += livingTarget.getMaxHealth() * 0.03F;
                }

                DamageSource source;
                if (owner instanceof LivingEntity livingOwner) {
                    source = livingOwner.damageSources().trident(this, livingOwner);
                } else {
                    source = this.level().damageSources().trident(this, this);
                }

                if (target.hurt(source, damage)) {
                    this.dealtDamage = true;
                    this.playSound(SoundEvents.TRIDENT_HIT, 1.0F, 1.0F);
                }
            }
        } else if (type == HitResult.Type.BLOCK) {
            this.playSound(this.getDefaultHitGroundSoundEvent(), 1.0F, 1.0F);
        }

        if (!this.level().isClientSide()) {
            this.discard();
        }
    }

    // ---- visual -------------------------------------------------------

    public boolean isFoil()          { return this.hasFoil; }
    public void setHasFoil(boolean f){ this.hasFoil = f; }

    @Override
    protected ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.TRIDENT_HIT_GROUND;
    }
}
