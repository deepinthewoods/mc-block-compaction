package com.blockcompaction.network.payload;

import com.blockcompaction.BlockCompactionMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * Payload carrying the selected transformation index for a specific item.
 */
public record SelectTransformationPayload(ResourceLocation itemId, ResourceLocation targetItemId, int selectionIndex) implements CustomPacketPayload {
	public static final Type<SelectTransformationPayload> TYPE =
		new Type<>(ResourceLocation.fromNamespaceAndPath(BlockCompactionMod.MOD_ID, "select_transformation"));
	public static final StreamCodec<FriendlyByteBuf, SelectTransformationPayload> STREAM_CODEC =
		StreamCodec.of((buf, payload) -> payload.write(buf), SelectTransformationPayload::new);

	public SelectTransformationPayload(FriendlyByteBuf buf) {
		this(buf.readResourceLocation(), buf.readResourceLocation(), buf.readVarInt());
	}

	public static SelectTransformationPayload from(Item hoveredItem, Item targetItem, int selectionIndex) {
		return new SelectTransformationPayload(BuiltInRegistries.ITEM.getKey(hoveredItem),
			BuiltInRegistries.ITEM.getKey(targetItem), selectionIndex);
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeResourceLocation(itemId);
		buf.writeResourceLocation(targetItemId);
		buf.writeVarInt(selectionIndex);
	}

	@Override
	public Type<SelectTransformationPayload> type() {
		return TYPE;
	}
}
