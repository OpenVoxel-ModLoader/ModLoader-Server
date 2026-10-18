package com.happyuky7.openvoxelmoding.server.command;

import com.happyuky7.openvoxelmoding.server.core.ServerContext;
import com.happyuky7.openvoxelmoding.server.permissions.Permissions;
import com.happyuky7.openvoxelmoding.server.world.Warps;
import net.openvoxel.server.plugin.CommandManager;
import net.openvoxel.server.plugin.Location;
import net.openvoxel.server.plugin.Player;

import java.util.Locale;

/**
 * Spawn, warps and whitelist: /spawn /setspawn /warp /setwarp /delwarp /warps /whitelist.
 * Spawn and warps are planned to move to a separate plugin (like EssentialsX in Minecraft).
 */
public final class WorldCommands {
    private final ServerContext ctx;

    public WorldCommands(ServerContext ctx) { this.ctx = ctx; }

    public void register(CommandManager c) {
        c.register("spawn", "teleport to the world spawn", (p, a) -> {
            Location l = ctx.warps.spawn();
            if (l != null && !Permissions.isConsole(p)) { p.setLocation(l); p.sendMessage("&aTeleported to spawn."); }
        }, ctx.plugin);
        c.register("setspawn", "set the world spawn here", (p, a) -> {
            if (Permissions.isConsole(p)) return;
            ctx.warps.setSpawn(p.getLocation());
            p.sendMessage("&aWorld spawn set.");
        }, ctx.plugin);
        c.register("warp", "<name> - teleport to a warp", this::warp, ctx.plugin);
        c.register("setwarp", "<name> - create a warp here", this::setwarp, ctx.plugin);
        c.register("delwarp", "<name> - delete a warp", (p, a) -> {
            if (a.length == 0) { p.sendMessage("&cUsage: /delwarp <name>"); return; }
            p.sendMessage(ctx.warps.delete(a[0]) ? "&aWarp deleted." : "&cNo such warp.");
        }, ctx.plugin);
        c.register("warps", "list warps", (p, a) -> p.sendMessage("&eWarps: &f" + String.join(", ", ctx.warps.names())), ctx.plugin);
        c.register("whitelist", "<add|remove|list> [player]", this::whitelist, ctx.plugin);
    }

    private void warp(Player p, String[] a) {
        if (a.length == 0) { p.sendMessage("&eWarps: &f" + String.join(", ", ctx.warps.names())); return; }
        Location l = ctx.warps.get(a[0]);
        if (l == null) { p.sendMessage("&cNo such warp: " + a[0]); return; }
        if (Permissions.isConsole(p)) return;
        String name = a[0].toLowerCase(Locale.ROOT);
        if (!Permissions.has(p, "openvoxel.warp." + name) && !Permissions.has(p, "openvoxel.warp.*") && !Permissions.has(p, "openvoxel.command.warp")) { p.sendMessage("&cYou can't use that warp."); return; }
        p.setLocation(l);
        p.sendMessage("&aWarped to " + a[0] + ".");
    }

    private void setwarp(Player p, String[] a) {
        if (a.length == 0 || Permissions.isConsole(p)) { p.sendMessage("&cUsage: /setwarp <name>"); return; }
        String name = a[0].toLowerCase(Locale.ROOT);
        if (!Warps.validName(name)) { p.sendMessage("&cWarp names: a-z, 0-9, _ and -"); return; }
        ctx.warps.set(name, p.getLocation());
        p.sendMessage("&aWarp " + name + " set.");
    }

    private void whitelist(Player p, String[] a) {
        String sub = a.length > 0 ? a[0].toLowerCase(Locale.ROOT) : "list";
        switch (sub) {
            case "add": if (a.length > 1) { ctx.whitelist.add(a[1]); p.sendMessage("&aAdded " + a[1] + " to the whitelist."); } break;
            case "remove": if (a.length > 1) { ctx.whitelist.remove(a[1]); p.sendMessage("&aRemoved " + a[1] + " from the whitelist."); } break;
            case "on": case "off": p.sendMessage("&eSet gameplay.whitelist: " + sub.equals("on") + " in server.properties.yml and run /perms reload."); break;
            default: p.sendMessage("&eWhitelist (" + (ctx.settings.whitelist() ? "on" : "off") + "): &f" + String.join(", ", ctx.whitelist.names()));
        }
    }
}
