package at.hexle;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages game modes (solo, coop, versus) for the AllAchievements plugin
 */
public class GameModeManager {

    public enum GameMode {
        SOLO,    // Each player progresses independently
        COOP,    // Players work together to complete advancements
        VERSUS   // Players compete to complete advancements first
    }

    private final AllAchievements plugin;
    private GameMode currentGameMode = GameMode.SOLO;
    private final List<UUID> activePlayers = new ArrayList<>();
    private boolean gameActive = false;

    private final File configFile;
    private FileConfiguration config;

    public GameModeManager(AllAchievements plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "gamemode.yml");
        loadConfig();
    }

    /**
     * Load game mode configuration
     */
    private void loadConfig() {
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create gamemode.yml: " + e.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        // Load game mode
        String modeStr = config.getString("gameMode", "SOLO");
        try {
            currentGameMode = GameMode.valueOf(modeStr);
        } catch (IllegalArgumentException e) {
            currentGameMode = GameMode.SOLO;
        }

        // Load active players
        List<String> playerIds = config.getStringList("activePlayers");
        activePlayers.clear();
        for (String id : playerIds) {
            try {
                activePlayers.add(UUID.fromString(id));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in gamemode.yml: " + id);
            }
        }

        gameActive = config.getBoolean("gameActive", false);
    }

    /**
     * Save game mode configuration
     */
    public void saveConfig() {
        config.set("gameMode", currentGameMode.name());

        List<String> playerIds = new ArrayList<>();
        for (UUID id : activePlayers) {
            playerIds.add(id.toString());
        }
        config.set("activePlayers", playerIds);
        config.set("gameActive", gameActive);

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save gamemode.yml: " + e.getMessage());
        }
    }

    /**
     * Set the active game mode
     */
    public void setGameMode(GameMode mode) {
        this.currentGameMode = mode;
        saveConfig();
    }

    /**
     * Get the current game mode
     */
    public GameMode getGameMode() {
        return currentGameMode;
    }

    /**
     * Add a player to the active game
     */
    public void addPlayer(UUID playerId) {
        if (!activePlayers.contains(playerId)) {
            activePlayers.add(playerId);
            saveConfig();
        }
    }

    /**
     * Remove a player from the active game
     */
    public void removePlayer(UUID playerId) {
        if (activePlayers.contains(playerId)) {
            activePlayers.remove(playerId);
            saveConfig();
        }
    }

    /**
     * Check if a player is part of the active game
     */
    public boolean isPlayerActive(UUID playerId) {
        return activePlayers.contains(playerId);
    }

    /**
     * Get all players in the active game
     */
    public List<UUID> getActivePlayers() {
        return new ArrayList<>(activePlayers);
    }

    /**
     * Start a new game with the current settings
     */
    public void startGame() {
        gameActive = true;

        // In cooperative or versus mode, start the timer for all active players
        if (currentGameMode != GameMode.SOLO) {
            for (UUID playerId : activePlayers) {
                plugin.start(playerId);
            }
        }

        saveConfig();
    }

    /**
     * End the current game
     */
    public void endGame() {
        gameActive = false;

        // In cooperative or versus mode, pause the timer for all active players
        if (currentGameMode != GameMode.SOLO) {
            for (UUID playerId : activePlayers) {
                if (plugin.isRunning(playerId)) {
                    plugin.pause(playerId);
                }
            }
        }

        saveConfig();
    }

    /**
     * Check if a game is currently active
     */
    public boolean isGameActive() {
        return gameActive;
    }

    /**
     * Handle advancement completion based on game mode
     */
    public void handleAdvancementCompletion(UUID playerId, String advancementKey) {
        switch (currentGameMode) {
            case SOLO:
                // In solo mode, advancements only count for the player who earned them
                break;

            case COOP:
                // In coop mode, share advancement with all active players
                for (UUID activeId : activePlayers) {
                    if (!plugin.getPlayerManager().hasCompletedAdvancement(activeId, advancementKey)) {
                        plugin.getPlayerManager().completeAdvancement(activeId, advancementKey);

                        // Notify the player if online
                        Player player = Bukkit.getPlayer(activeId);
                        if (player != null && player.isOnline() && !player.getUniqueId().equals(playerId)) {
                            Player achiever = Bukkit.getPlayer(playerId);
                            String achieverName = achiever != null ? achiever.getName() : "A teammate";
                            player.sendMessage("§7-------- §6AllAchievements§7 ----------");
                            player.sendMessage("§a" + achieverName + " has unlocked an advancement for the team!");
                            player.sendMessage("§7--------------------------------");
                        }
                    }
                }
                break;

            case VERSUS:
                // In versus mode, only the player who earned it gets credit
                // Check if this player has completed all advancements (winning the race)
                checkVersusWinCondition(playerId);
                break;
        }
    }

    /**
     * Check if a player has won in versus mode
     */
    private void checkVersusWinCondition(UUID playerId) {
        if (currentGameMode == GameMode.VERSUS && gameActive) {
            if (plugin.getPlayerManager().hasCompletedAllAdvancements(playerId, plugin.getAdvancementList())) {
                // Player has won the race!
                Player winner = Bukkit.getPlayer(playerId);
                if (winner != null) {
                    // Announce the winner to all players
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendMessage("§7-------- §6AllAchievements§7 ----------");
                        player.sendMessage("§a" + winner.getName() + " has won the advancement race!");
                        player.sendMessage("§7--------------------------------");
                    }

                    // End the game
                    endGame();
                }
            }
        }
    }

    /**
     * Reset all active players
     */
    public void resetAllPlayers() {
        for (UUID playerId : activePlayers) {
            plugin.reset(playerId);
        }
    }
}