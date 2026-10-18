package com.happyuky7.openvoxelmoding.server.core;

import net.openvoxel.server.plugin.Player;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Permission API for OpenVoxel servers (the game has none). Plugins call {@link #has(Player, String)};
 * a provider answers. The built-in provider uses operators ({@code plugins/ModLoader/ops.yml}) and the
 * default nodes from {@code plugins/ModLoader/permissions.yml}; a permission plugin such as LuckPerms
 * replaces it with {@link #setProvider(Provider)}.
 *
 * <p>Commands are protected automatically: running {@code /name} needs the node mapped to it with
 * {@link #registerCommand} or in permissions.yml, else {@code openvoxel.command.<name>}.
 */
public final class Permissions {
    private Permissions() {}

    /** Answers permission checks. Implement this to plug in a permission system. */
    public interface Provider {
        String name();
        /** true/false for a decision, null for "not set" (falls back to defaults/op). */
        Boolean check(Player player, String node);
        default String prefix(Player player) { return ""; }
        default String suffix(Player player) { return ""; }
    }

    private static volatile Provider provider;
    private static final Map<String, String> COMMAND_NODES = new ConcurrentHashMap<>();
    private static final Set<String> DEFAULTS = ConcurrentHashMap.newKeySet();
    private static final Map<String, String> OPS = new ConcurrentHashMap<>();   // lowercase name -> uuid ("" if unknown)
    static Path dir = Paths.get("plugins", "ModLoader");

    // ---- public API ------------------------------------------------------------------

    public static boolean has(Player player, String node) {
        if (player == null) return false;
        if (isConsole(player)) return true;
        Provider p = provider;
        if (p != null) {
            try { Boolean b = p.check(player, node); if (b != null) return b; }
            catch (Throwable error) { System.err.println("[ModLoader] permission provider " + p.name() + ": " + error); }
        }
        if (isOp(player)) return true;
        return matches(DEFAULTS, node);
    }

    public static void setProvider(Provider p) {
        provider = p;
        System.out.println("[ModLoader] permissions provided by " + (p == null ? "ModLoader (ops + permissions.yml)" : p.name()));
    }

    public static Provider getProvider() { return provider; }
    public static String providerName() { return provider == null ? "ModLoader" : provider.name(); }

    public static boolean isOp(Player player) {
        if (player == null) return false;
        if (isConsole(player)) return true;
        String uuid = OPS.get(player.getName().toLowerCase(Locale.ROOT));
        if (uuid != null) return true;
        String id = player.getUuid();
        return id != null && !id.isEmpty() && OPS.containsValue(id);
    }

    public static boolean isConsole(Player player) { return "CONSOLE".equals(player.getName()) && "console".equals(player.getUuid()); }

    /** Maps a command (without slash) to the node needed to run it. */
    public static void registerCommand(String command, String node) { COMMAND_NODES.put(command.toLowerCase(Locale.ROOT), node); }

    /** Node required to run a command. */
    public static String nodeFor(String command) {
        String c = command.toLowerCase(Locale.ROOT);
        return COMMAND_NODES.getOrDefault(c, "openvoxel.command." + c);
    }

    public static String prefix(Player p) { Provider pr = provider; try { return pr == null ? "" : nz(pr.prefix(p)); } catch (Throwable t) { return ""; } }
    public static String suffix(Player p) { Provider pr = provider; try { return pr == null ? "" : nz(pr.suffix(p)); } catch (Throwable t) { return ""; } }

    // ---- ops / defaults --------------------------------------------------------------

    static boolean op(String name, String uuid) {
        boolean added = !OPS.containsKey(name.toLowerCase(Locale.ROOT));
        OPS.put(name.toLowerCase(Locale.ROOT), uuid == null ? "" : uuid);
        saveOps();
        return added;
    }

    static boolean deop(String name) { boolean removed = OPS.remove(name.toLowerCase(Locale.ROOT)) != null; saveOps(); return removed; }

    static Collection<String> ops() { return new TreeSet<>(OPS.keySet()); }

    static void load() {
        Yaml.writeDefault(dir.resolve("permissions.yml"), String.join("\n",
                "# ModLoader permissions (used when no permission plugin such as LuckPerms is installed).",
                "# Operators (/op) have every permission. Everyone else gets the nodes listed in 'default'.",
                "# Nodes support wildcards: 'openvoxel.command.*' or '*'.",
                "default:",
                "  - openvoxel.command.help",
                "  - openvoxel.command.list",
                "  - openvoxel.command.players",
                "  - openvoxel.command.version",
                "  - openvoxel.command.msg",
                "  - openvoxel.command.tell",
                "  - openvoxel.command.spawn",
                "  - openvoxel.command.warp",
                "  - openvoxel.command.warps",
                "  - openvoxel.command.modtest",
                "",
                "# Command -> permission node. Commands not listed need 'openvoxel.command.<name>'.",
                "commands:",
                "  hg: hungergames.use",
                ""));
        Yaml y = Yaml.load(dir.resolve("permissions.yml"));
        DEFAULTS.clear();
        DEFAULTS.addAll(y.list("default"));
        DEFAULTS.add("openvoxel.command.mlhello");
        for (Map.Entry<String, String> e : y.values.entrySet())
            if (e.getKey().startsWith("commands.")) registerCommand(e.getKey().substring(9), e.getValue());
        OPS.clear();
        Yaml ops = Yaml.load(dir.resolve("ops.yml"));
        for (String entry : ops.list("ops")) {
            String[] p = entry.split("\\|", 2);
            if (!p[0].trim().isEmpty()) OPS.put(p[0].trim().toLowerCase(Locale.ROOT), p.length > 1 ? p[1].trim() : "");
        }
    }

    private static void saveOps() {
        StringBuilder sb = new StringBuilder("# Operators: name|uuid. Edit with /op and /deop.\nops:\n");
        for (Map.Entry<String, String> e : new TreeMap<>(OPS).entrySet()) sb.append("  - ").append(Yaml.quote(e.getKey() + (e.getValue().isEmpty() ? "" : "|" + e.getValue()))).append('\n');
        if (OPS.isEmpty()) sb.setLength(sb.length() - 1);
        if (OPS.isEmpty()) sb.append(" []\n");
        try { Files.createDirectories(dir); Files.writeString(dir.resolve("ops.yml"), sb.toString(), StandardCharsets.UTF_8); }
        catch (Exception error) { System.err.println("[ModLoader] ops.yml: " + error); }
    }

    static boolean matches(Collection<String> granted, String node) {
        String n = node.toLowerCase(Locale.ROOT);
        for (String g : granted) {
            String x = g.toLowerCase(Locale.ROOT).trim();
            if (x.equals("*") || x.equals(n)) return true;
            if (x.endsWith(".*") && n.startsWith(x.substring(0, x.length() - 1))) return true;
        }
        return false;
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
