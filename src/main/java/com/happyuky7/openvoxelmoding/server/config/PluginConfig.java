package com.happyuky7.openvoxelmoding.server.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/** {@code plugins/ModLoaderServer/config.yml}: test/diagnostic options (welcome toggle, /modtest, /platform, world features). */
public final class PluginConfig {
    private final Map<String, String> values = new HashMap<>();

    public void load() {
        values.clear();
        values.put("welcome.enabled", "true"); values.put("welcome.message", "&aModLoader server active. Use &e/modtest&a.");
        values.put("messages.test", "&b[ModLoader] Server mod system is working."); values.put("platform.radius", "2"); values.put("platform.block", "stone");
        values.put("world.features.enabled", "true"); values.put("world.features.seed-salt", "7351");
        Path path = Paths.get("plugins", "ModLoaderServer", "config.yml");
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) Files.writeString(path, "welcome.enabled: true\nwelcome.message: '&aModLoader server active. Use &e/modtest&a.'\nmessages.test: '&b[ModLoader] Server mod system is working.'\nplatform.radius: 2\nplatform.block: stone\nworld.features.enabled: true\nworld.features.seed-salt: 7351\n", StandardCharsets.UTF_8);
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String s = line.trim();
                if (s.isEmpty() || s.startsWith("#") || !s.contains(":")) continue;
                int at = s.indexOf(':');
                String value = s.substring(at + 1).trim();
                if ((value.startsWith("'") && value.endsWith("'")) || (value.startsWith("\"") && value.endsWith("\""))) value = value.substring(1, value.length() - 1);
                values.put(s.substring(0, at).trim(), value);
            }
        } catch (Exception error) { System.err.println("[ModLoaderServer] config error: " + error); }
    }

    public String get(String key) { return values.get(key); }
    public Map<String, String> asMap() { return values; }
}
