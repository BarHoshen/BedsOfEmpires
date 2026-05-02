package com.bedsofempires.integration;

import com.bedsofempires.BedsOfEmpires;

import java.io.IOException;
import java.nio.file.*;
import java.util.Properties;
import java.util.Random;
import java.util.stream.Stream;

public class ServerRestartHelper {

    public static void prepareWorldReset(Path serverDir) {
        updateSeed(serverDir);
        deleteOverworldRegions(serverDir);
    }

    private static void updateSeed(Path serverDir) {
        Path propsFile = serverDir.resolve("server.properties");
        if (!Files.exists(propsFile)) {
            BedsOfEmpires.LOGGER.warn("server.properties not found at {}", propsFile);
            return;
        }

        try {
            Properties props = new Properties();
            try (var reader = Files.newBufferedReader(propsFile)) {
                props.load(reader);
            }
            long newSeed = new Random().nextLong();
            props.setProperty("level-seed", String.valueOf(newSeed));
            try (var writer = Files.newBufferedWriter(propsFile)) {
                props.store(writer, null);
            }
            BedsOfEmpires.LOGGER.info("Updated world seed to {}", newSeed);
        } catch (IOException e) {
            BedsOfEmpires.LOGGER.error("Failed to update server.properties: {}", e.getMessage());
        }
    }

    private static void deleteOverworldRegions(Path serverDir) {
        // The overworld is stored in the "world" directory (default level-name)
        // Try common names
        for (String worldName : new String[]{"world", "."}) {
            Path regionDir = serverDir.resolve(worldName).resolve("region");
            if (Files.exists(regionDir)) {
                deleteDirectoryContents(regionDir);
                BedsOfEmpires.LOGGER.info("Deleted overworld region files from {}", regionDir);
            }

            // Also delete poi and entities directories
            for (String subdir : new String[]{"poi", "entities"}) {
                Path dir = serverDir.resolve(worldName).resolve(subdir);
                if (Files.exists(dir)) {
                    deleteDirectoryContents(dir);
                }
            }

            // Delete level.dat to force regeneration
            Path levelDat = serverDir.resolve(worldName).resolve("level.dat");
            try {
                Files.deleteIfExists(levelDat);
                Files.deleteIfExists(serverDir.resolve(worldName).resolve("level.dat_old"));
            } catch (IOException e) {
                BedsOfEmpires.LOGGER.warn("Failed to delete level.dat: {}", e.getMessage());
            }
        }
    }

    private static void deleteDirectoryContents(Path dir) {
        try (Stream<Path> paths = Files.list(dir)) {
            paths.forEach(path -> {
                try {
                    if (Files.isDirectory(path)) {
                        deleteDirectoryContents(path);
                        Files.delete(path);
                    } else {
                        Files.delete(path);
                    }
                } catch (IOException e) {
                    BedsOfEmpires.LOGGER.warn("Failed to delete {}: {}", path, e.getMessage());
                }
            });
        } catch (IOException e) {
            BedsOfEmpires.LOGGER.warn("Failed to list directory {}: {}", dir, e.getMessage());
        }
    }

    public static void triggerRestart(net.minecraft.server.MinecraftServer server) {
        BedsOfEmpires.LOGGER.info("Triggering server restart for world reset...");
        server.getPlayerList().broadcastSystemMessage(
                net.minecraft.network.chat.Component.literal("Server restarting with a new world in 5 seconds..."), false
        );
        // Schedule the halt - the server wrapper/script should auto-restart
        server.halt(false);
    }
}
