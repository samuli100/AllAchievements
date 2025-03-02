package at.hexle;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
    private boolean gamePaused = false; // New field to track pause state

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

        // Load game mode (using toUpperCase to handle lowercase config values)
        String modeStr = config.getString("gameMode", "SOLO");
        try {
            currentGameMode = GameMode.valueOf(modeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid game mode in gamemode.yml: " + modeStr + ". Defaulting to SOLO.");
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
        gamePaused = config.getBoolean("gamePaused", false); // Load pause state
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
        config.set("gamePaused", gamePaused); // Save pause state

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
        // Return an unmodifiable copy to prevent external modifications
        return Collections.unmodifiableList(new ArrayList<>(activePlayers));
    }

    /**
     * Start a new game with the current settings
     */
    public void startGame() {
        gameActive = true;
        gamePaused = false; // Ensure game starts unpaused

        // In cooperative or versus mode, start the timer for all active players
        if (currentGameMode != GameMode.SOLO) {
            for (UUID playerId : activePlayers) {
                plugin.getPlayerManager().startTimer(playerId);
            }
        }

        saveConfig();
    }

    /**
     * End the current game
     */
    public void endGame() {
        gameActive = false;
        gamePaused = false; // Reset pause state

        // In cooperative or versus mode, pause the timer for all active players
        if (currentGameMode != GameMode.SOLO) {
            for (UUID playerId : activePlayers) {
                PlayerManager.PlayerData data = plugin.getPlayerManager().getPlayerData(playerId);
                if (data.isTimerRunning()) {
                    plugin.getPlayerManager().stopTimer(playerId);
                }
            }
        }

        saveConfig();
    }

    /**
     * Pause or resume the current game
     * @return true if the game is now paused, false if it's now running
     */
    public boolean togglePauseGame() {
        if (!gameActive) {
            return false; // Can't pause/resume if game isn't active
        }

        gamePaused = !gamePaused;

        // Toggle timers for all active players based on game mode
        if (currentGameMode != GameMode.SOLO) {
            for (UUID playerId : activePlayers) {
                PlayerManager.PlayerData data = plugin.getPlayerManager().getPlayerData(playerId);
                data.setTimerRunning(!gamePaused);
                plugin.getPlayerManager().savePlayerData(playerId);

                // Notify player of game state change
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    player.sendMessage("§7-------- §6AllAchievements§7 ----------");
                    player.sendMessage(gamePaused ?
                            "§cThe team achievement challenge has been paused!" :
                            "§aThe team achievement challenge has been resumed!");
                    player.sendMessage("§7--------------------------------");
                }
            }
        }

        saveConfig();
        plugin.getLogger().info("Game " + (gamePaused ? "paused" : "resumed") + " in " + currentGameMode + " mode");
        return gamePaused;
    }

    /**
     * Check if a game is currently active
     */
    public boolean isGameActive() {
        return gameActive;
    }

    /**
     * Check if the game is currently paused
     */
    public boolean isGamePaused() {
        return gamePaused;
    }

    /**
     * Handle advancement completion based on game mode
     * Returns false if advancements are currently disabled due to pause
     */
    public boolean handleAdvancementCompletion(UUID playerId, String advancementKey) {
        // Check if game is paused - if so, don't process advancements
        if (gamePaused) {
            plugin.getLogger().info("Ignoring advancement " + advancementKey +
                    " for player " + playerId + " because game is paused");
            return false;
        }

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

        return true;
    }

    /**
     * Check if a player has won in versus mode
     */
    private void checkVersusWinCondition(UUID playerId) {
        if (currentGameMode == GameMode.VERSUS && gameActive && !gamePaused) {
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