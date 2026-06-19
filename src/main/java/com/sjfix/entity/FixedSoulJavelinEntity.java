package com.sjfix.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
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

    // ---- lifecycle ---------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.tickCount > MAX_LIFETIME) {
            this.discard();
        }
    }

    // ---- collision throttle ------------------------------------------

    @Override
    protected EntityHitResult findHitEntity(Vec3 start, Vec3 end) {
        if (this.level().isClientSide()) {
            return super.findHitEntity(start, end);
        }
        if (this.dealtDamage) {
            return null;
        }
        if (this.tickCount % SCAN_INTERVAL != 0) {
            return null;
        }
        return super.findHitEntity(start, end);
    }

    // ---- hit ---------------------------------------------------------
    // We override onHitEntity (not onHit) so that we fully control damage
    // without AbstractArrow.onHitEntity applying a second (or third) hit.
    // onHit → calls onHitEntity (our override, single damage) or onHitBlock.

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result); // delegates to onHitEntity / onHitBlock
        if (!this.level().isClientSide()) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity hitEntity = result.getEntity();
        Entity owner = this.getOwner();

        if (this.dealtDamage || hitEntity == owner) {
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
            source = new DamageSource(
                this.level().registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(DamageTypes.TRIDENT),
                this, this
            );
        }

        if (hitEntity.hurt(source, damage)) {
            this.dealtDamage = true;
            this.playSound(SoundEvents.TRIDENT_HIT, 1.0F, 1.0F);
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
        return new ItemStack(Items.TRIDENT);
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.TRIDENT_HIT_GROUND;
    }
}
