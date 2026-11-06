package com.fakedispenser;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;

import java.util.List;

/** Click an enchantment to raise its level (wraps back to 0 after the max level). */
public class EnchantScreen extends Screen {
	private static final int PER_PAGE = 12;
	private final Screen parent;
	private final Config.Entry draft;
	private int page = 0;

	public EnchantScreen(Screen parent, Config.Entry draft) {
		super(Text.literal("Enchantments"));
		this.parent = parent;
		this.draft = draft;
	}

	private static String pretty(String id) {
		String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
		StringBuilder sb = new StringBuilder();
		for (String w : path.split("_")) {
			if (w.isEmpty()) continue;
			if (sb.length() > 0) sb.append(' ');
			sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
		}
		return sb.toString();
	}

	@Override
	protected void init() {
		List<RegistryEntry.Reference<Enchantment>> list = Config.allEnchants();
		int pages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
		if (page >= pages) page = pages - 1;
		int cx = this.width / 2;
		int start = page * PER_PAGE;

		for (int i = 0; i < PER_PAGE && start + i < list.size(); i++) {
			RegistryEntry.Reference<Enchantment> ref = list.get(start + i);
			String id = ref.registryKey().getValue().toString();
			int max = Math.max(1, ref.value().getMaxLevel());
			int lv = draft.enchants.getOrDefault(id, 0);
			String label = (lv > 0 ? "\u00a7a" : "") + pretty(id) + ": " + lv;
			int col = i % 2, row = i / 2;
			addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {
				int cur = draft.enchants.getOrDefault(id, 0);
				int next = cur + 1;
				if (next > max) next = 0;
				if (next == 0) draft.enchants.remove(id);
				else draft.enchants.put(id, next);
				this.clearAndInit();
			}).dimensions(cx - 155 + col * 160, 38 + row * 24, 150, 20).build());
		}

		addDrawableChild(ButtonWidget.builder(Text.literal("< Prev"), b -> {
			if (page > 0) page--;
			this.clearAndInit();
		}).dimensions(cx - 155, 190, 70, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Clear all"), b -> {
			draft.enchants.clear();
			this.clearAndInit();
		}).dimensions(cx - 75, 190, 150, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Next >"), b -> {
			if (page < pages - 1) page++;
			this.clearAndInit();
		}).dimensions(cx + 85, 190, 70, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> this.close())
				.dimensions(cx - 75, 214, 150, 20).build());
	}

	@Override
	public void close() {
		this.client.setScreen(parent);
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.render(ctx, mouseX, mouseY, delta);
		ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFFFF);
		ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Click an enchant to raise its level"),
				this.width / 2, 24, 0xFFAAAAAA);
	}
}
