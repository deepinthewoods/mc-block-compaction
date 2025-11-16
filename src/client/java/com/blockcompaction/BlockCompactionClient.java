package com.blockcompaction;

import com.blockcompaction.client.FractionalBlockTracker;
import com.blockcompaction.client.ModKeybinds;
import com.blockcompaction.client.StonecutterRecipeManager;
import com.blockcompaction.client.TransformationSelectionManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import com.blockcompaction.util.InventoryTransformationHelper;

import java.util.UUID;

public class BlockCompactionClient implements ClientModInitializer {
	private static boolean enabled = true;
	private static boolean autoRefillEnabled = true;

	@Override
	public void onInitializeClient() {
		ModKeybinds.register();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client == null) {
				return;
			}

			if (client.level != null && client.getConnection() != null) {
				boolean ready = StonecutterRecipeManager.ensureInitialized(client.level);

				// Handle manual transformation of carried items
				if (ready && enabled && client.player != null && client.player.containerMenu != null) {
					handleCarriedItemTransformation(client);
				}
			} else if (StonecutterRecipeManager.isInitialized()) {
				StonecutterRecipeManager.reset();
				TransformationSelectionManager.clearAll();
				FractionalBlockTracker.clearAll();
			}
		});
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void toggle() {
		enabled = !enabled;
	}

	public static boolean isAutoRefillEnabled() {
		return autoRefillEnabled;
	}

	public static void toggleAutoRefill() {
		autoRefillEnabled = !autoRefillEnabled;
	}

	/**
	 * Handle transformation of carried items every tick
	 * This ensures transformations persist even after server inventory updates
	 */
	private static ItemStack lastCarriedStack = ItemStack.EMPTY;
	private static boolean transformationApplied = false;

	private static void handleCarriedItemTransformation(Minecraft client) {
		UUID playerId = client.player.getUUID();
		ItemStack carried = client.player.containerMenu.getCarried();

		// Check if carried stack changed (new pickup)
		boolean referenceChanged = carried != lastCarriedStack;
		boolean contentChanged = !ItemStack.matches(carried, lastCarriedStack);

		if (referenceChanged || contentChanged) {
			lastCarriedStack = carried.copy(); // Use copy to avoid reference issues
			transformationApplied = false;
		}

		// Only transform once per pickup
		if (transformationApplied || carried.isEmpty()) {
			return;
		}

		Item sourceItem = carried.getItem();
		Item targetItem = TransformationSelectionManager.getSelectedTransformation(playerId, sourceItem);

		if (targetItem != null && targetItem != sourceItem) {
			transformCarriedStack(client, carried, sourceItem, targetItem, playerId);
			transformationApplied = true;
		}
	}

	private static void transformCarriedStack(Minecraft mc, ItemStack carried, Item sourceItem, Item targetItem, UUID playerId) {
		ItemStack transformed = InventoryTransformationHelper.createTransformedStack(playerId, carried, sourceItem, targetItem);

		if (!transformed.isEmpty()) {
			mc.player.containerMenu.setCarried(transformed);
			lastCarriedStack = transformed;
		} else {
			// No whole items, clear the carried stack
			mc.player.containerMenu.setCarried(ItemStack.EMPTY);
			lastCarriedStack = ItemStack.EMPTY;
		}
	}
}
