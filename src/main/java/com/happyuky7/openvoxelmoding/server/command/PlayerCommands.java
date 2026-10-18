package com.happyuky7.openvoxelmoding.server.command;

import com.happyuky7.openvoxelmoding.server.core.ServerContext;
import com.happyuky7.openvoxelmoding.server.permissions.Permissions;
import com.happyuky7.openvoxelmoding.server.player.GameModes;
import net.openvoxel.server.plugin.CommandManager;
import net.openvoxel.server.plugin.Player;
import net.openvoxel.server.plugin.VoxelServer;

/**
 * Commands that act on a player's ModLoader client: /gamemode (/gm), /give and the client handshake /mlhello.
 * Items and game modes live on the client, so these send control lines through the {@link com.happyuky7.openvoxelmoding.server.net.ClientChannel}.
 */
public final class PlayerCommands {
    private static final int MAX_GIVE = 64 * 36;
    private final ServerContext ctx;

    public PlayerCommands(ServerContext ctx) { this.ctx = ctx; }

    public void register(CommandManager c) {
        c.register("gamemode", "<creative|survival> [player] - change game mode (ModLoader clients)", this::gamemode, ctx.plugin);
        c.register("gm", "alias of /gamemode", this::gamemode, ctx.plugin);
        c.register("give", "[player] <item> [count] - give items (ModLoader clients)", this::give, ctx.plugin);
        c.register("mlhello", "", (p, a) -> hello(p, a.length > 0 ? a[0] : "?"), ctx.plugin);
        Permissions.registerCommand("gm", "openvoxel.command.gamemode");
    }

    /** First message of a ModLoader client: answers with version, usable commands, game mode and tab list. */
    private void hello(Player p, String clientVersion) {
        if (Permissions.isConsole(p)) return;
        ctx.channel.markModded(p);
        ctx.channel.send(p, "hello " + ctx.version());
        ctx.channel.refreshCommands(p);
        ctx.channel.send(p, "gm " + ctx.gameModes.of(p));
        ctx.tabList.send(p);
        System.out.println("[ModLoader] " + p.getName() + " uses ModLoader client " + clientVersion);
    }

    private void gamemode(Player sender, String[] a) {
        if (a.length == 0) { sender.sendMessage("&cUsage: /gamemode <creative|survival> [player]"); return; }
        String mode = GameModes.parse(a[0]);
        if (mode == null) { sender.sendMessage("&cUnknown game mode: " + a[0]); return; }
        Player target = sender;
        if (a.length > 1) {
            if (!Permissions.has(sender, "openvoxel.command.gamemode.others")) { sender.sendMessage("&cYou can't change other players' game mode &8(openvoxel.command.gamemode.others)"); return; }
            target = VoxelServer.getPlayer(a[1]);
            if (target == null) { sender.sendMessage("&cPlayer not online: " + a[1]); return; }
        } else if (Permissions.isConsole(sender)) { sender.sendMessage("Usage from console: gamemode <mode> <player>"); return; }
        ctx.gameModes.set(target, mode);
        ctx.channel.send(target, "gm " + mode);
        if (!ctx.channel.isModded(target)) target.sendMessage("&7(Game modes need the OpenVoxel ModLoader client.)");
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
        if (!ctx.channel.isModded(target)) { sender.sendMessage("&c" + target.getName() + " is not using the ModLoader client (items are client-side)."); return; }
        String item = a[i];
        int count = 1;
        if (a.length > i + 1) try { count = Math.max(1, Math.min(MAX_GIVE, Integer.parseInt(a[i + 1]))); } catch (NumberFormatException e) { sender.sendMessage("&cNot a number: " + a[i + 1]); return; }
        ctx.channel.send(target, "give " + item + " " + count);
        if (target != sender) sender.sendMessage("&aGave " + count + " " + item + " to " + target.getName() + ".");
    }
}
