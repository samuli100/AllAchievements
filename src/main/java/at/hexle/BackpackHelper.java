package at.hexle;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class BackpackHelper {


    public static Material getBackpackMaterial(String version) {
        // Always use CHEST instead of Bundle to avoid conflict with native Bundle behavior
        return Material.CHEST;
    }

    public static ItemStack createLegacyBackpack() {
        ItemStack backpack = new ItemStack(Material.CHEST, 1);
        ItemMeta meta = backpack.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + "Backpack");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Right-click to open");
        lore.add(ChatColor.GRAY + "Double chest inventory");
        meta.setLore(lore);

        backpack.setItemMeta(meta);
        return backpack;
    }
}