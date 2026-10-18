package com.happyuky7.openvoxelmoding.server.core;

import net.openvoxel.server.plugin.Player;
import net.openvoxel.server.plugin.VoxelServer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Reads commands from stdin so the server window works like a console (with or without leading '/'). */
final class ServerConsole {
    private ServerConsole() {}

    static void start() {
        if (Boolean.parseBoolean(System.getProperty("happy.noconsole", "false"))) return;
        Player console = new Player("CONSOLE", "console", "127.0.0.1");
        console.messageSink = message -> System.out.println("[console] " + message.replaceAll("(?i)[&§]([0-9a-fk-or]|#[0-9a-f]{6})", ""));
        Thread t = new Thread(() -> {
            System.out.println("[console] ready - type help, say <msg>, players, kick <name>, stop");
            try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = in.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    if (!line.startsWith("/")) line = "/" + line;
                    VoxelServer.getCommandManager().dispatch(console, line);
                }
            } catch (Exception error) { System.out.println("[console] closed: " + error.getMessage()); }
        }, "ModLoader-console");
        t.setDaemon(true);
        t.start();
    }
}
