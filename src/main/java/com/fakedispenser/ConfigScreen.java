package com.fakedispenser;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class ConfigScreen extends Screen {
	private final BlockPos pos;
	private final Config.Entry draft;
	private TextFieldWidget itemField, nameField, countField, randomField;
	private String status = "";

	public ConfigScreen(BlockPos pos) {
		super(Text.literal("Fake Dispenser"));
		this.pos = pos;
		String dim = MinecraftClient.getInstance().world.getRegistryKey().getValue().toString();
		Config.Entry existing = Config.find(dim, pos.getX(), pos.getY(), pos.getZ());
		if (existing != null) {
			this.draft = existing.copy();
		} else {
			this.draft = new Config.Entry();
			this.draft.dimension = dim;
			this.draft.x = pos.getX();
			this.draft.y = pos.getY();
			this.draft.z = pos.getZ();
		}
	}

	private void syncDraft() {
		draft.item = itemField.getText().trim();
		draft.randomItems = new java.util.ArrayList<>();
		for (String id : randomField.getText().split(",")) if (!id.trim().isEmpty()) draft.randomItems.add(id.trim());
		draft.name = nameField.getText();
		try {
			draft.count = Integer.parseInt(countField.getText().trim());
		} catch (NumberFormatException ex) {
			draft.count = 1;
		}
	}

	@Override
	protected void init() {
		int cx = this.width / 2;

		itemField = new TextFieldWidget(this.textRenderer, cx - 100, 55, 200, 20, Text.literal("Item"));
		itemField.setMaxLength(100);
		itemField.setText(draft.item);
		addDrawableChild(itemField);

		nameField = new TextFieldWidget(this.textRenderer, cx - 100, 95, 200, 20, Text.literal("Name"));
		nameField.setMaxLength(100);
		nameField.setText(draft.name == null ? "" : draft.name);
		addDrawableChild(nameField);

		randomField = new TextFieldWidget(this.textRenderer, cx - 100, 165, 200, 20, Text.literal("Random items"));
		randomField.setMaxLength(500);
		randomField.setText(draft.randomItems == null ? "" : String.join(", ", draft.randomItems));
		addDrawableChild(randomField);

		countField = new TextFieldWidget(this.textRenderer, cx - 100, 135, 60, 20, Text.literal("Count"));
		countField.setMaxLength(2);
		countField.setText(String.valueOf(draft.count));
		addDrawableChild(countField);

		addDrawableChild(ButtonWidget.builder(Text.literal("Enchants (" + draft.enchants.size() + ")..."), b -> {
			syncDraft();
			this.client.setScreen(new EnchantScreen(this, draft));
		}).dimensions(cx - 34, 135, 134, 20).build());

		addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> save())
				.dimensions(cx - 100, 195, 98, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Remove"), b -> remove())
				.dimensions(cx + 2, 195, 98, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> this.close())
				.dimensions(cx - 100, 220, 200, 20).build());
	}

	private void save() {
		syncDraft();
		if (draft.build() == null) {
			status = "Unknown item id: " + draft.item;
			return;
		}
		Config.Entry e = draft.copy();
		Config.entries.removeIf(o -> o.at(e.dimension, e.x, e.y, e.z));
		Config.entries.add(e);
		Config.save();
		status = "Saved!";
	}

	private void remove() {
		Config.entries.removeIf(o -> o.at(draft.dimension, pos.getX(), pos.getY(), pos.getZ()));
		Config.save();
		status = "Removed.";
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.render(ctx, mouseX, mouseY, delta);
		int cx = this.width / 2;
		ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, cx, 15, 0xFFFFFFFF);
		ctx.drawCenteredTextWithShadow(this.textRenderer,
				Text.literal("Dispenser at " + pos.getX() + " " + pos.getY() + " " + pos.getZ()), cx, 30, 0xFFAAAAAA);
		ctx.drawTextWithShadow(this.textRenderer, Text.literal("Item ID (e.g. netherite_ingot)"), cx - 100, 44, 0xFFFFFFFF);
		ctx.drawTextWithShadow(this.textRenderer, Text.literal("Display name (& color codes ok)"), cx - 100, 84, 0xFFFFFFFF);
		ctx.drawTextWithShadow(this.textRenderer, Text.literal("Count"), cx - 100, 124, 0xFFFFFFFF);
		ctx.drawTextWithShadow(this.textRenderer, Text.literal("Random pool (comma-separated IDs; optional)"), cx - 100, 154, 0xFFFFFFFF);
		if (!status.isEmpty()) {
			ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(status), cx, 228, 0xFFFFFF55);
		}
	}
}
