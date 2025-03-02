package at.hexle;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for backpack compatibility with different Minecraft versions
 */
public class BackpackHelper {

    /**
     * Get the appropriate material for the backpack based on server version
     */
    public static Material getBackpackMaterial(String version) {
        // Check if the server supports bundles (1.17+)
        if (version.startsWith("v1_17") ||
                version.startsWith("v1_18") ||
                version.startsWith("v1_19") ||
                version.startsWith("v1_20") ||
                version.startsWith("v1_21")) {
            return Material.BUNDLE;
        }

        // For 1.16
        if (version.startsWith("v1_16")) {
            return Material.LEATHER;
        }

        // For 1.14-1.15
        if (version.startsWith("v1_14") || version.startsWith("v1_15")) {
            return Material.LEATHER;
        }

        // For 1.13
        if (version.startsWith("v1_13")) {
            return Material.LEATHER;
        }

        // For 1.12 (needs to use legacy material names)
        if (version.startsWith("v1_12")) {
            try {
                return Material.valueOf("LEATHER");
            } catch (IllegalArgumentException e) {
                // Fallback to a common item that should exist
                return Material.valueOf("CHEST");
            }
        }

        // Default fallback
        return Material.CHEST;
    }

    /**
     * Create a backpack-like item for versions that don't have bundles
     */
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