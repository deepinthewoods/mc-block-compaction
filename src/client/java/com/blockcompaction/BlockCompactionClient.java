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

public class BlockCompactionClient implements ClientModInitializer {
	private static boolean enabled = true;
	private static boolean autoRefillEnabled = true;
	private static boolean loggedInitAttempt = false;

	@Override
	public void onInitializeClient() {
		ModKeybinds.register();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client == null) {
				return;
			}

			if (client.level != null && client.getConnection() != null) {
				if (!StonecutterRecipeManager.isInitialized()) {
					if (!loggedInitAttempt) {
						loggedInitAttempt = true;
						BlockCompactionMod.LOGGER.info("BlockCompaction: triggering stonecutter recipe initialization");
					}
					StonecutterRecipeManager.initialize();
				} else if (loggedInitAttempt) {
					loggedInitAttempt = false;
				}

				// Handle manual transformation of carried items
				if (enabled && client.player != null && client.player.containerMenu != null) {
					handleCarriedItemTransformation(client);
				}
			} else if (StonecutterRecipeManager.isInitialized()) {
				StonecutterRecipeManager.reset();
				TransformationSelectionManager.clearAll();
				FractionalBlockTracker.clearAll();
				loggedInitAttempt = false;
			}
		});

		BlockCompactionMod.LOGGER.info("Block Compaction client initialized!");
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void toggle() {
		enabled = !enabled;
		BlockCompactionMod.LOGGER.info("Block Compaction " + (enabled ? "enabled" : "disabled"));
	}

	public static boolean isAutoRefillEnabled() {
		return autoRefillEnabled;
	}

	public static void toggleAutoRefill() {
		autoRefillEnabled = !autoRefillEnabled;
		BlockCompactionMod.LOGGER.info("Auto-Refill " + (autoRefillEnabled ? "enabled" : "disabled"));
	}

	/**
	 * Handle transformation of carried items every tick
	 * This ensures transformations persist even after server inventory updates
	 */
	private static ItemStack lastCarriedStack = ItemStack.EMPTY;
	private static boolean transformationApplied = false;

	private static void handleCarriedItemTransformation(Minecraft client) {
		ItemStack carried = client.player.containerMenu.getCarried();

		// Check if carried stack changed (new pickup)
		boolean referenceChanged = carried != lastCarriedStack;
		boolean contentChanged = !ItemStack.matches(carried, lastCarriedStack);

		if (referenceChanged || contentChanged) {
			BlockCompactionMod.LOGGER.info("BlockCompaction tick: carried changed - refChanged={}, contentChanged={}, carried={}, last={}",
				referenceChanged, contentChanged,
				carried.isEmpty() ? "EMPTY" : carried.getItem() + "x" + carried.getCount(),
				lastCarriedStack.isEmpty() ? "EMPTY" : lastCarriedStack.getItem() + "x" + lastCarriedStack.getCount());
			lastCarriedStack = carried.copy(); // Use copy to avoid reference issues
			transformationApplied = false;
		}

		// Only transform once per pickup
		if (transformationApplied || carried.isEmpty()) {
			return;
		}

		Item sourceItem = carried.getItem();
		Item targetItem = TransformationSelectionManager.getSelectedTransformation(sourceItem);

		BlockCompactionMod.LOGGER.info("BlockCompaction tick: checking transform - source={}, target={}, hasTransformations={}",
			sourceItem, targetItem, StonecutterRecipeManager.hasTransformations(sourceItem));

		if (targetItem != null && targetItem != sourceItem) {
			transformCarriedStack(client, carried, sourceItem, targetItem);
			transformationApplied = true;
		}
	}

	private static void transformCarriedStack(Minecraft mc, ItemStack carried, Item sourceItem, Item targetItem) {
		int sourceCount = carried.getCount();

		// Calculate conversion ratio
		double ratio = StonecutterRecipeManager.getConversionRatio(sourceItem, targetItem);

		// Calculate total target items (as fractional amount)
		double totalTargetAmount = sourceCount * ratio;

		// Add any existing fractional amount for this target item
		double existingFractional = FractionalBlockTracker.getFractional(targetItem);
		totalTargetAmount += existingFractional;

		// Extract whole items
		int wholeItems = (int) totalTargetAmount;
		double remainder = totalTargetAmount - wholeItems;

		// Update fractional tracker
		if (remainder > 0.0001) {
			FractionalBlockTracker.clear(targetItem);
			FractionalBlockTracker.addAndExtract(targetItem, remainder);
		} else {
			FractionalBlockTracker.clear(targetItem);
		}

		// Create transformed stack with whole items only
		if (wholeItems > 0) {
			ItemStack newStack = new ItemStack(targetItem, wholeItems);
			newStack.applyComponents(carried.getComponents());
			mc.player.containerMenu.setCarried(newStack);
			lastCarriedStack = newStack;
			BlockCompactionMod.LOGGER.info("BlockCompaction tick: transformed {} x{} to {} x{}",
				sourceItem, sourceCount, targetItem, wholeItems);
		} else {
			// No whole items, clear the carried stack
			mc.player.containerMenu.setCarried(ItemStack.EMPTY);
			lastCarriedStack = ItemStack.EMPTY;
			BlockCompactionMod.LOGGER.info("BlockCompaction tick: cleared carried (no whole items, stored {})", remainder);
		}
	}
}
