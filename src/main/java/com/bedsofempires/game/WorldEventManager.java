package com.bedsofempires.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Set;

public class WorldEventManager {

    private static final String[] EVENT_NAMES = {
            "Pillager Scouts", "Zombie Siege", "Phantom Swarm", "Blood Moon"
    };

    private static int nextEventIndex = 0;
    private static long nextEventTick = 0;
    private static boolean scheduled = false;
    private static boolean warningGiven = false;
    private static boolean bloodMoonActive = false;

    public static boolean isBloodMoonActive() {
        return bloodMoonActive;
    }

    public static void reset() {
        nextEventIndex = 0;
        nextEventTick = 0;
        scheduled = false;
        warningGiven = false;
        bloodMoonActive = false;
    }

    public static void tick(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        GameSavedData data = GameSavedData.get(overworld);
        if (data.getGameState() != GameState.IN_PROGRESS) return;
        if (!data.getSettings().isWorldEvents()) return;

        long gameTime = overworld.getGameTime();
        long gameStartTick = data.getGameStartTick();

        if (!scheduled) {
            scheduleNextEvent(gameStartTick, data.getSettings().getEventInterval());
            scheduled = true;
        }

        long warningTick = nextEventTick - (60L * 20L);

        if (!warningGiven && gameTime >= warningTick) {
            int eventType = nextEventIndex % EVENT_NAMES.length;
            broadcastTitle(server, "WARNING", EVENT_NAMES[eventType] + " in 60 seconds!");
            warningGiven = true;
        }

        if (gameTime >= nextEventTick) {
            int eventType = nextEventIndex % EVENT_NAMES.length;
            executeEvent(server, overworld, data, eventType);
            nextEventIndex++;
            warningGiven = false;
            scheduleNextEvent(gameStartTick, data.getSettings().getEventInterval());
        }
    }

    private static void scheduleNextEvent(long gameStartTick, int baseIntervalMinutes) {
        java.util.Random rng = new java.util.Random();
        // Exponential base: event N waits base * 2^N minutes
        long baseMinutes = baseIntervalMinutes * (1L << nextEventIndex);
        // Random variance ±0-20%
        double variancePercent = (rng.nextDouble() * 0.4 - 0.2);
        long actualMinutes = Math.max(1, (long) (baseMinutes * (1.0 + variancePercent)));
        // Cumulative: sum all previous intervals
        long cumulativeTicks = 0;
        for (int i = 0; i < nextEventIndex; i++) {
            cumulativeTicks += baseIntervalMinutes * (1L << i) * 60L * 20L;
        }
        nextEventTick = gameStartTick + cumulativeTicks + actualMinutes * 60L * 20L;
    }

    private static void executeEvent(MinecraftServer server, ServerLevel level, GameSavedData data, int eventType) {
        Set<BlockPos> beds = data.getBedRegistry().getAllBedPositions();

        broadcastTitle(server, "EVENT", EVENT_NAMES[eventType] + "!");

        switch (eventType) {
            case 0 -> spawnPillagerScouts(level, beds);
            case 1 -> spawnZombieSiege(level, beds);
            case 2 -> spawnPhantomSwarm(level, beds);
            case 3 -> activateBloodMoon(server, level);
        }
    }

    private static void broadcastTitle(MinecraftServer server, String title, String subtitle) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title)));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitle)));
        }
    }

    private static void spawnPillagerScouts(ServerLevel level, Set<BlockPos> beds) {
        for (BlockPos bedPos : beds) {
            for (int i = 0; i < 3; i++) {
                spawnMobFarAndTarget(level, bedPos, EntityType.PILLAGER, 40, 60);
            }
        }
    }

    private static void spawnZombieSiege(ServerLevel level, Set<BlockPos> beds) {
        for (BlockPos bedPos : beds) {
            for (int i = 0; i < 8; i++) {
                spawnMobFarAndTarget(level, bedPos, EntityType.ZOMBIE, 45, 65);
            }
        }
    }

    private static void spawnPhantomSwarm(ServerLevel level, Set<BlockPos> beds) {
        for (BlockPos bedPos : beds) {
            for (int i = 0; i < 5; i++) {
                double angle = level.getRandom().nextDouble() * Math.PI * 2;
                int dist = 50 + level.getRandom().nextInt(20);
                int x = bedPos.getX() + (int) (Math.cos(angle) * dist);
                int z = bedPos.getZ() + (int) (Math.sin(angle) * dist);
                BlockPos spawnPos = new BlockPos(x, bedPos.getY() + 20 + level.getRandom().nextInt(15), z);
                EntityType.PHANTOM.spawn(level, spawnPos, MobSpawnType.EVENT);
            }
        }
    }

    private static <T extends Entity> void spawnMobFarAndTarget(ServerLevel level, BlockPos bedPos, EntityType<T> type, int minDist, int maxDist) {
        double angle = level.getRandom().nextDouble() * Math.PI * 2;
        int dist = minDist + level.getRandom().nextInt(maxDist - minDist);
        int x = bedPos.getX() + (int) (Math.cos(angle) * dist);
        int z = bedPos.getZ() + (int) (Math.sin(angle) * dist);
        BlockPos spawnPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));

        T entity = type.spawn(level, spawnPos, MobSpawnType.EVENT);
        if (entity instanceof Mob mob) {
            mob.getNavigation().moveTo(bedPos.getX() + 0.5, bedPos.getY(), bedPos.getZ() + 0.5, 1.0);
            mob.setPersistenceRequired();
        }
    }

    private static void activateBloodMoon(MinecraftServer server, ServerLevel level) {
        bloodMoonActive = true;
        broadcastTitle(server, "Blood Moon", "A Blood Moon is rising...");
        level.setDayTime(13000);
    }
}
