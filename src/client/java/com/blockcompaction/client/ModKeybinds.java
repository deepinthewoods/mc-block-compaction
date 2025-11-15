package com.blockcompaction.client;

import com.blockcompaction.BlockCompactionClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class ModKeybinds {
	private static KeyMapping toggleKey;

	public static void register() {
		toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.blockcompaction.toggle",
			GLFW.GLFW_KEY_B,
			"category.blockcompaction"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKey.consumeClick()) {
				BlockCompactionClient.toggle();
				if (client.player != null) {
					client.player.displayClientMessage(
						net.minecraft.network.chat.Component.literal(
							"Block Compaction: " + (BlockCompactionClient.isEnabled() ? "ON" : "OFF")
						),
						true
					);
				}
			}
		});
	}
}
