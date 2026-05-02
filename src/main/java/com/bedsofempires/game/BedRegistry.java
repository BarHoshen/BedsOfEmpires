package com.bedsofempires.game;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.*;

public class BedRegistry {
    private final Map<BlockPos, UUID> bedToOwner = new HashMap<>();
    private final Map<UUID, BlockPos> ownerToBed = new HashMap<>();

    public void registerBed(BlockPos pos, UUID owner) {
        bedToOwner.put(pos, owner);
        ownerToBed.put(owner, pos);
    }

    public void removeBed(BlockPos pos) {
        UUID owner = bedToOwner.remove(pos);
        if (owner != null) {
            ownerToBed.remove(owner);
        }
    }

    public void removeByOwner(UUID owner) {
        BlockPos pos = ownerToBed.remove(owner);
        if (pos != null) {
            bedToOwner.remove(pos);
        }
    }

    public UUID getOwner(BlockPos pos) {
        return bedToOwner.get(pos);
    }

    public BlockPos getBedPos(UUID owner) {
        return ownerToBed.get(owner);
    }

    public boolean isGameBed(BlockPos pos) {
        return bedToOwner.containsKey(pos);
    }

    public boolean hasAliveBed(UUID owner) {
        return ownerToBed.containsKey(owner);
    }

    public Set<BlockPos> getAllBedPositions() {
        return Collections.unmodifiableSet(bedToOwner.keySet());
    }

    public Map<BlockPos, UUID> getAllBeds() {
        return Collections.unmodifiableMap(bedToOwner);
    }

    public Set<UUID> getAliveOwners() {
        return Collections.unmodifiableSet(new HashSet<>(bedToOwner.values()));
    }

    public boolean isWithinProtectionRadius(BlockPos pos, int radius) {
        for (BlockPos bedPos : bedToOwner.keySet()) {
            if (bedPos.closerThan(pos, radius + 0.5)) {
                return true;
            }
        }
        return false;
    }

    public void clear() {
        bedToOwner.clear();
        ownerToBed.clear();
    }

    public int size() {
        return bedToOwner.size();
    }

    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<BlockPos, UUID> entry : bedToOwner.entrySet()) {
            CompoundTag bedTag = new CompoundTag();
            BlockPos pos = entry.getKey();
            bedTag.putInt("x", pos.getX());
            bedTag.putInt("y", pos.getY());
            bedTag.putInt("z", pos.getZ());
            bedTag.putUUID("owner", entry.getValue());
            list.add(bedTag);
        }
        tag.put("beds", list);
        return tag;
    }

    public static BedRegistry load(CompoundTag tag) {
        BedRegistry registry = new BedRegistry();
        if (tag.contains("beds")) {
            ListTag list = tag.getList("beds", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag bedTag = list.getCompound(i);
                BlockPos pos = new BlockPos(bedTag.getInt("x"), bedTag.getInt("y"), bedTag.getInt("z"));
                UUID owner = bedTag.getUUID("owner");
                registry.registerBed(pos, owner);
            }
        }
        return registry;
    }
}
