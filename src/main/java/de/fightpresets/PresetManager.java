package de.fightpresets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class PresetManager {
    public static class Preset {
        public String name;
        public String icon;
        /** Inventar-Slot (0-8 Hotbar, 9-35 Inventar, 36-39 Ruestung, 40 Offhand) -> Item-ID */
        public Map<Integer, String> slots = new TreeMap<>();
    }

    private static class Data {
        List<Preset> presets = new ArrayList<>();
        String active;
        boolean enabled = true;
    }

    public static final String[] ICONS = {
            "minecraft:diamond_sword", "minecraft:netherite_sword", "minecraft:mace",
            "minecraft:diamond_axe", "minecraft:bow", "minecraft:crossbow", "minecraft:trident",
            "minecraft:shield", "minecraft:golden_apple", "minecraft:enchanted_golden_apple",
            "minecraft:ender_pearl", "minecraft:totem_of_undying", "minecraft:end_crystal",
            "minecraft:splash_potion", "minecraft:netherite_chestplate", "minecraft:elytra",
            "minecraft:fire_charge", "minecraft:wind_charge"
    };

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("fightpresets.json");
    private static Data data = new Data();
    private static Map<String, Item> itemCache;

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                Data d = GSON.fromJson(Files.readString(FILE), Data.class);
                if (d != null) data = d;
            }
        } catch (Exception e) {
            System.err.println("[FightPresets] Laden fehlgeschlagen: " + e);
        }
        if (data.presets == null) data.presets = new ArrayList<>();
        for (Preset p : data.presets) if (p.slots == null) p.slots = new TreeMap<>();
    }

    public static void save() {
        try {
            Files.writeString(FILE, GSON.toJson(data));
        } catch (Exception e) {
            System.err.println("[FightPresets] Speichern fehlgeschlagen: " + e);
        }
    }

    public static List<Preset> presets() { return data.presets; }
    public static boolean isEnabled() { return data.enabled; }
    public static void toggle() { data.enabled = !data.enabled; save(); }

    public static Preset active() {
        if (data.active == null) return null;
        for (Preset p : data.presets) if (p.name.equals(data.active)) return p;
        return null;
    }

    public static void setActive(String name) { data.active = name; save(); }

    public static void delete(String name) {
        data.presets.removeIf(p -> p.name.equals(name));
        if (name.equals(data.active)) data.active = null;
        save();
    }

    /** Liest das aktuelle Inventar und speichert/ueberschreibt ein Preset mit dem Namen. */
    public static void saveFromInventory(String name, String icon) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || name.isBlank()) return;
        Inventory inv = mc.player.getInventory();
        Preset p = null;
        for (Preset e : data.presets) if (e.name.equals(name)) p = e;
        if (p == null) { p = new Preset(); p.name = name; data.presets.add(p); }
        p.icon = icon;
        p.slots.clear();
        for (int i = 0; i <= 40; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty()) p.slots.put(i, idOf(s.getItem()));
        }
        data.active = name;
        save();
    }

    public static String idOf(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    public static Item itemOf(String id) {
        if (itemCache == null) {
            itemCache = new HashMap<>();
            for (Item i : BuiltInRegistries.ITEM) itemCache.put(idOf(i), i);
        }
        return itemCache.getOrDefault(id, Items.BARRIER);
    }

    public static ItemStack iconOf(Preset p) {
        return new ItemStack(itemOf(p.icon == null ? ICONS[0] : p.icon));
    }
}
