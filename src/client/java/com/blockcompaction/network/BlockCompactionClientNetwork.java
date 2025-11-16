package com.blockcompaction.network;

import com.blockcompaction.network.payload.SelectTransformationPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.item.Item;

public final class BlockCompactionClientNetwork {
	private BlockCompactionClientNetwork() {
	}

	public static void sendSelectionUpdate(Item hoveredItem, Item targetItem, int index) {
		if (hoveredItem == null || targetItem == null) {
			return;
		}

		ClientPlayNetworking.send(SelectTransformationPayload.from(hoveredItem, targetItem, index));
	}
}
