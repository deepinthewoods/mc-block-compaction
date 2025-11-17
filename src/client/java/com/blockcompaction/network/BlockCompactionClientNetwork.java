package com.blockcompaction.network;

import com.blockcompaction.network.payload.SelectTransformationPayload;
import com.blockcompaction.network.payload.TransformSlotPayload;
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

	public static void sendSlotTransformation(int slotIndex, Item sourceItem, Item targetItem) {
		if (sourceItem == null || targetItem == null) {
			return;
		}

		ClientPlayNetworking.send(TransformSlotPayload.from(slotIndex, sourceItem, targetItem));
	}
}
