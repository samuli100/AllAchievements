package at.hexle;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
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
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "setup":
                // Game setup command - requires permission
                if (!sender.hasPermission("av.admin")) {
                    noPerm(sender);
                    return true;
                }

                // Open the game setup UI
                GameSetup.showSetupUI((Player) sender);
                return true;

            case "start":
                if (!sender.hasPermission("av.timer.start")) {
                    noPerm(sender);
                    return true;
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
                    } else {
                        sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                        sender.sendMessage("§cPlease use /av setup to manage " + currentMode + " mode games");
                        sender.sendMessage("§7--------------------------------");
                    }
                }
                return true;

            case "pause":
                if (!sender.hasPermission("av.timer.pause")) {
                    noPerm(sender);
                    return true;
                }

                // Enhanced pause command - works for all game modes
                handlePauseCommand(sender, args);
                return true;

            case "reset":
                if (!sender.hasPermission("av.timer.reset")) {
                    noPerm(sender);
                    return true;
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
                return true;

            case "stats":
                if (!sender.hasPermission("av.stats")) {
                    noPerm(sender);
                    return true;
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
                return true;

            case "leaderboard":
                if (!sender.hasPermission("av.leaderboard")) {
                    noPerm(sender);
                    return true;
                }

                // Show leaderboard
                Leaderboard.showLeaderboard((Player) sender, 0);
                return true;

            case "mode":
                if (!sender.hasPermission("av.admin")) {
                    noPerm(sender);
                    return true;
                }

                // If no mode specified, show current mode
                if (args.length < 2) {
                    sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                    sender.sendMessage("§6Current game mode: §e" +
                            AllAchievements.getInstance().getGameModeManager().getGameMode());
                    sender.sendMessage("§7Available modes: SOLO, COOP, VERSUS");
                    sender.sendMessage("§7Use /av mode [type] to change the mode.");
                    sender.sendMessage("§7--------------------------------");
                    return true;
                }

                // Try to parse the game mode
                try {
                    GameModeManager.GameMode newMode = GameModeManager.GameMode.valueOf(args[1].toUpperCase());

                    // Cannot change mode if a game is active
                    if (AllAchievements.getInstance().getGameModeManager().isGameActive()) {
                        sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                        sender.sendMessage("§cCannot change game mode while a game is active!");
                        sender.sendMessage("§cStop the current game first with /av setup.");
                        sender.sendMessage("§7--------------------------------");
                        return true;
                    }

                    // Set the mode
                    AllAchievements.getInstance().getGameModeManager().setGameMode(newMode);
                    sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                    sender.sendMessage("§aGame mode set to " + newMode);
                    sender.sendMessage("§7--------------------------------");
                } catch (IllegalArgumentException e) {
                    sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                    sender.sendMessage("§cInvalid game mode: " + args[1]);
                    sender.sendMessage("§7Available modes: SOLO, COOP, VERSUS");
                    sender.sendMessage("§7--------------------------------");
                }
                return true;

            case "players":
                // Player management command - requires admin permission
                if (!sender.hasPermission("av.admin")) {
                    noPerm(sender);
                    return true;
                }

                if (args.length < 2) {
                    // Show player management UI if sender is a player
                    if (sender instanceof Player) {
                        GameSetup.showPlayerManagementUI((Player) sender, 0);
                    } else {
                        sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                        sender.sendMessage("§cUsage: /av players <add|remove|clear> [player]");
                        sender.sendMessage("§7--------------------------------");
                    }
                    return true;
                }

                // Get the game mode manager
                GameModeManager gameModeManager = AllAchievements.getInstance().getGameModeManager();

                // Handle sub-commands
                switch (args[1].toLowerCase()) {
                    case "add":
                        if (args.length < 3) {
                            sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                            sender.sendMessage("§cUsage: /av players add <player>");
                            sender.sendMessage("§7--------------------------------");
                            return true;
                        }

                        Player targetToAdd = Bukkit.getPlayer(args[2]);
                        if (targetToAdd == null) {
                            sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                            sender.sendMessage("§cPlayer not found: " + args[2]);
                            sender.sendMessage("§7--------------------------------");
                            return true;
                        }

                        UUID targetAddId = targetToAdd.getUniqueId();
                        if (gameModeManager.isPlayerActive(targetAddId)) {
                            sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                            sender.sendMessage("§c" + targetToAdd.getName() + " is already in the game.");
                            sender.sendMessage("§7--------------------------------");
                            return true;
                        }

                        // Add the player
                        gameModeManager.addPlayer(targetAddId);
                        sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                        sender.sendMessage("§aAdded " + targetToAdd.getName() + " to the game.");
                        sender.sendMessage("§7--------------------------------");

                        // Notify the player
                        targetToAdd.sendMessage("§7-------- §6AllAchievements§7 ----------");
                        targetToAdd.sendMessage("§aYou have been added to the " +
                                gameModeManager.getGameMode() + " achievement challenge!");
                        targetToAdd.sendMessage("§7--------------------------------");
                        break;

                    case "remove":
                        if (args.length < 3) {
                            sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                            sender.sendMessage("§cUsage: /av players remove <player>");
                            sender.sendMessage("§7--------------------------------");
                            return true;
                        }

                        Player targetToRemove = Bukkit.getPlayer(args[2]);
                        if (targetToRemove == null) {
                            sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                            sender.sendMessage("§cPlayer not found: " + args[2]);
                            sender.sendMessage("§7--------------------------------");
                            return true;
                        }

                        UUID targetRemoveId = targetToRemove.getUniqueId();
                        if (!gameModeManager.isPlayerActive(targetRemoveId)) {
                            sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                            sender.sendMessage("§c" + targetToRemove.getName() + " is not in the game.");
                            sender.sendMessage("§7--------------------------------");
                            return true;
                        }

                        // Remove the player
                        gameModeManager.removePlayer(targetRemoveId);
                        sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                        sender.sendMessage("§cRemoved " + targetToRemove.getName() + " from the game.");
                        sender.sendMessage("§7--------------------------------");

                        // Notify the player
                        targetToRemove.sendMessage("§7-------- §6AllAchievements§7 ----------");
                        targetToRemove.sendMessage("§cYou have been removed from the achievement challenge.");
                        targetToRemove.sendMessage("§7--------------------------------");
                        break;

                    case "clear":
                        int playerCount = gameModeManager.getActivePlayers().size();

                        if (playerCount == 0) {
                            sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                            sender.sendMessage("§cThere are no players in the game.");
                            sender.sendMessage("§7--------------------------------");
                            return true;
                        }

                        // Store the player list for notifications
                        List<UUID> affectedPlayers = new ArrayList<>(gameModeManager.getActivePlayers());

                        // Clear all players
                        gameModeManager.getActivePlayers().clear();
                        gameModeManager.saveConfig();

                        sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                        sender.sendMessage("§cRemoved all " + playerCount + " players from the game.");
                        sender.sendMessage("§7--------------------------------");

                        // Notify affected players
                        for (UUID playerId : affectedPlayers) {
                            Player affectedPlayer = Bukkit.getPlayer(playerId);
                            if (affectedPlayer != null && affectedPlayer.isOnline()) {
                                affectedPlayer.sendMessage("§7-------- §6AllAchievements§7 ----------");
                                affectedPlayer.sendMessage("§cYou have been removed from the achievement challenge.");
                                affectedPlayer.sendMessage("§7--------------------------------");
                            }
                        }
                        break;

                    default:
                        sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                        sender.sendMessage("§cUnknown sub-command: " + args[1]);
                        sender.sendMessage("§7Available commands: add, remove, clear");
                        sender.sendMessage("§7--------------------------------");
                        break;
                }
                return true;

            case "forcestart":
                if (!sender.hasPermission("av.admin")) {
                    noPerm(sender);
                    return true;
                }

                // End any active game first
                if (AllAchievements.getInstance().getGameModeManager().isGameActive()) {
                    AllAchievements.getInstance().getGameModeManager().endGame();
                    sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                    sender.sendMessage("§cEnding current active game first.");
                    sender.sendMessage("§7--------------------------------");
                }

                // If COOP or VERSUS mode, make sure there are players
                if (AllAchievements.getInstance().getGameModeManager().getGameMode() != GameModeManager.GameMode.SOLO) {
                    // Auto-add the sender if they're a player
                    if (sender instanceof Player) {
                        AllAchievements.getInstance().getGameModeManager().addPlayer(((Player) sender).getUniqueId());
                    }

                    // If still no players, try to add all online players
                    if (AllAchievements.getInstance().getGameModeManager().getActivePlayers().isEmpty()) {
                        sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                        sender.sendMessage("§eNo players in game. Adding all online players...");
                        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                            AllAchievements.getInstance().getGameModeManager().addPlayer(onlinePlayer.getUniqueId());
                            sender.sendMessage("§aAdded " + onlinePlayer.getName() + " to the game.");
                        }
                        sender.sendMessage("§7--------------------------------");
                    }
                }

                // Force start the game
                AllAchievements.getInstance().getGameModeManager().startGame();

                sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                sender.sendMessage("§a§lGame forcefully started in " +
                        AllAchievements.getInstance().getGameModeManager().getGameMode() + " mode!");
                sender.sendMessage("§7--------------------------------");

                // Notify all players
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (onlinePlayer != sender) {
                        onlinePlayer.sendMessage("§7-------- §6AllAchievements§7 ----------");
                        onlinePlayer.sendMessage("§aA new " +
                                AllAchievements.getInstance().getGameModeManager().getGameMode() + " game has started!");
                        onlinePlayer.sendMessage("§7--------------------------------");
                    }
                }
                return true;

            default:
                // Unknown command
                sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                sender.sendMessage("§cUnknown command: " + args[0]);
                sender.sendMessage("§7Type /av for help.");
                sender.sendMessage("§7--------------------------------");
                return true;
        }
    }

    /**
     * Handle the pause command in any game mode
     */
    private void handlePauseCommand(CommandSender sender, String[] args) {
        GameModeManager gameModeManager = AllAchievements.getInstance().getGameModeManager();
        GameModeManager.GameMode currentMode = gameModeManager.getGameMode();

        // For COOP and VERSUS modes with admin permission
        if (currentMode != GameModeManager.GameMode.SOLO) {
            if (sender.hasPermission("av.admin")) {
                // Check if a game is active
                if (!gameModeManager.isGameActive()) {
                    sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                    sender.sendMessage("§cNo active game to pause. Start a game first.");
                    sender.sendMessage("§7--------------------------------");
                    return;
                }

                // Toggle pause for the entire game
                boolean isPaused = gameModeManager.togglePauseGame();

                // Notify the admin
                sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                sender.sendMessage(isPaused ?
                        "§cThe " + currentMode + " game has been paused. No advancements will be tracked." :
                        "§aThe " + currentMode + " game has been resumed. Advancements will now be tracked.");
                sender.sendMessage("§7--------------------------------");

                // Broadcast to all players
                for (UUID playerId : gameModeManager.getActivePlayers()) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null && player != sender) {
                        player.sendMessage("§7-------- §6AllAchievements§7 ----------");
                        player.sendMessage(isPaused ?
                                "§cThe game has been paused by an admin. Advancements are temporarily disabled." :
                                "§aThe game has been resumed by an admin. Advancements are now being tracked.");
                        player.sendMessage("§7--------------------------------");
                    }
                }
            } else {
                // Non-admin trying to pause team game
                sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                sender.sendMessage("§cOnly admins can pause/resume " + currentMode + " games.");
                sender.sendMessage("§7--------------------------------");
            }
            return;
        }

        // For SOLO mode
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
            target.sendMessage("§cAdvancements won't be tracked while paused.");
            target.sendMessage("§6Current time: " + AllAchievements.getInstance().getTime(playerId));
        } else {
            target.sendMessage("§aYour achievement challenge has been resumed!");
            target.sendMessage("§aAdvancements will now be tracked.");
        }
        target.sendMessage("§7--------------------------------");

        if (sender != target) {
            sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
            if (wasRunning) {
                sender.sendMessage("§cPaused achievement challenge for " + target.getName());
                sender.sendMessage("§6Current time: " + AllAchievements.getInstance().getTime(playerId));
            } else {
                sender.sendMessage("§aResumed achievement challenge for " + target.getName());
            }
            sender.sendMessage("§7--------------------------------");
        }
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