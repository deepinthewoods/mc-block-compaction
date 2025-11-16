package com.blockcompaction;

import com.blockcompaction.client.FractionalBlockTracker;
import com.blockcompaction.client.ModKeybinds;
import com.blockcompaction.client.StonecutterRecipeManager;
import com.blockcompaction.client.TransformationSelectionManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class BlockCompactionClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ModKeybinds.register();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client == null) {
				return;
			}

			if (client.level != null && client.getConnection() != null) {
				// Ensure recipe data is loaded when connected to a server
				StonecutterRecipeManager.ensureInitialized(client.level);
			} else if (StonecutterRecipeManager.isInitialized()) {
				// Clean up when disconnected from server
				StonecutterRecipeManager.reset();
				TransformationSelectionManager.clearAll();
				FractionalBlockTracker.clearAll();
			}
		});
	}
}
