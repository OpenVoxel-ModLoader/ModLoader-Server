package com.happyuky7.openvoxelmoding.server.core;

import com.happyuky7.openvoxelmoding.server.command.AdminCommands;
import com.happyuky7.openvoxelmoding.server.command.ChatCommands;
import com.happyuky7.openvoxelmoding.server.command.DebugCommands;
import com.happyuky7.openvoxelmoding.server.command.PlayerCommands;
import com.happyuky7.openvoxelmoding.server.command.WorldCommands;
import com.happyuky7.openvoxelmoding.server.listener.CommandGate;
import com.happyuky7.openvoxelmoding.server.listener.PlayerListener;
import com.happyuky7.openvoxelmoding.server.listener.WorldListener;
import com.happyuky7.openvoxelmoding.server.world.WorldFeatures;
import net.openvoxel.server.plugin.CommandManager;
import net.openvoxel.server.plugin.Plugin;
import net.openvoxel.server.plugin.PluginManager;
import net.openvoxel.server.plugin.VoxelServer;

/** Plugin entry point: loads the configuration, registers listeners and commands, starts the tab list and the console. */
public final class ModLoaderServer extends Plugin {
    private ServerContext ctx;

    @Override public void onEnable() {
        ctx = new ServerContext(this);
        ctx.reload();

        PluginManager plugins = VoxelServer.getPluginManager();
        plugins.registerEvents(new CommandGate(), this);
        plugins.registerEvents(new PlayerListener(ctx), this);
        plugins.registerEvents(new WorldListener(new WorldFeatures(ctx.config.asMap())), this);

        CommandManager commands = VoxelServer.getCommandManager();
        new ChatCommands(ctx).register(commands);
        new AdminCommands(ctx).register(commands);
        new PlayerCommands(ctx).register(commands);
        new WorldCommands(ctx).register(commands);
        new DebugCommands(ctx).register(commands);

        ctx.tabList.start();
        ServerConsole.start();
    }
}
