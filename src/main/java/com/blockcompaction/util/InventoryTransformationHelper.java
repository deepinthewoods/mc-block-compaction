package com.blockcompaction.util;

import com.blockcompaction.client.FractionalBlockTracker;
import com.blockcompaction.client.StonecutterRecipeManager;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Shared logic for transforming slot contents between block variants.
 */
public final class InventoryTransformationHelper {
	private InventoryTransformationHelper() {
	}

	public static boolean transformSlotStack(UUID ownerId, Slot slot, ItemStack slotStack, Item sourceItem, Item targetItem) {
		if (slot == null || slotStack.isEmpty()) {
			return false;
		}

		ItemStack transformed = createTransformedStack(ownerId, slotStack, sourceItem, targetItem);
		if (transformed.isEmpty()) {
			slot.set(ItemStack.EMPTY);
		} else {
			slot.set(transformed);
		}
		return true;
	}

	/**
	 * Create a transformed stack for the given carried/slot stack.
	 */
	public static ItemStack createTransformedStack(UUID ownerId, ItemStack sourceStack, Item sourceItem, Item targetItem) {
		if (sourceStack == null || sourceStack.isEmpty()) {
			return ItemStack.EMPTY;
		}

		int sourceCount = sourceStack.getCount();

		// Calculate conversion ratio
		double ratio = StonecutterRecipeManager.getConversionRatio(sourceItem, targetItem);

		// Calculate total target items (as fractional amount)
		double totalTargetAmount = sourceCount * ratio;

		// Add any existing fractional amount for this target item
		double existingFractional = FractionalBlockTracker.getFractional(ownerId, targetItem);
		totalTargetAmount += existingFractional;

		// Extract whole items
		int wholeItems = (int) totalTargetAmount;
		double remainder = totalTargetAmount - wholeItems;

		// Update fractional tracker
		FractionalBlockTracker.clear(ownerId, targetItem);
		if (remainder > 0.0001) {
			FractionalBlockTracker.addAndExtract(ownerId, targetItem, remainder);
		}

		// Replace slot contents with transformed items
		if (wholeItems > 0) {
			ItemStack newStack = new ItemStack(targetItem, wholeItems);
			newStack.applyComponents(sourceStack.getComponents());
			return newStack;
		}

		// No whole items, clear the stack
		return ItemStack.EMPTY;
	}
}
