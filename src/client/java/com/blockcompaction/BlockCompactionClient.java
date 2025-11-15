package com.blockcompaction;

import com.blockcompaction.client.FractionalBlockTracker;
import com.blockcompaction.client.ModKeybinds;
import com.blockcompaction.client.StonecutterRecipeManager;
import com.blockcompaction.client.TransformationSelectionManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class BlockCompactionClient implements ClientModInitializer {
	private static boolean enabled = true;
	private static boolean autoRefillEnabled = true;
	private static boolean loggedInitAttempt = false;

	@Override
	public void onInitializeClient() {
		ModKeybinds.register();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client == null) {
				return;
			}

			if (client.level != null && client.getConnection() != null) {
				if (!StonecutterRecipeManager.isInitialized()) {
					if (!loggedInitAttempt) {
						loggedInitAttempt = true;
						BlockCompactionMod.LOGGER.info("BlockCompaction: triggering stonecutter recipe initialization");
					}
					StonecutterRecipeManager.initialize();
				} else if (loggedInitAttempt) {
					loggedInitAttempt = false;
				}
			} else if (StonecutterRecipeManager.isInitialized()) {
				StonecutterRecipeManager.reset();
				TransformationSelectionManager.clearAll();
				FractionalBlockTracker.clearAll();
				loggedInitAttempt = false;
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

	public static boolean isAutoRefillEnabled() {
		return autoRefillEnabled;
	}

	public static void toggleAutoRefill() {
		autoRefillEnabled = !autoRefillEnabled;
		BlockCompactionMod.LOGGER.info("Auto-Refill " + (autoRefillEnabled ? "enabled" : "disabled"));
	}
}
