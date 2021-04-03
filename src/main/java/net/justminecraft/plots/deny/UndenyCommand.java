package net.justminecraft.plots.deny;

import net.justminecraft.plots.JustPlots;
import net.justminecraft.plots.Plot;
import net.justminecraft.plots.commands.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class UndenyCommand extends SubCommand {
    public UndenyCommand() {
        super("/p undeny <player>", "Allow a denied player back into your plot", "undeny");
    }

    @Override
    public boolean onCommand(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can execute this command");
            return false;
        }

        Plot plot = JustPlots.getPlotAt((Player) sender);

        if (plot == null) {
            sender.sendMessage(ChatColor.RED + "You are not standing on a plot");
            return false;
        }

        if (!plot.isOwner((Player) sender) && !sender.hasPermission("justplots.deny.other")) {
            sender.sendMessage(ChatColor.RED + JustPlots.getUsername(plot.getOwner()) + " owns that plot");
            return false;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: " + getUsage());
            return false;
        }

        OfflinePlayer toUndeny = Bukkit.getOfflinePlayer(args[0]);

        if (!JustPlotsDeny.isDenied(plot, toUndeny)) {
            sender.sendMessage(ChatColor.RED + toUndeny.getName() + " has not been denied from that plot");
            return false;
        }

        if (plot.isOwner(toUndeny)) {
            sender.sendMessage(ChatColor.RED + toUndeny.getName() + " is the owner of that plot");
            return false;
        }

        PlotPlayerUndenyEvent event = new PlotPlayerUndenyEvent(plot, toUndeny.getUniqueId());

        Bukkit.getServer().getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            throw new RuntimeException("Event was cancelled");
        }

        JustPlotsDeny.undenyPlayer(plot, toUndeny.getUniqueId());

        String whos = plot.isOwner((Player) sender) ? "your" : JustPlots.getUsername(plot.getOwner()) + "'s";

        sender.sendMessage(ChatColor.GREEN + "Succesfully undenied " + toUndeny.getName() + " from " + whos + " plot");

        return true;
    }

    @Override
    public void onTabComplete(CommandSender sender, String[] args, List<String> tabCompletion) {
        if (args.length == 1) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    tabCompletion.add(player.getName());
                }
            }
        }
    }

}