package com.blockcompaction;

import com.blockcompaction.client.FractionalBlockTracker;
import com.blockcompaction.client.TransformationSelectionManager;
import com.blockcompaction.network.BlockCompactionNetwork;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class BlockCompactionMod implements ModInitializer {
	public static final String MOD_ID = "blockcompaction";

	@Override
	public void onInitialize() {
		BlockCompactionNetwork.registerServerReceivers();

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			if (handler.player == null) {
				return;
			}
			var playerId = handler.player.getUUID();
			FractionalBlockTracker.clearAll(playerId);
			TransformationSelectionManager.clearAll(playerId);
		});

	}
}
