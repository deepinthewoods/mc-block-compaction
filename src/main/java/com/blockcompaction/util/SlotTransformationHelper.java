package com.blockcompaction.util;

import com.blockcompaction.client.FractionalBlockTracker;
import com.blockcompaction.client.StonecutterRecipeManager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles transformation of items in inventory slots, including:
 * - 2:1 ratios (blocks → slabs) with overflow handling
 * - 1:2 ratios (slabs → blocks) pulling from multiple stacks
 */
public final class SlotTransformationHelper {
	private SlotTransformationHelper() {
	}

	/**
	 * Transform an inventory slot from source item to target item.
	 * Handles special cases for 2:1 and 1:2 ratios.
	 *
	 * @return true if transformation was successful
	 */
	public static boolean transformSlot(Player player, int slotIndex, Item sourceItem, Item targetItem) {
		if (player == null || sourceItem == null || targetItem == null) {
			return false;
		}

		Inventory inventory = player.getInventory();
		if (slotIndex < 0 || slotIndex >= inventory.getContainerSize()) {
			return false;
		}

		ItemStack slotStack = inventory.getItem(slotIndex);
		if (slotStack.isEmpty() || slotStack.getItem() != sourceItem) {
			return false; // Slot doesn't contain source item
		}

		// Calculate conversion ratio
		double ratio = StonecutterRecipeManager.getConversionRatio(sourceItem, targetItem);
		if (ratio <= 0) {
			return false; // Invalid transformation
		}

		UUID playerId = player.getUUID();

		// Check if this is a 2:1 ratio (e.g., blocks → slabs)
		if (ratio > 1.5) {
			return handleExpansionTransformation(inventory, slotIndex, slotStack, sourceItem, targetItem, ratio, playerId);
		}
		// Check if this is a 1:2 ratio (e.g., slabs → blocks)
		else if (ratio < 0.75) {
			return handleContractionTransformation(inventory, slotIndex, slotStack, sourceItem, targetItem, ratio, playerId);
		}
		// 1:1 ratio
		else {
			return handleSimpleTransformation(inventory, slotIndex, slotStack, sourceItem, targetItem, playerId);
		}
	}

	/**
	 * Handle 2:1 transformations (blocks → slabs).
	 * Creates extra stacks if there's space, otherwise skips.
	 */
	private static boolean handleExpansionTransformation(Inventory inventory, int slotIndex, ItemStack slotStack,
	                                                     Item sourceItem, Item targetItem, double ratio, UUID playerId) {
		int sourceCount = slotStack.getCount();
		double totalTargetAmount = sourceCount * ratio;

		// Add existing fractional amount
		double existingFractional = FractionalBlockTracker.getFractional(playerId, targetItem);
		totalTargetAmount += existingFractional;

		int wholeItems = (int) totalTargetAmount;
		double remainder = totalTargetAmount - wholeItems;

		if (wholeItems == 0) {
			// Not enough for even one item, just store fractional
			FractionalBlockTracker.clear(playerId, targetItem);
			FractionalBlockTracker.addAndExtract(playerId, targetItem, totalTargetAmount);
			inventory.setItem(slotIndex, ItemStack.EMPTY);
			return true;
		}

		int maxStackSize = new ItemStack(targetItem).getMaxStackSize();
		int itemsInMainStack = Math.min(wholeItems, maxStackSize);
		int overflow = wholeItems - itemsInMainStack;

		// If there's overflow, try to find space for it
		if (overflow > 0) {
			int placedOverflow = 0;

			// First, try to add to existing partial stacks of target item
			for (int i = 0; i < inventory.getContainerSize(); i++) {
				if (i == slotIndex) continue;

				ItemStack invStack = inventory.getItem(i);
				if (!invStack.isEmpty() && invStack.getItem() == targetItem && invStack.getCount() < maxStackSize) {
					int space = maxStackSize - invStack.getCount();
					int toAdd = Math.min(overflow - placedOverflow, space);
					invStack.grow(toAdd);
					placedOverflow += toAdd;

					if (placedOverflow >= overflow) {
						break;
					}
				}
			}

			// If still have overflow, try to find empty slots
			if (placedOverflow < overflow) {
				for (int i = 0; i < inventory.getContainerSize(); i++) {
					if (i == slotIndex) continue;

					ItemStack invStack = inventory.getItem(i);
					if (invStack.isEmpty()) {
						int toAdd = Math.min(overflow - placedOverflow, maxStackSize);
						ItemStack newStack = new ItemStack(targetItem, toAdd);
						// Copy components from source if compatible
						try {
							newStack.applyComponents(slotStack.getComponents());
						} catch (Exception ignored) {
						}
						inventory.setItem(i, newStack);
						placedOverflow += toAdd;

						if (placedOverflow >= overflow) {
							break;
						}
					}
				}
			}

			// If we couldn't place all overflow, don't do the transformation
			if (placedOverflow < overflow) {
				return false;
			}
		}

		// All overflow was placed (or there was none), proceed with transformation
		ItemStack newStack = new ItemStack(targetItem, itemsInMainStack);
		try {
			newStack.applyComponents(slotStack.getComponents());
		} catch (Exception ignored) {
		}
		inventory.setItem(slotIndex, newStack);

		// Update fractional tracker
		FractionalBlockTracker.clear(playerId, targetItem);
		if (remainder > 0.0001) {
			FractionalBlockTracker.addAndExtract(playerId, targetItem, remainder);
		}

		return true;
	}

