package net.justminecraft.plots.deny;

import net.justminecraft.plots.JustPlots;
import net.justminecraft.plots.Plot;
import net.justminecraft.plots.commands.SubCommand;
import net.justminecraft.plots.util.PaperUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.List;

public class DenyCommand extends SubCommand {
    public DenyCommand() {
        super("/p deny <player>", "Deny a player from your plot", "deny", "d");
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

        OfflinePlayer toDeny = Bukkit.getOfflinePlayer(args[0]);

        if (JustPlotsDeny.isDenied(plot, toDeny)) {
            sender.sendMessage(ChatColor.RED + toDeny.getName() + " is already denied to that plot");
            return false;
        }

        PlotPlayerDenyEvent event = new PlotPlayerDenyEvent(plot, toDeny.getUniqueId());

        Bukkit.getServer().getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            throw new RuntimeException("Event was cancelled");
        }

        JustPlotsDeny.denyPlayer(plot, toDeny.getUniqueId());
        
        for (Player player : plot.getPlayersInPlot()) {
            if (player.equals(toDeny)) {
                player.sendMessage(ChatColor.RED + "You have been denied from that plot!");
                PaperUtil.teleportAsync(player, plot.getHome(), PlayerTeleportEvent.TeleportCause.PLUGIN);
            }
        }

        String whos = plot.isOwner((Player) sender) ? "your" : JustPlots.getUsername(plot.getOwner()) + "'s";

        sender.sendMessage(ChatColor.GREEN + "Succesfully denied " + toDeny.getName() + " from " + whos + " plot");

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