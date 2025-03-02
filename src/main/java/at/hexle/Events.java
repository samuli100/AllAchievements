package at.hexle;

import at.hexle.api.AdvancementInfo;
import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Events implements Listener {

    // Static instance for access from other classes
    private static Events instance;

    // Map to track if players have been notified about paused state (to prevent spam)
    private Map<UUID, Boolean> pauseNotified = new HashMap<>();

    // Map to track advancements earned during pause (to be revoked)
    private Map<UUID, String> pausedAdvancements = new HashMap<>();

    // Constructor to set the instance
    public Events() {
        instance = this;
    }

    // Static getter for the instance
    public static Events getInstance() {
        return instance;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAchievement(PlayerAdvancementDoneEvent event){
        // Get the player and advancement
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        Advancement advancement = event.getAdvancement();

        // Check if game is paused
        GameModeManager gameModeManager = AllAchievements.getInstance().getGameModeManager();
        if (gameModeManager.isGamePaused()) {
            // Only act if player is part of an active game
            if (gameModeManager.isPlayerActive(playerId) ||
                    (gameModeManager.getGameMode() == GameModeManager.GameMode.SOLO &&
                            AllAchievements.getInstance().getPlayerManager().getPlayerData(playerId).isTimerRunning())) {

                // We can't cancel the event, but we can schedule a task to revoke the advancement
                // Store the advancement key for later revocation
                pausedAdvancements.put(playerId, advancement.getKey().toString());

                // Schedule the revocation task (1 tick delay to ensure it happens after the event)
                Bukkit.getScheduler().runTaskLater(AllAchievements.getInstance(), () -> {
                    try {
                        // Get the advancement progress
                        AdvancementProgress progress = player.getAdvancementProgress(advancement);

                        // Revoke all criteria that were just awarded
                        for (String criteria : progress.getAwardedCriteria()) {
                            progress.revokeCriteria(criteria);
                        }

                        // Log and notify player
                        Bukkit.getLogger().info("Revoked advancement " + advancement.getKey() +
                                " from " + player.getName() + " because game is paused");

                        // Notify player (only once per session to avoid spam)
                        notifyPausedState(player);

                    } catch (Exception e) {
                        Bukkit.getLogger().warning("Failed to revoke paused advancement: " + e.getMessage());
                    }
                }, 1L);

                // Don't process this advancement any further
                return;
            }
        }

        // Check if this is a valid advancement we should track
        boolean isValidAdvancement = false;
        // Handle 1.19-1.21 versions
        if(AllAchievements.getInstance().getVersion().startsWith("v1_19") ||
                AllAchievements.getInstance().getVersion().startsWith("v1_20") ||
                AllAchievements.getInstance().getVersion().startsWith("v1_21")) {
            if (advancement != null && advancement.getDisplay() != null &&
                    advancement.getDisplay().shouldAnnounceChat()) {
                isValidAdvancement = true;
            }
        } else {
            if(advancement != null) {
                Advancement adv = Bukkit.getAdvancement(advancement.getKey());
                AdvancementInfo info = new AdvancementInfo(adv);
                if(info != null && info.announceToChat()) {
                    isValidAdvancement = true;
                }
            }
        }

        if (!isValidAdvancement) return;

        // Check if player already has this advancement
        if (AllAchievements.getInstance().hasCompletedAdvancement(playerId, advancement)) return;

        // Mark the advancement as completed based on the game mode
        // This will also respect the pause state through the handleAdvancementCompletion method
        boolean processed = gameModeManager.handleAdvancementCompletion(
                playerId, advancement.getKey().toString());

        // If advancement wasn't processed (game is paused), don't continue
        if (!processed) return;

        // Get total advancement count and completed count for this specific player
        List<Advancement> totalAdvancements = AllAchievements.getInstance().getAdvancementList();
        int completedCount = AllAchievements.getInstance().getPlayerManager()
                .getPlayerData(playerId).getCompletedAdvancements().size();

        // Notify based on game mode
        switch (gameModeManager.getGameMode()) {
            case SOLO:
                // Solo mode - only notify the player about their progress
                player.sendMessage("§7------- §6AllAchievements§7 ---------");
                player.sendMessage("§6" + completedCount + "/" + totalAdvancements.size() + " achievements completed!");
                player.sendMessage("§7------------------------------");

                // Check if all advancements are completed for this player
                if (completedCount >= totalAdvancements.size()) {
                    player.sendMessage("§7------- §6AllAchievements§7 ---------");
                    player.sendMessage("§aYou have completed all achievements!");
                    player.sendMessage("§7------------------------------");
                    player.sendMessage(" ");
                    player.sendMessage("§6Want to play again? Type §a/av reset");
                    player.sendMessage(" ");

                    // Broadcast the completion to all players
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        if (onlinePlayer != player) {
                            onlinePlayer.sendMessage("§7------- §6AllAchievements§7 ---------");
                            onlinePlayer.sendMessage("§a" + player.getName() + " has completed all achievements!");
                            onlinePlayer.sendMessage("§7------------------------------");
                        }
                    }

                    // Pause the timer for this player
                    AllAchievements.getInstance().pause(playerId);
                }
                break;

            case COOP:
                // For COOP mode, we get team progress from any player since they're all synchronized
                // If team is empty (unlikely), use this player's progress
                int teamCompletedCount = completedCount;
                if (!gameModeManager.getActivePlayers().isEmpty()) {
                    // Get team progress from first player (all players should have the same in COOP)
                    UUID teamMemberId = gameModeManager.getActivePlayers().iterator().next();
                    teamCompletedCount = AllAchievements.getInstance().getPlayerManager()
                            .getPlayerData(teamMemberId)
                            .getCompletedAdvancements().size();
                }

                for (UUID teamPlayerId : gameModeManager.getActivePlayers()) {
                    Player teamPlayer = Bukkit.getPlayer(teamPlayerId);
                    if (teamPlayer != null && teamPlayer.isOnline()) {
                        teamPlayer.sendMessage("§7------- §6AllAchievements §aCoop§7 ---------");
                        teamPlayer.sendMessage("§6Team progress: " + teamCompletedCount + "/" + totalAdvancements.size() + " achievements!");
                        teamPlayer.sendMessage("§7------------------------------");

                        // If achievement was completed by another player, give credit
                        if (!teamPlayerId.equals(playerId)) {
                            teamPlayer.sendMessage("§a" + player.getName() + " §6completed: §e" +
                                    (advancement.getDisplay() != null ? advancement.getDisplay().getTitle() : advancement.getKey().getKey()));
                        }
                    }
                }

                // Check if all advancements are completed for the team
                if (teamCompletedCount >= totalAdvancements.size()) {
                    // Announce completion to all team members
                    for (UUID teamPlayerId : gameModeManager.getActivePlayers()) {
                        Player teamPlayer = Bukkit.getPlayer(teamPlayerId);
                        if (teamPlayer != null && teamPlayer.isOnline()) {
                            teamPlayer.sendMessage("§7------- §6AllAchievements §aCoop§7 ---------");
                            teamPlayer.sendMessage("§aYour team has completed all achievements!");
                            teamPlayer.sendMessage("§6Time: §a" + formatCoopTime(gameModeManager.getCoopSharedTimer()));
                            teamPlayer.sendMessage("§7------------------------------");
                            teamPlayer.sendMessage(" ");
                            teamPlayer.sendMessage("§6Want to play again? Type §a/av reset");
                            teamPlayer.sendMessage(" ");
                        }
                    }

                    // End the game
                    gameModeManager.endGame();
                }
                break;

            case VERSUS:
                // Versus mode - notify the player about their progress
                player.sendMessage("§7------- §6AllAchievements §cVersus§7 ---------");
                player.sendMessage("§6Your progress: " + completedCount + "/" + totalAdvancements.size() + " achievements!");
                player.sendMessage("§7------------------------------");

                // Also notify other players in the race about this player's progress
                int percentage = (int)((double)completedCount / totalAdvancements.size() * 100);
                if (percentage % 25 == 0 || completedCount == totalAdvancements.size()) { // 25%, 50%, 75%, 100%
                    for (UUID competitorId : gameModeManager.getActivePlayers()) {
                        if (!competitorId.equals(playerId)) {
                            Player competitor = Bukkit.getPlayer(competitorId);
                            if (competitor != null && competitor.isOnline()) {
                                competitor.sendMessage("§7------- §6AllAchievements §cVersus§7 ---------");
                                competitor.sendMessage("§c" + player.getName() + " has reached §e" + percentage + "% §ccompletion!");
                                competitor.sendMessage("§7------------------------------");
                            }
                        }
                    }
                }
                break;
        }
    }

    /**
     * Format COOP timer value for display
     */
    private String formatCoopTime(int seconds) {
        int hours = seconds / 3600;
        int remainder = seconds % 3600;
        int minutes = remainder / 60;
        int secs = remainder % 60;
        return String.format("§6%02d:%02d:%02d", hours, minutes, secs);
    }

    /**
     * Notify a player that the game is paused (only once per session)
     */
    private void notifyPausedState(Player player) {
        UUID playerId = player.getUniqueId();
        if (!pauseNotified.getOrDefault(playerId, false)) {
            player.sendMessage("§7------- §6AllAchievements§7 ---------");
            player.sendMessage("§c§lAchievements are currently paused!");
            player.sendMessage("§cAdvancements will not be counted while the game is paused.");
            player.sendMessage("§7------------------------------");

            pauseNotified.put(playerId, true);
        }
    }

    /**
     * Reset notification status when game is resumed
     */
    public void resetPauseNotifications() {
        pauseNotified.clear();
        pausedAdvancements.clear();
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();

        // Always cancel clicks in custom UIs
        if (title.contains("§6")) {
            event.setCancelled(true);
        }

        // Handle clicks in game setup and player management UIs
        if (title.equals("§6AllAchievements §7- §eGame Setup") ||
                title.equals("§6AllAchievements §7- §ePlayer Management")) {
            // Delegate handling to the GameSetup class
            GameSetup.handleInventoryClick(event);
            return; // Important to return here to prevent further processing
        }

        // Handle clicks in achievement stats UI
        if (title.contains("Achievements")) {
            // Get the player who clicked
            Player player = (Player) event.getWhoClicked();

            // Skip if not clicking navigation buttons
            if (event.getSlot() != 48 && event.getSlot() != 50) {
                return;
            }

            // Get the current page and total pages from the inventory item
            if (event.getInventory().getItem(49) != null) {
                String pageText = event.getInventory().getItem(49).getItemMeta().getDisplayName();
                String[] parts = pageText.split(" ");
                int currentPage = Integer.parseInt(parts[1]) - 1; // Convert to 0-based
                int totalPages = parts.length >= 4 ? Integer.parseInt(parts[3]) : 1;

                // Navigate to the new page with bounds checking
                if (event.getSlot() == 48 && currentPage > 0) {
                    // Previous page
                    currentPage--;
                } else if (event.getSlot() == 50 && currentPage < totalPages - 1) {
                    // Next page
                    currentPage++;
                }

                // If looking at own stats
                if (title.equals("§6" + player.getName() + "'s Achievements")) {
                    Stats.showStats(player, player, currentPage);
                }
                // If looking at someone else's stats
                else if (title.contains("'s Achievements")) {
                    String targetName = title.replace("§6", "").replace("'s Achievements", "");
                    Player targetPlayer = Bukkit.getPlayer(targetName);
                    if (targetPlayer != null) {
                        Stats.showStats(player, targetPlayer, currentPage);
                    } else {
                        player.closeInventory();
                        player.sendMessage("§cPlayer is no longer online.");
                    }
                }
            }
        }
        // Handle clicks in leaderboard UI
        else if (title.equals("§6Achievement Leaderboard")) {
            // Get the player who clicked
            Player player = (Player) event.getWhoClicked();

            // Navigation buttons
            if (event.getSlot() == 48 || event.getSlot() == 50) {
                // Get the current page and total pages
                if (event.getInventory().getItem(49) != null) {
                    String pageText = event.getInventory().getItem(49).getItemMeta().getDisplayName();
                    String[] parts = pageText.split(" ");
                    int currentPage = Integer.parseInt(parts[1]) - 1; // Convert from 1-based to 0-based
                    int totalPages = Integer.parseInt(parts[3]);

                    // Navigate to the new page with bounds checking
                    if (event.getSlot() == 48 && currentPage > 0) {
                        // Previous page
                        currentPage--;
                    } else if (event.getSlot() == 50 && currentPage < totalPages - 1) {
                        // Next page
                        currentPage++;
                    }

                    // Show updated leaderboard
                    Leaderboard.showLeaderboard(player, currentPage);
                }
            }
            // Clicking on a player entry to view their stats
            else if (event.getCurrentItem() != null &&
                    (event.getCurrentItem().getType().name().contains("PLAYER_HEAD") ||
                            (AllAchievements.getInstance().getVersion().startsWith("v1_12") &&
                                    event.getCurrentItem().getType().name().equals("PAPER")))) {

                // Extract player name from the item
                String displayName = event.getCurrentItem().getItemMeta().getDisplayName();
                if (displayName != null && displayName.contains(" ")) {
                    String playerName = displayName.substring(displayName.indexOf(" ") + 1); // Remove rank prefix

                    // Find the player and show their stats
                    Player targetPlayer = Bukkit.getPlayer(playerName);
                    if (targetPlayer != null) {
                        Stats.showStats(player, targetPlayer, 0);
                    } else {
                        player.sendMessage("§cPlayer " + playerName + " is not online.");
                    }
                }
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Load player data if not already loaded
        AllAchievements.getInstance().getPlayerManager().loadPlayerData(playerId);

        // If player joins during active COOP game and is in the active players list, sync them
        GameModeManager gameModeManager = AllAchievements.getInstance().getGameModeManager();
        if (gameModeManager.isGameActive() &&
                gameModeManager.getGameMode() == GameModeManager.GameMode.COOP &&
                gameModeManager.isPlayerActive(playerId)) {

            // COOP sync is handled in the GameModeManager when a player is added
            // This ensures the player gets synced on join, even if they were already added
            gameModeManager.addPlayer(playerId);

            player.sendMessage("§7------- §6AllAchievements §aCoop§7 ---------");
            player.sendMessage("§aYou've joined an active cooperative game!");
            player.sendMessage("§aYour achievements have been synchronized with your team.");
            player.sendMessage("§7--------------------------------");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Save player data when they leave
        AllAchievements.getInstance().getPlayerManager().savePlayerData(playerId);

        // Remove from notification maps
        pauseNotified.remove(playerId);
        pausedAdvancements.remove(playerId);
    }
}