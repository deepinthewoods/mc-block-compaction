package com.blockcompaction;

import com.blockcompaction.client.ModKeybinds;
import com.blockcompaction.client.StonecutterRecipeManager;
import com.blockcompaction.client.TransformationSelectionManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class BlockCompactionClient implements ClientModInitializer {
	private static boolean enabled = true;

	@Override
	public void onInitializeClient() {
		ModKeybinds.register();

		// Initialize recipe manager after resources are loaded
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.level != null && !StonecutterRecipeManager.isInitialized()) {
				StonecutterRecipeManager.initialize(client.level.getRecipeManager());
			}
		});

		BlockCompactionMod.LOGGER.info("Block Compaction client initialized!");
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void toggle() {
		enabled = !enabled;
		BlockCompactionMod.LOGGER.info("Block Compaction " + (enabled ? "enabled" : "disabled"));
	}
}
