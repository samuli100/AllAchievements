package at.hexle;

import at.hexle.api.AdvancementInfo;
import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.UUID;

public class Events implements Listener {

    @EventHandler
    public void onAchievement(PlayerAdvancementDoneEvent event){
        // Get the player and advancement
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        Advancement advancement = event.getAdvancement();

        // Check if game is paused - skip all achievement processing if so
        GameModeManager gameModeManager = AllAchievements.getInstance().getGameModeManager();
        if (gameModeManager.isGamePaused()) {
            // Log and ignore advancements during pause
            Bukkit.getLogger().info("Advancement " + advancement.getKey() +
                    " by " + player.getName() + " ignored because game is paused");
            return;
        }

        // Check if this is a valid advancement we should track
        boolean isValidAdvancement = false;
        // FIXED: Add 1.21 to the version check
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
                // Coop mode - notify all team members about progress
                int teamCompletedCount = completedCount; // Using the player's count as team count

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
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Save player data when they leave
        AllAchievements.getInstance().getPlayerManager().savePlayerData(playerId);
    }
}