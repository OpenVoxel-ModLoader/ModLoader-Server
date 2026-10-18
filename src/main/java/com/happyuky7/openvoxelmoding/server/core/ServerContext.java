package com.happyuky7.openvoxelmoding.server.core;

import com.happyuky7.openvoxelmoding.server.config.PluginConfig;
import com.happyuky7.openvoxelmoding.server.config.PropertiesFile;
import com.happyuky7.openvoxelmoding.server.config.ServerSettings;
import com.happyuky7.openvoxelmoding.server.net.ClientChannel;
import com.happyuky7.openvoxelmoding.server.permissions.Permissions;
import com.happyuky7.openvoxelmoding.server.player.GameModes;
import com.happyuky7.openvoxelmoding.server.player.Whitelist;
import com.happyuky7.openvoxelmoding.server.tablist.TabList;
import com.happyuky7.openvoxelmoding.server.world.Warps;
import net.openvoxel.server.plugin.Plugin;

/** The server components, created once by {@link ModLoaderServer} and handed to commands and listeners. */
public final class ServerContext {
    public final Plugin plugin;
    public final ServerSettings settings = new ServerSettings();
    public final PluginConfig config = new PluginConfig();
    public final ClientChannel channel = new ClientChannel();
    public final Placeholders placeholders;
    public final GameModes gameModes = new GameModes(settings);
    private final PropertiesFile warpsFile = new PropertiesFile("warps.properties");
    public final Warps warps = new Warps(warpsFile);
    public final Whitelist whitelist = new Whitelist(warpsFile);
    public final TabList tabList;

    ServerContext(Plugin plugin) {
        this.plugin = plugin;
        this.placeholders = new Placeholders(settings, plugin.getDescription().getVersion());
        this.tabList = new TabList(plugin, channel, placeholders);
    }

    public String version() { return plugin.getDescription().getVersion(); }

    /** Reloads every file: permissions, ops, server.properties.yml, tablist.yml, players and warps. */
    public void reload() {
        Permissions.load();
        settings.load();
        config.load();
        tabList.load();
        gameModes.load();
        warpsFile.load();
    }
}
