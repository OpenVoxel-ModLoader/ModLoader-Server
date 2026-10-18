package com.happyuky7.openvoxelmoding.server.core;

import com.happyuky7.openvoxelmoding.server.config.ServerSettings;
import com.happyuky7.openvoxelmoding.server.permissions.Permissions;
import net.openvoxel.server.plugin.Player;
import net.openvoxel.server.plugin.VoxelServer;

/** Text placeholders: {online} {max} {player} {prefix} {suffix} {server} {version} {provider}. */
public final class Placeholders {
    private final ServerSettings settings;
    private final String version;

    public Placeholders(ServerSettings settings, String version) { this.settings = settings; this.version = version; }

    public String apply(String s, Player p) {
        int max = settings.maxPlayers();
        return s.replace("{online}", String.valueOf(VoxelServer.getOnlinePlayers().size()))
                .replace("{max}", max > 0 ? String.valueOf(max) : "-")
                .replace("{player}", p.getName())
                .replace("{prefix}", Permissions.prefix(p))
                .replace("{suffix}", Permissions.suffix(p))
                .replace("{server}", settings.serverName())
                .replace("{version}", version)
                .replace("{provider}", Permissions.providerName());
    }
}
