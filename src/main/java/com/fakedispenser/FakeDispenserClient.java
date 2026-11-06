package com.fakedispenser;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.DispenserBlock;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.lwjgl.glfw.GLFW;

public class FakeDispenserClient implements ClientModInitializer {
	public static KeyBinding openKey;
	public static KeyBinding toggleKey;
	public static boolean enabled = true;

	@Override
	public void onInitializeClient() {
		Config.load();

		openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.fakedispenser.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, KeyBinding.Category.MISC));

		toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.fakedispenser.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, KeyBinding.Category.MISC));

		// Press K while looking at a dispenser -> open the menu for it
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKey.wasPressed()) {
				enabled = !enabled;
			}
			while (openKey.wasPressed()) {
				if (client.world == null || client.player == null || client.currentScreen != null) continue;
				if (client.crosshairTarget instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
					BlockPos p = hit.getBlockPos();
					if (client.world.getBlockState(p).getBlock() instanceof DispenserBlock) {
						client.setScreen(new ConfigScreen(p.toImmutable()));
						return;
					}
				}
				client.player.sendMessage(Text.literal("Look at a dispenser and press K"), true);
			}
		});

		// When an item entity appears right in front of a configured dispenser, swap what it looks like
		ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (!(entity instanceof ItemEntity itemEntity)) return;
			if (!enabled || Config.entries.isEmpty()) return;
			String dim = world.getRegistryKey().getValue().toString();
			for (Config.Entry e : Config.entries) {
				if (!e.dimension.equals(dim)) continue;
				BlockPos dp = new BlockPos(e.x, e.y, e.z);
				BlockState st = world.getBlockState(dp);
				if (!(st.getBlock() instanceof DispenserBlock)) continue;
				Direction facing = st.get(DispenserBlock.FACING);
				BlockPos front = dp.offset(facing);
				double dx = itemEntity.getX() - (front.getX() + 0.5);
				double dy = itemEntity.getY() - (front.getY() + 0.5);
				double dz = itemEntity.getZ() - (front.getZ() + 0.5);
				if (dx * dx + dy * dy + dz * dz >= 1.0) continue;
				ItemStack fake = e.build();
				if (fake == null) continue;
				((FakeStackHolder) itemEntity).fakedispenser$setFake(fake);
				if (e.name != null && !e.name.isEmpty()) {
					itemEntity.setCustomName(Text.literal(e.name.replace('&', '\u00a7')));
				}
				return;
			}
		});
	}
}
