package com.happyuky7.openvoxelmoding.server.world;

import net.openvoxel.server.plugin.VoxelServer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Server-authoritative terrain decoration for generated/loaded chunks. */
public final class WorldFeatures {
    private static final int CHUNK = 16;
    private static final int HEIGHT = 256;
    private static final int REGION = 64;
    private final boolean enabled;
    private final long salt;
    private final Path index;
    private final Set<Long> processed = Collections.synchronizedSet(new HashSet<>());

    public WorldFeatures(Map<String, String> config) {
        enabled = Boolean.parseBoolean(config.getOrDefault("world.features.enabled", "true"));
        long parsed;
        try { parsed = Long.parseLong(config.getOrDefault("world.features.seed-salt", "7351")); }
        catch (NumberFormatException ignored) { parsed = 7351L; }
        salt = parsed;
        index = Paths.get("plugins", "ModLoaderServer", "generated-chunks-" + salt + ".txt");
        loadIndex();
    }

    public void populate(int chunkX, int chunkZ) {
        if (!enabled) return;
        long key = key(chunkX, chunkZ);
        if (!processed.add(key)) return;
        try {
            int[] surface = surfaceMap(chunkX, chunkZ);
            decorateBiomes(chunkX, chunkZ, surface);
            carveRegionalCaves(chunkX, chunkZ, surface);
            append(chunkX, chunkZ);
        } catch (Throwable error) {
            processed.remove(key);
            System.err.println("[ModLoaderServer] world features failed for " + chunkX + "," + chunkZ + ": " + error);
        }
    }

    private int[] surfaceMap(int chunkX, int chunkZ) {
        int[] out = new int[CHUNK * CHUNK];
        int ox = chunkX * CHUNK, oz = chunkZ * CHUNK;
        for (int z = 0; z < CHUNK; z++) for (int x = 0; x < CHUNK; x++) {
            int y = HEIGHT - 2;
            while (y > 2 && isAirOrFoliage(name(ox + x, y, oz + z))) y--;
            out[z * CHUNK + x] = y;
        }
        return out;
    }

    private void decorateBiomes(int chunkX, int chunkZ, int[] surface) {
        int ox = chunkX * CHUNK, oz = chunkZ * CHUNK;
        long biome = mix(salt ^ ((long) Math.floorDiv(ox, 96) * 0x9E3779B97F4A7C15L) ^ Math.floorDiv(oz, 96));
        int kind = (int) Math.floorMod(biome, 4);
        for (int z = 0; z < CHUNK; z++) for (int x = 0; x < CHUNK; x++) {
            int wx = ox + x, wz = oz + z, y = surface[z * CHUNK + x];
            String top = name(wx, y, wz);
            long h = mix(salt ^ ((long) wx * 341873128712L) ^ ((long) wz * 132897987541L));
            if (kind == 0 && y > 52 && grass(top) && Math.floorMod(h, 13) == 0 && air(wx, y + 1, wz))
                set(wx, y + 1, wz, "flower");                         // flower meadow
            else if (kind == 1 && y > 58 && grass(top) && Math.floorMod(h, 97) == 0 && air(wx, y + 1, wz))
                set(wx, y + 1, wz, "pumpkin");                        // pumpkin grove
            else if (kind == 2 && y > 58 && grass(top) && Math.floorMod(h, 7) < 2)
                set(wx, y, wz, Math.floorMod(h, 3) == 0 ? "gravel" : "stone"); // rocky highland
            else if (kind == 3 && y > 72 && (grass(top) || top.equals("stone"))) {
                set(wx, y, wz, "snowy_grass");
                if (air(wx, y + 1, wz)) set(wx, y + 1, wz, "snow");   // snowy ridge
            }
        }
    }

