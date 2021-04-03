package net.justminecraft.plots.deny;

import net.justminecraft.plots.Plot;
import net.justminecraft.plots.events.PlotEvent;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Called before a player is denied entry to a plot
 */
public class PlotPlayerDenyEvent extends PlotEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private boolean cancelled = false;

    public PlotPlayerDenyEvent(Plot plot, UUID player) {
        super(plot, player, true);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}