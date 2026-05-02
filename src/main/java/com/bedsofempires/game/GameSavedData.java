package com.bedsofempires.game;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public class GameSavedData extends SavedData {
    private GameState gameState = GameState.LOBBY;
    private GameSettings settings = new GameSettings();
    private BedRegistry bedRegistry = new BedRegistry();
    private TeamManager teamManager = new TeamManager();
    private final Set<UUID> participants = new HashSet<>();
    private final Set<UUID> eliminatedPlayers = new HashSet<>();
    private long gameStartTick = 0;
    private final Map<UUID, Long> eliminationTimes = new HashMap<>();

    public static GameSavedData create() {
        return new GameSavedData();
    }

    public static GameSavedData load(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        GameSavedData data = create();
        if (tag.contains("gameState")) {
            data.gameState = GameState.valueOf(tag.getString("gameState"));
        }
        if (tag.contains("settings")) {
            data.settings = GameSettings.load(tag.getCompound("settings"));
        }
        if (tag.contains("bedRegistry")) {
            data.bedRegistry = BedRegistry.load(tag.getCompound("bedRegistry"));
        }
        if (tag.contains("participants")) {
            ListTag list = tag.getList("participants", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                data.participants.add(list.getCompound(i).getUUID("uuid"));
            }
        }
        if (tag.contains("eliminatedPlayers")) {
            ListTag list = tag.getList("eliminatedPlayers", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                data.eliminatedPlayers.add(list.getCompound(i).getUUID("uuid"));
            }
        }
        if (tag.contains("teamManager")) {
            data.teamManager = TeamManager.load(tag.getCompound("teamManager"));
        }
        if (tag.contains("gameStartTick")) {
            data.gameStartTick = tag.getLong("gameStartTick");
        }
        if (tag.contains("eliminationTimes")) {
            ListTag list = tag.getList("eliminationTimes", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                data.eliminationTimes.put(entry.getUUID("uuid"), entry.getLong("tick"));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString("gameState", gameState.name());
        tag.put("settings", settings.save(new CompoundTag()));
        tag.put("bedRegistry", bedRegistry.save(new CompoundTag()));
        tag.put("teamManager", teamManager.save(new CompoundTag()));

        ListTag participantsList = new ListTag();
        for (UUID uuid : participants) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("uuid", uuid);
            participantsList.add(entry);
        }
        tag.put("participants", participantsList);

        ListTag eliminatedList = new ListTag();
        for (UUID uuid : eliminatedPlayers) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("uuid", uuid);
            eliminatedList.add(entry);
        }
        tag.put("eliminatedPlayers", eliminatedList);
        tag.putLong("gameStartTick", gameStartTick);

        ListTag eliminationTimesList = new ListTag();
        for (Map.Entry<UUID, Long> entry : eliminationTimes.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID("uuid", entry.getKey());
            entryTag.putLong("tick", entry.getValue());
            eliminationTimesList.add(entryTag);
        }
        tag.put("eliminationTimes", eliminationTimesList);

        return tag;
    }

    public static GameSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new Factory<>(GameSavedData::create, GameSavedData::load),
                "ageofbeds_game"
        );
    }

    public GameState getGameState() { return gameState; }
    public void setGameState(GameState state) { this.gameState = state; setDirty(); }

    public GameSettings getSettings() { return settings; }

    public BedRegistry getBedRegistry() { return bedRegistry; }

    public TeamManager getTeamManager() { return teamManager; }

    public Set<UUID> getParticipants() { return participants; }
    public void addParticipant(UUID uuid) { participants.add(uuid); setDirty(); }

    public Set<UUID> getEliminatedPlayers() { return eliminatedPlayers; }
    public void addEliminatedPlayer(UUID uuid) { eliminatedPlayers.add(uuid); setDirty(); }
    public boolean isEliminated(UUID uuid) { return eliminatedPlayers.contains(uuid); }

    public long getGameStartTick() { return gameStartTick; }
    public void setGameStartTick(long tick) { this.gameStartTick = tick; setDirty(); }

    public void recordElimination(UUID uuid, long tick) {
        eliminationTimes.put(uuid, tick);
        setDirty();
    }

    public long getEliminationTime(UUID uuid) {
        return eliminationTimes.getOrDefault(uuid, 0L);
    }

    public void resetAll() {
        gameState = GameState.LOBBY;
        bedRegistry.clear();
        teamManager.clear();
        participants.clear();
        eliminatedPlayers.clear();
        eliminationTimes.clear();
        gameStartTick = 0;
        setDirty();
    }

    public void markDirty() {
        setDirty();
    }
}
