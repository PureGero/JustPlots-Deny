package net.justminecraft.plots.deny;

import net.justminecraft.plots.JustPlots;
import net.justminecraft.plots.Plot;
import net.justminecraft.plots.PlotInfoEntry;
import net.justminecraft.plots.bstats.bukkit.Metrics;
import net.justminecraft.plots.events.PlotEnterEvent;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class JustPlotsDeny extends JavaPlugin implements Listener {

    private static HashMap<String, HashSet<UUID>> deniedPlayers = new HashMap<>();

    @NotNull
    private static HashSet<UUID> getDeniedPlayers(String plotId) {
        return deniedPlayers.computeIfAbsent(plotId, key -> new HashSet<>());
    }
    
    public static boolean isDenied(Plot plot, OfflinePlayer player) {
        return isDenied(plot.toString(), player.getUniqueId());
    }

    public static boolean isDenied(String plotId, UUID uuid) {
        HashSet<UUID> denied = deniedPlayers.get(plotId);
        return denied != null && denied.contains(uuid);
    }

    public static void denyPlayer(Plot plot, UUID uuid) {
        try (PreparedStatement statement = JustPlots.getDatabase().prepareStatement(
                "INSERT OR IGNORE INTO justplots_denied (world, x, z, uuid) VALUES (?, ?, ?, ?)"
        )) {
            statement.setString(1, plot.getWorldName());
            statement.setInt(2, plot.getId().getX());
            statement.setInt(3, plot.getId().getZ());
            statement.setString(4, uuid.toString());
            statement.executeUpdate();

            getDeniedPlayers(plot.toString()).add(uuid);
        } catch (SQLException e) {
            throw  new RuntimeException(e);
        }
    }

    public static void undenyPlayer(Plot plot, UUID uuid) {
        try (PreparedStatement statement = JustPlots.getDatabase().prepareStatement(
                "DELETE FROM justplots_denied WHERE world = ? AND x = ? AND z = ? AND uuid = ?"
        )) {
            statement.setString(1, plot.getWorldName());
            statement.setInt(2, plot.getId().getX());
            statement.setInt(3, plot.getId().getZ());
            statement.setString(4, uuid.toString());
            statement.executeUpdate();

            getDeniedPlayers(plot.toString()).remove(uuid);
        } catch (SQLException e) {
            throw  new RuntimeException(e);
        }
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        
        getServer().getScheduler().runTaskAsynchronously(this, this::loadDenied);

        JustPlots.getCommandExecuter().addCommand(new DenyCommand());
        JustPlots.getCommandExecuter().addCommand(new UndenyCommand());

        new Metrics(this, 10955);
        
        new PlotInfoEntry("Denied players") {
            @Override
            public @Nullable BaseComponent[] getValue(@NotNull Plot plot) {
                ComponentBuilder builder = new ComponentBuilder();

                Set<UUID> deniedUuids = deniedPlayers.get(plot.toString());

                if (deniedUuids == null || deniedUuids.isEmpty()) {
                    return builder.append("No one").color(ChatColor.GRAY).create();
                }

                boolean first = true;
                for (UUID uuid : deniedUuids) {
                    if (!first) {
                        builder.append(", ");
                    }

                    builder.append(JustPlots.getUsername(uuid));

                    first = false;
                }

                return builder.create();
            }
        };
    }

    private void loadDenied() {
        int counter = 0;
        
        createTable();

        try (PreparedStatement statement = JustPlots.getDatabase().prepareStatement("SELECT * FROM justplots_denied")) {
            ResultSet results = statement.executeQuery();

            while (results.next()) {
                String world = results.getString("world");
                int x = results.getInt("x");
                int z = results.getInt("z");
                String owner = results.getString("uuid");
                
                try {
                    getDeniedPlayers(world + ";" + x + ";" + z).add(UUID.fromString(owner));
                } catch (Exception e) {
                    getLogger().warning("Could not load denied player for plot " + world + ";" + x + ";" + z);
                    e.printStackTrace();
                }

                if (++counter % 10000 == 0) {
                    getLogger().info("Loading denied players... (" + counter + ")");
                }
            }
        } catch (SQLException e) {
            getLogger().severe("FAILED TO LOAD DENIED PLAYERS");
            e.printStackTrace();
        }
    }
    
    private void createTable() {
        try (PreparedStatement statement = JustPlots.getDatabase().prepareStatement(
                "CREATE TABLE IF NOT EXISTS justplots_denied ("
                        + "world VARCHAR(45) NOT NULL,"
                        + "x INT NOT NULL,"
                        + "z INT NOT NULL,"
                        + "uuid CHAR(36) NOT NULL,"
                        + "UNIQUE (world, x, z, uuid))")) {
            statement.execute();
        } catch (SQLException e) {
            getLogger().severe("FAILED TO CREATE DENIED PLAYERS DATABASE TABLE");
            e.printStackTrace();
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlotEnter(PlotEnterEvent event) {
        if (isDenied(event.getPlot(), event.getPlayer()) && !event.getPlayer().hasPermission("justplots.deny.bypass")) {
            event.setCancelled(true);

            // Send the player a message if the event is still cancelled
            Bukkit.getScheduler().runTask(this, () -> {
                if (event.isCancelled()) {
                    event.getPlayer().spigot().sendMessage(
                            ChatMessageType.ACTION_BAR,
                            new ComponentBuilder("You have been denied from that plot").color(ChatColor.RED).create()
                    );
                }
            });
        }
    }
}
