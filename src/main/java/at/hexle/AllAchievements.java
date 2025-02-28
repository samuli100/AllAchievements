package at.hexle;

import at.hexle.api.AdvancementInfo;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class AllAchievements extends JavaPlugin implements Listener {

    private List<Advancement> advancementList;
    private List<Advancement> finishedAdvancementList;

    private String version = "";

    private boolean timer = false;
    private int timerseconds = 0;
    private List<String> resetPlayers;

    private static AllAchievements instance;

    @Override
    public void onEnable(){
        instance = this;
        advancementList = new ArrayList<>();
        finishedAdvancementList = new ArrayList<>();
        resetPlayers = new ArrayList<>();

        try{
            version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        }catch(Exception e){
            e.printStackTrace();
        }

        this.saveDefaultConfig();
        timerseconds = this.getConfig().getInt("timer");

        List<String> list = this.getConfig().getStringList("advancements");
        for(String s : list){
            if (s.contains(":")) {
                try {
                    Advancement adv = Bukkit.getAdvancement(NamespacedKey.minecraft(s.split(":")[1]));
                    if (adv != null) {
                        finishedAdvancementList.add(adv);
                    }
                } catch (Exception e) {
                    getLogger().warning("Failed to load advancement: " + s);
                }
            }
        }

        Bukkit.getConsoleSender().sendMessage("------------------------------------------------------");
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage("._   _   _____  __  __  _       _____");
        Bukkit.getConsoleSender().sendMessage("| | | | | ____| \\ \\/ / | |     | ____|                         ");
        Bukkit.getConsoleSender().sendMessage("| |_| | |  _|    \\  /  | |     |  _|                           ");
        Bukkit.getConsoleSender().sendMessage("|  _  | | |___   /  \\  | |___  | |___");
        Bukkit.getConsoleSender().sendMessage("|_| |_| |_____| /_/\\_\\ |_____| |_____|");
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage("Hexle_Development_Systems - https://hexle.at");
        Bukkit.getConsoleSender().sendMessage("");

        // Updated version check to include newest Minecraft versions (1.21.4, 1.21+)
        if(!version.startsWith("v1_21")
                && !version.startsWith("v1_20")
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
            return;
        }

        getLogger().info("Minecraft version detected: " + version);
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage("------------------------------------------------------");

        this.getCommand("av").setExecutor(new Commands());
        this.getCommand("av").setTabCompleter(new TabCompleter());

        Bukkit.getPluginManager().registerEvents(new Events(), this);

        // Use BukkitRunnable for timer
        new BukkitRunnable() {
            @Override
            public void run() {
                if(timer && Bukkit.getOnlinePlayers().size() > 0){
                    timerseconds++;
                }
                if(timerseconds > 0){
                    for(Player player : Bukkit.getOnlinePlayers()){
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(getTime()));
                    }
                } else {
                    for(Player player : Bukkit.getOnlinePlayers()){
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("§6--:--"));
                    }
                }
            }
        }.runTaskTimer(this, 0L, 20L);

        init();
    }

    @Override
    public void onDisable(){
        Bukkit.getConsoleSender().sendMessage("Plugin shutdown...");
        FileConfiguration config = this.getConfig();
        List<String> names = new ArrayList<>();
        for(Advancement a : finishedAdvancementList){
            if (a != null && a.getKey() != null) {
                names.add(a.getKey().toString());
            }
        }
        config.set("advancements", names);
        config.set("timer", timerseconds);
        saveConfig();
    }

    public void init(){
        Iterator<Advancement> advancementIterator = Bukkit.getServer().advancementIterator();
        while(advancementIterator.hasNext()){
            Advancement a = advancementIterator.next();
            try {
                // Updated version check to include newest Minecraft versions
                if(isNewerVersion()){
                    if (a != null && a.getDisplay() != null && a.getDisplay().shouldAnnounceChat()) {
                        advancementList.add(a);
                    }
                } else {
                    AdvancementInfo info = new AdvancementInfo(a);
                    if(info != null && info.announceToChat()){
                        advancementList.add(a);
                    }
                }
            } catch (Exception e){
                getLogger().warning("Error processing advancement: " + e.getMessage());
            }
        }
        getLogger().info("Loaded " + advancementList.size() + " advancements");
    }

    /**
     * Helper method to check if we're on Minecraft 1.19+
     */
    private boolean isNewerVersion() {
        return version.startsWith("v1_21") || version.startsWith("v1_20") || version.startsWith("v1_19");
    }

    public List<String> getFinishedAchievements(){
        List<String> finishedStrings = new ArrayList<>();
        // Updated version check to include newest Minecraft versions
        if(isNewerVersion()){
            for(Advancement advancement : finishedAdvancementList){
                if (advancement != null && advancement.getDisplay() != null) {
                    finishedStrings.add(advancement.getDisplay().getTitle());
                }
            }
        } else {
            for(Advancement advancement : finishedAdvancementList){
                if (advancement != null) {
                    AdvancementInfo info = new AdvancementInfo(advancement);
                    if (info.getTitle() != null) {
                        finishedStrings.add(info.getTitle());
                    }
                }
            }
        }

        return finishedStrings;
    }

    public List<String> getAllAchievements(){
        List<String> allStrings = new ArrayList<>();
        // Updated version check to include newest Minecraft versions
        if(isNewerVersion()){
            for(Advancement advancement : advancementList){
                if (advancement != null && advancement.getDisplay() != null) {
                    allStrings.add(advancement.getDisplay().getTitle());
                }
            }
        } else {
            for(Advancement advancement : advancementList){
                if (advancement != null) {
                    Advancement adv = Bukkit.getAdvancement(advancement.getKey());
                    AdvancementInfo info = new AdvancementInfo(adv);
                    if (info.getTitle() != null) {
                        allStrings.add(info.getTitle());
                    }
                }
            }
        }
        return allStrings;
    }

    public void start(){
        timer = true;
    }

    public void pause(){
        timer = !timer;
    }

    public void reset(){
        timer = false;
        timerseconds = 0;
        finishedAdvancementList.clear();
        for(OfflinePlayer player : Bukkit.getOfflinePlayers()){
            if(player.getPlayer() == null){
                resetPlayers.add(player.getUniqueId().toString());
                continue;
            }
            Iterator<Advancement> iterator = Bukkit.getServer().advancementIterator();
            while (iterator.hasNext()){
                try {
                    AdvancementProgress progress = player.getPlayer().getAdvancementProgress(iterator.next());
                    for (String criteria : progress.getAwardedCriteria()) {
                        progress.revokeCriteria(criteria);
                    }
                } catch (Exception e) {
                    getLogger().warning("Error resetting advancement for player: " + player.getName());
                }
            }
        }
    }

    public static AllAchievements getInstance(){
        return instance;
    }

    public List<Advancement> getAdvancementList() {
        return advancementList;
    }

    public List<Advancement> getFinishedAdvancementList() {
        return finishedAdvancementList;
    }

    public String getTime(){
        int hours = timerseconds / 3600;
        int remainder = timerseconds % 3600;
        int minutes = remainder / 60;
        int seconds = remainder % 60;

        String time = String.format("%02d:%02d:%02d", hours, minutes, seconds);

        return "§6" + time;
    }

    public boolean isRunning(){
        return timer;
    }

    public String getVersion(){
        return version;
    }

    public List<String> getResetPlayers(){
        return resetPlayers;
    }
}