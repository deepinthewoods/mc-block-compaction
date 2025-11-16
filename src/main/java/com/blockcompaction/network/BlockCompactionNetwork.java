package com.blockcompaction.network;

import com.blockcompaction.client.TransformationSelectionManager;
import com.blockcompaction.network.payload.SelectTransformationPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;

public final class BlockCompactionNetwork {
	private BlockCompactionNetwork() {
	}

	public static void registerServerReceivers() {
		PayloadTypeRegistry.playC2S().register(SelectTransformationPayload.TYPE, SelectTransformationPayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(SelectTransformationPayload.TYPE, (payload, context) -> {
			context.server().execute(() -> handleSelectionPacket(context.player(), payload));
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
}
