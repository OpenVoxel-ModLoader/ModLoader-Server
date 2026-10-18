package com.happyuky7.openvoxelmoding.server.command;

import com.happyuky7.openvoxelmoding.server.core.ServerContext;
import net.openvoxel.server.plugin.CommandManager;
import net.openvoxel.server.plugin.Location;
import net.openvoxel.server.plugin.VoxelServer;

/** Test commands from {@code plugins/ModLoaderServer/config.yml}: /modtest /modreload /platform. */
public final class DebugCommands {
    private final ServerContext ctx;

    public DebugCommands(ServerContext ctx) { this.ctx = ctx; }

    public void register(CommandManager c) {
        c.register("modtest", "Checks the modded server", (p, a) -> {
            p.sendMessage(ctx.config.get("messages.test"));
            p.sendMessage("Try /platform to test world editing.");
        }, ctx.plugin);
        c.register("modreload", "Reloads ModLoaderServer config", (p, a) -> { ctx.config.load(); p.sendMessage("[ModLoader] Configuration reloaded."); }, ctx.plugin);
        c.register("platform", "Creates a test platform", (p, a) -> {
            Location l = p.getLocation();
            int x = l.getBlockX(), y = l.getBlockY() - 1, z = l.getBlockZ();
            int radius = Math.max(1, Math.min(16, Integer.parseInt(ctx.config.get("platform.radius"))));
            long block = VoxelServer.blockState(ctx.config.get("platform.block"));
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) VoxelServer.setBlock(x + dx, y, z + dz, block);
            p.sendMessage("[ModLoader] Test platform created.");
        }, ctx.plugin);
    }
}
