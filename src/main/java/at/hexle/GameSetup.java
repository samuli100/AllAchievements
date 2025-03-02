package at.hexle;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
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
 * Enhanced UI for game setup and configuration for Minecraft 1.21
 */
public class GameSetup {

    /**
     * Shows the main game setup UI with improved visual elements
     */
    public static void showSetupUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36, "§6AllAchievements §7- §eGame Setup");

        // Fill with glass
        ItemStack fillerItem = createGlassPane();
        for (int i = 0; i < 36; i++) {
            inv.setItem(i, fillerItem);
        }

        GameModeManager gameModeManager = AllAchievements.getInstance().getGameModeManager();
        GameModeManager.GameMode currentMode = gameModeManager.getGameMode();

        // Title item at the top center
        ItemStack titleItem = new ItemStack(Material.BOOK, 1);
        ItemMeta titleMeta = titleItem.getItemMeta();
        titleMeta.setDisplayName("§6§lGame Setup");
        List<String> titleLore = new ArrayList<>();
        titleLore.add("§7Configure your achievement challenge");
        titleLore.add("§7Current game status: " +
                (gameModeManager.isGameActive() ? "§aActive" : "§cInactive"));
        if (gameModeManager.isGamePaused()) {
            titleLore.add("§e§lGame is currently PAUSED");
        }
        titleMeta.setLore(titleLore);
        titleItem.setItemMeta(titleMeta);
        inv.setItem(4, titleItem);

        // Solo mode item
        boolean soloSelected = currentMode == GameModeManager.GameMode.SOLO;
        ItemStack soloItem = createModeItem(
                soloSelected ? Material.DIAMOND_HELMET : Material.DIAMOND_SWORD,
                "§e§lSolo Mode" + (soloSelected ? " §a✓" : ""),
                "§7Each player works on their own advancements",
                "§7independently from other players.",
                "",
                soloSelected ? "§a§lCURRENTLY SELECTED" : "§7Click to select"
        );
        inv.setItem(11, soloItem);

        // Coop mode item
        boolean coopSelected = currentMode == GameModeManager.GameMode.COOP;
        ItemStack coopItem = createModeItem(
                coopSelected ? Material.GOLDEN_HELMET : Material.GOLDEN_APPLE,
                "§a§lCoop Mode" + (coopSelected ? " §a✓" : ""),
                "§7Players work together to complete advancements.",
                "§7When one player gets an advancement,",
                "§7all team members receive credit for it.",
                "",
                coopSelected ? "§a§lCURRENTLY SELECTED" : "§7Click to select"
        );
        inv.setItem(13, coopItem);

        // Versus mode item
        boolean versusSelected = currentMode == GameModeManager.GameMode.VERSUS;
        ItemStack versusItem = createModeItem(
                versusSelected ? Material.IRON_HELMET : Material.BOW,
                "§c§lVersus Mode" + (versusSelected ? " §a✓" : ""),
                "§7Players compete to complete all advancements",
                "§7first. The first to finish wins the challenge!",
                "",
                versusSelected ? "§a§lCURRENTLY SELECTED" : "§7Click to select"
        );
        inv.setItem(15, versusItem);

        // Player management button
        ItemStack playersItem = new ItemStack(Material.PLAYER_HEAD, 1);
        ItemMeta playersMeta = playersItem.getItemMeta();
        playersMeta.setDisplayName("§d§lManage Players");
        List<String> playersLore = new ArrayList<>();
        playersLore.add("§7Add or remove players from the game");
        playersLore.add("");
        playersLore.add("§7Currently active: §e" + gameModeManager.getActivePlayers().size() + " players");
        if (currentMode != GameModeManager.GameMode.SOLO && gameModeManager.getActivePlayers().isEmpty()) {
            playersLore.add("§c§lNo players added! Add players to start.");
        }
        playersMeta.setLore(playersLore);
        playersItem.setItemMeta(playersMeta);
        inv.setItem(22, playersItem);

        // Start/Stop button
        ItemStack gameControlItem;
        if (gameModeManager.isGameActive()) {
            gameControlItem = new ItemStack(Material.REDSTONE_BLOCK, 1);
            ItemMeta controlMeta = gameControlItem.getItemMeta();
            controlMeta.setDisplayName("§c§lStop Game");
            List<String> controlLore = new ArrayList<>();
            controlLore.add("§7End the current game session");
            if (gameModeManager.isGamePaused()) {
                controlLore.add("§e§lGame is currently PAUSED");

                // Add "Resume Game" button when paused
                ItemStack resumeItem = new ItemStack(Material.EMERALD, 1);
                ItemMeta resumeMeta = resumeItem.getItemMeta();
                resumeMeta.setDisplayName("§a§lResume Game");
                List<String> resumeLore = new ArrayList<>();
                resumeLore.add("§7Continue the paused game");
                resumeMeta.setLore(resumeLore);
                resumeItem.setItemMeta(resumeMeta);
                inv.setItem(30, resumeItem);

                // Add "Pause Game" button (disabled)
                ItemStack pauseItem = new ItemStack(Material.GRAY_DYE, 1);
                ItemMeta pauseMeta = pauseItem.getItemMeta();
                pauseMeta.setDisplayName("§8§lPause Game");
                List<String> pauseLore = new ArrayList<>();
                pauseLore.add("§7Game is already paused");
                pauseMeta.setLore(pauseLore);
                pauseItem.setItemMeta(pauseMeta);
                inv.setItem(32, pauseItem);
            } else {
                // Add "Pause Game" button
                ItemStack pauseItem = new ItemStack(Material.CLOCK, 1);
                ItemMeta pauseMeta = pauseItem.getItemMeta();
                pauseMeta.setDisplayName("§e§lPause Game");
                List<String> pauseLore = new ArrayList<>();
                pauseLore.add("§7Temporarily pause the game");
                pauseMeta.setLore(pauseLore);
                pauseItem.setItemMeta(pauseMeta);
                inv.setItem(31, pauseItem);
            }
            controlMeta.setLore(controlLore);
            gameControlItem.setItemMeta(controlMeta);
        } else {
            gameControlItem = new ItemStack(Material.EMERALD_BLOCK, 1);
            ItemMeta controlMeta = gameControlItem.getItemMeta();
            controlMeta.setDisplayName("§a§lStart Game");
            List<String> controlLore = new ArrayList<>();
            controlLore.add("§7Begin a new game with the selected settings");
            if (currentMode != GameModeManager.GameMode.SOLO && gameModeManager.getActivePlayers().isEmpty()) {
                controlLore.add("§c§lWARNING: No players added to the game!");
                controlLore.add("§c§lAdd players before starting.");
            }
            controlMeta.setLore(controlLore);
            gameControlItem.setItemMeta(controlMeta);
        }
        inv.setItem(31, gameControlItem);

        // Stats button
        ItemStack statsItem = new ItemStack(Material.PAPER, 1);
        ItemMeta statsMeta = statsItem.getItemMeta();
        statsMeta.setDisplayName("§b§lView Stats");
        List<String> statsLore = new ArrayList<>();
        statsLore.add("§7Check achievement progress");
        statsMeta.setLore(statsLore);
        statsItem.setItemMeta(statsMeta);
        inv.setItem(27, statsItem);

        // Leaderboard button
        ItemStack leaderboardItem = new ItemStack(Material.GOLD_INGOT, 1);
        ItemMeta leaderboardMeta = leaderboardItem.getItemMeta();
        leaderboardMeta.setDisplayName("§6§lLeaderboard");
        List<String> leaderboardLore = new ArrayList<>();
        leaderboardLore.add("§7View top players");
        leaderboardMeta.setLore(leaderboardLore);
        leaderboardItem.setItemMeta(leaderboardMeta);
        inv.setItem(35, leaderboardItem);

        player.openInventory(inv);
    }

    /**
     * Create a glass pane for UI background
     */
    private static ItemStack createGlassPane() {
        ItemStack fillerItem = new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1);
        ItemMeta meta = fillerItem.getItemMeta();
        meta.setDisplayName(" ");
        fillerItem.setItemMeta(meta);
        return fillerItem;
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
     * Shows the player management UI with improved visual feedback
     */
    public static void showPlayerManagementUI(Player admin, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, "§6AllAchievements §7- §ePlayer Management");

        // Fill with glass
        ItemStack fillerItem = createGlassPane();
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, fillerItem);
        }

        GameModeManager gameModeManager = AllAchievements.getInstance().getGameModeManager();

        // Title item
        ItemStack titleItem = new ItemStack(Material.BOOK, 1);
        ItemMeta titleMeta = titleItem.getItemMeta();
        titleMeta.setDisplayName("§d§lManage Game Players");

        List<String> titleLore = new ArrayList<>();
        titleLore.add("§7Click on players to toggle their participation");
        titleLore.add("§7Current game mode: §e" + gameModeManager.getGameMode().name());
        titleLore.add("§7Active players: §e" + gameModeManager.getActivePlayers().size());
        titleMeta.setLore(titleLore);
        titleItem.setItemMeta(titleMeta);
        inv.setItem(4, titleItem);

        // Back button
        ItemStack backItem = new ItemStack(Material.ARROW, 1);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName("§c§lBack to Game Setup");
        backItem.setItemMeta(backMeta);
        inv.setItem(0, backItem);

        // Reset all button
        ItemStack resetItem = new ItemStack(Material.BARRIER, 1);
        ItemMeta resetMeta = resetItem.getItemMeta();
        resetMeta.setDisplayName("§c§lReset All Players");
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

        // Add player items
        int slot = 9; // Start after the title row
        for (int i = startIndex; i < endIndex; i++) {
            Player targetPlayer = onlinePlayers.get(i);
            UUID targetId = targetPlayer.getUniqueId();
            boolean isActive = gameModeManager.isPlayerActive(targetId);

            // Create player head
            ItemStack playerItem = new ItemStack(Material.PLAYER_HEAD, 1);
            SkullMeta skullMeta = (SkullMeta) playerItem.getItemMeta();
            skullMeta.setOwningPlayer(targetPlayer);
            playerItem.setItemMeta(skullMeta);

            // Set display name and status
            ItemMeta meta = playerItem.getItemMeta();
            meta.setDisplayName((isActive ? "§a" : "§c") + targetPlayer.getName() +
                    (isActive ? " §a✓" : ""));

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
            prevMeta.setDisplayName("§6§lPrevious Page");
            prevPage.setItemMeta(prevMeta);
            inv.setItem(45, prevPage);

            // Page indicator
            ItemStack pageIndicator = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE, 1);
            ItemMeta pageMeta = pageIndicator.getItemMeta();
            pageMeta.setDisplayName("§6Page " + (page + 1) + " of " + totalPages);
            pageIndicator.setItemMeta(pageMeta);
            inv.setItem(49, pageIndicator);

            // Next page button
            ItemStack nextPage = new ItemStack(Material.ARROW, 1);
            ItemMeta nextMeta = nextPage.getItemMeta();
            nextMeta.setDisplayName("§6§lNext Page");
            nextPage.setItemMeta(nextMeta);
            inv.setItem(53, nextPage);
        }

        admin.openInventory(inv);
    }

    /**
     * Handle inventory click events in the setup UI with improved feedback
     */
    public static void handleInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        // Always cancel the click to prevent item taking
        event.setCancelled(true);

        // Main game setup UI
        if (title.equals("§6AllAchievements §7- §eGame Setup")) {
            // Game mode selection
            if (slot == 11) { // Solo mode
                setGameMode(player, GameModeManager.GameMode.SOLO);
                playClickSound(player);
                showSetupUI(player);
            } else if (slot == 13) { // Coop mode
                setGameMode(player, GameModeManager.GameMode.COOP);
                playClickSound(player);
                showSetupUI(player);
            } else if (slot == 15) { // Versus mode
                setGameMode(player, GameModeManager.GameMode.VERSUS);
                playClickSound(player);
                showSetupUI(player);
            }
            // Player management
            else if (slot == 22) {
                playClickSound(player);
                showPlayerManagementUI(player, 0);
            }
            // Start/Stop game
            else if (slot == 31) {
                GameModeManager gameModeManager = AllAchievements.getInstance().getGameModeManager();

                if (gameModeManager.isGameActive()) {
                    // Stop the game
                    gameModeManager.endGame();
                    player.sendMessage("§7-------- §6AllAchievements§7 ----------");
                    player.sendMessage("§c§lGame has been stopped.");
                    player.sendMessage("§7--------------------------------");
                    playSuccessSound(player);
                } else {
                    // Start the game
                    if (gameModeManager.getGameMode() != GameModeManager.GameMode.SOLO &&
                            gameModeManager.getActivePlayers().isEmpty()) {
                        player.sendMessage("§7-------- §6AllAchievements§7 ----------");
                        player.sendMessage("§c§lCannot start game: No players added!");
                        player.sendMessage("§7--------------------------------");
                        playErrorSound(player);
                    } else {
                        gameModeManager.startGame();
                        player.sendMessage("§7-------- §6AllAchievements§7 ----------");
                        player.sendMessage("§a§lGame has been started in " + gameModeManager.getGameMode() + " mode!");
                        player.sendMessage("§7--------------------------------");
                        playSuccessSound(player);

                        // Notify all active players
                        for (UUID playerId : gameModeManager.getActivePlayers()) {
                            Player gamePlayer = Bukkit.getPlayer(playerId);
                            if (gamePlayer != null && gamePlayer != player) {
                                gamePlayer.sendMessage("§7-------- §6AllAchievements§7 ----------");
                                gamePlayer.sendMessage("§a§lA new " + gameModeManager.getGameMode() + " game has started!");
                                gamePlayer.sendMessage("§7--------------------------------");
                                playSuccessSound(gamePlayer);
                            }
                        }
                    }
                }

                // Update the UI
                showSetupUI(player);
            }
            // Resume game button
            else if (slot == 30 && AllAchievements.getInstance().getGameModeManager().isGamePaused()) {
                // Resume game
                boolean paused = AllAchievements.getInstance().getGameModeManager().togglePauseGame();
                playClickSound(player);
                showSetupUI(player);
            }
            // Pause game button
            else if (slot == 32 || (slot == 31 && AllAchievements.getInstance().getGameModeManager().isGameActive())) {
                // Pause game
                boolean paused = AllAchievements.getInstance().getGameModeManager().togglePauseGame();
                playClickSound(player);
                showSetupUI(player);
            }
            // Stats button
            else if (slot == 27) {
                playClickSound(player);
                Stats.showStats(player, player, 0);
            }
            // Leaderboard button
            else if (slot == 35) {
                playClickSound(player);
                Leaderboard.showLeaderboard(player, 0);
            }
        }
        // Player management UI
        else if (title.equals("§6AllAchievements §7- §ePlayer Management")) {
            GameModeManager gameModeManager = AllAchievements.getInstance().getGameModeManager();

            // Back button
            if (slot == 0) {
                playClickSound(player);
                showSetupUI(player);
            }
            // Reset all players button
            else if (slot == 8) {
                gameModeManager.getActivePlayers().clear();
                gameModeManager.saveConfig(); // Save the changes to persist them
                player.sendMessage("§7-------- §6AllAchievements§7 ----------");
                player.sendMessage("§c§lAll players have been removed from the game.");
                player.sendMessage("§7--------------------------------");
                playSuccessSound(player);

                // Update the UI
                showPlayerManagementUI(player, 0);
            }
            // Navigation buttons
            else if (slot == 45 || slot == 53) {
                // Get current page
                int currentPage = 0;
                if (event.getInventory().getItem(49) != null) {
                    String pageText = event.getInventory().getItem(49).getItemMeta().getDisplayName();
                    String[] parts = pageText.split(" ");
                    currentPage = Integer.parseInt(parts[1]) - 1; // Convert from 1-based to 0-based

                    // Get total pages
                    int totalPages = Integer.parseInt(parts[3]);

                    // Navigate to new page with bounds checking
                    int newPage;
                    if (slot == 45) {
                        // Previous page
                        newPage = Math.max(0, currentPage - 1);
                    } else {
                        // Next page
                        newPage = Math.min(totalPages - 1, currentPage + 1);
                    }

                    playClickSound(player);
                    showPlayerManagementUI(player, newPage);
                }
            }
            // Player selection (slots 9-44)
            else if (slot >= 9 && slot < 45) {
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName()) {
                    String playerName = clickedItem.getItemMeta().getDisplayName();
                    if (playerName.startsWith("§a") || playerName.startsWith("§c")) {
                        playerName = playerName.replace(" §a✓", "").substring(2); // Remove color code and check mark

                        Player targetPlayer = Bukkit.getPlayer(playerName);
                        if (targetPlayer != null) {
                            UUID targetId = targetPlayer.getUniqueId();
                            boolean isActive = gameModeManager.isPlayerActive(targetId);

                            // Toggle player's active status
                            if (isActive) {
                                // Remove player
                                gameModeManager.removePlayer(targetId);
                                player.sendMessage("§7-------- §6AllAchievements§7 ----------");
                                player.sendMessage("§c§lRemoved " + targetPlayer.getName() + " from the game.");
                                player.sendMessage("§7--------------------------------");
                                playClickSound(player);
                            } else {
                                // Add player
                                gameModeManager.addPlayer(targetId);
                                player.sendMessage("§7-------- §6AllAchievements§7 ----------");
                                player.sendMessage("§a§lAdded " + targetPlayer.getName() + " to the game.");
                                player.sendMessage("§7--------------------------------");
                                playSuccessSound(player);
                            }

                            // Get current page
                            int currentPage = 0;
                            if (event.getInventory().getItem(49) != null) {
                                String pageText = event.getInventory().getItem(49).getItemMeta().getDisplayName();
                                String[] parts = pageText.split(" ");
                                currentPage = Integer.parseInt(parts[1]) - 1;
                            }

                            // Update the UI
                            showPlayerManagementUI(player, currentPage);
                        }
                    }
                }
            }
        }
    }

    /**
     * Set the active game mode with improved feedback
     */
    private static void setGameMode(Player player, GameModeManager.GameMode mode) {
        GameModeManager gameModeManager = AllAchievements.getInstance().getGameModeManager();

        // Cannot change mode if a game is active
        if (gameModeManager.isGameActive()) {
            player.sendMessage("§7-------- §6AllAchievements§7 ----------");
            player.sendMessage("§c§lCannot change game mode while a game is active!");
            player.sendMessage("§cStop the current game first.");
            player.sendMessage("§7--------------------------------");
            playErrorSound(player);
            return;
        }

        // Set the mode and save configuration
        gameModeManager.setGameMode(mode);
        player.sendMessage("§7-------- §6AllAchievements§7 ----------");
        player.sendMessage("§a§lGame mode set to " + mode);
        player.sendMessage("§7--------------------------------");
        playSuccessSound(player);
    }

    /**
     * Play a click sound for UI feedback
     */
    private static void playClickSound(Player player) {
        try {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        } catch (Exception e) {
            // Ignore if sound doesn't exist
        }
    }

    /**
     * Play a success sound for positive feedback
     */
    private static void playSuccessSound(Player player) {
        try {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f);
        } catch (Exception e) {
            // Ignore if sound doesn't exist
        }
    }

    /**
     * Play an error sound for negative feedback
     */
    private static void playErrorSound(Player player) {
        try {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
        } catch (Exception e) {
            // Ignore if sound doesn't exist
        }
    }
}