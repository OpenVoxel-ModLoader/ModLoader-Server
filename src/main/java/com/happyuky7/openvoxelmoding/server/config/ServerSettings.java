package com.happyuky7.openvoxelmoding.server.config;

import java.nio.file.Paths;
import java.util.Locale;

/**
 * The {@code gameplay} section of {@code server.properties.yml}.
 *
 * <ul>
 *   <li>{@code motd}: text shown under the server name in the multiplayer server list (ModLoader clients,
 *       needs the server started with the ModLoader agent). Lines split with {@code \n}.</li>
 *   <li>{@code welcome-message}: chat message sent when a player joins (the old {@code motd} key is still
 *       read here if {@code welcome-message} is missing, so old configs keep working).</li>
 * </ul>
 */
public final class ServerSettings {
    /** System properties read by the ModLoader agent when it answers server-list pings. */
    public static final String MOTD_PROPERTY = "openvoxel.modloader.motd", MAX_PLAYERS_PROPERTY = "openvoxel.modloader.maxPlayers";

    private Yaml yaml = new Yaml();

    public void load() {
        yaml = Yaml.load(Paths.get("server.properties.yml"));
        System.setProperty(MOTD_PROPERTY, motd());
        System.setProperty(MAX_PLAYERS_PROPERTY, String.valueOf(maxPlayers()));
    }

    public String serverName() { return yaml.get("gameplay.server-name", "OpenVoxel"); }

    /** Server list text; empty when not configured (or when the old {@code motd} key still holds the welcome message). */
    public String motd() {
        if (!yaml.has("gameplay.welcome-message")) return "";              // old file: "motd" was the join message
        return yaml.get("gameplay.motd", "").replace("\\n", "\n");
    }

    public String welcomeMessage() {
        return yaml.has("gameplay.welcome-message") ? yaml.get("gameplay.welcome-message", "") : yaml.get("gameplay.motd", "");
    }

    public String defaultGameMode() { return yaml.get("gameplay.default-gamemode", "survival").toLowerCase(Locale.ROOT).startsWith("c") ? "creative" : "survival"; }
    public int maxPlayers() { return yaml.getInt("gameplay.max-players", 0); }
    public boolean whitelist() { return yaml.getBool("gameplay.whitelist", false); }
    public String whitelistMessage() { return yaml.get("gameplay.whitelist-message", "You are not whitelisted on this server."); }
}
