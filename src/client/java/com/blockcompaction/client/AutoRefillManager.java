package com.blockcompaction.client;

import com.blockcompaction.BlockCompactionMod;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Handles automatic refilling of hotbar slots when blocks are placed
 */
public class AutoRefillManager {

	/**
	 * Try to refill a hotbar slot that's running low on items
	 * Priority: identical items > base blocks > other family blocks
	 *
	 * @param player The player
	 * @param hotbarSlot The hotbar slot index (0-8)
	 * @param item The item type that needs refilling
	 */
	public static void tryRefill(Player player, int hotbarSlot, Item item) {
		if (!StonecutterRecipeManager.isInitialized()) {
			return;
		}

		Inventory inventory = player.getInventory();
		ItemStack hotbarStack = inventory.getItem(hotbarSlot);

		// Calculate how many items we need to refill to
		int maxStackSize = item.getMaxStackSize();
		int refillAmount = calculateRefillAmount(item);
		int currentCount = hotbarStack.getCount();

		if (currentCount >= refillAmount) {
			return; // Already has enough
		}

		int needed = refillAmount - currentCount;

		// Priority 1: Look for identical items in main inventory
		int found = findAndTakeIdenticalItems(inventory, item, needed, hotbarSlot);
		if (found > 0) {
			hotbarStack.grow(found);
			BlockCompactionMod.LOGGER.debug("Refilled {} with {} identical items", item, found);
			return;
		}

		// Priority 2: Try to convert from base blocks
		Item baseItem = StonecutterRecipeManager.getBaseBlock(item);
		if (baseItem != item) {
			int converted = tryConvertFromItem(inventory, baseItem, item, needed, hotbarSlot);
			if (converted > 0) {
				hotbarStack.grow(converted);
				BlockCompactionMod.LOGGER.debug("Refilled {} with {} converted from base", item, converted);
				return;
			}
		}

		// Priority 3: Try to convert from other family members
		for (Item familyMember : StonecutterRecipeManager.getTransformations(item)) {
			int converted = tryConvertFromItem(inventory, familyMember, item, needed, hotbarSlot);
			if (converted > 0) {
				hotbarStack.grow(converted);
				BlockCompactionMod.LOGGER.debug("Refilled {} with {} converted from {}", item, converted, familyMember);
				return;
			}
		}
	}

	/**
	 * Calculate the refill amount based on recipe ratios
	 * For slabs (2:1 ratio), refill to 2
	 * For normal blocks (1:1 ratio), refill to 1
	 */
	private static int calculateRefillAmount(Item item) {
		Item baseItem = StonecutterRecipeManager.getBaseBlock(item);
		if (baseItem == item) {
			return 1; // Base block, refill to 1
		}

		// Check the ratio from base to this item
		double ratio = StonecutterRecipeManager.getConversionRatio(baseItem, item);
		return (int) Math.ceil(ratio); // For slabs (ratio 2.0), this gives 2
	}

	/**
	 * Find and take identical items from inventory (excluding hotbar slot)
	 */
	private static int findAndTakeIdenticalItems(Inventory inventory, Item item, int needed, int excludeSlot) {
		int taken = 0;

		// Search main inventory (slots 9-35) and offhand (slot 40)
		for (int i = 9; i < inventory.getContainerSize(); i++) {
			if (i == excludeSlot) continue;

			ItemStack stack = inventory.getItem(i);
			if (!stack.isEmpty() && stack.getItem() == item) {
				int takeAmount = Math.min(stack.getCount(), needed - taken);
				stack.shrink(takeAmount);
				taken += takeAmount;

				if (taken >= needed) {
					break;
				}
			}
		}

		return taken;
	}

	/**
	 * Try to convert from a source item to target item
	 */
	private static int tryConvertFromItem(Inventory inventory, Item sourceItem, Item targetItem, int needed, int excludeSlot) {
		// Calculate conversion ratio
		double ratio = StonecutterRecipeManager.getConversionRatio(sourceItem, targetItem);
		if (ratio <= 0) {
			return 0;
		}

		// Calculate how many source items we need
		int sourceNeeded = (int) Math.ceil(needed / ratio);

		// Find source items in inventory
		int sourceFound = 0;
		for (int i = 9; i < inventory.getContainerSize(); i++) {
			if (i == excludeSlot) continue;

			ItemStack stack = inventory.getItem(i);
			if (!stack.isEmpty() && stack.getItem() == sourceItem) {
				int takeAmount = Math.min(stack.getCount(), sourceNeeded - sourceFound);
				sourceFound += takeAmount;

				if (sourceFound >= sourceNeeded) {
					break;
				}
			}
		}

		if (sourceFound == 0) {
			return 0;
		}

		// Calculate how many target items we can make
		int canMake = (int) (sourceFound * ratio);
		int actuallyMake = Math.min(canMake, needed);

		// Calculate how many source items to actually consume
		int sourceToConsume = (int) Math.ceil(actuallyMake / ratio);

		// Consume source items
		int remaining = sourceToConsume;
		for (int i = 9; i < inventory.getContainerSize() && remaining > 0; i++) {
			if (i == excludeSlot) continue;

			ItemStack stack = inventory.getItem(i);
			if (!stack.isEmpty() && stack.getItem() == sourceItem) {
				int takeAmount = Math.min(stack.getCount(), remaining);
				stack.shrink(takeAmount);
				remaining -= takeAmount;
			}
		}

		return actuallyMake;
	}
}
