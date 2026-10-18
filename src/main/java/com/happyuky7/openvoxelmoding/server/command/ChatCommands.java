package com.happyuky7.openvoxelmoding.server.command;

import com.happyuky7.openvoxelmoding.server.core.ServerContext;
import com.happyuky7.openvoxelmoding.server.permissions.Permissions;
import net.openvoxel.server.plugin.CommandManager;
import net.openvoxel.server.plugin.Player;
import net.openvoxel.server.plugin.VoxelServer;

import java.util.Arrays;
import java.util.StringJoiner;

/** Information and messaging: /help /version /players /list /msg /tell. */
public final class ChatCommands {
    private final ServerContext ctx;

    public ChatCommands(ServerContext ctx) { this.ctx = ctx; }

    public void register(CommandManager c) {
        c.register("help", "Lists available commands", (p, a) -> {
            p.sendMessage("&e--- OpenVoxel Server Commands ---");
            for (String command : c.describeAll()) {
                String name = command.substring(1).split(" ", 2)[0];
                if (name.equals("mlhello") || !Permissions.has(p, Permissions.nodeFor(name))) continue;
                p.sendMessage("&a" + command.replaceFirst(" - ", " &7- "));
            }
        }, ctx.plugin);
        c.register("version", "Shows server and mod versions", (p, a) -> {
            p.sendMessage("OpenVoxel Server 0.0.8-SNAPSHOT");
            p.sendMessage("OpenVoxel ModLoader Server " + ctx.version() + " | API 0.0.8");
        }, ctx.plugin);
        c.register("players", "Lists online players", (p, a) -> {
            StringJoiner names = new StringJoiner(", ");
            for (Player online : VoxelServer.getOnlinePlayers()) names.add(online.getName());
            p.sendMessage("Online (" + VoxelServer.getOnlinePlayers().size() + "): " + names);
        }, ctx.plugin);
        c.register("list", "Alias of /players", (p, a) -> c.dispatch(p, "/players"), ctx.plugin);
        c.register("msg", "<player> <message> - private message", this::msg, ctx.plugin);
        c.register("tell", "alias of /msg", this::msg, ctx.plugin);
        Permissions.registerCommand("tell", "openvoxel.command.msg");
        Permissions.registerCommand("list", "openvoxel.command.list");
    }

    private void msg(Player p, String[] a) {
        if (a.length < 2) { p.sendMessage("&cUsage: /msg <player> <message>"); return; }
        Player t = VoxelServer.getPlayer(a[0]);
        if (t == null) { p.sendMessage("&cPlayer not online: " + a[0]); return; }
        String text = String.join(" ", Arrays.copyOfRange(a, 1, a.length));
        p.sendMessage("&7[me -> " + t.getName() + "] &f" + text);
        t.sendMessage("&7[" + p.getName() + " -> me] &f" + text);
    }
}
