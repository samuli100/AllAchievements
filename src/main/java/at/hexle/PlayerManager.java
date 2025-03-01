package at.hexle;

import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Class to handle player-specific advancement tracking
 */
public class PlayerManager {

    private final AllAchievements plugin;
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    private final File dataFolder;

    public PlayerManager(AllAchievements plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "players");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        loadAllPlayerData();
    }

    /**
     * Data structure to hold player-specific information
     */
    public static class PlayerData {
        private final UUID playerId;
        private final List<String> completedAdvancements = new ArrayList<>();
        private int timerSeconds = 0;
        private boolean timerRunning = false;

        public PlayerData(UUID playerId) {
            this.playerId = playerId;
        }

        public UUID getPlayerId() {
            return playerId;
        }

        public List<String> getCompletedAdvancements() {
            return completedAdvancements;
        }

        public int getTimerSeconds() {
            return timerSeconds;
        }

        public void setTimerSeconds(int timerSeconds) {
            this.timerSeconds = timerSeconds;
        }

        public boolean isTimerRunning() {
            return timerRunning;
        }

        public void setTimerRunning(boolean timerRunning) {
            this.timerRunning = timerRunning;
        }

        public void addAdvancement(String advancement) {
            if (!completedAdvancements.contains(advancement)) {
                completedAdvancements.add(advancement);
            }
        }

        public boolean hasCompletedAdvancement(String advancement) {
            return completedAdvancements.contains(advancement);
        }

        public void clearAdvancements() {
            completedAdvancements.clear();
        }
    }

    /**
     * Get or create player data for a specific player
     */
    public PlayerData getPlayerData(UUID playerId) {
        PlayerData data = playerDataMap.get(playerId);
        if (data == null) {
            data = new PlayerData(playerId);
            playerDataMap.put(playerId, data);
        }
        return data;
    }

    /**
     * Load all player data from files
     */
    public void loadAllPlayerData() {
        File[] playerFiles = dataFolder.listFiles();
        if (playerFiles == null) return;

        for (File file : playerFiles) {
            if (file.isFile() && file.getName().endsWith(".yml")) {
                String uuidString = file.getName().replace(".yml", "");
                try {
                    UUID playerId = UUID.fromString(uuidString);
                    loadPlayerData(playerId);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid player data file name: " + file.getName());
                }
            }
        }
    }

    /**
     * Load a specific player's data
     */
    public void loadPlayerData(UUID playerId) {
        File playerFile = new File(dataFolder, playerId.toString() + ".yml");
        if (!playerFile.exists()) {
            playerDataMap.put(playerId, new PlayerData(playerId));
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        PlayerData data = new PlayerData(playerId);

        // Load completed advancements
        List<String> advancements = config.getStringList("completed_advancements");
        data.getCompletedAdvancements().addAll(advancements);

        // Load timer info
        data.setTimerSeconds(config.getInt("timer_seconds", 0));
        data.setTimerRunning(config.getBoolean("timer_running", false));

        playerDataMap.put(playerId, data);
    }

    /**
     * Save all player data
     */
    public void saveAllPlayerData() {
        for (UUID playerId : playerDataMap.keySet()) {
            savePlayerData(playerId);
        }
    }

    /**
     * Save a specific player's data
     */
    public void savePlayerData(UUID playerId) {
        PlayerData data = playerDataMap.get(playerId);
        if (data == null) {
            return;
        }

        File playerFile = new File(dataFolder, playerId.toString() + ".yml");
        FileConfiguration config = new YamlConfiguration();

        // Save completed advancements
        config.set("completed_advancements", data.getCompletedAdvancements());

        // Save timer info
        config.set("timer_seconds", data.getTimerSeconds());
        config.set("timer_running", data.isTimerRunning());

        try {
            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save player data for " + playerId + ": " + e.getMessage());
        }
    }

    /**
     * Check if a player has completed an advancement
     */
    public boolean hasCompletedAdvancement(UUID playerId, String advancement) {
        PlayerData data = getPlayerData(playerId);
        return data.hasCompletedAdvancement(advancement);
    }

    /**
     * Mark an advancement as completed for a player
     */
    public void completeAdvancement(UUID playerId, String advancement) {
        PlayerData data = getPlayerData(playerId);
        data.addAdvancement(advancement);
        savePlayerData(playerId);
    }

    /**
     * Get the formatted time for a player
     */
    public String getFormattedTime(UUID playerId) {
        PlayerData data = getPlayerData(playerId);
        int seconds = data.getTimerSeconds();

        int hours = seconds / 3600;
        int remainder = seconds % 3600;
        int minutes = remainder / 60;
        int secs = remainder % 60;

        return String.format("§6%02d:%02d:%02d", hours, minutes, secs);
    }

    /**
     * Start the timer for a player
     */
    public void startTimer(UUID playerId) {
        PlayerData data = getPlayerData(playerId);
        data.setTimerRunning(true);
        savePlayerData(playerId);
    }

    /**
     * Stop the timer for a player
     */
    public void stopTimer(UUID playerId) {
        PlayerData data = getPlayerData(playerId);
        data.setTimerRunning(false);
        savePlayerData(playerId);
    }

    /**
     * Toggle the timer for a player
     */
    public void toggleTimer(UUID playerId) {
        PlayerData data = getPlayerData(playerId);
        data.setTimerRunning(!data.isTimerRunning());
        savePlayerData(playerId);
    }

    /**
     * Reset a player's progress
     */
    public void resetPlayer(UUID playerId) {
        PlayerData data = getPlayerData(playerId);
        data.clearAdvancements();
        data.setTimerSeconds(0);
        data.setTimerRunning(false);
        savePlayerData(playerId);

        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            Iterator<Advancement> iterator = Bukkit.getServer().advancementIterator();
            while (iterator.hasNext()) {
                Advancement advancement = iterator.next();
                player.getAdvancementProgress(advancement).getAwardedCriteria().forEach(
                        criteria -> player.getAdvancementProgress(advancement).revokeCriteria(criteria)
                );
            }
        }
    }

    /**
     * Update all player timers (called each second)
     */
    public void updateTimers() {
        for (UUID playerId : playerDataMap.keySet()) {
            PlayerData data = playerDataMap.get(playerId);
            if (data.isTimerRunning()) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    data.setTimerSeconds(data.getTimerSeconds() + 1);
                }
            }
        }
    }

    /**
     * Get completion percentage for a player
     */
    public double getCompletionPercentage(UUID playerId, List<Advancement> totalAdvancements) {
        PlayerData data = getPlayerData(playerId);
        if (totalAdvancements.isEmpty()) {
            return 0.0;
        }
        return (double) data.getCompletedAdvancements().size() / totalAdvancements.size() * 100.0;
    }

    /**
     * Check if a player has completed all advancements
     */
    public boolean hasCompletedAllAdvancements(UUID playerId, List<Advancement> totalAdvancements) {
        PlayerData data = getPlayerData(playerId);
        return data.getCompletedAdvancements().size() >= totalAdvancements.size();
    }}