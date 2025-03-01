package at.hexle;

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

        // Get the target player's completed advancements and all available advancements
        UUID playerId = targetPlayer.getUniqueId();
        List<String> finishedAdvancements = AllAchievements.getInstance().getFinishedAchievements(playerId);
        List<String> allAdvancements = AllAchievements.getInstance().getAllAchievements();

        // Setup pagination
        Pagination<String> pagination = new Pagination<>(45, allAdvancements);

        // Validate page number
        int totalPages = pagination.totalPages();
        if (page < 0) page = 0;
        if (page >= totalPages && totalPages > 0) page = totalPages - 1;

        // Get the current page of advancements
        List<String> pageAdvancements = pagination.getPage(page);

        // Add each advancement to the inventory
        int slot = 0;
        for (String advancementTitle : pageAdvancements) {
            ItemStack itemStack;

            // Check if this advancement is completed by the player
            boolean isCompleted = false;
            for (String completedAdv : finishedAdvancements) {
                if (completedAdv.equals(advancementTitle)) {
                    isCompleted = true;
                    break;
                }
            }

            // Set appropriate item based on completion status
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