    /** World-space ellipsoids naturally continue through chunk borders. */
    private void carveRegionalCaves(int chunkX, int chunkZ, int[] surface) {
        int ox = chunkX * CHUNK, oz = chunkZ * CHUNK;
        int minCellX = Math.floorDiv(ox - 32, REGION), maxCellX = Math.floorDiv(ox + CHUNK + 32, REGION);
        int minCellZ = Math.floorDiv(oz - 32, REGION), maxCellZ = Math.floorDiv(oz + CHUNK + 32, REGION);
        for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            long h = mix(salt ^ ((long) cellX * 0xD1B54A32D192ED03L) ^ ((long) cellZ * 0x94D049BB133111EBL));
            if (Math.floorMod(h, 3) != 0) continue;
            int cx = cellX * REGION + 16 + (int) Math.floorMod(h >>> 8, 32);
            int cz = cellZ * REGION + 16 + (int) Math.floorMod(h >>> 16, 32);
            int cy = 18 + (int) Math.floorMod(h >>> 24, 30);
            double rx = 18 + Math.floorMod(h >>> 32, 11), ry = 7 + Math.floorMod(h >>> 40, 7), rz = 18 + Math.floorMod(h >>> 48, 11);
            carveEllipsoid(ox, oz, surface, cx, cy, cz, rx, ry, rz);
            if (Math.floorMod(h >>> 4, 4) == 0) carveEntrance(ox, oz, surface, cx, cy, cz, h);
        }
    }

    private void carveEllipsoid(int ox, int oz, int[] surface, int cx, int cy, int cz, double rx, double ry, double rz) {
        for (int z = 0; z < CHUNK; z++) for (int x = 0; x < CHUNK; x++) {
            int wx = ox + x, wz = oz + z, roof = surface[z * CHUNK + x] - 6;
            double dx = (wx - cx) / rx, dz = (wz - cz) / rz;
            double horizontal = dx * dx + dz * dz;
            if (horizontal >= 1) continue;
            int span = (int) Math.ceil(ry * Math.sqrt(1 - horizontal));
            for (int y = Math.max(5, cy - span); y <= Math.min(roof, cy + span); y++) carve(wx, y, wz);
        }
    }

    /** A winding 3x3 descending mouth joins the surface to its regional chamber. */
    private void carveEntrance(int ox, int oz, int[] surface, int caveX, int caveY, int caveZ, long h) {
        if (caveX < ox + 2 || caveX > ox + 13 || caveZ < oz + 2 || caveZ > oz + 13) return;
        int sy = surface[(caveZ - oz) * CHUNK + caveX - ox] + 1;
        int x = caveX, z = caveZ;
        for (int y = sy; y >= caveY; y--) {
            if ((sy - y) % 5 == 0) { x += ((h >>> (y & 31)) & 1) == 0 ? 1 : -1; x = Math.max(ox + 2, Math.min(ox + 13, x)); }
            for (int dz = -1; dz <= 1; dz++) for (int dx = -1; dx <= 1; dx++)
                if (dx * dx + dz * dz <= 2) carve(x + dx, y, z + dz);
        }
    }

    private void carve(int x, int y, int z) {
        String n = name(x, y, z);
        if (n.equals("stone") || n.equals("dirt") || n.equals("grass") || n.equals("gravel") || n.equals("snow") || n.equals("snowy_grass")) set(x, y, z, "air");
    }

    private static boolean grass(String n) { return n.equals("grass") || n.equals("snowy_grass"); }
    private static boolean isAirOrFoliage(String n) { return n.equals("air") || n.equals("water") || n.equals("oak_leaves") || n.equals("tallgrass") || n.equals("flower") || n.equals("snow"); }
    private static String name(int x, int y, int z) { String n = VoxelServer.blockName(VoxelServer.getBlock(x, y, z)); return n == null ? "air" : n.toLowerCase(); }
    private static boolean air(int x, int y, int z) { return name(x, y, z).equals("air"); }
    private static void set(int x, int y, int z, String block) { VoxelServer.setBlock(x, y, z, VoxelServer.blockState(block)); }
    private static long key(int x, int z) { return ((long) x << 32) ^ (z & 0xffffffffL); }
    private static long mix(long z) { z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L; z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL; return z ^ (z >>> 31); }

    private void loadIndex() {
        try {
            if (!Files.isRegularFile(index)) return;
            List<String> lines = Files.readAllLines(index, StandardCharsets.UTF_8);
            for (String line : lines) { String[] p = line.trim().split(","); if (p.length == 2) processed.add(key(Integer.parseInt(p[0]), Integer.parseInt(p[1]))); }
        } catch (Exception error) { System.err.println("[ModLoaderServer] feature index: " + error); }
    }

    private synchronized void append(int x, int z) {
        try { Files.createDirectories(index.getParent()); Files.writeString(index, x + "," + z + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND); }
        catch (Exception error) { System.err.println("[ModLoaderServer] feature index save: " + error); }
    }
}
