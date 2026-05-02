package com.bedsofempires.integration;

import com.bedsofempires.BedsOfEmpires;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class JourneyMapIntegration {

    public static void disablePlayerTracking(Path serverDir) {
        try {
            Path jmConfigDir = serverDir.resolve("serverconfig").resolve("journeymap");
            if (!Files.exists(jmConfigDir)) {
                Files.createDirectories(jmConfigDir);
            }

            Path configFile = jmConfigDir.resolve("jm-serverconfig.json");
            String config = """
                    {
                      "playerTrackingEnable": false,
                      "playerTrackerShowHeading": false,
                      "deathWaypointsEnable": false
                    }
                    """;
            Files.writeString(configFile, config);
            BedsOfEmpires.LOGGER.info("JourneyMap server config written: player tracking disabled");
        } catch (IOException e) {
            BedsOfEmpires.LOGGER.warn("Failed to write JourneyMap config: {}", e.getMessage());
        }
    }

    public static void clearPlayerData(Path serverDir, UUID playerId) {
        try {
            Path jmDataDir = serverDir.resolve("journeymap").resolve("data").resolve("mp");
            if (!Files.exists(jmDataDir)) return;

            // JourneyMap stores per-player data in various subdirectories
            // Try to find and delete directories containing the player UUID
            try (var stream = Files.walk(jmDataDir, 2)) {
                stream.filter(Files::isDirectory)
                        .filter(p -> p.getFileName().toString().contains(playerId.toString()))
                        .forEach(p -> {
                            try {
                                deleteRecursive(p);
                                BedsOfEmpires.LOGGER.info("Cleared JourneyMap data for player {}", playerId);
                            } catch (IOException ex) {
                                BedsOfEmpires.LOGGER.warn("Failed to clear JourneyMap data: {}", ex.getMessage());
                            }
                        });
            }
        } catch (IOException e) {
            BedsOfEmpires.LOGGER.warn("Failed to clear JourneyMap player data: {}", e.getMessage());
        }
    }

    private static void deleteRecursive(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var entries = Files.list(path)) {
                entries.forEach(entry -> {
                    try {
                        deleteRecursive(entry);
                    } catch (IOException e) {
                        BedsOfEmpires.LOGGER.warn("Failed to delete: {}", e.getMessage());
                    }
                });
            }
        }
        Files.deleteIfExists(path);
    }
}
