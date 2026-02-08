package dev.geco.gmusic.api.event;

import dev.geco.gmusic.GMusicMain;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.server.PluginEvent;
import org.jetbrains.annotations.NotNull;

public class GMusicReloadEvent extends PluginEvent implements Cancellable {

    private final GMusicMain gMusicMain;
    private boolean cancel = false;
    private static final HandlerList handlers = new HandlerList();

    public GMusicReloadEvent(@NotNull GMusicMain gMusicMain) {
        super(gMusicMain);
        this.gMusicMain = gMusicMain;
    }

    @Override
    public @NotNull GMusicMain getPlugin() { return gMusicMain; }

    @Override
    public boolean isCancelled() { return cancel; }

    @Override
    public void setCancelled(boolean cancelled) { cancel = cancelled; }

    @Override
    public @NotNull HandlerList getHandlers() { return handlers; }

    public static @NotNull HandlerList getHandlerList() { return handlers; }

}