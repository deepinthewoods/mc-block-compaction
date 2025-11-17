package com.blockcompaction.network;

import com.blockcompaction.BlockCompactionState;
import com.blockcompaction.client.StonecutterRecipeManager;
import com.blockcompaction.client.TransformationSelectionManager;
import com.blockcompaction.network.payload.SelectTransformationPayload;
import com.blockcompaction.network.payload.TransformSlotPayload;
import com.blockcompaction.util.SlotTransformationHelper;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

public final class BlockCompactionNetwork {
	private BlockCompactionNetwork() {
	}

	public static void registerServerReceivers() {
		PayloadTypeRegistry.playC2S().register(SelectTransformationPayload.TYPE, SelectTransformationPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TransformSlotPayload.TYPE, TransformSlotPayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(SelectTransformationPayload.TYPE, (payload, context) -> {
			context.server().execute(() -> handleSelectionPacket(context.player(), payload));
		});

		ServerPlayNetworking.registerGlobalReceiver(TransformSlotPayload.TYPE, (payload, context) -> {
			context.server().execute(() -> handleSlotTransformationPacket(context.player(), payload));
		});
	}

	private static void handleSelectionPacket(ServerPlayer player, SelectTransformationPayload payload) {
		if (player == null) {
			return;
		}
		BuiltInRegistries.ITEM.getOptional(payload.itemId()).ifPresent(hoveredItem -> {
			BuiltInRegistries.ITEM.getOptional(payload.targetItemId()).ifPresent(targetItem -> {
				TransformationSelectionManager.setSelection(player.getUUID(), hoveredItem, targetItem);
			});
		});
	}

	private static void handleSlotTransformationPacket(ServerPlayer player, TransformSlotPayload payload) {
		if (player == null || !BlockCompactionState.isEnabled()) {
			return;
		}

		if (!StonecutterRecipeManager.ensureInitialized(player.level())) {
			return;
		}

		BuiltInRegistries.ITEM.getOptional(payload.sourceItemId()).ifPresent(sourceItem -> {
			BuiltInRegistries.ITEM.getOptional(payload.targetItemId()).ifPresent(targetItem -> {
				// Validate that the transformation is legal to prevent cheating
				if (!StonecutterRecipeManager.getTransformations(sourceItem).contains(targetItem)) {
					return; // Invalid transformation - client sent bad data
				}

				// Update the player's selection so tooltip stays in sync
				TransformationSelectionManager.setSelection(player.getUUID(), sourceItem, targetItem);

				// Perform the transformation
				SlotTransformationHelper.transformSlot(player, payload.slotIndex(), sourceItem, targetItem);
			});
		});
	}
}

