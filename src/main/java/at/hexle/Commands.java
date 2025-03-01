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

            case "mode":
                if (!sender.hasPermission("av.admin")) {
                    noPerm(sender);
                    return false;
                }

                // Display or set game mode
                GameModeManager gameModeManager = AllAchievements.getInstance().getGameModeManager();

                if (args.length < 2) {
                    // Display current mode
                    sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                    sender.sendMessage("§6Current game mode: §e" + gameModeManager.getGameMode());
                    sender.sendMessage("§6Game active: §e" + gameModeManager.isGameActive());
                    sender.sendMessage("§6Active players: §e" + gameModeManager.getActivePlayers().size());
                    sender.sendMessage("§7--------------------------------");
                } else {
                    // Cannot change mode if a game is active
                    if (gameModeManager.isGameActive()) {
                        sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                        sender.sendMessage("§cCannot change game mode while a game is active!");
                        sender.sendMessage("§7--------------------------------");
                        return false;
                    }

                    // Try to parse the mode
                    try {
                        GameModeManager.GameMode newMode = GameModeManager.GameMode.valueOf(args[1].toUpperCase());
                        gameModeManager.setGameMode(newMode);
                        sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                        sender.sendMessage("§aGame mode set to " + newMode);
                        sender.sendMessage("§7--------------------------------");
                    } catch (IllegalArgumentException e) {
                        sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                        sender.sendMessage("§cInvalid game mode: " + args[1]);
                        sender.sendMessage("§7Valid modes: SOLO, COOP, VERSUS");
                        sender.sendMessage("§7--------------------------------");
                    }
                }
                break;

            case "players":
                if (!sender.hasPermission("av.admin")) {
                    noPerm(sender);
                    return false;
                }

                // List or manage active players
                gameModeManager = AllAchievements.getInstance().getGameModeManager();

                if (args.length < 2) {
                    // List active players
                    List<UUID> activePlayers = gameModeManager.getActivePlayers();
                    sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                    sender.sendMessage("§6Active players: §e" + activePlayers.size());

                    if (!activePlayers.isEmpty()) {
                        StringBuilder playerList = new StringBuilder("§e");
                        int count = 0;

                        for (UUID playerId : activePlayers) {
                            Player player = Bukkit.getPlayer(playerId);
                            String name = player != null ? player.getName() : "[Offline:" + playerId + "]";

                            if (count > 0) playerList.append("§7, §e");
                            playerList.append(name);
                            count++;
                        }

                        sender.sendMessage(playerList.toString());
                    }

                    sender.sendMessage("§7--------------------------------");
                } else if (args[1].equalsIgnoreCase("add") && args.length >= 3) {
                    // Add a player
                    Player targetPlayer = Bukkit.getPlayer(args[2]);

                    if (targetPlayer != null) {
                        gameModeManager.addPlayer(targetPlayer.getUniqueId());
                        sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                        sender.sendMessage("§aAdded " + targetPlayer.getName() + " to the game");
                        sender.sendMessage("§7--------------------------------");
                    } else {
                        sender.sendMessage("§cPlayer not found: " + args[2]);
                    }
                } else if (args[1].equalsIgnoreCase("remove") && args.length >= 3) {
                    // Remove a player
                    Player targetPlayer = Bukkit.getPlayer(args[2]);

                    if (targetPlayer != null) {
                        gameModeManager.removePlayer(targetPlayer.getUniqueId());
                        sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                        sender.sendMessage("§cRemoved " + targetPlayer.getName() + " from the game");
                        sender.sendMessage("§7--------------------------------");
                    } else {
                        sender.sendMessage("§cPlayer not found: " + args[2]);
                    }
                } else if (args[1].equalsIgnoreCase("clear")) {
                    // Clear all players
                    gameModeManager.getActivePlayers().clear();
                    sender.sendMessage("§7-------- §6AllAchievements§7 ----------");
                    sender.sendMessage("§cAll players have been removed from the game");
                    sender.sendMessage("§7--------------------------------");
                } else {
                    sender.sendMessage("§cUsage: /av players [add|remove|clear] [player]");
                }
        }
        return false;
    }
}