package com.bedsofempires.game;

import com.bedsofempires.BedsOfEmpires;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class LobbyBuilder {

    private static boolean built = false;
    private static BlockPos roomOrigin = null;

    private static final int ROOM_WIDTH = 19;
    private static final int ROOM_DEPTH = 13;
    private static final int ROOM_HEIGHT = 6;

    public static void buildIfNeeded(ServerLevel level) {
        if (built) return;
        BlockPos spawn = level.getSharedSpawnPos();
        BlockPos surface = findFloor(level, spawn.getX(), spawn.getZ());
        if (surface == null) {
            surface = spawn;
        }
        roomOrigin = surface.offset(-ROOM_WIDTH / 2, 0, -ROOM_DEPTH / 2);
        buildRoom(level, roomOrigin);

        BlockPos center = roomOrigin.offset(ROOM_WIDTH / 2, 0, ROOM_DEPTH / 2);
        level.setDefaultSpawnPos(center, 0.0F);

        // Teleport all online players into the lobby and set their respawn point
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            player.setRespawnPosition(level.dimension(), center, 0.0F, true, false);
            player.teleportTo(level,
                    center.getX() + 0.5, center.getY(), center.getZ() + 0.5,
                    player.getYRot(), player.getXRot());
        }

        built = true;
        BedsOfEmpires.LOGGER.info("Lobby room built at {}", roomOrigin);
    }

    public static BlockPos getRoomCenter() {
        if (roomOrigin == null) return null;
        return roomOrigin.offset(ROOM_WIDTH / 2, 0, ROOM_DEPTH / 2);
    }

    public static void markNeedsRebuild() {
        built = false;
        roomOrigin = null;
    }

    private static BlockPos findFloor(ServerLevel level, int x, int z) {
        for (int y = level.getMaxBuildHeight(); y > level.getMinBuildHeight(); y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (level.getBlockState(pos).isSolidRender(level, pos)
                    && level.getBlockState(pos.above()).isAir()) {
                return pos.above();
            }
        }
        return null;
    }

    private static void buildRoom(ServerLevel level, BlockPos origin) {
        int ox = origin.getX();
        int oy = origin.getY();
        int oz = origin.getZ();

        // Floor
        for (int x = 0; x < ROOM_WIDTH; x++) {
            for (int z = 0; z < ROOM_DEPTH; z++) {
                setBlock(level, ox + x, oy - 1, oz + z, Blocks.POLISHED_DEEPSLATE);
            }
        }

        // Ceiling
        for (int x = 0; x < ROOM_WIDTH; x++) {
            for (int z = 0; z < ROOM_DEPTH; z++) {
                if ((x + z) % 4 == 0) {
                    setBlock(level, ox + x, oy + ROOM_HEIGHT, oz + z, Blocks.SEA_LANTERN);
                } else {
                    setBlock(level, ox + x, oy + ROOM_HEIGHT, oz + z, Blocks.SMOOTH_STONE_SLAB);
                }
            }
        }

        // Walls
        for (int y = 0; y < ROOM_HEIGHT; y++) {
            for (int x = 0; x < ROOM_WIDTH; x++) {
                setBlock(level, ox + x, oy + y, oz, Blocks.STONE_BRICKS);
                setBlock(level, ox + x, oy + y, oz + ROOM_DEPTH - 1, Blocks.STONE_BRICKS);
            }
            for (int z = 0; z < ROOM_DEPTH; z++) {
                setBlock(level, ox, oy + y, oz + z, Blocks.STONE_BRICKS);
                setBlock(level, ox + ROOM_WIDTH - 1, oy + y, oz + z, Blocks.STONE_BRICKS);
            }
        }

        // Clear interior
        for (int x = 1; x < ROOM_WIDTH - 1; x++) {
            for (int z = 1; z < ROOM_DEPTH - 1; z++) {
                for (int y = 0; y < ROOM_HEIGHT; y++) {
                    setBlock(level, ox + x, oy + y, oz + z, Blocks.AIR);
                }
            }
        }

        // Glass panes on side walls (windows)
        for (int y = 1; y < ROOM_HEIGHT - 1; y++) {
            for (int z = 2; z < ROOM_DEPTH - 2; z += 2) {
                setBlock(level, ox, oy + y, oz + z, Blocks.GLASS);
                setBlock(level, ox + ROOM_WIDTH - 1, oy + y, oz + z, Blocks.GLASS);
            }
        }

        // Player list monitor - left side of north wall (signs facing south), 5 wide x 4 tall
        placeSignColumn(level, ox + 2, oy + 1, oz + 1, Direction.SOUTH, 5, 4);

        // Settings monitor - right side of north wall (signs facing south), 5 wide x 4 tall
        placeSignColumn(level, ox + 12, oy + 1, oz + 1, Direction.SOUTH, 5, 4);

        // "PLAYERS" label above left monitor
        placeSign(level, ox + 4, oy + 5, oz + 1, Direction.SOUTH);
        setSignText(level, new BlockPos(ox + 4, oy + 5, oz + 1),
                Component.literal("═══════════"),
                Component.literal("  PLAYERS  "),
                Component.literal("═══════════"),
                Component.empty());

        // "SETTINGS" label above right monitor
        placeSign(level, ox + 14, oy + 5, oz + 1, Direction.SOUTH);
        setSignText(level, new BlockPos(ox + 14, oy + 5, oz + 1),
                Component.literal("═══════════"),
                Component.literal(" SETTINGS  "),
                Component.literal("═══════════"),
                Component.empty());
    }

    private static void placeSignColumn(ServerLevel level, int startX, int startY, int z, Direction facing, int width, int height) {
        for (int dx = 0; dx < width; dx++) {
            for (int dy = 0; dy < height; dy++) {
                placeSign(level, startX + dx, startY + dy, z, facing);
            }
        }
    }

    private static void placeSign(ServerLevel level, int x, int y, int z, Direction facing) {
        BlockPos wallPos = new BlockPos(x, y, z);
        // Place a wall block behind the sign
        setBlock(level, x, y, z - 1 + (facing == Direction.SOUTH ? 0 : 1), Blocks.STONE_BRICKS);

        BlockState signState = Blocks.OAK_WALL_SIGN.defaultBlockState()
                .setValue(WallSignBlock.FACING, facing);
        level.setBlock(wallPos, signState, Block.UPDATE_ALL);
    }

    private static void setBlock(ServerLevel level, int x, int y, int z, Block block) {
        level.setBlock(new BlockPos(x, y, z), block.defaultBlockState(), Block.UPDATE_ALL);
    }

    private static void setSignText(ServerLevel level, BlockPos pos, Component line1, Component line2, Component line3, Component line4) {
        if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
            SignText text = sign.getFrontText()
                    .setMessage(0, line1)
                    .setMessage(1, line2)
                    .setMessage(2, line3)
                    .setMessage(3, line4)
                    .setHasGlowingText(true);
            sign.setText(text, true);
            sign.setChanged();
            level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), Block.UPDATE_ALL);
        }
    }

    public static void updateMonitors(MinecraftServer server) {
        if (roomOrigin == null) return;

        ServerLevel level = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
        if (level == null) return;

        GameSavedData data = GameSavedData.get(level);
        int ox = roomOrigin.getX();
        int oy = roomOrigin.getY();
        int oz = roomOrigin.getZ();

        updatePlayerMonitor(server, level, data, ox + 2, oy + 1, oz + 1);
        updateSettingsMonitor(level, data, ox + 12, oy + 1, oz + 1);
    }

    private static void updatePlayerMonitor(MinecraftServer server, ServerLevel level, GameSavedData data, int startX, int startY, int z) {
        List<Component> lines = new ArrayList<>();
        Map<UUID, String> colors = GameManager.getPlayerColors();
        TeamManager tm = data.getTeamManager();
        Map<UUID, Set<UUID>> teams = tm.getAllTeams();

        // Group players by team
        Set<UUID> listedPlayers = new HashSet<>();

        // First list teams
        int teamNum = 1;
        for (Map.Entry<UUID, Set<UUID>> team : teams.entrySet()) {
            lines.add(Component.literal("Team " + teamNum + ":").withStyle(Style.EMPTY.withBold(true)));
            for (UUID memberId : team.getValue()) {
                ServerPlayer p = server.getPlayerList().getPlayer(memberId);
                String name = p != null ? p.getGameProfile().getName() : "???";
                String color = colors.getOrDefault(memberId, "?");
                lines.add(Component.literal(" " + name + " [" + color + "]"));
                listedPlayers.add(memberId);
            }
            teamNum++;
        }

        // Then solo players
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!listedPlayers.contains(p.getUUID())) {
                String color = colors.getOrDefault(p.getUUID(), "?");
                lines.add(Component.literal(p.getGameProfile().getName() + " [" + color + "]"));
            }
        }

        writeLinesToSignGrid(level, startX, startY, z, 5, 4, lines);
    }

    private static void updateSettingsMonitor(ServerLevel level, GameSavedData data, int startX, int startY, int z) {
        GameSettings s = data.getSettings();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("Mode: " + s.getTeamMode()));
        lines.add(Component.literal("AutoEnd: " + s.isAutoEnd()));
        lines.add(Component.literal("Team Size: " + s.getMaxTeamSize()));
        lines.add(Component.literal("Bed Dist: " + s.getBedDistance()));
        lines.add(Component.literal("Protection: " + s.getProtectionRadius()));
        lines.add(Component.literal("Search: x" + s.getSearchRadiusScale()));
        lines.add(Component.literal("Respawn CD: " + s.getRespawnCooldown() + "s"));
        lines.add(Component.literal("Difficulty: " + s.getDifficulty().getKey()));
        lines.add(Component.literal("Events: " + (s.isWorldEvents() ? "ON" : "OFF")));
        if (s.isWorldEvents()) {
            lines.add(Component.literal("Interval: " + s.getEventInterval() + "min"));
        }

        writeLinesToSignGrid(level, startX, startY, z, 5, 4, lines);
    }

    private static void writeLinesToSignGrid(ServerLevel level, int startX, int startY, int z, int width, int height, List<Component> lines) {
        int lineIdx = 0;
        // Signs are placed bottom-to-top visually, but we write top-to-bottom
        for (int dy = height - 1; dy >= 0; dy--) {
            for (int dx = 0; dx < width; dx++) {
                BlockPos signPos = new BlockPos(startX + dx, startY + dy, z);
                Component[] signLines = new Component[4];
                for (int row = 0; row < 4; row++) {
                    if (lineIdx < lines.size()) {
                        signLines[row] = lines.get(lineIdx++);
                    } else {
                        signLines[row] = Component.empty();
                    }
                }
                setSignText(level, signPos, signLines[0], signLines[1], signLines[2], signLines[3]);
            }
        }
    }
}
