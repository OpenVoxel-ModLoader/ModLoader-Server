package com.happyuky7.openvoxelmoding.server.command;

import com.happyuky7.openvoxelmoding.server.core.ServerContext;
import com.happyuky7.openvoxelmoding.server.permissions.Permissions;
import net.openvoxel.server.plugin.CommandManager;
import net.openvoxel.server.plugin.Player;
import net.openvoxel.server.plugin.VoxelServer;

import java.util.Arrays;
import java.util.Locale;

/** Operators and server control: /op /deop /ops /perms /kick /say /heal /stop. */
public final class AdminCommands {
    private final ServerContext ctx;

    public AdminCommands(ServerContext ctx) { this.ctx = ctx; }

    public void register(CommandManager c) {
        c.register("op", "<player> - give operator", (p, a) -> op(p, a, true), ctx.plugin);
        c.register("deop", "<player> - remove operator", (p, a) -> op(p, a, false), ctx.plugin);
        c.register("ops", "list operators", (p, a) -> p.sendMessage("&eOperators (" + Permissions.ops().size() + "): &f" + String.join(", ", Permissions.ops())), ctx.plugin);
        c.register("perms", "<check|reload> - permission tools", this::perms, ctx.plugin);
        c.register("kick", "<player> [reason] - kick a player", this::kick, ctx.plugin);
        c.register("say", "<message> - broadcast a message", (p, a) -> {
            if (a.length == 0) { p.sendMessage("Usage: /say <message>"); return; }
            VoxelServer.broadcast("&d[" + p.getName() + "] &f" + String.join(" ", a));
        }, ctx.plugin);
        c.register("heal", "Restores your health", (p, a) -> {
            if (p.getEntity() != null) p.getEntity().health = p.getEntity().maxHealth;
            p.sendMessage("You have been healed.");
        }, ctx.plugin);
        c.register("stop", "Stops the server (saves the world)", (p, a) -> {
            VoxelServer.broadcast("&cServer is stopping.");
            System.out.println("[console] stopping server");
            new Thread(() -> { try { Thread.sleep(300); } catch (InterruptedException ignored) { } System.exit(0); }, "stop").start();
        }, ctx.plugin);
    }

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
        if (target != null) ctx.channel.refreshCommands(target);
    }

    private void perms(Player p, String[] a) {
        String sub = a.length > 0 ? a[0].toLowerCase(Locale.ROOT) : "";
        if (sub.equals("reload")) { ctx.reload(); ctx.tabList.start(); p.sendMessage("&aReloaded permissions, tablist and server settings."); return; }
        if (sub.equals("check") && a.length >= 3) {
            Player t = VoxelServer.getPlayer(a[1]);
            if (t == null) { p.sendMessage("&cPlayer not online: " + a[1]); return; }
            p.sendMessage("&e" + t.getName() + " &7" + a[2] + " = " + (Permissions.has(t, a[2]) ? "&atrue" : "&cfalse") + " &8(provider: " + Permissions.providerName() + ", op: " + Permissions.isOp(t) + ")");
            return;
        }
        p.sendMessage("&eProvider: &f" + Permissions.providerName() + "&e. Usage: /perms check <player> <node> | /perms reload");
    }

    private void kick(Player p, String[] a) {
        if (a.length == 0) { p.sendMessage("Usage: /kick <player> [reason]"); return; }
        Player target = VoxelServer.getPlayer(a[0]);
        if (target == null) { p.sendMessage("No such player: " + a[0]); return; }
        VoxelServer.kickPlayer(target, a.length > 1 ? String.join(" ", Arrays.copyOfRange(a, 1, a.length)) : "Kicked by " + p.getName());
    }
}
