package com.happyuky7.openvoxelmoding.server.config;

import com.happyuky7.openvoxelmoding.server.permissions.Permissions;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** A {@code .properties} file in {@code plugins/ModLoader}, loaded on {@link #load()} and written on every {@link #save()}. */
public final class PropertiesFile {
    private final String name;
    private final Properties props = new Properties();

    public PropertiesFile(String name) { this.name = name; }

    public Path path() { return Permissions.dataFolder().resolve(name); }

    public PropertiesFile load() {
        props.clear();
        Path f = path();
        if (!Files.isRegularFile(f)) return this;
        try (InputStream in = Files.newInputStream(f)) { props.load(in); }
        catch (Exception error) { System.err.println("[ModLoader] " + name + ": " + error); }
        return this;
    }

    public void save() {
        try {
            Files.createDirectories(Permissions.dataFolder());
            try (OutputStream out = Files.newOutputStream(path())) { props.store(out, "OpenVoxel ModLoader"); }
        } catch (Exception error) { System.err.println("[ModLoader] " + name + ": " + error); }
    }

    public String get(String key) { return props.getProperty(key); }
    public String get(String key, String def) { return props.getProperty(key, def); }
    public void set(String key, String value) { props.setProperty(key, value); }
    public boolean remove(String key) { return props.remove(key) != null; }
    public java.util.Set<String> keys() { return props.stringPropertyNames(); }
}
