package com.blockcompaction.mixin;

import com.blockcompaction.BlockCompactionClient;
import com.blockcompaction.client.AutoRefillManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

	@Shadow
	public LocalPlayer player;

	@Unique
	private ItemStack blockCompaction$previousHeldItem = ItemStack.EMPTY;

	/**
	 * Track item usage and trigger auto-refill when needed
	 */
	@Inject(method = "tick", at = @At("RETURN"))
	private void onClientTick(CallbackInfo ci) {
		if (!BlockCompactionClient.isEnabled() || !BlockCompactionClient.isAutoRefillEnabled() || player == null) {
			return;
		}

		ItemStack currentHeldItem = player.getMainHandItem();

		// Check if we just used an item (count decreased)
		if (!blockCompaction$previousHeldItem.isEmpty() && !currentHeldItem.isEmpty()) {
			if (blockCompaction$previousHeldItem.getItem() == currentHeldItem.getItem()) {
				int previousCount = blockCompaction$previousHeldItem.getCount();
				int currentCount = currentHeldItem.getCount();

				// Item was used (count decreased) and is now at 1 or 0
				if (previousCount > currentCount && currentCount <= 1) {
					Item item = currentHeldItem.getItem();
					int selectedSlot = ((InventoryAccessor) player.getInventory()).getSelected();

					// Try to refill
					AutoRefillManager.tryRefill(player, selectedSlot, item);
				}
			}
		}

		// Store current held item for next tick
		blockCompaction$previousHeldItem = currentHeldItem.copy();
	}
}
