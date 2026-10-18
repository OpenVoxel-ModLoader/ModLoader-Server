package com.happyuky7.openvoxelmoding.server.listener;

import com.happyuky7.openvoxelmoding.server.permissions.Permissions;
import net.openvoxel.server.plugin.event.EventHandler;
import net.openvoxel.server.plugin.event.EventPriority;
import net.openvoxel.server.plugin.event.Listener;
import net.openvoxel.server.plugin.event.player.PlayerCommandPreprocessEvent;

import java.util.Locale;

/** Blocks every command the player has no permission node for ({@link Permissions#nodeFor}). */
public final class CommandGate implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        String msg = e.getMessage();
        if (msg == null || !msg.startsWith("/")) return;
        String name = msg.substring(1).trim().split("\\s+")[0].toLowerCase(Locale.ROOT);
        if (name.isEmpty() || name.equals("mlhello")) return;
        String node = Permissions.nodeFor(name);
        if (!Permissions.has(e.getPlayer(), node)) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("&cYou don't have permission to use /" + name + " &8(" + node + ")");
        }
    }
}
