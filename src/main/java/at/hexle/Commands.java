package at.hexle;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class Commands implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
        } else if (args[0].equalsIgnoreCase("start")){
            AllAchievements.start();
        } else if (args[0].equalsIgnoreCase("stop")){
            AllAchievements.stop();
        } else if (args[0].equalsIgnoreCase("pause")){
            AllAchievements.pause();
        } else if (args[0].equalsIgnoreCase("reset")) {
            AllAchievements.reset();
        } else if (args[0].equalsIgnoreCase("finished")){
            List<String> finished = AllAchievements.getFinishedAchievements();
            if (finished.size() == 0) {
                sender.sendMessage("§cNo achievements finished yet.");
            } else {
                sender.sendMessage("§6The following achievements are finished:");
                for (String a : finished) {
                    sender.sendMessage("§a" + a);
                }
            }
        } else {
            sendHelp(sender);
        }
        return true;
    }

    public static void sendHelp(CommandSender sender){
        sender.sendMessage("§6AllAchievements §7- §eCommands");
        sender.sendMessage("§6/av start §7- §eStarts the achievementhunt");
        sender.sendMessage("§6/av stop §7- §eStops the achievementhunt");
        sender.sendMessage("§6/av pause §7- §ePauses the achievementhunt");
        sender.sendMessage("§6/av reset §7- §eResets the achievementhunt");
        sender.sendMessage("§6/av finished §7- §eShows all finished achievements");
    }

}
