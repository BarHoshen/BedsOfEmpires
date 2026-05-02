package com.bedsofempires.game;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Difficulty;

public class GameSettings {
    private int bedDistance = 3200;
    private int protectionRadius = 12;
    private String teamMode = "solo";
    private int maxTeamSize = 4;
    private boolean autoEnd = false;
    private int respawnCooldown = 300;
    private int lobbyRadius = 16;
    private int searchRadiusScale = 1;
    private boolean worldEvents = true;
    private int eventInterval = 60;
    private Difficulty difficulty = Difficulty.NORMAL;

    public int getBedDistance() { return bedDistance; }
    public void setBedDistance(int bedDistance) { this.bedDistance = bedDistance; }

    public int getProtectionRadius() { return protectionRadius; }
    public void setProtectionRadius(int protectionRadius) { this.protectionRadius = protectionRadius; }

    public String getTeamMode() { return teamMode; }
    public void setTeamMode(String teamMode) { this.teamMode = teamMode; }

    public int getMaxTeamSize() { return maxTeamSize; }
    public void setMaxTeamSize(int maxTeamSize) { this.maxTeamSize = maxTeamSize; }

    public boolean isAutoEnd() { return autoEnd; }
    public void setAutoEnd(boolean autoEnd) { this.autoEnd = autoEnd; }

    public int getRespawnCooldown() { return respawnCooldown; }
    public void setRespawnCooldown(int respawnCooldown) { this.respawnCooldown = respawnCooldown; }

    public int getLobbyRadius() { return lobbyRadius; }
    public void setLobbyRadius(int lobbyRadius) { this.lobbyRadius = lobbyRadius; }

    public int getSearchRadiusScale() { return searchRadiusScale; }
    public void setSearchRadiusScale(int searchRadiusScale) { this.searchRadiusScale = searchRadiusScale; }

    public boolean isWorldEvents() { return worldEvents; }
    public void setWorldEvents(boolean worldEvents) { this.worldEvents = worldEvents; }

    public int getEventInterval() { return eventInterval; }
    public void setEventInterval(int eventInterval) { this.eventInterval = eventInterval; }

    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }

    public CompoundTag save(CompoundTag tag) {
        tag.putInt("bedDistance", bedDistance);
        tag.putInt("protectionRadius", protectionRadius);
        tag.putString("teamMode", teamMode);
        tag.putInt("maxTeamSize", maxTeamSize);
        tag.putBoolean("autoEnd", autoEnd);
        tag.putInt("respawnCooldown", respawnCooldown);
        tag.putInt("lobbyRadius", lobbyRadius);
        tag.putInt("searchRadiusScale", searchRadiusScale);
        tag.putBoolean("worldEvents", worldEvents);
        tag.putInt("eventInterval", eventInterval);
        tag.putString("difficulty", difficulty.getKey());
        return tag;
    }

    public static GameSettings load(CompoundTag tag) {
        GameSettings settings = new GameSettings();
        if (tag.contains("bedDistance")) settings.bedDistance = tag.getInt("bedDistance");
        if (tag.contains("protectionRadius")) settings.protectionRadius = tag.getInt("protectionRadius");
        if (tag.contains("teamMode")) settings.teamMode = tag.getString("teamMode");
        if (tag.contains("maxTeamSize")) settings.maxTeamSize = tag.getInt("maxTeamSize");
        if (tag.contains("autoEnd")) settings.autoEnd = tag.getBoolean("autoEnd");
        if (tag.contains("respawnCooldown")) settings.respawnCooldown = tag.getInt("respawnCooldown");
        if (tag.contains("lobbyRadius")) settings.lobbyRadius = tag.getInt("lobbyRadius");
        if (tag.contains("searchRadiusScale")) settings.searchRadiusScale = tag.getInt("searchRadiusScale");
        if (tag.contains("worldEvents")) settings.worldEvents = tag.getBoolean("worldEvents");
        if (tag.contains("eventInterval")) settings.eventInterval = tag.getInt("eventInterval");
        if (tag.contains("difficulty")) {
            String key = tag.getString("difficulty");
            for (Difficulty d : Difficulty.values()) {
                if (d.getKey().equals(key)) {
                    settings.difficulty = d;
                    break;
                }
            }
        }
        return settings;
    }
}
