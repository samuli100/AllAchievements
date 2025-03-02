package at.hexle;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
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

    /**
     * Custom inventory holder for backpack inventories
     */
    public class BackpackHolder implements InventoryHolder {
        private final String backpackId;
        private Inventory inventory;

        public BackpackHolder(String backpackId) {
            this.backpackId = backpackId;
        }

        public String getBackpackId() {
            return backpackId;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }
    }

    private final AllAchievements plugin;
    private final File backpackFolder;
    private final NamespacedKey backpackKey;
    private final Map<UUID, String> openBackpackIds = new HashMap<>();

    public Backpack(AllAchievements plugin) {
        this.plugin = plugin;
        this.backpackFolder = new File(plugin.getDataFolder(), "backpacks");
        this.backpackKey = new NamespacedKey(plugin, "backpack_id");

        if (!backpackFolder.exists()) {
            backpackFolder.mkdirs();
        }

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
        recipe.shape("LLL", "LCL", "LLL");

        // Define the ingredients
        recipe.setIngredient('L', Material.LEATHER);
        recipe.setIngredient('C', Material.CHEST);

        // Register the recipe
        Bukkit.addRecipe(recipe);

        plugin.getLogger().info("Registered backpack recipe");
    }

    /**
     * Creates a new backpack item with a unique ID
     */
    public ItemStack createBackpack() {
        // Always use CHEST instead of Bundle to avoid native behavior conflicts
        Material material = Material.CHEST;

        ItemStack backpack = new ItemStack(material, 1);
        ItemMeta meta = backpack.getItemMeta();

        // Set display name and lore
        meta.setDisplayName(ChatColor.GOLD + "Backpack");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Right-click to open");
        lore.add(ChatColor.GRAY + "Double chest inventory");

        // Add a third lore line to help identify this as a backpack
        lore.add(ChatColor.DARK_GRAY + "ID: " + UUID.randomUUID().toString().substring(0, 8));
        meta.setLore(lore);

        // Set custom model data if supported by this version
        try {
            meta.setCustomModelData(1001);
        } catch (NoSuchMethodError e) {
            // Custom model data not supported in this version, ignore
        }

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

        // Store the backpack ID for this player
        openBackpackIds.put(player.getUniqueId(), backpackId);
    }

    /**
     * Loads a backpack inventory from file or creates a new one
     */
    private Inventory loadBackpack(String backpackId) {
        File backpackFile = new File(backpackFolder, backpackId + ".yml");

        // Create a custom inventory holder with the backpack ID
        BackpackHolder holder = new BackpackHolder(backpackId);
        Inventory inventory = Bukkit.createInventory(holder, 54, ChatColor.GOLD + "Backpack");

        // Store the inventory in the holder for direct access
        holder.setInventory(inventory);

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
     * Handle player interaction with backpack - with high priority to override Bundle behavior
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Check if the player is right-clicking with a backpack
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && item != null) {
            // Verify this is a backpack
            if (isBackpack(item)) {
                // Cancel the event to prevent default bundle behavior and block placing
                event.setCancelled(true);

                // Get the backpack ID
                String backpackId = getBackpackId(item);
                if (backpackId != null) {
                    // Use a small delay to ensure the event cancellation takes effect
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        openBackpack(player, backpackId);
                    }, 1L);
                }
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
        if (openBackpackIds.containsKey(playerId)) {
            String backpackId = openBackpackIds.get(playerId);

            // Check if this inventory holder is a backpack holder
            if (event.getInventory().getHolder() instanceof BackpackHolder) {
                BackpackHolder holder = (BackpackHolder) event.getInventory().getHolder();

                // Make sure it's the same backpack ID
                if (holder.getBackpackId().equals(backpackId)) {
                    // Save the backpack contents
                    saveBackpack(backpackId, event.getInventory());

                    // Remove from open backpacks map
                    openBackpackIds.remove(playerId);
                }
            }
        }
    }

    /**
     * Handle inventory click events to prevent nesting backpacks, but allow normal item movement
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Only check for backpacks in backpacks, don't interfere with normal item moving
        if (event.getInventory().getHolder() instanceof BackpackHolder) {
            ItemStack cursor = event.getCursor();
            ItemStack current = event.getCurrentItem();

            // Check if player is trying to place a backpack inside a backpack
            if (cursor != null && isBackpack(cursor)) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player) {
                    Player player = (Player) event.getWhoClicked();
                    player.sendMessage(ChatColor.RED + "You cannot put a backpack inside another backpack!");
                }
                return;
            }

            // Also check if player is shift-clicking a backpack from their inventory
            if (event.isShiftClick() && current != null && isBackpack(current)) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player) {
                    Player player = (Player) event.getWhoClicked();
                    player.sendMessage(ChatColor.RED + "You cannot put a backpack inside another backpack!");
                }
                return;
            }
        }
    }

    /**
     * Handle inventory drag events
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        // Only check for backpacks in backpacks, don't interfere with normal item moving
        if (event.getInventory().getHolder() instanceof BackpackHolder) {
            // Check if player is trying to drag a backpack into a backpack
            ItemStack draggedItem = event.getOldCursor();

            if (draggedItem != null && isBackpack(draggedItem)) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player) {
                    Player player = (Player) event.getWhoClicked();
                    player.sendMessage(ChatColor.RED + "You cannot put a backpack inside another backpack!");
                }
            }
        }
    }
}