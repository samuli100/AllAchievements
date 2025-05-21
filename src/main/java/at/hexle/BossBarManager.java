package at.hexle;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages BossBars for displaying timer and completion progress
 */
public class BossBarManager {

    private final AllAchievements plugin;
    private final Map<UUID, BossBar> playerBossBars = new HashMap<>();

    public BossBarManager(AllAchievements plugin) {
        this.plugin = plugin;
    }

    /**
     * Create or get a BossBar for a player
     */
    public BossBar getPlayerBossBar(UUID playerId) {
        BossBar bossBar = playerBossBars.get(playerId);
        if (bossBar == null) {
            // Create a new BossBar for this player
            bossBar = Bukkit.createBossBar(
                    ChatColor.GOLD + "Loading...",
                    BarColor.BLUE,
                    BarStyle.SEGMENTED_10
            );
            playerBossBars.put(playerId, bossBar);
        }
        return bossBar;
    }

    /**
     * Update the BossBar for a player with current time and completion data
     */
    public void updatePlayerBossBar(Player player) {
        UUID playerId = player.getUniqueId();
        BossBar bossBar = getPlayerBossBar(playerId);
        GameModeManager gameManager = plugin.getGameModeManager();
        GameModeManager.GameMode currentMode = gameManager.getGameMode();

        // Get completion percentage
        int totalAdvancements = plugin.getAdvancementList().size();
        int completedAdvancements = plugin.getPlayerManager()
                .getPlayerData(playerId).getCompletedAdvancements().size();
        double completionPercentage = totalAdvancements > 0
                ? (double) completedAdvancements / totalAdvancements
                : 0.0;

        // Set BossBar progress based on completion percentage
        bossBar.setProgress(Math.max(0.0, Math.min(1.0, completionPercentage)));

        // Set color based on game mode
        switch (currentMode) {
            case SOLO:
                bossBar.setColor(BarColor.BLUE);
                break;
            case COOP:
                bossBar.setColor(BarColor.GREEN);
                break;
            case VERSUS:
                bossBar.setColor(BarColor.RED);
                break;
        }

        // Format the title with timer and completion info
        String timerValue;
        String gameModeName = "";

        if (currentMode == GameModeManager.GameMode.COOP && gameManager.isGameActive()) {
            // Use shared timer for COOP mode
            int seconds = gameManager.getCoopSharedTimer();
            int hours = seconds / 3600;
            int remainder = seconds % 3600;
            int minutes = remainder / 60;
            int secs = remainder % 60;
            timerValue = String.format("%02d:%02d:%02d", hours, minutes, secs);
            gameModeName = " §7[§aCoop§7]";
        } else {
            // Use player-specific timer for other modes
            timerValue = plugin.getPlayerManager().getFormattedTime(playerId).replace("§6", "");
            if (currentMode == GameModeManager.GameMode.VERSUS) {
                gameModeName = " §7[§cVersus§7]";
            }
        }

        // Create title with timer, game mode, and completion info
        String title = ChatColor.GOLD + timerValue +
                gameModeName +
                ChatColor.WHITE + " Completion: " +
                ChatColor.YELLOW + completedAdvancements + "/" + totalAdvancements +
                " (" + String.format("%.1f", completionPercentage * 100) + "%)";

        // Add pause indicator if game is paused
        if (gameManager.isGamePaused()) {
            title = ChatColor.RED + "⏸ PAUSED ⏸ " + ChatColor.GOLD + title;
        }

        bossBar.setTitle(title);

        // Show the BossBar to the player
        bossBar.addPlayer(player);
    }

    /**
     * Hide the BossBar for a specific player
     */
    public void hidePlayerBossBar(UUID playerId) {
        BossBar bossBar = playerBossBars.get(playerId);
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }

    /**
     * Hide BossBars for all players
     */
    public void hideAllBossBars() {
        for (BossBar bossBar : playerBossBars.values()) {
            bossBar.removeAll();
        }
    }

    /**
     * Clean up resources when plugin is disabled
     */
    public void cleanup() {
        hideAllBossBars();
        playerBossBars.clear();
    }
}