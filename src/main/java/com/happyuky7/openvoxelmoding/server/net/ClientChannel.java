package com.happyuky7.openvoxelmoding.server.net;

import com.happyuky7.openvoxelmoding.server.permissions.Permissions;
import net.openvoxel.server.plugin.Player;
import net.openvoxel.server.plugin.VoxelServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Control channel with ModLoader clients. The game has no custom packets, so the server sends chat lines
 * starting with {@link #PREFIX} that the client hides and handles: {@code hello}, {@code cmds}, {@code gm},
 * {@code tab}, {@code give}. A client announces itself with {@code /mlhello <version>}.
 */
public final class ClientChannel {
    public static final String PREFIX = "\u0001ML:";
    private final Set<String> modded = ConcurrentHashMap.newKeySet();      // names of players running the ModLoader client

    /** Sends one control line to a player's ModLoader client. */
    public void send(Player p, String line) { p.sendMessage(PREFIX + line); }

    public boolean isModded(Player p) { return modded.contains(p.getName()); }
    public void markModded(Player p) { modded.add(p.getName()); }
    public void forget(Player p) { modded.remove(p.getName()); }

    /** Commands this player may run, for the client's Tab completion. */
    public List<String> usableCommands(Player p) {
        List<String> out = new ArrayList<>();
        for (String d : VoxelServer.getCommandManager().describeAll()) {
            String name = d.substring(1).split(" ", 2)[0];
            if (name.equals("mlhello")) continue;
            if (Permissions.has(p, Permissions.nodeFor(name))) out.add(name);
        }
        Collections.sort(out);
        return out;
    }

    /** Re-sends the usable command list (after op/deop or a permission change). */
    public void refreshCommands(Player p) { if (isModded(p)) send(p, "cmds " + String.join(",", usableCommands(p))); }
}
