package com.sjfix.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class FixedSoulJavelinEntity extends AbstractArrow {

    private static final int MAX_LIFETIME = 600;
    private static final int SCAN_INTERVAL = 4;
    private boolean hasFoil;

    public FixedSoulJavelinEntity(EntityType<? extends FixedSoulJavelinEntity> type, Level level) {
        super(type, level);
    }

    public FixedSoulJavelinEntity(Level level, LivingEntity shooter) {
        super(SoulJavelinFixMod.FIXED_SOUL_JAVELIN.get(), shooter, level);
    }

    // ---- lifecycle ---------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            // Never drop as an item
            this.pickup = Pickup.DISALLOWED;
            if (this.tickCount > MAX_LIFETIME) {
                this.discard();
            }
        }
    }

    // ---- collision throttle ------------------------------------------

    @Override
    protected EntityHitResult findHitEntity(Vec3 start, Vec3 end) {
        if (this.level().isClientSide()) {
            return super.findHitEntity(start, end);
        }
        if (this.tickCount % SCAN_INTERVAL != 0) {
            return null;
        }
        return super.findHitEntity(start, end);
    }

    // ---- hit: single damage only, no AbstractArrow double-hit ---------

    @Override
    protected void onHit(HitResult result) {
        // Do NOT call super.onHit() — it would trigger AbstractArrow.onHitEntity
        // which deals a second damage instance.
        if (result instanceof EntityHitResult entityResult) {
            onHitTarget(entityResult);
        } else {
            // Block hit: play sound and discard
            this.playSound(this.getDefaultHitGroundSoundEvent(), 1.0F, 1.0F);
        }
        if (!this.level().isClientSide()) {
            this.discard();
        }
    }

    private void onHitTarget(EntityHitResult result) {
        Entity hitEntity = result.getEntity();
        Entity owner = this.getOwner();
        if (hitEntity == owner) {
            return;
        }

        float damage = 8.0F;
        if (hitEntity instanceof LivingEntity livingTarget) {
            damage += livingTarget.getMaxHealth() * 0.03F;
        }

        DamageSource source;
        if (owner instanceof LivingEntity livingOwner) {
            source = livingOwner.damageSources().trident(this, livingOwner);
        } else {
            source = this.level().damageSources().trident(this, this);
        }

        if (hitEntity.hurt(source, damage)) {
            this.playSound(SoundEvents.TRIDENT_HIT, 1.0F, 1.0F);
            // ghostly soul particles (matching original)
            if (this.level() instanceof ServerLevel sl) {
                for (int i = 0; i < 7; i++) {
                    sl.sendParticles(ParticleTypes.SOUL,
                        this.getX(), this.getY(), this.getZ(),
                        1, 0.3, 0.3, 0.3, 0.02);
                }
            }
        }
    }

    // ---- visual / sound / pickup --------------------------------------

    public boolean isFoil() {
        return this.hasFoil;
    }

    public void setHasFoil(boolean foil) {
        this.hasFoil = foil;
    }

    @Override
    protected ItemStack getPickupItem() {
        // Never drop — pickup is forced to DISALLOWED every tick
        return ItemStack.EMPTY;
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.TRIDENT_HIT_GROUND;
    }
}
