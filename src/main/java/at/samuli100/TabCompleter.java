package at.samuli100;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TabCompleter implements org.bukkit.command.TabCompleter {

    final String[] commands;
    final String[] adminCommands;
    final String[] gameModes;
    final String[] playerCommands;

    public TabCompleter() {
        // Basic commands available to all players with permissions
        this.commands = new String[] {
                "start", "reset", "pause", "stats", "leaderboard"
        };

        // Extra commands for admins
        this.adminCommands = new String[] {
                "setup", "mode", "players"
        };

        // Game modes for the mode command
        this.gameModes = new String[] {
                "SOLO", "COOP", "VERSUS"
        };

        // Player management commands
        this.playerCommands = new String[] {
                "add", "remove", "clear"
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] args) {
        List<String> completions = new ArrayList<>();

        // First argument - command
        if (args.length == 1) {
            List<String> allCommands = new ArrayList<>(Arrays.asList(this.commands));

            // Add admin commands if player has permission
            if (sender.hasPermission("av.admin")) {
                allCommands.addAll(Arrays.asList(this.adminCommands));
            }

            StringUtil.copyPartialMatches(args[0], allCommands, completions);
        }
        // Second argument - depends on first argument
        else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "start":
                case "pause":
                case "stats":
                    // Show player names if admin
                    if (sender.hasPermission("av.admin")) {
                        List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .collect(Collectors.toList());
                        StringUtil.copyPartialMatches(args[1], playerNames, completions);
                    }
                    break;

                case "reset":
                    // Show player names or "all" if admin
                    if (sender.hasPermission("av.admin")) {
                        List<String> options = Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .collect(Collectors.toList());
                        options.add("all");
                        StringUtil.copyPartialMatches(args[1], options, completions);
                    }
                    break;

                case "mode":
                    // Show game modes if admin
                    if (sender.hasPermission("av.admin")) {
                        StringUtil.copyPartialMatches(args[1], Arrays.asList(this.gameModes), completions);
                    }
                    break;

                case "players":
                    // Show player commands if admin
                    if (sender.hasPermission("av.admin")) {
                        StringUtil.copyPartialMatches(args[1], Arrays.asList(this.playerCommands), completions);
                    }
                    break;
            }
        }
        // Third argument - depends on first and second arguments
        else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("players") &&
                    (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove")) &&
                    sender.hasPermission("av.admin")) {

                // Show player names for player add/remove commands
                List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
                StringUtil.copyPartialMatches(args[2], playerNames, completions);
            }
        }

        return completions;
    }
}