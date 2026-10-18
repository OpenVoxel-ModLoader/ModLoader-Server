package com.happyuky7.openvoxelmoding.server.player;

import com.happyuky7.openvoxelmoding.server.config.PropertiesFile;
import com.happyuky7.openvoxelmoding.server.config.ServerSettings;
import net.openvoxel.server.plugin.Player;

import java.util.Locale;

/** Per-player game mode for ModLoader clients, stored in {@code players.properties} (key: uuid, or lowercase name). */
public final class GameModes {
    private final PropertiesFile file = new PropertiesFile("players.properties");
    private final ServerSettings settings;

    public GameModes(ServerSettings settings) { this.settings = settings; }

    public void load() { file.load(); }

    public String of(Player p) {
        String m = file.get(key(p));
        return m != null ? m : settings.defaultGameMode();
    }

    public void set(Player p, String mode) { file.set(key(p), mode); file.save(); }

    /** "creative"/"survival" from user input (c, creative, 1, s, survival, 0), or null. */
    public static String parse(String input) {
        String m = input.toLowerCase(Locale.ROOT);
        return m.startsWith("c") || m.equals("1") ? "creative" : m.startsWith("s") || m.equals("0") ? "survival" : null;
    }

    static String key(Player p) { String u = p.getUuid(); return u != null && !u.isEmpty() ? u : p.getName().toLowerCase(Locale.ROOT); }
}
