package at.hexle;

import at.hexle.api.AdvancementInfo;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
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

        Bukkit.getConsoleSender().sendMessage("AllAchievements");
        if(!version.startsWith("v1_20")
                && !version.startsWith("v1_19")
                && !version.startsWith("v1_18")
                && !version.startsWith("v1_17")
                && !version.startsWith("v1_16")
                && !version.startsWith("v1_15")
                && !version.startsWith("v1_14")
                && !version.startsWith("v1_13")
                && !version.startsWith("v1_12")){
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED+"Version not compatible: found "+version+" - required: 1.12+");
            Bukkit.getConsoleSender().sendMessage("");
            Bukkit.getConsoleSender().sendMessage("------------------------------------------------------");
            getPluginLoader().disablePlugin(this);
        }
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage("------------------------------------------------------");

        this.getCommand("av").setExecutor(new Commands());
        this.getCommand("av").setTabCompleter(new TabCompleter());

        Bukkit.getPluginManager().registerEvents(new Events(), this);

        Leaderboard.init();
        // Timer task - now updates player-specific timers
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                // Update player timers
                playerManager.updateTimers();

                // Update action bar for each player
                for(Player player : Bukkit.getOnlinePlayers()){
                    UUID playerId = player.getUniqueId();

                    // Check if player is in an active game mode
                    if (gameModeManager.getGameMode() != GameModeManager.GameMode.SOLO &&
                            !gameModeManager.isPlayerActive(playerId)) {
                        continue; // Skip players not in the active game
                    }

                    PlayerManager.PlayerData data = playerManager.getPlayerData(playerId);

                    // Display timer or just a default message
                    if(data.getTimerSeconds() > 0 || data.isTimerRunning()){
                        String gameMode = "";
                        if (gameModeManager.getGameMode() == GameModeManager.GameMode.COOP) {
                            gameMode = " §7[§aCoop§7]";
                        } else if (gameModeManager.getGameMode() == GameModeManager.GameMode.VERSUS) {
                            gameMode = " §7[§cVersus§7]";
                        }

                        // Get player progress
                        int total = advancementList.size();
                        int completed = playerManager.getPlayerData(playerId).getCompletedAdvancements().size();
                        String progress = String.format("§7[%d/%d]", completed, total);

                        // Show timer with game mode and progress
                        player.spigot().sendMessage(
                                ChatMessageType.ACTION_BAR,
                                TextComponent.fromLegacyText(playerManager.getFormattedTime(playerId) + gameMode + " " + progress)
                        );
                    } else {
                        player.spigot().sendMessage(
                                ChatMessageType.ACTION_BAR,
                                TextComponent.fromLegacyText("§6--:--")
                        );
                    }
                }
            }
        }, 0L, 20L);

        init();
    }

    @Override
    public void onDisable(){
        Bukkit.getConsoleSender().sendMessage("Plugin shutdown...");
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
                if(version.startsWith("v1_19") || version.startsWith("v1_20")){
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
        if(version.startsWith("v1_19") || version.startsWith("v1_20")){
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
    }

    // Get formatted time for a specific player
    public String getTime(UUID playerId){
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