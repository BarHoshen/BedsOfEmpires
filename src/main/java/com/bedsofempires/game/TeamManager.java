package com.bedsofempires.game;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.*;

public class TeamManager {
    private final Map<UUID, UUID> playerToTeam = new HashMap<>();
    private final Map<UUID, Set<UUID>> teams = new HashMap<>();
    private final Map<UUID, UUID> pendingInvites = new HashMap<>();
    private final Map<UUID, Set<UUID>> respawnBlacklist = new HashMap<>();

    public UUID getTeam(UUID player) {
        return playerToTeam.get(player);
    }

    public Set<UUID> getTeamMembers(UUID teamId) {
        return teams.getOrDefault(teamId, Collections.emptySet());
    }

    public boolean isOnSameTeam(UUID player1, UUID player2) {
        UUID team1 = playerToTeam.get(player1);
        UUID team2 = playerToTeam.get(player2);
        return team1 != null && team1.equals(team2);
    }

    public boolean invitePlayer(UUID inviter, UUID invitee, int maxTeamSize) {
        UUID teamId = playerToTeam.get(inviter);
        if (teamId == null) {
            teamId = inviter;
            teams.computeIfAbsent(teamId, k -> new HashSet<>()).add(inviter);
            playerToTeam.put(inviter, teamId);
        }
        Set<UUID> members = teams.get(teamId);
        if (members != null && members.size() >= maxTeamSize) return false;
        pendingInvites.put(invitee, teamId);
        return true;
    }

    public boolean acceptInvite(UUID player, int maxTeamSize) {
        UUID teamId = pendingInvites.remove(player);
        if (teamId == null) return false;
        if (playerToTeam.containsKey(player)) return false;

        Set<UUID> members = teams.get(teamId);
        if (members == null || members.size() >= maxTeamSize) return false;

        members.add(player);
        playerToTeam.put(player, teamId);
        return true;
    }

    public boolean leaveTeam(UUID player) {
        UUID teamId = playerToTeam.remove(player);
        if (teamId == null) return false;
        Set<UUID> members = teams.get(teamId);
        if (members != null) {
            members.remove(player);
            if (members.isEmpty()) {
                teams.remove(teamId);
            }
        }
        return true;
    }

    public void removeFromTeamOnRespawn(UUID player) {
        UUID teamId = playerToTeam.get(player);
        if (teamId != null) {
            leaveTeam(player);
            respawnBlacklist.computeIfAbsent(player, k -> new HashSet<>()).add(teamId);
        }
    }

    public boolean isBlacklistedFromTeam(UUID player, UUID teamId) {
        Set<UUID> blacklisted = respawnBlacklist.get(player);
        return blacklisted != null && blacklisted.contains(teamId);
    }

    public Map<UUID, Set<UUID>> getAllTeams() {
        return Collections.unmodifiableMap(teams);
    }

    public boolean hasPendingInvite(UUID player) {
        return pendingInvites.containsKey(player);
    }

    public void clear() {
        playerToTeam.clear();
        teams.clear();
        pendingInvites.clear();
        respawnBlacklist.clear();
    }

    public CompoundTag save(CompoundTag tag) {
        ListTag teamsList = new ListTag();
        for (Map.Entry<UUID, Set<UUID>> entry : teams.entrySet()) {
            CompoundTag teamTag = new CompoundTag();
            teamTag.putUUID("teamId", entry.getKey());
            ListTag membersList = new ListTag();
            for (UUID member : entry.getValue()) {
                CompoundTag memberTag = new CompoundTag();
                memberTag.putUUID("uuid", member);
                membersList.add(memberTag);
            }
            teamTag.put("members", membersList);
            teamsList.add(teamTag);
        }
        tag.put("teams", teamsList);

        ListTag blacklistTag = new ListTag();
        for (Map.Entry<UUID, Set<UUID>> entry : respawnBlacklist.entrySet()) {
            CompoundTag blTag = new CompoundTag();
            blTag.putUUID("player", entry.getKey());
            ListTag teamsTag = new ListTag();
            for (UUID teamId : entry.getValue()) {
                CompoundTag t = new CompoundTag();
                t.putUUID("teamId", teamId);
                teamsTag.add(t);
            }
            blTag.put("blacklistedTeams", teamsTag);
            blacklistTag.add(blTag);
        }
        tag.put("respawnBlacklist", blacklistTag);

        return tag;
    }

    public static TeamManager load(CompoundTag tag) {
        TeamManager manager = new TeamManager();
        if (tag.contains("teams")) {
            ListTag teamsList = tag.getList("teams", Tag.TAG_COMPOUND);
            for (int i = 0; i < teamsList.size(); i++) {
                CompoundTag teamTag = teamsList.getCompound(i);
                UUID teamId = teamTag.getUUID("teamId");
                Set<UUID> members = new HashSet<>();
                ListTag membersList = teamTag.getList("members", Tag.TAG_COMPOUND);
                for (int j = 0; j < membersList.size(); j++) {
                    UUID member = membersList.getCompound(j).getUUID("uuid");
                    members.add(member);
                    manager.playerToTeam.put(member, teamId);
                }
                manager.teams.put(teamId, members);
            }
        }
        if (tag.contains("respawnBlacklist")) {
            ListTag blacklistTag = tag.getList("respawnBlacklist", Tag.TAG_COMPOUND);
            for (int i = 0; i < blacklistTag.size(); i++) {
                CompoundTag blTag = blacklistTag.getCompound(i);
                UUID player = blTag.getUUID("player");
                Set<UUID> blacklistedTeams = new HashSet<>();
                ListTag teamsTag = blTag.getList("blacklistedTeams", Tag.TAG_COMPOUND);
                for (int j = 0; j < teamsTag.size(); j++) {
                    blacklistedTeams.add(teamsTag.getCompound(j).getUUID("teamId"));
                }
                manager.respawnBlacklist.put(player, blacklistedTeams);
            }
        }
        return manager;
    }
}
