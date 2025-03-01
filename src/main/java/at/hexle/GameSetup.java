package at.hexle;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles the game setup UI for configuring Solo, Coop, and Versus modes
 */
public class GameSetup {

    /**
     * Shows the main game setup UI
     */
    public static void showSetupUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§6AllAchievements §7- §eGame Setup");

        // Fill with glass
        for (int i = 0; i < 27; i++) {
            if (AllAchievements.getInstance().getVersion().startsWith("v1_12")) {
                inv.setItem(i, new ItemStack(Material.getMaterial("STAINED_GLASS_PANE"), 1, (byte)15));
            } else {
                inv.setItem(i, new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1));
            }
        }

        GameModeManager gameModeManager = AllAchievements.getInstance().getGameModeManager();
        GameModeManager.GameMode currentMode = gameModeManager.getGameMode();

        // Solo mode item
        ItemStack soloItem = createModeItem(Material.DIAMOND_SWORD, "§eSolo Mode",
                "§7Each player works on their own advancements",
                "§7independently from other players.",
                "",
                currentMode == GameModeManager.GameMode.SOLO ? "§a§lCURRENTLY SELECTED" : "§7Click to select");
        inv.setItem(10, soloItem);

        // Coop mode item
        ItemStack coopItem = createModeItem(Material.GOLDEN_APPLE, "§aCoop Mode",
                "§7Players work together to complete advancements.",
                "§7When one player gets an advancement,",
                "§7all team members receive credit for it.",
                "",
                currentMode == GameModeManager.GameMode.COOP ? "§a§lCURRENTLY SELECTED" : "§7Click to select");
        inv.setItem(13, coopItem);

        // Versus mode item
        ItemStack versusItem = createModeItem(Material.BOW, "§cVersus Mode",
                "§7Players compete to complete all advancements",
                "§7first. The first to finish wins the challenge!",
                "",
                currentMode == GameModeManager.GameMode.VERSUS ? "§a§lCURRENTLY SELECTED" : "§7Click to select");
        inv.setItem(16, versusItem);

        // Player management button
        ItemStack playersItem = new ItemStack(Material.BOOK, 1);
        ItemMeta playersMeta = playersItem.getItemMeta();
        playersMeta.setDisplayName("§6Manage Players");
        List<String> playersLore = new ArrayList<>();
        playersLore.add("§7Add or remove players from the game");
        playersLore.add("§7Currently active: §e" + gameModeManager.getActivePlayers().size() + " players");
        playersMeta.setLore(playersLore);
        playersItem.setItemMeta(playersMeta);
        inv.setItem(22, playersItem);

        // Start/Stop button
        ItemStack gameControlItem;
        if (gameModeManager.isGameActive()) {
            gameControlItem = new ItemStack(Material.REDSTONE_BLOCK, 1);
            ItemMeta controlMeta = gameControlItem.getItemMeta();
            controlMeta.setDisplayName("§cStop Game");
            List<String> controlLore = new ArrayList<>();
            controlLore.add("§7End the current game session");
            controlMeta.setLore(controlLore);
            gameControlItem.setItemMeta(controlMeta);
        } else {
            gameControlItem = new ItemStack(Material.EMERALD_BLOCK, 1);
            ItemMeta controlMeta = gameControlItem.getItemMeta();
            controlMeta.setDisplayName("§aStart Game");
            List<String> controlLore = new ArrayList<>();
            controlLore.add("§7Begin a new game with the selected settings");
            if (currentMode != GameModeManager.GameMode.SOLO && gameModeManager.getActivePlayers().isEmpty()) {
                controlLore.add("§c§lWARNING: No players added to the game!");
            }
            controlMeta.setLore(controlLore);
            gameControlItem.setItemMeta(controlMeta);
        }
        inv.setItem(4, gameControlItem);

        player.openInventory(inv);
    }

    /**
     * Helper method to create a game mode selection item
     */
    private static ItemStack createModeItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);

        List<String> loreList = new ArrayList<>();
        for (String line : lore) {
            loreList.add(line);
        }

        meta.setLore(loreList);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Shows the player management UI
     */
    public static void showPlayerManagementUI(Player admin, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, "§6AllAchievements §7- §ePlayer Management");

        // Fill with glass
        for (int i = 0; i < 54; i++) {
            if (AllAchievements.getInstance().getVersion().startsWith("v1_12")) {
                inv.setItem(i, new ItemStack(Material.getMaterial("STAINED_GLASS_PANE"), 1, (byte)15));
            } else {
                inv.setItem(i, new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1));
            }
        }

        GameModeManager gameModeManager = AllAchievements.getInstance().getGameModeManager();

        // Title item
        ItemStack titleItem = new ItemStack(Material.BOOK, 1);
        ItemMeta titleMeta = titleItem.getItemMeta();
        titleMeta.setDisplayName("§6Manage Game Players");

        List<String> titleLore = new ArrayList<>();
        titleLore.add("§7Click on players to toggle their participation");
        titleLore.add("§7Current game mode: §e" + gameModeManager.getGameMode().name());
        titleMeta.setLore(titleLore);
        titleItem.setItemMeta(titleMeta);
        inv.setItem(4, titleItem);

        // Back button
        ItemStack backItem = new ItemStack(Material.ARROW, 1);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName("§cBack to Game Setup");
        backItem.setItemMeta(backMeta);
        inv.setItem(0, backItem);

        // Reset all button
        ItemStack resetItem = new ItemStack(Material.BARRIER, 1);
        ItemMeta resetMeta = resetItem.getItemMeta();
        resetMeta.setDisplayName("§cReset All Players");
        List<String> resetLore = new ArrayList<>();
        resetLore.add("§7Remove all players from the game");
        resetLore.add("§c§lWARNING: Cannot be undone!");
        resetMeta.setLore(resetLore);
        resetItem.setItemMeta(resetMeta);
        inv.setItem(8, resetItem);

        // Get all online players
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());

        // Create pagination system
        int playersPerPage = 36; // 9x4 grid for players
        int totalPages = (int) Math.ceil((double) onlinePlayers.size() / playersPerPage);

        if (page < 0) page = 0;
        if (page >= totalPages && totalPages > 0) page = totalPages - 1;

        // Calculate range for current page
        int startIndex = page * playersPerPage;
        int endIndex = Math.min(startIndex + playersPerPage, onlinePlayers.size());

        // Add player heads
        int slot = 9; // Start after the title row
        for (int i = startIndex; i < endIndex; i++) {
            Player targetPlayer = onlinePlayers.get(i);
            UUID targetId = targetPlayer.getUniqueId();
            boolean isActive = gameModeManager.isPlayerActive(targetId);

            // Create player head
            ItemStack playerItem;
            if (!AllAchievements.getInstance().getVersion().startsWith("v1_12")) {
                playerItem = new ItemStack(Material.PLAYER_HEAD, 1);
                SkullMeta skullMeta = (SkullMeta) playerItem.getItemMeta();
                skullMeta.setOwningPlayer(targetPlayer);
                playerItem.setItemMeta(skullMeta);
            } else {
                playerItem = isActive ?
                        new ItemStack(Material.EMERALD, 1) :
                        new ItemStack(Material.REDSTONE, 1);
            }

            // Set display name and status
            ItemMeta meta = playerItem.getItemMeta();
            meta.setDisplayName((isActive ? "§a" : "§c") + targetPlayer.getName());

            List<String> lore = new ArrayList<>();
            lore.add(isActive ? "§aActive in game" : "§cNot in game");
            lore.add("§7Click to " + (isActive ? "remove from" : "add to") + " game");
            meta.setLore(lore);

            playerItem.setItemMeta(meta);
            inv.setItem(slot, playerItem);
            slot++;
        }

        // Add navigation buttons if needed
        if (totalPages > 1) {
            // Previous page button
            ItemStack prevPage = new ItemStack(Material.ARROW, 1);
            ItemMeta prevMeta = prevPage.getItemMeta();
            prevMeta.setDisplayName("§6Previous Page");
            prevPage.setItemMeta(prevMeta);
            inv.setItem(45, prevPage);

            // Page indicator
            ItemStack pageIndicator;
            if (AllAchievements.getInstance().getVersion().startsWith("v1_12")) {
                pageIndicator = new ItemStack(Material.getMaterial("STAINED_GLASS_PANE"), 1, (byte)4);
            } else {
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
            inv.setItem(53, nextPage);
        }

        admin.openInventory(inv);
    }

    /**
     * Handle inventory click events in the setup UI
     */
    public static void handleInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        // Main game setup UI
        if (title.equals("§6AllAchievements §7- §eGame Setup")) {
            // Game mode selection
            if (slot == 10) { // Solo mode
                setGameMode(player, GameModeManager.GameMode.SOLO);
                showSetupUI(player);
            } else if (slot == 13) { // Coop mode
                setGameMode(player, GameModeManager.GameMode.COOP);
                showSetupUI(player);
            } else if (slot == 16) { // Versus mode
                setGameMode(player, GameModeManager.GameMode.VERSUS);
                showSetupUI(player);
            }
            // Player management
            else if (slot == 22) {
                showPlayerManagementUI(player, 0);
            }
            // Start/Stop game
            else if (slot == 4) {
                GameModeManager gameModeManager = AllAchievements.getInstance().getGameModeManager();

                if (gameModeManager.isGameActive()) {
                    // Stop the game
                    gameModeManager.endGame();
                    player.sendMessage("§7-------- §6AllAchievements§7 ----------");
                    player.sendMessage("§cGame has been stopped.");
                    player.sendMessage("§7--------------------------------");
                } else {
                    // Start the game
                    if (gameModeManager.getGameMode() != GameModeManager.GameMode.SOLO &&
                            gameModeManager.getActivePlayers().isEmpty()) {
                        player.sendMessage("§7-------- §6AllAchievements§7 ----------");
                        player.sendMessage("§cCannot start game: No players added!");
                        player.sendMessage("§7--------------------------------");
                    } else {
                        gameModeManager.startGame();
                        player.sendMessage("§7-------- §6AllAchievements§7 ----------");
                        player.sendMessage("§aGame has been started in " + gameModeManager.getGameMode() + " mode!");
                        player.sendMessage("§7--------------------------------");

                        // Notify all active players
                        for (UUID playerId : gameModeManager.getActivePlayers()) {
                            Player gamePlayer = Bukkit.getPlayer(playerId);
                            if (gamePlayer != null && gamePlayer != player) {
                                gamePlayer.sendMessage("§7-------- §6AllAchievements§7 ----------");
                                gamePlayer.sendMessage("§aA new " + gameModeManager.getGameMode() + " game has started!");
                                gamePlayer.sendMessage("§7--------------------------------");
                            }
                        }
                    }
                }

                // Update the UI
                showSetupUI(player);
            }
        }
        // Player management UI
        else if (title.equals("§6AllAchievements §7- §ePlayer Management")) {
            GameModeManager gameModeManager = AllAchievements.getInstance().getGameModeManager();

            // Back button
            if (slot == 0) {
                showSetupUI(player);
            }
            // Reset all players button
            else if (slot == 8) {
                gameModeManager.getActivePlayers().clear();
                player.sendMessage("§7-------- §6AllAchievements§7 ----------");
                player.sendMessage("§cAll players have been removed from the game.");
                player.sendMessage("§7--------------------------------");

                // Update the UI
                showPlayerManagementUI(player, 0);
            }
            // Navigation buttons
            else if (slot == 45 || slot == 53) {
                // Get current page
                int currentPage = 0;
                if (event.getInventory().getItem(49) != null) {
                    String pageText = event.getInventory().getItem(49).getItemMeta().getDisplayName();
                    currentPage = Integer.parseInt(pageText.split(" ")[1]) - 1;
                }

                // Navigate to new page
                int newPage = slot == 45 ? currentPage - 1 : currentPage + 1;
                showPlayerManagementUI(player, newPage);
            }
            // Player selection
            else if (slot >= 9 && slot < 45) {
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName()) {
                    String playerName = clickedItem.getItemMeta().getDisplayName();
                    playerName = playerName.substring(2); // Remove color code

                    Player targetPlayer = Bukkit.getPlayer(playerName);
                    if (targetPlayer != null) {
                        UUID targetId = targetPlayer.getUniqueId();
                        boolean isActive = gameModeManager.isPlayerActive(targetId);

                        if (isActive) {
                            // Remove player
                            gameModeManager.removePlayer(targetId);
                            player.sendMessage("§7-------- §6AllAchievements§7 ----------");
                            player.sendMessage("§cRemoved " + targetPlayer.getName() + " from the game.");
                            player.sendMessage("§7--------------------------------");
                        } else {
                            // Add player
                            gameModeManager.addPlayer(targetId);
                            player.sendMessage("§7-------- §6AllAchievements§7 ----------");
                            player.sendMessage("§aAdded " + targetPlayer.getName() + " to the game.");
                            player.sendMessage("§7--------------------------------");
                        }

                        // Get current page
                        int currentPage = 0;
                        if (event.getInventory().getItem(49) != null) {
                            String pageText = event.getInventory().getItem(49).getItemMeta().getDisplayName();
                            currentPage = Integer.parseInt(pageText.split(" ")[1]) - 1;
                        }

                        // Update the UI
                        showPlayerManagementUI(player, currentPage);
                    }
                }
            }
        }
    }

    /**
     * Set the active game mode
     */
    private static void setGameMode(Player player, GameModeManager.GameMode mode) {
        GameModeManager gameModeManager = AllAchievements.getInstance().getGameModeManager();

        // Cannot change mode if a game is active
        if (gameModeManager.isGameActive()) {
            player.sendMessage("§7-------- §6AllAchievements§7 ----------");
            player.sendMessage("§cCannot change game mode while a game is active!");
            player.sendMessage("§cStop the current game first.");
            player.sendMessage("§7--------------------------------");
            return;
        }

        // Set the mode
        gameModeManager.setGameMode(mode);
        player.sendMessage("§7-------- §6AllAchievements§7 ----------");
        player.sendMessage("§aGame mode set to " + mode);
        player.sendMessage("§7--------------------------------");
    }
}