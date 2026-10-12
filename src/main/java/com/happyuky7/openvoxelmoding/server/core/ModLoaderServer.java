package com.happyuky7.openvoxelmoding.server.core;

import net.openvoxel.server.plugin.*;
import net.openvoxel.server.plugin.event.*;
import net.openvoxel.server.plugin.event.player.PlayerJoinEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** ModLoaderServer: diagnostics commands, welcome message and an interactive server console. */
public final class ModLoaderServer extends Plugin implements Listener {
    private final Map<String,String> config = new HashMap<>();
    private Thread console;

    @Override public void onEnable() {
        loadConfig();
        VoxelServer.getPluginManager().registerEvents(this, this);
        CommandManager commands = VoxelServer.getCommandManager();
        commands.register("modtest", "Checks the modded server", (player, args) -> {
            player.sendMessage(config.get("messages.test"));
            player.sendMessage("Try /platform to test world editing.");
        }, this);
        commands.register("modreload", "Reloads ModLoaderServer config", (player, args) -> {
            loadConfig(); player.sendMessage("[ModLoader] Configuration reloaded.");
        }, this);
        commands.register("help", "Lists available commands", (player, args) -> {
            player.sendMessage("&e--- OpenVoxel Server Commands ---");
            for(String command : commands.describeAll()) player.sendMessage("&a" + command.replaceFirst(" - ", " &7- "));
        }, this);
        commands.register("version", "Shows server and mod versions", (player, args) -> {
            player.sendMessage("OpenVoxel Server 0.0.8-SNAPSHOT");
            player.sendMessage("OpenVoxel ModLoader Server 1.0.0 | API 0.0.8");
        }, this);
        commands.register("players", "Lists online players", (player, args) -> {
            StringJoiner names=new StringJoiner(", ");
            for(Player online:VoxelServer.getOnlinePlayers()) names.add(online.getName());
            player.sendMessage("Online ("+VoxelServer.getOnlinePlayers().size()+"): "+names);
        }, this);
        commands.register("list", "Alias of /players", (player, args) -> commands.dispatch(player, "/players"), this);
        commands.register("heal", "Restores your health", (player, args) -> {
            if(player.getEntity()!=null) player.getEntity().health=player.getEntity().maxHealth;
            player.sendMessage("You have been healed.");
        }, this);
        commands.register("say", "<message> - broadcast a message", (player, args) -> {
            if (args.length == 0) { player.sendMessage("Usage: /say <message>"); return; }
            VoxelServer.broadcast("&d[" + player.getName() + "] &f" + String.join(" ", args));
        }, this);
        commands.register("kick", "<player> [reason] - kick a player (console only)", (player, args) -> {
            if (!player.getName().equals("CONSOLE")) { player.sendMessage("Console only."); return; }
            if (args.length == 0) { player.sendMessage("Usage: /kick <player> [reason]"); return; }
            Player target = VoxelServer.getPlayer(args[0]);
            if (target == null) { player.sendMessage("No such player: " + args[0]); return; }
            VoxelServer.kickPlayer(target, args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "Kicked by console");
        }, this);
        commands.register("stop", "Stops the server (console only)", (player, args) -> {
            if (!player.getName().equals("CONSOLE")) { player.sendMessage("Console only."); return; }
            VoxelServer.broadcast("&cServer is stopping.");
            System.out.println("[console] stopping server");
            new Thread(() -> { try { Thread.sleep(300); } catch (InterruptedException ignored) {} System.exit(0); }, "stop").start();
        }, this);
        commands.register("platform", "Creates a test platform", (player, args) -> {
            Location l = player.getLocation();
            int x=l.getBlockX(), y=l.getBlockY()-1, z=l.getBlockZ();
            int radius=Math.max(1,Math.min(16,Integer.parseInt(config.get("platform.radius"))));
            long stone=VoxelServer.blockState(config.get("platform.block"));
            for(int dx=-radius;dx<=radius;dx++) for(int dz=-radius;dz<=radius;dz++) VoxelServer.setBlock(x+dx,y,z+dz,stone);
            player.sendMessage("[ModLoader] Test platform created.");
        }, this);
        startConsole();
    }

    /** Reads commands from stdin so the server window works like a console (with or without leading '/'). */
    private void startConsole() {
        if (Boolean.parseBoolean(System.getProperty("happy.noconsole", "false"))) return;
        Player consolePlayer = new Player("CONSOLE", "console", "127.0.0.1");
        consolePlayer.messageSink = message -> System.out.println("[console] " + message.replaceAll("(?i)[&§]([0-9a-fk-or]|#[0-9a-f]{6})", ""));
        console = new Thread(() -> {
            System.out.println("[console] ready - type help, say <msg>, players, kick <name>, stop");
            try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = in.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    if (!line.startsWith("/")) line = "/" + line;
                    VoxelServer.getCommandManager().dispatch(consolePlayer, line);
                }
            } catch (Exception error) { System.out.println("[console] closed: " + error.getMessage()); }
        }, "ModLoader-console");
        console.setDaemon(true);
        console.start();
    }

    @EventHandler public void onJoin(PlayerJoinEvent event) {
        if(Boolean.parseBoolean(config.get("welcome.enabled"))) event.getPlayer().sendMessage(config.get("welcome.message"));
    }

    private void loadConfig() {
        config.clear(); config.put("welcome.enabled","true"); config.put("welcome.message","&aModLoader server active. Use &e/modtest&a.");
        config.put("messages.test","&b[ModLoader] Server mod system is working."); config.put("platform.radius","2"); config.put("platform.block","stone");
        Path path=Paths.get("plugins","ModLoaderServer","config.yml");
        try {
            Files.createDirectories(path.getParent());
            if(!Files.exists(path)) Files.writeString(path,"welcome.enabled: true\nwelcome.message: '&aModLoader server active. Use &e/modtest&a.'\nmessages.test: '&b[ModLoader] Server mod system is working.'\nplatform.radius: 2\nplatform.block: stone\n",StandardCharsets.UTF_8);
            for(String line:Files.readAllLines(path,StandardCharsets.UTF_8)){String s=line.trim();if(s.isEmpty()||s.startsWith("#")||!s.contains(":"))continue;int at=s.indexOf(':');String value=s.substring(at+1).trim();if((value.startsWith("'")&&value.endsWith("'"))||(value.startsWith("\"")&&value.endsWith("\"")))value=value.substring(1,value.length()-1);config.put(s.substring(0,at).trim(),value);}
        } catch(Exception error) { System.err.println("[ModLoaderServer] config error: "+error); }
    }
}