	/**
	 * Handle 1:2 transformations (slabs → blocks).
	 * Pulls from all available stacks of source item.
	 */
	private static boolean handleContractionTransformation(Inventory inventory, int slotIndex, ItemStack slotStack,
	                                                       Item sourceItem, Item targetItem, double ratio, UUID playerId) {
		// Find all stacks of source item in inventory
		List<Integer> sourceSlots = new ArrayList<>();
		int totalSourceCount = 0;

		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack invStack = inventory.getItem(i);
			if (!invStack.isEmpty() && invStack.getItem() == sourceItem) {
				sourceSlots.add(i);
				totalSourceCount += invStack.getCount();
			}
		}

		// Calculate how many target items we can make
		double existingFractional = FractionalBlockTracker.getFractional(playerId, targetItem);
		double totalTargetAmount = totalSourceCount * ratio + existingFractional;
		int wholeItems = (int) totalTargetAmount;
		double remainder = totalTargetAmount - wholeItems;

		if (wholeItems == 0) {
			// Not enough for even one item, just store fractional and clear the slot
			FractionalBlockTracker.clear(playerId, targetItem);
			FractionalBlockTracker.addAndExtract(playerId, targetItem, totalTargetAmount);
			inventory.setItem(slotIndex, ItemStack.EMPTY);
			return true;
		}

		// Calculate how many source items we need to consume for the whole items
		int sourceNeeded = (int) Math.ceil(wholeItems / ratio);
		sourceNeeded = Math.min(sourceNeeded, totalSourceCount);

		// Remove source items from all stacks
		int remaining = sourceNeeded;
		for (int slot : sourceSlots) {
			if (remaining <= 0) break;

			ItemStack invStack = inventory.getItem(slot);
			int toRemove = Math.min(remaining, invStack.getCount());
			invStack.shrink(toRemove);
			if (invStack.isEmpty()) {
				inventory.setItem(slot, ItemStack.EMPTY);
			}
			remaining -= toRemove;
		}

		// Create the transformed stack in the original slot
		int maxStackSize = new ItemStack(targetItem).getMaxStackSize();
		int itemsInSlot = Math.min(wholeItems, maxStackSize);

		ItemStack newStack = new ItemStack(targetItem, itemsInSlot);
		try {
			newStack.applyComponents(slotStack.getComponents());
		} catch (Exception ignored) {
		}
		inventory.setItem(slotIndex, newStack);

		// Update fractional tracker
		FractionalBlockTracker.clear(playerId, targetItem);
		if (remainder > 0.0001) {
			FractionalBlockTracker.addAndExtract(playerId, targetItem, remainder);
		}

		// If there are extra target items that don't fit in the slot, try to place them
		int overflow = wholeItems - itemsInSlot;
		if (overflow > 0) {
			// Try to add to existing stacks or empty slots
			for (int i = 0; i < inventory.getContainerSize(); i++) {
				if (overflow <= 0) break;
				if (i == slotIndex) continue;

				ItemStack invStack = inventory.getItem(i);
				if (invStack.isEmpty()) {
					int toAdd = Math.min(overflow, maxStackSize);
					inventory.setItem(i, new ItemStack(targetItem, toAdd));
					overflow -= toAdd;
				} else if (invStack.getItem() == targetItem && invStack.getCount() < maxStackSize) {
					int space = maxStackSize - invStack.getCount();
					int toAdd = Math.min(overflow, space);
					invStack.grow(toAdd);
					overflow -= toAdd;
				}
			}
		}

		return true;
	}

	/**
	 * Handle 1:1 transformations (simple conversions).
	 */
	private static boolean handleSimpleTransformation(Inventory inventory, int slotIndex, ItemStack slotStack,
	                                                  Item sourceItem, Item targetItem, UUID playerId) {
		int sourceCount = slotStack.getCount();

		// For 1:1, just replace the item
		ItemStack newStack = new ItemStack(targetItem, sourceCount);
		try {
			newStack.applyComponents(slotStack.getComponents());
		} catch (Exception ignored) {
		}
		inventory.setItem(slotIndex, newStack);

		return true;
	}
}
