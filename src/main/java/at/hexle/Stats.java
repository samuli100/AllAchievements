package at.hexle;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.List;

public class Stats {

    public static void showStats(Player player, Player targetPlayer, int page){
        Inventory inv = Bukkit.createInventory(null, 54, "§6" + targetPlayer.getName() + "'s Achievements");

        // Fill the inventory with black glass panes
        ItemStack fillerItem;
        if(AllAchievements.getInstance().getVersion().startsWith("v1_12")){
            fillerItem = new ItemStack(Material.getMaterial("STAINED_GLASS_PANE"), 1, (byte)15);
        } else {
            fillerItem = new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1);
        }

        for(int i = 0; i < 54; i++){
            inv.setItem(i, fillerItem);
        }

        // Get the target player's completed advancements
        List<String> finishedAdvancements = AllAchievements.getInstance().getFinishedAchievements(targetPlayer.getUniqueId());
        Pagination<String> pagination = new Pagination<>(45, AllAchievements.getInstance().getAllAchievements());

        if(page < 0) page = 0;
        if(page > pagination.totalPages()-1) page = pagination.totalPages()-1;

        List<String> pag = pagination.getPage(page);

        int i = 0;
        for(String p : pag){
            ItemStack is;
            if(finishedAdvancements.contains(p)){
                // Use modern Material names for newer versions
                if(AllAchievements.getInstance().getVersion().startsWith("v1_12")) {
                    is = new ItemStack(Material.EMERALD, 1);
                } else {
                    // FIXED: Use Material.GREEN_DYE for 1.13+ versions
                    is = new ItemStack(Material.GREEN_DYE, 1);
                }
            } else {
                // Use modern Material names for newer versions
                if(AllAchievements.getInstance().getVersion().startsWith("v1_12")) {
                    is = new ItemStack(Material.REDSTONE, 1);
                } else {
                    // FIXED: Use Material.RED_DYE for 1.13+ versions
                    is = new ItemStack(Material.RED_DYE, 1);
                }
            }

            ItemMeta im = is.getItemMeta();
            im.setDisplayName("§6" + p);
            is.setItemMeta(im);
            inv.setItem(i, is);
            i++;
        }

        // Navigation buttons
        ItemStack prevPage = new ItemStack(Material.ARROW, 1);
        ItemMeta prevMeta = prevPage.getItemMeta();
        prevMeta.setDisplayName("§6Last Page");
        prevPage.setItemMeta(prevMeta);
        inv.setItem(48, prevPage);

        ItemStack pageIndicator;
        if(AllAchievements.getInstance().getVersion().startsWith("v1_12")){
            pageIndicator = new ItemStack(Material.getMaterial("STAINED_GLASS_PANE"), 1, (byte)15);
        } else {
            pageIndicator = new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1);
        }
        ItemMeta indicatorMeta = pageIndicator.getItemMeta();
        indicatorMeta.setDisplayName("§6Page " + (page + 1));
        pageIndicator.setItemMeta(indicatorMeta);
        inv.setItem(49, pageIndicator);

        ItemStack nextPage = new ItemStack(Material.ARROW, 1);
        ItemMeta nextMeta = nextPage.getItemMeta();
        nextMeta.setDisplayName("§6Next Page");
        nextPage.setItemMeta(nextMeta);
        inv.setItem(50, nextPage);

        player.openInventory(inv);
    }
}