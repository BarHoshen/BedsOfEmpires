package com.bedsofempires.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class BedFinderEntity extends Entity implements ItemSupplier {
    private static final EntityDataAccessor<ItemStack> DATA_ITEM_STACK =
            SynchedEntityData.defineId(BedFinderEntity.class, EntityDataSerializers.ITEM_STACK);

    private double tx;
    private double ty;
    private double tz;
    private int life;
    private boolean surviveAfterDeath;

    public BedFinderEntity(EntityType<? extends BedFinderEntity> type, Level level) {
        super(type, level);
    }

    public void setItem(ItemStack stack) {
        this.getEntityData().set(DATA_ITEM_STACK, stack.copyWithCount(1));
    }

    @Override
    public ItemStack getItem() {
        ItemStack stack = this.getEntityData().get(DATA_ITEM_STACK);
        return stack.isEmpty() ? new ItemStack(Items.ENDER_EYE) : stack;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ITEM_STACK, ItemStack.EMPTY);
    }

    public void setTargetPos(BlockPos pos) {
        double dx = pos.getX() - this.getX();
        double dz = pos.getZ() - this.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (dist > 12.0) {
            this.tx = this.getX() + dx / dist * 12.0;
            this.tz = this.getZ() + dz / dist * 12.0;
        } else {
            this.tx = pos.getX();
            this.tz = pos.getZ();
        }

        this.ty = this.getY() + 8.0;
        this.life = 0;
        this.surviveAfterDeath = this.random.nextInt(5) > 0;
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 movement = this.getDeltaMovement();
        double x = this.getX() + movement.x;
        double y = this.getY() + movement.y;
        double z = this.getZ() + movement.z;

        float horizDist = (float) Math.sqrt(movement.x * movement.x + movement.z * movement.z);

        if (this.level().isClientSide) {
            if (this.life > 0) {
                // particle trail
            }
        } else {
            double dx = this.tx - x;
            double dz = this.tz - z;
            float targetYaw = (float) Mth.atan2(dz, dx);
            float frac = (float) this.life / 80.0F;

            float rise;
            if (frac < 0.4F) {
                rise = 0.8F;
            } else if (frac < 0.7F) {
                rise = 0.2F;
            } else {
                rise = -0.3F;
            }

            float speed = 0.35F;
            this.setDeltaMovement(
                    Math.cos(targetYaw) * speed,
                    rise,
                    Math.sin(targetYaw) * speed
            );
        }

        this.setPos(x, y, z);
        this.life++;

        if (!this.level().isClientSide && this.life > 80) {
            this.playSound(SoundEvents.ENDER_EYE_DEATH, 1.0F, 1.0F);
            this.discard();

            if (this.surviveAfterDeath) {
                ItemEntity itemEntity = new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), this.getItem());
                itemEntity.setDefaultPickUpDelay();
                this.level().addFreshEntity(itemEntity);
            } else {
                this.level().levelEvent(2003, this.blockPosition(), 0);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}
