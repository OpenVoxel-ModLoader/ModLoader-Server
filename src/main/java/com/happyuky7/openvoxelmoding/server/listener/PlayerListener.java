package com.happyuky7.openvoxelmoding.server.listener;

import com.happyuky7.openvoxelmoding.server.core.ServerContext;
import com.happyuky7.openvoxelmoding.server.permissions.Permissions;
import net.openvoxel.server.plugin.Location;
import net.openvoxel.server.plugin.Player;
import net.openvoxel.server.plugin.VoxelServer;
import net.openvoxel.server.plugin.event.EventHandler;
import net.openvoxel.server.plugin.event.EventPriority;
import net.openvoxel.server.plugin.event.Listener;
import net.openvoxel.server.plugin.event.player.PlayerJoinEvent;
import net.openvoxel.server.plugin.event.player.PlayerQuitEvent;
import net.openvoxel.server.plugin.event.player.PlayerRespawnEvent;

/** Join checks (whitelist, max players), welcome messages, respawn at the world spawn. */
public final class PlayerListener implements Listener {
    private final ServerContext ctx;

    public PlayerListener(ServerContext ctx) { this.ctx = ctx; }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (ctx.settings.whitelist() && !Permissions.isOp(p) && !ctx.whitelist.contains(p.getName())) {
            e.setCancelled(true); e.setKickReason(ctx.settings.whitelistMessage()); return;
        }
        int max = ctx.settings.maxPlayers();
        if (max > 0 && VoxelServer.getOnlinePlayers().size() > max && !Permissions.isOp(p)) {
            e.setCancelled(true); e.setKickReason("The server is full (" + max + " players)."); return;
        }
        String welcome = ctx.settings.welcomeMessage();
        if (!welcome.isEmpty()) p.sendMessage(ctx.placeholders.apply(welcome, p));
        if (Boolean.parseBoolean(ctx.config.get("welcome.enabled"))) p.sendMessage(ctx.config.get("welcome.message"));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) { ctx.channel.forget(e.getPlayer()); }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) { Location l = ctx.warps.spawn(); if (l != null) e.setRespawnLocation(l); }
}
