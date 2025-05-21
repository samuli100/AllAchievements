package at.hexle;

import at.hexle.api.AdvancementInfo;
import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class AllAchievements extends JavaPlugin implements Listener {

    private List<Advancement> advancementList;
    private String version = "";

    // Player manager for multiplayer support
    private PlayerManager playerManager;

    // Game mode manager for coop/versus modes
    private GameModeManager gameModeManager;
    private Backpack backpackManager;

    // BossBar manager for UI display
    private BossBarManager bossBarManager;

    private static AllAchievements instance;

    @Override
    public void onEnable(){
        instance = this;
        advancementList = new ArrayList<>();

        try{
            version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        }catch(Exception e){
            e.printStackTrace();
        }

        this.saveDefaultConfig();

        // Initialize player manager
        playerManager = new PlayerManager(this);

        // Initialize game mode manager
        gameModeManager = new GameModeManager(this);

        // Initialize backpack manager
        backpackManager = new Backpack(this);
        Bukkit.getPluginManager().registerEvents(backpackManager, this);

        // Initialize BossBar manager
        bossBarManager = new BossBarManager(this);

        Bukkit.getConsoleSender().sendMessage("AllAchievements");
        // FIXED: Add 1.21 to the supported versions list and reverse the logic
        if(version.startsWith("v1_12") || version.startsWith("v1_13") ||
                version.startsWith("v1_14") || version.startsWith("v1_15") ||
                version.startsWith("v1_16") || version.startsWith("v1_17") ||
                version.startsWith("v1_18") || version.startsWith("v1_19") ||
                version.startsWith("v1_20") || version.startsWith("v1_21")){
            // Version is compatible, continue loading
            Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Version compatible: " + version);
        } else {
            // Version is not compatible
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Version not compatible: found " + version + " - required: 1.12-1.21");
            Bukkit.getConsoleSender().sendMessage("");
            Bukkit.getConsoleSender().sendMessage("------------------------------------------------------");
            getPluginLoader().disablePlugin(this);
            return; // Exit early to prevent further initialization
        }

        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage("------------------------------------------------------");

        this.getCommand("av").setExecutor(new Commands());
        this.getCommand("av").setTabCompleter(new TabCompleter());

        Events eventsListener = new Events();
        Bukkit.getPluginManager().registerEvents(eventsListener, this);

        // Timer task - now updates player-specific timers and BossBars
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                // Update shared COOP timer if needed
                if (gameModeManager.getGameMode() == GameModeManager.GameMode.COOP &&
                        gameModeManager.isGameActive() &&
                        !gameModeManager.isGamePaused()) {
                    gameModeManager.updateCoopSharedTimer();
                } else {
                    // Update individual player timers for other modes
                    playerManager.updateTimers();
                }

                // Update the BossBar for each player
                for(Player player : Bukkit.getOnlinePlayers()){
                    UUID playerId = player.getUniqueId();

                    // Check if player is in an active game mode
                    if (gameModeManager.getGameMode() != GameModeManager.GameMode.SOLO &&
                            !gameModeManager.isPlayerActive(playerId)) {
                        continue; // Skip players not in the active game
                    }

                    PlayerManager.PlayerData data = playerManager.getPlayerData(playerId);

                    // Display BossBar for active players
                    if(data.getTimerSeconds() > 0 || data.isTimerRunning()){
                        bossBarManager.updatePlayerBossBar(player);
                    }
                }
            }
        }, 0L, 20L);

        init();
    }

    @Override
    public void onDisable(){
        Bukkit.getConsoleSender().sendMessage("Plugin shutdown...");
        // Clean up BossBars
        if (bossBarManager != null) {
            bossBarManager.cleanup();
        }
        // Save all player data
        playerManager.saveAllPlayerData();
        // Save game mode data
        gameModeManager.saveConfig();
    }

    public void init(){
        Iterator<Advancement> advancementIterator = Bukkit.getServer().advancementIterator();
        while(advancementIterator.hasNext()){
            Advancement a = advancementIterator.next();
            try {
                // FIXED: Add 1.21 to the version check
                if(version.startsWith("v1_19") || version.startsWith("v1_20") || version.startsWith("v1_21")){
                    if (Objects.requireNonNull(a.getDisplay()).shouldAnnounceChat()) {
                        advancementList.add(a);
                    }
                }else {
                    AdvancementInfo info = new AdvancementInfo(a);
                    if(info != null && info.announceToChat()){
                        advancementList.add(a);
                    }
                }
            }catch (Exception e){}
        }
    }

    // Get a player's completed advancements as string list
    public List<String> getFinishedAchievements(UUID playerId){
        return playerManager.getPlayerData(playerId).getCompletedAdvancements();
    }

    // Get all advancements as string list
    public List<String> getAllAchievements(){
        List<String> allStrings = new ArrayList<>();
        // FIXED: Add 1.21 to the version check
        if(version.startsWith("v1_19") || version.startsWith("v1_20") || version.startsWith("v1_21")){
            for(Advancement advancement : advancementList){
                allStrings.add(advancement.getDisplay().getTitle());
            }
        }else{
            for(Advancement advancement : advancementList){
                Advancement adv = Bukkit.getAdvancement(advancement.getKey());
                AdvancementInfo info = new AdvancementInfo(adv);
                allStrings.add(info.getTitle());
            }
        }
        return allStrings;
    }

    public Backpack getBackpackManager() {
        return backpackManager;
    }

    // Start timer for a specific player
    public void start(UUID playerId){
        playerManager.startTimer(playerId);
    }

    // Pause/resume timer for a specific player
    public void pause(UUID playerId){
        playerManager.toggleTimer(playerId);
    }

    // Reset progress for a specific player
    public void reset(UUID playerId){
        playerManager.resetPlayer(playerId);

        // Hide BossBar when resetting a player
        if (bossBarManager != null) {
            bossBarManager.hidePlayerBossBar(playerId);
        }
    }

    // Get formatted time for a specific player
    public String getTime(UUID playerId){
        // For COOP mode, return the shared timer
        if (gameModeManager.getGameMode() == GameModeManager.GameMode.COOP &&
                gameModeManager.isGameActive() &&
                gameModeManager.isPlayerActive(playerId)) {
            int seconds = gameModeManager.getCoopSharedTimer();
            int hours = seconds / 3600;
            int remainder = seconds % 3600;
            int minutes = remainder / 60;
            int secs = remainder % 60;
            return String.format("§6%02d:%02d:%02d", hours, minutes, secs);
        }

        // For other modes, use the player-specific timer
        return playerManager.getFormattedTime(playerId);
    }

    // Check if timer is running for a specific player
    public boolean isRunning(UUID playerId){
        return playerManager.getPlayerData(playerId).isTimerRunning();
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public GameModeManager getGameModeManager() {
        return gameModeManager;
    }

    public BossBarManager getBossBarManager() {
        return bossBarManager;
    }

    public static AllAchievements getInstance(){
        return instance;
    }

    public List<Advancement> getAdvancementList() {
        return advancementList;
    }

    public String getVersion(){
        return version;
    }

    // Check if a player has completed an advancement
    public boolean hasCompletedAdvancement(UUID playerId, Advancement advancement) {
        return playerManager.hasCompletedAdvancement(playerId, advancement.getKey().toString());
    }

    // Mark an advancement as completed for a player
    public void completeAdvancement(UUID playerId, Advancement advancement) {
        String advancementKey = advancement.getKey().toString();

        // Handle based on game mode (solo, coop, versus)
        if (gameModeManager.isGameActive() && gameModeManager.isPlayerActive(playerId)) {
            gameModeManager.handleAdvancementCompletion(playerId, advancementKey);
        } else {
            // Normal solo handling
            playerManager.completeAdvancement(playerId, advancementKey);
        }
    }
}