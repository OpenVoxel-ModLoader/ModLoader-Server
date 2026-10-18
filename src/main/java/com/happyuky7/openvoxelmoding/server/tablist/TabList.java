package com.happyuky7.openvoxelmoding.server.tablist;

import com.happyuky7.openvoxelmoding.server.config.Yaml;
import com.happyuky7.openvoxelmoding.server.core.Placeholders;
import com.happyuky7.openvoxelmoding.server.net.ClientChannel;
import com.happyuky7.openvoxelmoding.server.permissions.Permissions;
import net.openvoxel.server.plugin.Player;
import net.openvoxel.server.plugin.Plugin;
import net.openvoxel.server.plugin.VoxelServer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Tab list for ModLoader clients (hold Tab), configured in {@code tablist.yml}. Header and footer are a YAML
 * list (one line each) or one text with {@code \n}; lines travel separated by {@code \u0002}.
 */
public final class TabList {
    private final Plugin plugin;
    private final ClientChannel channel;
    private final Placeholders placeholders;
    private Yaml config = new Yaml();
    private int task = -1;

    public TabList(Plugin plugin, ClientChannel channel, Placeholders placeholders) {
        this.plugin = plugin; this.channel = channel; this.placeholders = placeholders;
    }

    public void load() {
        Yaml.writeDefault(Permissions.dataFolder().resolve("tablist.yml"), String.join("\n",
                "# Tab list for players using the ModLoader client (hold Tab).",
                "# Placeholders: {online} {max} {player} {prefix} {suffix} {server} {version} {provider}",
                "enabled: true",
                "refresh-seconds: 2",
                "# header and footer: a list (one line each) or a single text where \\n starts a new line.",
                "header:",
                "  - '&b&lOpenVoxel &f&lServer'",
                "  - '&7Welcome, &e{player}'",
                "  - ''",
                "footer:",
                "  - ''",
                "  - '&7Online: &a{online}&7/&a{max}'",
                "  - '&8ModLoader {version} | permissions: {provider}'",
                "# How each player is shown. {prefix}/{suffix} come from the permission plugin (LuckPerms meta).",
                "name-format: '{prefix}{player}{suffix}'",
                ""));
        config = Yaml.load(Permissions.dataFolder().resolve("tablist.yml"));
    }

    /** (Re)starts the periodic refresh with the configured interval. */
    public void start() {
        if (task >= 0) VoxelServer.getScheduler().cancel(task);
        long period = Math.max(1, config.getInt("refresh-seconds", 2)) * 20L;
        task = VoxelServer.getScheduler().runTimer(plugin, () -> { for (Player p : VoxelServer.getOnlinePlayers()) if (channel.isModded(p)) send(p); }, 40L, period);
    }

    public void send(Player viewer) {
        if (!config.getBool("enabled", true)) return;
        List<String> shown = new ArrayList<>(), raw = new ArrayList<>();
        List<Player> online = new ArrayList<>(VoxelServer.getOnlinePlayers());
        online.sort(Comparator.comparing(p -> p.getName().toLowerCase(Locale.ROOT)));
        for (Player p : online) { raw.add(p.getName()); shown.add(placeholders.apply(config.get("name-format", "{prefix}{player}{suffix}"), p).replace(",", " ")); }
        String header = placeholders.apply(multiline("header"), viewer).replace("\t", " ").replace("\n", "\u0002");
        String footer = placeholders.apply(multiline("footer"), viewer).replace("\t", " ").replace("\n", "\u0002");
        channel.send(viewer, "tab " + header + "\t" + footer + "\t" + String.join(",", shown) + "\t" + String.join(",", raw));
    }

    private String multiline(String key) {
        List<String> list = config.list(key);
        if (!list.isEmpty()) return String.join("\n", list);
        return config.get(key, "").replace("\\n", "\n");
    }
}
