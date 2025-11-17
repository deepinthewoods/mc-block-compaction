package com.blockcompaction.network.payload;

import com.blockcompaction.BlockCompactionMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * Payload requesting transformation of a specific inventory slot.
 */
public record TransformSlotPayload(int slotIndex, ResourceLocation sourceItemId, ResourceLocation targetItemId) implements CustomPacketPayload {
	public static final Type<TransformSlotPayload> TYPE =
		new Type<>(ResourceLocation.fromNamespaceAndPath(BlockCompactionMod.MOD_ID, "transform_slot"));
	public static final StreamCodec<FriendlyByteBuf, TransformSlotPayload> STREAM_CODEC =
		StreamCodec.of((buf, payload) -> payload.write(buf), TransformSlotPayload::new);

	public TransformSlotPayload(FriendlyByteBuf buf) {
		this(buf.readVarInt(), buf.readResourceLocation(), buf.readResourceLocation());
	}

	public static TransformSlotPayload from(int slotIndex, Item sourceItem, Item targetItem) {
		return new TransformSlotPayload(slotIndex,
			BuiltInRegistries.ITEM.getKey(sourceItem),
			BuiltInRegistries.ITEM.getKey(targetItem));
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(slotIndex);
		buf.writeResourceLocation(sourceItemId);
		buf.writeResourceLocation(targetItemId);
	}

	@Override
	public Type<TransformSlotPayload> type() {
		return TYPE;
	}
}
