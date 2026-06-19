package com.sjfix.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Safe replacement for Legendary Monsters SoulJavelinEntity.
 * Extends Projectile directly instead of AbstractArrow to avoid ALL
 * automatic damage, pickup, and tick interference from vanilla arrows.
 */
public class FixedSoulJavelinEntity extends Projectile {

    private static final int MAX_LIFETIME = 600;
    private static final int SCAN_INTERVAL = 4;
    private static final float DAMAGE = 15.0F;
    private static final EntityDataAccessor<Boolean> DATA_FOIL =
        SynchedEntityData.defineId(FixedSoulJavelinEntity.class, EntityDataSerializers.BOOLEAN);

    private int life;

    public FixedSoulJavelinEntity(EntityType<? extends FixedSoulJavelinEntity> type, Level level) {
        super(type, level);
    }

    public FixedSoulJavelinEntity(Level level, LivingEntity shooter) {
        super(SoulJavelinFixMod.FIXED_SOUL_JAVELIN.get(), level);
        this.setOwner(shooter);
        this.setPos(shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ());
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_FOIL, false);
    }

    // ---- tick (fully manual, no AbstractArrow interference) -----------

    @Override
    public void tick() {
        // Entity.baseTick() — handles fire, nether portal, etc.
        super.baseTick();

        if (!this.level().isClientSide()) {
            if (++this.life > MAX_LIFETIME) {
                this.discard();
                return;
            }
        }

        // Movement + collision
        Vec3 movement = this.getDeltaMovement();
        double dx = this.getX() + movement.x;
        double dy = this.getY() + movement.y;
        double dz = this.getZ() + movement.z;
        this.setPos(dx, dy, dz);

        // Only check for entity hits on throttle ticks (server side)
        if (!this.level().isClientSide() && this.tickCount % SCAN_INTERVAL == 0) {
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() == HitResult.Type.ENTITY) {
                this.onHitTarget((EntityHitResult) hit);
            } else if (hit.getType() == HitResult.Type.BLOCK) {
                this.onHitBlock();
            }
        }

        // Throttled scan skipped ticks: reuse last scan position
        // But we already moved, so we just continue flying.

        // Gravity
        if (!this.isNoGravity()) {
            this.setDeltaMovement(movement.x * 0.99, movement.y * 0.99 - 0.05, movement.z * 0.99);
        } else {
            this.setDeltaMovement(movement.x * 0.99, movement.y * 0.99, movement.z * 0.99);
        }

        // Ghost particles (client only)
        if (this.level().isClientSide()) {
            for (int i = 0; i < 2; i++) {
                this.level().addParticle(ParticleTypes.SOUL,
                    this.getX(), this.getY(), this.getZ(),
                    0, 0, 0);
            }
        }
    }

    private void onHitTarget(EntityHitResult result) {
        Entity hitEntity = result.getEntity();
        Entity owner = this.getOwner();
        if (hitEntity == owner) return;

        DamageSource source;
        if (owner instanceof LivingEntity livingOwner) {
            source = this.level().damageSources().trident(this, livingOwner);
        } else {
            source = this.level().damageSources().trident(this, this);
        }

        // Single 15.0 fixed damage
        if (hitEntity.hurt(source, DAMAGE)) {
            this.playSound(SoundEvents.TRIDENT_HIT, 1.0F, 1.0F);
            if (this.level() instanceof ServerLevel sl) {
                for (int i = 0; i < 10; i++) {
                    sl.sendParticles(ParticleTypes.SOUL,
                        this.getX(), this.getY(), this.getZ(),
                        1, 0.3, 0.3, 0.3, 0.05);
                }
            }
        }
        this.discard();
    }

    private void onHitBlock() {
        this.playSound(SoundEvents.TRIDENT_HIT_GROUND, 1.0F, 1.0F);
        this.discard();
    }

    // ---- net/sync ----------------------------------------------------

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.life = tag.getInt("Life");
        this.entityData.set(DATA_FOIL, tag.getBoolean("Foil"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Life", this.life);
        tag.putBoolean("Foil", this.isFoil());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    // ---- accessors ---------------------------------------------------

    public boolean isFoil() {
        return this.entityData.get(DATA_FOIL);
    }

    public void setHasFoil(boolean foil) {
        this.entityData.set(DATA_FOIL, foil);
    }
}
