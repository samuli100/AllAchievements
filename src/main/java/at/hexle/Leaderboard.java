package at.hexle;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles the leaderboard UI for achievement tracking
 */
public class Leaderboard {

    /**
     * Shows the leaderboard UI to a player
     * @param player The player viewing the leaderboard
     * @param page The page number to display
     */
    public static void showLeaderboard(Player player, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, "§6Achievement Leaderboard");

        // Fill with glass panes
        ItemStack fillerItem;
        if (AllAchievements.getInstance().getVersion().startsWith("v1_12")) {
            fillerItem = new ItemStack(Material.getMaterial("STAINED_GLASS_PANE"), 1, (byte)15);
        } else {
            fillerItem = new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1);
        }

        for (int i = 0; i < 54; i++) {
            inv.setItem(i, fillerItem);
        }

        // Get all players' completion data
        List<Map.Entry<UUID, Double>> leaderboard = new ArrayList<>();

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            UUID playerId = onlinePlayer.getUniqueId();
            double completion = AllAchievements.getInstance().getPlayerManager()
                    .getCompletionPercentage(playerId, AllAchievements.getInstance().getAdvancementList());

            leaderboard.add(new AbstractMap.SimpleEntry<>(playerId, completion));
        }

        // Sort by completion percentage (highest first)
        leaderboard.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // Pagination
        int playersPerPage = 45;  // 9x5 grid
        int totalPages = (int) Math.ceil((double) leaderboard.size() / playersPerPage);

        if (page < 0) page = 0;
        if (page >= totalPages && totalPages > 0) page = totalPages - 1;

        // Calculate range for current page
        int startIndex = page * playersPerPage;
        int endIndex = Math.min(startIndex + playersPerPage, leaderboard.size());

        // Add player entries to the leaderboard
        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<UUID, Double> entry = leaderboard.get(i);
            UUID playerId = entry.getKey();
            double completion = entry.getValue();

            Player targetPlayer = Bukkit.getPlayer(playerId);
            if (targetPlayer != null) {
                // Create player entry item
                ItemStack playerItem;

                // FIXED: Handle player heads in 1.21
                if (!AllAchievements.getInstance().getVersion().startsWith("v1_12")) {
                    playerItem = new ItemStack(Material.PLAYER_HEAD, 1);
                    SkullMeta skullMeta = (SkullMeta) playerItem.getItemMeta();
                    skullMeta.setOwningPlayer(targetPlayer);
                    playerItem.setItemMeta(skullMeta);
                } else {
                    playerItem = new ItemStack(Material.PAPER, 1);
                }

                // Set metadata
                ItemMeta meta = playerItem.getItemMeta();
                meta.setDisplayName("§e#" + (i + 1) + " " + targetPlayer.getName());

                // Create lore with completion info
                List<String> lore = new ArrayList<>();
                lore.add("§6Completion: §a" + String.format("%.1f%%", completion));
                lore.add("§6Advancements: §a" +
                        AllAchievements.getInstance().getFinishedAchievements(playerId).size() +
                        "/" + AllAchievements.getInstance().getAdvancementList().size());
                lore.add("§6Time: §a" + AllAchievements.getInstance().getTime(playerId));

                meta.setLore(lore);
                playerItem.setItemMeta(meta);

                inv.setItem(slot, playerItem);
                slot++;
            }
        }

        // Add navigation buttons if needed
        if (totalPages > 1) {
            // Previous page button
            ItemStack prevPage = new ItemStack(Material.ARROW, 1);
            ItemMeta prevMeta = prevPage.getItemMeta();
            prevMeta.setDisplayName("§6Previous Page");
            prevPage.setItemMeta(prevMeta);
            inv.setItem(48, prevPage);

            // Page indicator
            ItemStack pageIndicator;
            if (AllAchievements.getInstance().getVersion().startsWith("v1_12")) {
                pageIndicator = new ItemStack(Material.getMaterial("STAINED_GLASS_PANE"), 1, (byte)4);
            } else {
                // FIXED: Use YELLOW_STAINED_GLASS_PANE for 1.13+ versions
                pageIndicator = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE, 1);
            }
            ItemMeta pageMeta = pageIndicator.getItemMeta();
            pageMeta.setDisplayName("§6Page " + (page + 1) + " of " + totalPages);
            pageIndicator.setItemMeta(pageMeta);
            inv.setItem(49, pageIndicator);

            // Next page button
            ItemStack nextPage = new ItemStack(Material.ARROW, 1);
            ItemMeta nextMeta = nextPage.getItemMeta();
            nextMeta.setDisplayName("§6Next Page");
            nextPage.setItemMeta(nextMeta);
            inv.setItem(50, nextPage);
        }

        player.openInventory(inv);
    }
}