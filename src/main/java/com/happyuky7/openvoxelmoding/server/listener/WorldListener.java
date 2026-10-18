package com.happyuky7.openvoxelmoding.server.listener;

import com.happyuky7.openvoxelmoding.server.world.WorldFeatures;
import net.openvoxel.server.plugin.event.EventHandler;
import net.openvoxel.server.plugin.event.Listener;
import net.openvoxel.server.plugin.event.world.ChunkLoadEvent;

/** Decorates chunks with {@link WorldFeatures} when they load. */
public final class WorldListener implements Listener {
    private final WorldFeatures features;

    public WorldListener(WorldFeatures features) { this.features = features; }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) { features.populate(event.getChunkX(), event.getChunkZ()); }
}
