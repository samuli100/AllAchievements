package at.hexle;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class Commands implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        // Commands that require a player sender
        if (!(sender instanceof Player) && (args[0].equalsIgnoreCase("stats") ||
                args[0].equalsIgnoreCase("setup") ||
                args[0].equalsIgnoreCase("leaderboard"))) {
            sender.sendMessage("§cThis command must be run by a player!");
            return false;
        }

        switch (args[0].toLowerCase()) {
            case "setup":
                // Game setup command - requires permission
                if (!sender.hasPermission("av.admin")) {
                    noPerm(sender);
                    return false;
                }

                // Open the game setup UI
                GameSetup.showSetupUI((Player) sender);
                break;

            case "start":
                if (!sender.hasPermission("av.timer.start")) {
                    noPerm(sender);
                    return false;
                }

                // Check game mode
                GameModeManager.GameMode currentMode = AllAchievements.getInstance().getGameModeManager().getGameMode();

                if (currentMode == GameModeManager.GameMode.SOLO) {
                    // Solo mode - start individual timer
                    handleSoloStart(sender, args);
                } else {
                    // Coop/Versus mode - requires admin to start through setup UI
                    if (!sender.hasPermission("av.admin")) {
                        sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                        sender.sendMessage("§cIn " + currentMode + " mode, the game must be started through /av setup");
                        sender.sendMessage("§7--------------------------------");
                        return false;
                    } else {
                        sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                        sender.sendMessage("§cPlease use /av setup to manage " + currentMode + " mode games");
                        sender.sendMessage("§7--------------------------------");
                    }
                }
                break;

            case "pause":
                if (!sender.hasPermission("av.timer.pause")) {
                    noPerm(sender);
                    return false;
                }

                // Check game mode
                currentMode = AllAchievements.getInstance().getGameModeManager().getGameMode();

                if (currentMode == GameModeManager.GameMode.SOLO) {
                    // Solo mode - pause individual timer
                    handleSoloPause(sender, args);
                } else {
                    // Coop/Versus mode - requires admin to manage through setup UI
                    if (!sender.hasPermission("av.admin")) {
                        sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                        sender.sendMessage("§cIn " + currentMode + " mode, the game must be managed through /av setup");
                        sender.sendMessage("§7--------------------------------");
                        return false;
                    } else {
                        sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                        sender.sendMessage("§cPlease use /av setup to manage " + currentMode + " mode games");
                        sender.sendMessage("§7--------------------------------");
                    }
                }
                break;

            case "reset":
                if (!sender.hasPermission("av.timer.reset")) {
                    noPerm(sender);
                    return false;
                }

                // Check game mode for permission handling
                currentMode = AllAchievements.getInstance().getGameModeManager().getGameMode();

                if (currentMode == GameModeManager.GameMode.SOLO || sender.hasPermission("av.admin")) {
                    handleReset(sender, args);
                } else {
                    sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                    sender.sendMessage("§cIn " + currentMode + " mode, only admins can reset the game");
                    sender.sendMessage("§7--------------------------------");
                }
                break;

            case "stats":
                if (!sender.hasPermission("av.stats")) {
                    noPerm(sender);
                    return false;
                }

                // Handle viewing stats
                if (args.length >= 2 && sender.hasPermission("av.admin")) {
                    // Admin viewing another player's stats
                    Player targetPlayer = Bukkit.getPlayer(args[1]);
                    if (targetPlayer != null) {
                        Stats.showStats((Player) sender, targetPlayer, 0);
                    } else {
                        sender.sendMessage("§cPlayer not found: " + args[1]);
                    }
                } else {
                    // Player viewing their own stats
                    Player targetPlayer = (Player) sender;
                    Stats.showStats((Player) sender, targetPlayer, 0);
                }
                break;

            case "leaderboard":
                if (!sender.hasPermission("av.leaderboard")) {
                    noPerm(sender);
                    return false;
                }

                // Show leaderboard
                Leaderboard.showLeaderboard((Player) sender, 0);
                break;

            // Rest of the method remains the same...
        }
        return false;
    }

    /**
     * Send help information to the command sender
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
        sender.sendMessage("§6/av start §7- Start your achievement challenge");
        sender.sendMessage("§6/av pause §7- Pause your achievement challenge");
        sender.sendMessage("§6/av reset §7- Reset your achievement progress");
        sender.sendMessage("§6/av stats §7- See your achievement statistics");
        sender.sendMessage("§6/av leaderboard §7- View the achievement leaderboard");

        if (sender.hasPermission("av.admin")) {
            sender.sendMessage(" ");
            sender.sendMessage("§c§lAdmin Commands:");
            sender.sendMessage("§6/av setup §7- Open the game setup UI");
            sender.sendMessage("§6/av mode [type] §7- View or set game mode");
            sender.sendMessage("§6/av players §7- Manage players in the game");
            sender.sendMessage("§6/av stats [player] §7- View another player's stats");
        }

        sender.sendMessage("§7--------------------------------");
    }

    /**
     * Send a permission denied message to the command sender
     */
    private void noPerm(CommandSender sender) {
        sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
        sender.sendMessage("§cYou do not have permission to use this command.");
        sender.sendMessage("§7--------------------------------");
    }

    /**
     * Handle starting the timer in solo mode
     */
    private void handleSoloStart(CommandSender sender, String[] args) {
        Player target;

        // If args has a player name and sender has admin permission, use that player
        if (args.length >= 2 && sender.hasPermission("av.admin")) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found: " + args[1]);
                return;
            }
        } else if (sender instanceof Player) {
            // Otherwise use the sender if they're a player
            target = (Player) sender;
        } else {
            sender.sendMessage("§cPlease specify a player name: /av start <player>");
            return;
        }

        UUID playerId = target.getUniqueId();

        // Start the timer
        AllAchievements.getInstance().start(playerId);

        // Notify the player and admin
        target.sendMessage("§7-------- §6AllAchievements§7 ----------");
        target.sendMessage("§aYour achievement challenge has started!");
        target.sendMessage("§7--------------------------------");

        if (sender != target) {
            sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
            sender.sendMessage("§aStarted achievement challenge for " + target.getName());
            sender.sendMessage("§7--------------------------------");
        }
    }

    /**
     * Handle pausing the timer in solo mode
     */
    private void handleSoloPause(CommandSender sender, String[] args) {
        Player target;

        // If args has a player name and sender has admin permission, use that player
        if (args.length >= 2 && sender.hasPermission("av.admin")) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found: " + args[1]);
                return;
            }
        } else if (sender instanceof Player) {
            // Otherwise use the sender if they're a player
            target = (Player) sender;
        } else {
            sender.sendMessage("§cPlease specify a player name: /av pause <player>");
            return;
        }

        UUID playerId = target.getUniqueId();

        // Pause/unpause the timer
        boolean wasRunning = AllAchievements.getInstance().isRunning(playerId);
        AllAchievements.getInstance().pause(playerId);

        // Notify the player and admin
        target.sendMessage("§7-------- §6AllAchievements§7 ----------");
        if (wasRunning) {
            target.sendMessage("§cYour achievement challenge has been paused!");
        } else {
            target.sendMessage("§aYour achievement challenge has been resumed!");
        }
        target.sendMessage("§7--------------------------------");

        if (sender != target) {
            sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
            if (wasRunning) {
                sender.sendMessage("§cPaused achievement challenge for " + target.getName());
            } else {
                sender.sendMessage("§aResumed achievement challenge for " + target.getName());
            }
            sender.sendMessage("§7--------------------------------");
        }
    }

    /**
     * Handle resetting player progress
     */
    private void handleReset(CommandSender sender, String[] args) {
        // Check if resetting all players
        if (args.length >= 2 && args[1].equalsIgnoreCase("all") && sender.hasPermission("av.admin")) {
            // Reset all active players
            AllAchievements.getInstance().getGameModeManager().resetAllPlayers();

            sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
            sender.sendMessage("§cReset all players' achievement progress!");
            sender.sendMessage("§7--------------------------------");
            return;
        }

        // Otherwise reset a specific player
        Player target;

        // If args has a player name and sender has admin permission, use that player
        if (args.length >= 2 && sender.hasPermission("av.admin")) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found: " + args[1]);
                return;
            }
        } else if (sender instanceof Player) {
            // Otherwise use the sender if they're a player
            target = (Player) sender;
        } else {
            sender.sendMessage("§cPlease specify a player name: /av reset <player>");
            return;
        }

        UUID playerId = target.getUniqueId();

        // Reset the player's progress
        AllAchievements.getInstance().reset(playerId);

        // Notify the player and admin
        target.sendMessage("§7-------- §6AllAchievements§7 ----------");
        target.sendMessage("§cYour achievement progress has been reset!");
        target.sendMessage("§7--------------------------------");

        if (sender != target) {
            sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
            sender.sendMessage("§cReset achievement progress for " + target.getName());
            sender.sendMessage("§7--------------------------------");
        }
    }
}