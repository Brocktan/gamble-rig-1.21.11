package com.fakedispenser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class Config {
	public static class Entry {
		public String dimension = "minecraft:overworld";
		public int x, y, z;
		public String item = "minecraft:netherite_ingot";
		/** Optional comma-separated pool of item ids used for randomized fake stacks. */
		public List<String> randomItems = new ArrayList<>();
		public String name = "1";
		public int count = 1;
		/** enchantment id -> level */
		public Map<String, Integer> enchants = new LinkedHashMap<>();

		public boolean at(String dim, int px, int py, int pz) {
			return dimension.equals(dim) && x == px && y == py && z == pz;
		}

		public Entry copy() {
			Entry e = new Entry();
			e.dimension = dimension;
			e.x = x;
			e.y = y;
			e.z = z;
			e.item = item;
			e.name = name;
			e.count = count;
			e.enchants = enchants == null ? new LinkedHashMap<>() : new LinkedHashMap<>(enchants);
			return e;
		}

		/** Builds the fake stack, or null if the item id is unknown. */
		public ItemStack build() {
			String id = item.trim().toLowerCase();
			if (!id.contains(":")) id = "minecraft:" + id;
			Item found = null;
			for (Item i : Registries.ITEM) {
				if (Registries.ITEM.getId(i).toString().equals(id)) {
					found = i;
					break;
				}
			}
			if (found == null) return null;
			ItemStack stack = new ItemStack(found, Math.max(1, Math.min(count, 99)));
			if (name != null && !name.isEmpty()) {
				// '&' color codes, e.g. &bName
				stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name.replace('&', '\u00a7')));
			}
			if (enchants != null && !enchants.isEmpty()) {
				ItemEnchantmentsComponent.Builder b = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
				boolean any = false;
				for (RegistryEntry.Reference<Enchantment> ref : allEnchants()) {
					Integer lv = enchants.get(ref.registryKey().getValue().toString());
					if (lv != null && lv > 0) {
						b.add(ref, lv);
						any = true;
					}
				}
				if (any) stack.set(DataComponentTypes.ENCHANTMENTS, b.build());
			}
			return stack;
		}

		/** True if this stack looks like what this entry produces. */
		public boolean matches(ItemStack s) {
			ItemStack b = build();
			return b != null && s.isOf(b.getItem())
					&& Objects.equals(s.get(DataComponentTypes.CUSTOM_NAME), b.get(DataComponentTypes.CUSTOM_NAME))
					&& Objects.equals(s.get(DataComponentTypes.ENCHANTMENTS), b.get(DataComponentTypes.ENCHANTMENTS));
		}
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("fakedispenser.json");
	public static List<Entry> entries = new ArrayList<>();

	public static List<RegistryEntry.Reference<Enchantment>> allEnchants() {
		MinecraftClient mc = MinecraftClient.getInstance();
		List<RegistryEntry.Reference<Enchantment>> list = new ArrayList<>();
		if (mc.world == null) return list;
		mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).streamEntries().forEach(list::add);
		list.sort(Comparator.comparing(r -> r.registryKey().getValue().toString()));
		return list;
	}

	public static boolean isFake(ItemStack s) {
		if (s == null || s.isEmpty() || entries.isEmpty()) return false;
		for (Entry e : entries) if (e.matches(s)) return true;
		return false;
	}

	public static void load() {
		try {
			if (Files.exists(FILE)) {
				try (Reader r = Files.newBufferedReader(FILE)) {
					List<Entry> list = GSON.fromJson(r, new TypeToken<List<Entry>>() {}.getType());
					if (list != null) entries = list;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void save() {
		try (Writer w = Files.newBufferedWriter(FILE)) {
			GSON.toJson(entries, w);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Entry find(String dim, int x, int y, int z) {
		for (Entry e : entries) if (e.at(dim, x, y, z)) return e;
		return null;
	}
}
