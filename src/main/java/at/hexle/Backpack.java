package at.hexle;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the backpack feature
 */
public class Backpack implements Listener {

    private final AllAchievements plugin;
    private final File backpackFolder;
    private final NamespacedKey backpackKey;
    private final Map<UUID, Inventory> openBackpacks = new HashMap<>();

    public Backpack(AllAchievements plugin) {
        this.plugin = plugin;
        this.backpackFolder = new File(plugin.getDataFolder(), "backpacks");
        this.backpackKey = new NamespacedKey(plugin, "backpack_id");

        if (!backpackFolder.exists()) {
            backpackFolder.mkdirs();
        }

        // Register events
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Register recipe
        registerBackpackRecipe();
    }

    /**
     * Creates and registers the backpack crafting recipe
     */
    private void registerBackpackRecipe() {
        ItemStack backpack = createBackpack();

        // Create a unique key for the recipe
        NamespacedKey recipeKey = new NamespacedKey(plugin, "backpack");

        // Create the recipe
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, backpack);

        // Define the shape
        recipe.shape("SLS", "CCC", "LLL");

        // Define the ingredients
        recipe.setIngredient('L', Material.LEATHER);
        recipe.setIngredient('C', Material.CHEST);
        recipe.setIngredient('S', Material.STRING);

        // Register the recipe
        Bukkit.addRecipe(recipe);

        plugin.getLogger().info("Registered backpack recipe");
    }

    /**
     * Creates a new backpack item with a unique ID
     */
    public ItemStack createBackpack() {
        // Use the helper to get the appropriate material
        Material material = BackpackHelper.getBackpackMaterial(plugin.getVersion());

        ItemStack backpack = new ItemStack(material, 1);
        ItemMeta meta = backpack.getItemMeta();

        // Set display name and lore
        meta.setDisplayName(ChatColor.GOLD + "Backpack");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Right-click to open");
        lore.add(ChatColor.GRAY + "Double chest inventory");
        meta.setLore(lore);

        // Add unique ID to the item
        String uniqueId = UUID.randomUUID().toString();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(backpackKey, PersistentDataType.STRING, uniqueId);

        backpack.setItemMeta(meta);
        return backpack;
    }

    /**
     * Extracts the backpack ID from an item
     */
    private String getBackpackId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        if (container.has(backpackKey, PersistentDataType.STRING)) {
            return container.get(backpackKey, PersistentDataType.STRING);
        }

        return null;
    }

    /**
     * Checks if an item is a backpack
     */
    public boolean isBackpack(ItemStack item) {
        return getBackpackId(item) != null;
    }

    /**
     * Opens a backpack for a player
     */
    public void openBackpack(Player player, String backpackId) {
        // Create or load the backpack inventory
        Inventory backpackInventory = loadBackpack(backpackId);

        // Open the inventory for the player
        player.openInventory(backpackInventory);

        // Store reference to the open backpack
        openBackpacks.put(player.getUniqueId(), backpackInventory);
    }

    /**
     * Loads a backpack inventory from file or creates a new one
     */
    private Inventory loadBackpack(String backpackId) {
        File backpackFile = new File(backpackFolder, backpackId + ".yml");
        Inventory inventory = Bukkit.createInventory(null, 54, ChatColor.GOLD + "Backpack");

        if (backpackFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(backpackFile);

            // Load each item from config
            for (int i = 0; i < 54; i++) {
                if (config.contains("slot." + i)) {
                    ItemStack item = config.getItemStack("slot." + i);
                    if (item != null) {
                        inventory.setItem(i, item);
                    }
                }
            }
        }

        return inventory;
    }

    /**
     * Saves a backpack inventory to file
     */
    private void saveBackpack(String backpackId, Inventory inventory) {
        File backpackFile = new File(backpackFolder, backpackId + ".yml");
        FileConfiguration config = new YamlConfiguration();

        // Save each item to config
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null) {
                config.set("slot." + i, item);
            } else {
                config.set("slot." + i, null);
            }
        }

        try {
            config.save(backpackFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save backpack " + backpackId + ": " + e.getMessage());
        }
    }

    /**
     * Handle player interaction with backpack
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Check if the player is right-clicking with a backpack
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && isBackpack(item)) {
            event.setCancelled(true);

            // Get the backpack ID
            String backpackId = getBackpackId(item);
            if (backpackId != null) {
                // Open the backpack
                openBackpack(player, backpackId);
            }
        }
    }

    /**
     * Handle inventory close event to save backpack contents
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Check if this is a backpack inventory being closed
        if (openBackpacks.containsKey(playerId)) {
            Inventory backpackInventory = openBackpacks.get(playerId);

            // Make sure it's the same inventory being closed
            if (event.getInventory().equals(backpackInventory)) {
                // Find the backpack item in the player's inventory to get its ID
                ItemStack backpackItem = null;
                for (ItemStack item : player.getInventory().getContents()) {
                    if (item != null && isBackpack(item)) {
                        backpackItem = item;
                        break;
                    }
                }

                if (backpackItem != null) {
                    String backpackId = getBackpackId(backpackItem);
                    if (backpackId != null) {
                        // Save the backpack contents
                        saveBackpack(backpackId, backpackInventory);
                    }
                }

                // Remove from open backpacks map
                openBackpacks.remove(playerId);
            }
        }
    }

    /**
     * Get a command to give a player a backpack
     */
    public static String getGiveCommand(String playerName) {
        return "/give " + playerName + " bundle{display:{Name:'{\"text\":\"Backpack\",\"color\":\"gold\"}',Lore:['{\"text\":\"Right-click to open\",\"color\":\"gray\"}','{\"text\":\"Double chest inventory\",\"color\":\"gray\"}']},CustomModelData:1,CustomBackpack:1} 1";
    }
}