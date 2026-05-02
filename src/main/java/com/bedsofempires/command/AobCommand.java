package com.bedsofempires.command;

import com.bedsofempires.game.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import com.bedsofempires.game.TeamManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.commands.arguments.EntityArgument;

@EventBusSubscriber(modid = "bedsofempires")
public class AobCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("aob")
                .executes(ctx -> showHelp(ctx.getSource()))
                .then(Commands.literal("help")
                        .executes(ctx -> showHelp(ctx.getSource())))
                .then(Commands.literal("start")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> startGame(ctx.getSource())))
                .then(Commands.literal("reset")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> resetGame(ctx.getSource())))
                .then(Commands.literal("status")
                        .executes(ctx -> showStatus(ctx.getSource())))
                .then(Commands.literal("color")
                        .then(Commands.argument("color", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    for (String c : BedSpawner.getAllColorNames()) {
                                        builder.suggest(c);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> setColor(ctx.getSource(), StringArgumentType.getString(ctx, "color")))))
                .then(Commands.literal("respawn")
                        .executes(ctx -> respawnPlayer(ctx.getSource())))
                .then(Commands.literal("settings")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("list")
                                .executes(ctx -> listSettings(ctx.getSource())))
                        .then(Commands.literal("bedDistance")
                                .then(Commands.argument("value", IntegerArgumentType.integer(1))
                                        .executes(ctx -> setSetting(ctx.getSource(), "bedDistance",
                                                IntegerArgumentType.getInteger(ctx, "value")))))
                        .then(Commands.literal("protectionRadius")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                        .executes(ctx -> setSetting(ctx.getSource(), "protectionRadius",
                                                IntegerArgumentType.getInteger(ctx, "value")))))
                        .then(Commands.literal("lobbyRadius")
                                .then(Commands.argument("value", IntegerArgumentType.integer(1))
                                        .executes(ctx -> setSetting(ctx.getSource(), "lobbyRadius",
                                                IntegerArgumentType.getInteger(ctx, "value")))))
                        .then(Commands.literal("searchRadiusScale")
                                .then(Commands.argument("value", IntegerArgumentType.integer(1))
                                        .executes(ctx -> setSetting(ctx.getSource(), "searchRadiusScale",
                                                IntegerArgumentType.getInteger(ctx, "value")))))
                        .then(Commands.literal("maxTeamSize")
                                .then(Commands.argument("value", IntegerArgumentType.integer(1))
                                        .executes(ctx -> setSetting(ctx.getSource(), "maxTeamSize",
                                                IntegerArgumentType.getInteger(ctx, "value")))))
                        .then(Commands.literal("respawnCooldown")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                        .executes(ctx -> setSetting(ctx.getSource(), "respawnCooldown",
                                                IntegerArgumentType.getInteger(ctx, "value")))))
                        .then(Commands.literal("eventInterval")
                                .then(Commands.argument("value", IntegerArgumentType.integer(1))
                                        .executes(ctx -> setSetting(ctx.getSource(), "eventInterval",
                                                IntegerArgumentType.getInteger(ctx, "value")))))
                        .then(Commands.literal("autoEnd")
                                .then(Commands.argument("value", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            builder.suggest("true");
                                            builder.suggest("false");
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> setAutoEnd(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "value")))))
                        .then(Commands.literal("teamMode")
                                .then(Commands.argument("value", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            builder.suggest("solo");
                                            builder.suggest("allied");
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> setTeamMode(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "value")))))
                        .then(Commands.literal("worldEvents")
                                .then(Commands.argument("value", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            builder.suggest("on");
                                            builder.suggest("off");
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> setWorldEvents(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "value")))))
                        .then(Commands.literal("difficulty")
                                .then(Commands.argument("value", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            builder.suggest("peaceful");
                                            builder.suggest("easy");
                                            builder.suggest("normal");
                                            builder.suggest("hard");
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> setDifficulty(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "value"))))))
                .then(Commands.literal("team")
                        .then(Commands.literal("invite")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> teamInvite(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("accept")
                                .executes(ctx -> teamAccept(ctx.getSource())))
                        .then(Commands.literal("leave")
                                .executes(ctx -> teamLeave(ctx.getSource())))
                        .then(Commands.literal("list")
                                .executes(ctx -> teamList(ctx.getSource()))))
        );
    }

    private static int showHelp(CommandSourceStack source) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Beds of Empires Commands ===\n");
        sb.append("/aob start - Start the game (admin)\n");
        sb.append("/aob reset - Force-end and reset (admin)\n");
        sb.append("/aob status - Show game state and your bed status\n");
        sb.append("/aob color <color> - Choose bed color (lobby)\n");
        sb.append("/aob respawn - Respawn after elimination\n");
        sb.append("/aob team invite <player> - Invite to team\n");
        sb.append("/aob team accept - Accept team invite\n");
        sb.append("/aob team leave - Leave your team\n");
        sb.append("/aob team list - List all teams\n");
        sb.append("/aob settings list - Show all settings (admin)\n");
        sb.append("/aob settings <key> <value> - Change a setting (admin)");
        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int startGame(CommandSourceStack source) {
        ServerLevel overworld = source.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return 0;

        GameSavedData data = GameSavedData.get(overworld);
        if (data.getGameState() != GameState.LOBBY) {
            source.sendFailure(Component.literal("Game can only be started from LOBBY state."));
            return 0;
        }

        if (GameManager.startGame(source.getServer())) {
            source.sendSuccess(() -> Component.literal("Game started!"), true);
            return 1;
        }
        source.sendFailure(Component.literal("Failed to start game. Are there any players?"));
        return 0;
    }

    private static int resetGame(CommandSourceStack source) {
        GameManager.resetGame(source.getServer());
        source.sendSuccess(() -> Component.literal("Game reset."), true);
        return 1;
    }

    private static int showStatus(CommandSourceStack source) {
        ServerLevel overworld = source.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return 0;

        GameSavedData data = GameSavedData.get(overworld);
        StringBuilder sb = new StringBuilder();
        sb.append("=== Beds of Empires Status ===\n");
        sb.append("State: ").append(data.getGameState()).append("\n");
        sb.append("Beds alive: ").append(data.getBedRegistry().size()).append("\n");
        sb.append("Participants: ").append(data.getParticipants().size()).append("\n");
        sb.append("Eliminated: ").append(data.getEliminatedPlayers().size()).append("\n");

        if (source.getEntity() instanceof ServerPlayer player) {
            UUID playerId = player.getUUID();
            boolean hasBed = data.getBedRegistry().hasAliveBed(playerId);
            sb.append("Your bed: ").append(hasBed ? "ALIVE" : "DESTROYED");
        }

        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int setColor(CommandSourceStack source, String color) {
        ServerLevel overworld = source.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return 0;

        GameSavedData data = GameSavedData.get(overworld);
        if (data.getGameState() != GameState.LOBBY) {
            source.sendFailure(Component.literal("You can only choose a color during the lobby phase."));
            return 0;
        }

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can choose a color."));
            return 0;
        }

        if (!BedSpawner.getAllColorNames().contains(color.toLowerCase())) {
            source.sendFailure(Component.literal("Invalid color. Use /aob color <color> with one of: " +
                    String.join(", ", BedSpawner.getAllColorNames())));
            return 0;
        }

        GameManager.setPlayerColor(player.getUUID(), color.toLowerCase());
        source.sendSuccess(() -> Component.literal("Bed color set to " + color + "."), false);
        return 1;
    }

    private static int listSettings(CommandSourceStack source) {
        ServerLevel overworld = source.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return 0;

        GameSettings s = GameSavedData.get(overworld).getSettings();
        StringBuilder sb = new StringBuilder("=== Game Settings ===\n");
        sb.append("bedDistance: ").append(s.getBedDistance()).append("\n");
        sb.append("protectionRadius: ").append(s.getProtectionRadius()).append("\n");
        sb.append("teamMode: ").append(s.getTeamMode()).append("\n");
        sb.append("maxTeamSize: ").append(s.getMaxTeamSize()).append("\n");
        sb.append("autoEnd: ").append(s.isAutoEnd()).append("\n");
        sb.append("respawnCooldown: ").append(s.getRespawnCooldown()).append("s\n");
        sb.append("lobbyRadius: ").append(s.getLobbyRadius()).append("\n");
        sb.append("searchRadiusScale: ").append(s.getSearchRadiusScale()).append("x\n");
        sb.append("worldEvents: ").append(s.isWorldEvents() ? "on" : "off").append("\n");
        sb.append("eventInterval: ").append(s.getEventInterval()).append(" min\n");
        sb.append("difficulty: ").append(s.getDifficulty().getKey());

        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int setSetting(CommandSourceStack source, String key, int value) {
        ServerLevel overworld = source.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return 0;

        GameSavedData data = GameSavedData.get(overworld);
        if (data.getGameState() != GameState.LOBBY) {
            source.sendFailure(Component.literal("Settings can only be changed during the lobby phase."));
            return 0;
        }

        GameSettings s = data.getSettings();
        switch (key) {
            case "bedDistance" -> s.setBedDistance(value);
            case "protectionRadius" -> s.setProtectionRadius(value);
            case "lobbyRadius" -> s.setLobbyRadius(value);
            case "searchRadiusScale" -> s.setSearchRadiusScale(value);
            case "maxTeamSize" -> s.setMaxTeamSize(value);
            case "respawnCooldown" -> s.setRespawnCooldown(value);
            case "eventInterval" -> s.setEventInterval(value);
        }
        data.markDirty();
        source.sendSuccess(() -> Component.literal(key + " set to " + value), true);
        return 1;
    }

    private static int setAutoEnd(CommandSourceStack source, String value) {
        ServerLevel overworld = source.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return 0;

        GameSavedData data = GameSavedData.get(overworld);
        if (data.getGameState() != GameState.LOBBY) {
            source.sendFailure(Component.literal("Settings can only be changed during the lobby phase."));
            return 0;
        }

        boolean enabled = value.equalsIgnoreCase("true");
        data.getSettings().setAutoEnd(enabled);
        data.markDirty();
        source.sendSuccess(() -> Component.literal("autoEnd set to " + enabled), true);
        return 1;
    }

    private static int setTeamMode(CommandSourceStack source, String value) {
        ServerLevel overworld = source.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return 0;

        GameSavedData data = GameSavedData.get(overworld);
        if (data.getGameState() != GameState.LOBBY) {
            source.sendFailure(Component.literal("Settings can only be changed during the lobby phase."));
            return 0;
        }

        if (!value.equals("solo") && !value.equals("allied")) {
            source.sendFailure(Component.literal("teamMode must be 'solo' or 'allied'."));
            return 0;
        }

        data.getSettings().setTeamMode(value);
        data.markDirty();
        source.sendSuccess(() -> Component.literal("teamMode set to " + value), true);
        return 1;
    }

    private static int setWorldEvents(CommandSourceStack source, String value) {
        ServerLevel overworld = source.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return 0;

        GameSavedData data = GameSavedData.get(overworld);
        if (data.getGameState() != GameState.LOBBY) {
            source.sendFailure(Component.literal("Settings can only be changed during the lobby phase."));
            return 0;
        }

        boolean enabled = value.equalsIgnoreCase("on");
        data.getSettings().setWorldEvents(enabled);
        data.markDirty();
        source.sendSuccess(() -> Component.literal("worldEvents set to " + (enabled ? "on" : "off")), true);
        return 1;
    }

    private static int respawnPlayer(CommandSourceStack source) {
        ServerLevel overworld = source.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return 0;

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can respawn."));
            return 0;
        }

        GameSavedData data = GameSavedData.get(overworld);

        if (data.getGameState() != GameState.IN_PROGRESS) {
            source.sendFailure(Component.literal("No game in progress."));
            return 0;
        }

        if (data.getSettings().isAutoEnd()) {
            source.sendFailure(Component.literal("Respawn is disabled in auto-end mode."));
            return 0;
        }

        if (!data.isEliminated(player.getUUID())) {
            source.sendFailure(Component.literal("You are not eliminated."));
            return 0;
        }

        long elapsed = overworld.getGameTime() - data.getEliminationTime(player.getUUID());
        long cooldownTicks = data.getSettings().getRespawnCooldown() * 20L;
        if (elapsed < cooldownTicks) {
            long remainingSeconds = (cooldownTicks - elapsed) / 20;
            source.sendFailure(Component.literal("You must wait " + remainingSeconds + " more seconds to respawn."));
            return 0;
        }

        if (GameManager.respawnPlayer(player, source.getServer())) {
            source.sendSuccess(() -> Component.literal("You have respawned with a new bed!"), false);
            source.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal(player.getGameProfile().getName() + " has respawned!"), false
            );
            return 1;
        }

        source.sendFailure(Component.literal("Failed to find a valid bed location. Try again."));
        return 0;
    }

    private static int teamInvite(CommandSourceStack source, ServerPlayer target) {
        ServerLevel overworld = source.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return 0;
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;

        GameSavedData data = GameSavedData.get(overworld);
        if (data.getGameState() != GameState.LOBBY) {
            source.sendFailure(Component.literal("Teams can only be formed during the lobby phase."));
            return 0;
        }

        if (player.getUUID().equals(target.getUUID())) {
            source.sendFailure(Component.literal("You cannot invite yourself."));
            return 0;
        }

        TeamManager tm = data.getTeamManager();
        UUID inviterTeam = tm.getTeam(player.getUUID());
        UUID teamIdForBlacklistCheck = inviterTeam != null ? inviterTeam : player.getUUID();
        if (tm.isBlacklistedFromTeam(target.getUUID(), teamIdForBlacklistCheck)) {
            source.sendFailure(Component.literal("This player cannot rejoin your team."));
            return 0;
        }

        if (tm.invitePlayer(player.getUUID(), target.getUUID(), data.getSettings().getMaxTeamSize())) {
            source.sendSuccess(() -> Component.literal("Invited " + target.getGameProfile().getName() + " to your team."), false);
            target.sendSystemMessage(Component.literal(player.getGameProfile().getName() + " invited you to their team. Use /aob team accept to join."));
            data.markDirty();
            return 1;
        }
        source.sendFailure(Component.literal("Team is full."));
        return 0;
    }

    private static int teamAccept(CommandSourceStack source) {
        ServerLevel overworld = source.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return 0;
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;

        GameSavedData data = GameSavedData.get(overworld);
        // Allow accepting during LOBBY or IN_PROGRESS (for respawned players in allied mode)
        TeamManager tm = data.getTeamManager();
        if (!tm.hasPendingInvite(player.getUUID())) {
            source.sendFailure(Component.literal("You have no pending team invite."));
            return 0;
        }

        if (tm.acceptInvite(player.getUUID(), data.getSettings().getMaxTeamSize())) {
            source.sendSuccess(() -> Component.literal("You joined the team!"), false);
            data.markDirty();
            return 1;
        }
        source.sendFailure(Component.literal("Could not join team. It may be full."));
        return 0;
    }

    private static int teamLeave(CommandSourceStack source) {
        ServerLevel overworld = source.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return 0;
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;

        GameSavedData data = GameSavedData.get(overworld);
        if (data.getGameState() != GameState.LOBBY) {
            source.sendFailure(Component.literal("You can only leave a team during the lobby phase."));
            return 0;
        }

        TeamManager tm = data.getTeamManager();
        if (tm.leaveTeam(player.getUUID())) {
            source.sendSuccess(() -> Component.literal("You left your team."), false);
            data.markDirty();
            return 1;
        }
        source.sendFailure(Component.literal("You are not on a team."));
        return 0;
    }

    private static int teamList(CommandSourceStack source) {
        ServerLevel overworld = source.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return 0;

        GameSavedData data = GameSavedData.get(overworld);
        TeamManager tm = data.getTeamManager();
        Map<UUID, Set<UUID>> allTeams = tm.getAllTeams();

        if (allTeams.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No teams formed yet."), false);
            return 1;
        }

        StringBuilder sb = new StringBuilder("=== Teams ===\n");
        int teamNum = 1;
        for (Map.Entry<UUID, Set<UUID>> entry : allTeams.entrySet()) {
            sb.append("Team ").append(teamNum++).append(": ");
            List<String> names = new ArrayList<>();
            for (UUID memberId : entry.getValue()) {
                var memberPlayer = source.getServer().getPlayerList().getPlayer(memberId);
                names.add(memberPlayer != null ? memberPlayer.getGameProfile().getName() : memberId.toString().substring(0, 8));
            }
            sb.append(String.join(", ", names)).append("\n");
        }

        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int setDifficulty(CommandSourceStack source, String value) {
        ServerLevel overworld = source.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return 0;

        GameSavedData data = GameSavedData.get(overworld);
        if (data.getGameState() != GameState.LOBBY) {
            source.sendFailure(Component.literal("Settings can only be changed during the lobby phase."));
            return 0;
        }

        net.minecraft.world.Difficulty diff = switch (value.toLowerCase()) {
            case "peaceful" -> net.minecraft.world.Difficulty.PEACEFUL;
            case "easy" -> net.minecraft.world.Difficulty.EASY;
            case "normal" -> net.minecraft.world.Difficulty.NORMAL;
            case "hard" -> net.minecraft.world.Difficulty.HARD;
            default -> null;
        };

        if (diff == null) {
            source.sendFailure(Component.literal("Invalid difficulty. Use: peaceful, easy, normal, hard"));
            return 0;
        }

        data.getSettings().setDifficulty(diff);
        data.markDirty();
        source.sendSuccess(() -> Component.literal("difficulty set to " + value), true);
        return 1;
    }
}
