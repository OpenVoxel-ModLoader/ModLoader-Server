package com.happyuky7.openvoxelmoding.server.player;

import com.happyuky7.openvoxelmoding.server.config.PropertiesFile;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/** Whitelisted names. Stored as {@code @whitelist} in {@code warps.properties} (same file as older versions). */
public final class Whitelist {
    private final PropertiesFile file;

    public Whitelist(PropertiesFile file) { this.file = file; }

    public Set<String> names() {
        Set<String> list = new TreeSet<>(Arrays.asList(file.get("@whitelist", "").split(",")));
        list.remove("");
        return list;
    }

    public boolean contains(String name) { return names().contains(name.toLowerCase(Locale.ROOT)); }

    public void add(String name) { Set<String> n = names(); n.add(name.toLowerCase(Locale.ROOT)); store(n); }
    public void remove(String name) { Set<String> n = names(); n.remove(name.toLowerCase(Locale.ROOT)); store(n); }

    private void store(Set<String> names) { file.set("@whitelist", String.join(",", names)); file.save(); }
}
