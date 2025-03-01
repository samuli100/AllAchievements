package at.hexle;

import at.hexle.api.AdvancementInfo;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Stats {

    public static void showStats(Player player, Player targetPlayer, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, "§6" + targetPlayer.getName() + "'s Achievements");

        // Fill the inventory with black glass panes
        ItemStack fillerItem;
        if (AllAchievements.getInstance().getVersion().startsWith("v1_12")) {
            fillerItem = new ItemStack(Material.getMaterial("STAINED_GLASS_PANE"), 1, (byte)15);
        } else {
            fillerItem = new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1);
        }

        for (int i = 0; i < 54; i++) {
            inv.setItem(i, fillerItem);
        }

        // Get the target player's UUID for achievement checking
        UUID targetId = targetPlayer.getUniqueId();

        // Get a list of all advancement keys that the player has completed
        List<String> completedAdvancementKeys = AllAchievements.getInstance().getPlayerManager()
                .getPlayerData(targetId).getCompletedAdvancements();

        // Debug these to console to help troubleshoot
        Bukkit.getLogger().info("Player " + targetPlayer.getName() + " has completed " +
                completedAdvancementKeys.size() + " advancements");

        // Get all advancement titles
        List<String> allAdvancementTitles = AllAchievements.getInstance().getAllAchievements();

        // Set up pagination
        Pagination<String> pagination = new Pagination<>(45, allAdvancementTitles);

        // Validate page number
        int totalPages = pagination.totalPages();
        if (page < 0) page = 0;
        if (page >= totalPages && totalPages > 0) page = totalPages - 1;

        // Get the current page
        List<String> pageItems = pagination.getPage(page);

        // Add each advancement to the inventory
        int slot = 0;
        for (String advancementTitle : pageItems) {
            // Find if the advancement is completed by matching advancement titles
            boolean isCompleted = false;

            // Get the matching advancement (for title lookup)
            for (Advancement advancement : AllAchievements.getInstance().getAdvancementList()) {
                String title = "";

                // Get title based on version
                if (AllAchievements.getInstance().getVersion().startsWith("v1_19") ||
                        AllAchievements.getInstance().getVersion().startsWith("v1_20")) {
                    if (advancement.getDisplay() != null) {
                        title = advancement.getDisplay().getTitle();
                    }
                } else {
                    AdvancementInfo info = new AdvancementInfo(advancement);
                    title = info.getTitle();
                }

                // If title matches and the advancement key is in the completed list
                if (title.equals(advancementTitle) &&
                        completedAdvancementKeys.contains(advancement.getKey().toString())) {
                    isCompleted = true;
                    break;
                }
            }

            // Create the item based on completion status
            ItemStack itemStack;
            if (isCompleted) {
                // Use modern Material names for newer versions
                if (AllAchievements.getInstance().getVersion().startsWith("v1_12")) {
                    itemStack = new ItemStack(Material.EMERALD, 1);
                } else {
                    itemStack = new ItemStack(Material.GREEN_DYE, 1);
                }
            } else {
                // Use modern Material names for newer versions
                if (AllAchievements.getInstance().getVersion().startsWith("v1_12")) {
                    itemStack = new ItemStack(Material.REDSTONE, 1);
                } else {
                    itemStack = new ItemStack(Material.RED_DYE, 1);
                }
            }

            // Set item metadata
            ItemMeta meta = itemStack.getItemMeta();
            meta.setDisplayName("§6" + advancementTitle);

            // Add completion status to lore
            List<String> lore = new ArrayList<>();
            lore.add(isCompleted ? "§aCompleted" : "§cNot completed");
            meta.setLore(lore);

            itemStack.setItemMeta(meta);
            inv.setItem(slot, itemStack);
            slot++;
        }

        // Navigation buttons
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
            pageIndicator = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE, 1);
        }
        ItemMeta indicatorMeta = pageIndicator.getItemMeta();
        indicatorMeta.setDisplayName("§6Page " + (page + 1) + " of " + totalPages);
        pageIndicator.setItemMeta(indicatorMeta);
        inv.setItem(49, pageIndicator);

        // Next page button
        ItemStack nextPage = new ItemStack(Material.ARROW, 1);
        ItemMeta nextMeta = nextPage.getItemMeta();
        nextMeta.setDisplayName("§6Next Page");
        nextPage.setItemMeta(nextMeta);
        inv.setItem(50, nextPage);

        player.openInventory(inv);
    }
}