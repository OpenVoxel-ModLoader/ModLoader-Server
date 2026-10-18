package com.happyuky7.openvoxelmoding.server.core;

import net.openvoxel.server.plugin.*;
import net.openvoxel.server.plugin.event.EventHandler;
import net.openvoxel.server.plugin.event.EventPriority;
import net.openvoxel.server.plugin.event.Listener;
import net.openvoxel.server.plugin.event.player.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server administration on top of the official plugin API: operators, permission-checked commands,
 * per-player game mode for ModLoader clients, tab list (tablist.yml), whitelist / max players and
 * world spawn + warps. Talks to ModLoader clients through control chat lines ({@link #PREFIX}).
 */
final class Admin implements Listener {
    static final String PREFIX = "\u0001ML:";
    private final Plugin plugin;
    private final Set<String> modded = ConcurrentHashMap.newKeySet();           // names of players running the ModLoader client
    private final Properties modes = new Properties();
    private final Properties warps = new Properties();
    private Yaml server = new Yaml(), tablist = new Yaml();
    private int tabTask = -1;

    Admin(Plugin plugin) { this.plugin = plugin; }

    private Path dir() { return Permissions.dir; }

    void enable() {
        reload();
        VoxelServer.getPluginManager().registerEvents(this, plugin);
        CommandManager c = VoxelServer.getCommandManager();
        c.register("op", "<player> - give operator", (p, a) -> op(p, a, true), plugin);
        c.register("deop", "<player> - remove operator", (p, a) -> op(p, a, false), plugin);
        c.register("ops", "list operators", (p, a) -> p.sendMessage("&eOperators (" + Permissions.ops().size() + "): &f" + String.join(", ", Permissions.ops())), plugin);
        c.register("gamemode", "<creative|survival> [player] - change game mode (ModLoader clients)", this::gamemode, plugin);
        c.register("gm", "alias of /gamemode", this::gamemode, plugin);
        c.register("mlhello", "", (p, a) -> hello(p, a.length > 0 ? a[0] : "?"), plugin);
        c.register("whitelist", "<on|off|add|remove|list> [player]", this::whitelist, plugin);
        c.register("msg", "<player> <message> - private message", this::msg, plugin);
        c.register("tell", "alias of /msg", this::msg, plugin);
        c.register("spawn", "teleport to the world spawn", (p, a) -> { Location l = spawn(); if (l != null && !Permissions.isConsole(p)) { p.setLocation(l); p.sendMessage("&aTeleported to spawn."); } }, plugin);
        c.register("setspawn", "set the world spawn here", (p, a) -> { if (Permissions.isConsole(p)) return; Location l = p.getLocation(); warps.setProperty("@spawn", fmt(l)); save(warps, "warps.properties"); p.sendMessage("&aWorld spawn set."); }, plugin);
        c.register("warp", "<name> - teleport to a warp", this::warp, plugin);
        c.register("setwarp", "<name> - create a warp here", this::setwarp, plugin);
        c.register("delwarp", "<name> - delete a warp", (p, a) -> { if (a.length == 0) { p.sendMessage("&cUsage: /delwarp <name>"); return; } p.sendMessage(warps.remove(a[0].toLowerCase(Locale.ROOT)) != null ? "&aWarp deleted." : "&cNo such warp."); save(warps, "warps.properties"); }, plugin);
        c.register("warps", "list warps", (p, a) -> p.sendMessage("&eWarps: &f" + String.join(", ", warpNames())), plugin);
        c.register("perms", "<check|reload> - permission tools", this::perms, plugin);
        c.register("give", "[player] <item> [count] - give items (ModLoader clients)", this::give, plugin);
        Permissions.registerCommand("gm", "openvoxel.command.gamemode");
        Permissions.registerCommand("tell", "openvoxel.command.msg");
        Permissions.registerCommand("list", "openvoxel.command.list");
        startTab();
    }

    void reload() {
        Permissions.load();
        server = Yaml.load(Paths.get("server.properties.yml"));
        Yaml.writeDefault(dir().resolve("tablist.yml"), String.join("\n",
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
        tablist = Yaml.load(dir().resolve("tablist.yml"));
        load(modes, "players.properties");
        load(warps, "warps.properties");
    }

    // ---- permission gate -------------------------------------------------------------

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

    // ---- join / quit -----------------------------------------------------------------

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (server.getBool("gameplay.whitelist", false) && !Permissions.isOp(p) && !whitelisted(p.getName())) {
            e.setCancelled(true); e.setKickReason(server.get("gameplay.whitelist-message", "You are not whitelisted on this server.")); return;
        }
        int max = server.getInt("gameplay.max-players", 0);
        if (max > 0 && VoxelServer.getOnlinePlayers().size() > max && !Permissions.isOp(p)) {
            e.setCancelled(true); e.setKickReason("The server is full (" + max + " players)."); return;
        }
        String motd = server.get("gameplay.motd", "");
        if (!motd.isEmpty()) p.sendMessage(placeholders(motd, p));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) { modded.remove(e.getPlayer().getName()); }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) { Location l = spawn(); if (l != null) e.setRespawnLocation(l); }

    // ---- ModLoader client channel ------------------------------------------------------

    private void hello(Player p, String clientVersion) {
        if (Permissions.isConsole(p)) return;
        modded.add(p.getName());
        send(p, "hello " + plugin.getDescription().getVersion());
        send(p, "cmds " + String.join(",", usableCommands(p)));
        send(p, "gm " + modeOf(p));
        sendTab(p);
        System.out.println("[ModLoader] " + p.getName() + " uses ModLoader client " + clientVersion);
    }

    private static void send(Player p, String line) { p.sendMessage(PREFIX + line); }

    private List<String> usableCommands(Player p) {
        List<String> out = new ArrayList<>();
        for (String d : VoxelServer.getCommandManager().describeAll()) {
            String name = d.substring(1).split(" ", 2)[0];
            if (name.equals("mlhello")) continue;
            if (Permissions.has(p, Permissions.nodeFor(name))) out.add(name);
        }
        Collections.sort(out);
        return out;
    }

    private String modeOf(Player p) {
        String m = modes.getProperty(key(p));
        return m != null ? m : server.get("gameplay.default-gamemode", "survival").toLowerCase(Locale.ROOT).startsWith("c") ? "creative" : "survival";
    }

    private void gamemode(Player sender, String[] a) {
        if (a.length == 0) { sender.sendMessage("&cUsage: /gamemode <creative|survival> [player]"); return; }
        String m = a[0].toLowerCase(Locale.ROOT);
        String mode = m.startsWith("c") || m.equals("1") ? "creative" : m.startsWith("s") || m.equals("0") ? "survival" : null;
        if (mode == null) { sender.sendMessage("&cUnknown game mode: " + a[0]); return; }
        Player target = sender;
        if (a.length > 1) {
            if (!Permissions.has(sender, "openvoxel.command.gamemode.others")) { sender.sendMessage("&cYou can't change other players' game mode &8(openvoxel.command.gamemode.others)"); return; }
            target = VoxelServer.getPlayer(a[1]);
            if (target == null) { sender.sendMessage("&cPlayer not online: " + a[1]); return; }
        } else if (Permissions.isConsole(sender)) { sender.sendMessage("Usage from console: gamemode <mode> <player>"); return; }
        modes.setProperty(key(target), mode);
        save(modes, "players.properties");
        send(target, "gm " + mode);
        if (!modded.contains(target.getName())) target.sendMessage("&7(Game modes need the OpenVoxel ModLoader client.)");
        if (target != sender) sender.sendMessage("&aSet " + target.getName() + "'s game mode to " + mode + ".");
    }

    /** /give [player] <item> [count]: the target's ModLoader client adds the items (or drops what does not fit). */
    private void give(Player sender, String[] a) {
        if (a.length == 0) { sender.sendMessage("&cUsage: /give [player] <item> [count]"); return; }
        Player target = null;
        int i = 0;
        Player named = VoxelServer.getPlayer(a[0]);
        if (named != null && a.length >= 2) { target = named; i = 1; }
        if (target == null) {
            if (Permissions.isConsole(sender)) { sender.sendMessage("Usage from console: give <player> <item> [count]"); return; }
            target = sender;
        }
        if (target != sender && !Permissions.has(sender, "openvoxel.command.give.others")) { sender.sendMessage("&cYou can't give items to other players &8(openvoxel.command.give.others)"); return; }
        if (!modded.contains(target.getName())) { sender.sendMessage("&c" + target.getName() + " is not using the ModLoader client (items are client-side)."); return; }
        String item = a[i];
        int count = 1;
        if (a.length > i + 1) try { count = Math.max(1, Math.min(2304, Integer.parseInt(a[i + 1]))); } catch (NumberFormatException e) { sender.sendMessage("&cNot a number: " + a[i + 1]); return; }
        send(target, "give " + item + " " + count);
        if (target != sender) sender.sendMessage("&aGave " + count + " " + item + " to " + target.getName() + ".");
    }

    // ---- tab list --------------------------------------------------------------------

    private void startTab() {
        if (tabTask >= 0) VoxelServer.getScheduler().cancel(tabTask);
        long period = Math.max(1, tablist.getInt("refresh-seconds", 2)) * 20L;
        tabTask = VoxelServer.getScheduler().runTimer(plugin, () -> { for (Player p : VoxelServer.getOnlinePlayers()) if (modded.contains(p.getName())) sendTab(p); }, 40L, period);
    }

    private void sendTab(Player viewer) {
        if (!tablist.getBool("enabled", true)) return;
        List<String> shown = new ArrayList<>(), raw = new ArrayList<>();
        List<Player> online = new ArrayList<>(VoxelServer.getOnlinePlayers());
        online.sort(Comparator.comparing(p -> p.getName().toLowerCase(Locale.ROOT)));
        for (Player p : online) { raw.add(p.getName()); shown.add(placeholders(tablist.get("name-format", "{prefix}{player}{suffix}"), p).replace(",", " ")); }
        String header = placeholders(multiline("header"), viewer).replace("\t", " ").replace("\n", "\u0002");
        String footer = placeholders(multiline("footer"), viewer).replace("\t", " ").replace("\n", "\u0002");
        send(viewer, "tab " + header + "\t" + footer + "\t" + String.join(",", shown) + "\t" + String.join(",", raw));
    }

    /** header/footer may be a YAML list (one line per item) or a string with \\n line breaks. */
    private String multiline(String key) {
        List<String> list = tablist.list(key);
        if (!list.isEmpty()) return String.join("\n", list);
        return tablist.get(key, "").replace("\\n", "\n");
    }

    private String placeholders(String s, Player p) {
        int max = server.getInt("gameplay.max-players", 0);
        return s.replace("{online}", String.valueOf(VoxelServer.getOnlinePlayers().size()))
                .replace("{max}", max > 0 ? String.valueOf(max) : "-")
                .replace("{player}", p.getName())
                .replace("{prefix}", Permissions.prefix(p))
                .replace("{suffix}", Permissions.suffix(p))
                .replace("{server}", server.get("gameplay.server-name", "OpenVoxel"))
                .replace("{version}", plugin.getDescription().getVersion())
                .replace("{provider}", Permissions.providerName());
    }

    // ---- other commands ----------------------------------------------------------------

    private void op(Player sender, String[] a, boolean give) {
        if (a.length == 0) { sender.sendMessage("&cUsage: /" + (give ? "op" : "deop") + " <player>"); return; }
        Player target = VoxelServer.getPlayer(a[0]);
        String name = target != null ? target.getName() : a[0];
        if (give) {
            Permissions.op(name, target != null ? target.getUuid() : "");
            sender.sendMessage("&aMade " + name + " a server operator.");
            if (target != null && target != sender) target.sendMessage("&eYou are now a server operator.");
        } else {
            sender.sendMessage(Permissions.deop(name) ? "&aRemoved operator: " + name : "&c" + name + " is not an operator.");
            if (target != null && target != sender) target.sendMessage("&eYou are no longer a server operator.");
        }
        if (target != null && modded.contains(target.getName())) send(target, "cmds " + String.join(",", usableCommands(target)));
    }

    private void whitelist(Player p, String[] a) {
        String sub = a.length > 0 ? a[0].toLowerCase(Locale.ROOT) : "list";
        Set<String> list = new TreeSet<>(Arrays.asList(warps.getProperty("@whitelist", "").split(",")));
        list.remove("");
        switch (sub) {
            case "add": if (a.length > 1) { list.add(a[1].toLowerCase(Locale.ROOT)); p.sendMessage("&aAdded " + a[1] + " to the whitelist."); } break;
            case "remove": if (a.length > 1) { list.remove(a[1].toLowerCase(Locale.ROOT)); p.sendMessage("&aRemoved " + a[1] + " from the whitelist."); } break;
            case "on": case "off": p.sendMessage("&eSet gameplay.whitelist: " + sub.equals("on") + " in server.properties.yml and restart (or /perms reload)."); return;
            default: p.sendMessage("&eWhitelist (" + (server.getBool("gameplay.whitelist", false) ? "on" : "off") + "): &f" + String.join(", ", list)); return;
        }
        warps.setProperty("@whitelist", String.join(",", list));
        save(warps, "warps.properties");
    }

    private boolean whitelisted(String name) { return Arrays.asList(warps.getProperty("@whitelist", "").split(",")).contains(name.toLowerCase(Locale.ROOT)); }

    private void msg(Player p, String[] a) {
        if (a.length < 2) { p.sendMessage("&cUsage: /msg <player> <message>"); return; }
        Player t = VoxelServer.getPlayer(a[0]);
        if (t == null) { p.sendMessage("&cPlayer not online: " + a[0]); return; }
        String text = String.join(" ", Arrays.copyOfRange(a, 1, a.length));
        p.sendMessage("&7[me -> " + t.getName() + "] &f" + text);
        t.sendMessage("&7[" + p.getName() + " -> me] &f" + text);
    }

    private void warp(Player p, String[] a) {
        if (a.length == 0) { p.sendMessage("&eWarps: &f" + String.join(", ", warpNames())); return; }
        Location l = parse(warps.getProperty(a[0].toLowerCase(Locale.ROOT)));
        if (l == null) { p.sendMessage("&cNo such warp: " + a[0]); return; }
        if (Permissions.isConsole(p)) return;
        if (!Permissions.has(p, "openvoxel.warp." + a[0].toLowerCase(Locale.ROOT)) && !Permissions.has(p, "openvoxel.warp.*") && !Permissions.has(p, "openvoxel.command.warp")) { p.sendMessage("&cYou can't use that warp."); return; }
        p.setLocation(l);
        p.sendMessage("&aWarped to " + a[0] + ".");
    }

    private void setwarp(Player p, String[] a) {
        if (a.length == 0 || Permissions.isConsole(p)) { p.sendMessage("&cUsage: /setwarp <name>"); return; }
        String name = a[0].toLowerCase(Locale.ROOT);
        if (!name.matches("[a-z0-9_-]{1,32}")) { p.sendMessage("&cWarp names: a-z, 0-9, _ and -"); return; }
        warps.setProperty(name, fmt(p.getLocation()));
        save(warps, "warps.properties");
        p.sendMessage("&aWarp " + name + " set.");
    }

    private void perms(Player p, String[] a) {
        String sub = a.length > 0 ? a[0].toLowerCase(Locale.ROOT) : "";
        if (sub.equals("reload")) { reload(); startTab(); p.sendMessage("&aReloaded permissions, tablist and server settings."); return; }
        if (sub.equals("check") && a.length >= 3) {
            Player t = VoxelServer.getPlayer(a[1]);
            if (t == null) { p.sendMessage("&cPlayer not online: " + a[1]); return; }
            p.sendMessage("&e" + t.getName() + " &7" + a[2] + " = " + (Permissions.has(t, a[2]) ? "&atrue" : "&cfalse") + " &8(provider: " + Permissions.providerName() + ", op: " + Permissions.isOp(t) + ")");
            return;
        }
        p.sendMessage("&eProvider: &f" + Permissions.providerName() + "&e. Usage: /perms check <player> <node> | /perms reload");
    }

    private List<String> warpNames() { List<String> n = new ArrayList<>(); for (String k : warps.stringPropertyNames()) if (!k.startsWith("@")) n.add(k); Collections.sort(n); return n; }

    private Location spawn() { return parse(warps.getProperty("@spawn")); }

    private static String fmt(Location l) { return l.getX() + "," + l.getY() + "," + l.getZ() + "," + l.getYaw() + "," + l.getPitch(); }

    private static Location parse(String s) {
        if (s == null) return null;
        try { String[] p = s.split(","); return new Location(Double.parseDouble(p[0]), Double.parseDouble(p[1]), Double.parseDouble(p[2]), p.length > 3 ? Float.parseFloat(p[3]) : 0f, p.length > 4 ? Float.parseFloat(p[4]) : 0f); }
        catch (Exception e) { return null; }
    }

    private static String key(Player p) { String u = p.getUuid(); return u != null && !u.isEmpty() ? u : p.getName().toLowerCase(Locale.ROOT); }

    private void load(Properties props, String file) {
        props.clear();
        Path f = dir().resolve(file);
        if (!Files.isRegularFile(f)) return;
        try (InputStream in = Files.newInputStream(f)) { props.load(in); } catch (Exception error) { System.err.println("[ModLoader] " + file + ": " + error); }
    }

    private void save(Properties props, String file) {
        try { Files.createDirectories(dir()); try (OutputStream out = Files.newOutputStream(dir().resolve(file))) { props.store(out, "OpenVoxel ModLoader"); } }
        catch (Exception error) { System.err.println("[ModLoader] " + file + ": " + error); }
    }
}
