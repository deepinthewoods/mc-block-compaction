package com.blockcompaction.client;

import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks fractional block amounts for items.
 *
 * For example, if you pick up 1 stone slab (which is 0.5 base blocks),
 * we track that 0.5 and wait until you have enough for a full block.
 */
public class FractionalBlockTracker {
	// Maps item type -> fractional amount (in base block units)
	private static final Map<Item, Double> fractionalAmounts = new HashMap<>();

	/**
	 * Add a fractional amount for an item type
	 * @param item The item type
	 * @param amount The amount in base block units
	 * @return The number of whole blocks that can be extracted
	 */
	public static int addAndExtract(Item item, double amount) {
		double current = fractionalAmounts.getOrDefault(item, 0.0);
		current += amount;

		int wholeBlocks = (int) current;
		double remainder = current - wholeBlocks;

		if (remainder > 0.0001) { // Keep small remainders
			fractionalAmounts.put(item, remainder);
		} else {
			fractionalAmounts.remove(item);
		}

		return wholeBlocks;
	}

	/**
	 * Get the current fractional amount for an item
	 */
	public static double getFractional(Item item) {
		return fractionalAmounts.getOrDefault(item, 0.0);
	}

	/**
	 * Clear fractional amount for an item
	 */
	public static void clear(Item item) {
		fractionalAmounts.remove(item);
	}

	/**
	 * Clear all fractional amounts
	 */
	public static void clearAll() {
		fractionalAmounts.clear();
	}

	/**
	 * Try to extract whole blocks from fractional amounts
	 * Returns true if we can extract at least one whole block
	 */
	public static boolean canExtractWhole(Item item) {
		return fractionalAmounts.getOrDefault(item, 0.0) >= 1.0;
	}

	/**
	 * Extract up to maxBlocks from the fractional tracker
	 * Returns the actual number extracted
	 */
	public static int extract(Item item, int maxBlocks) {
		double current = fractionalAmounts.getOrDefault(item, 0.0);
		int extractable = Math.min((int) current, maxBlocks);

		if (extractable > 0) {
			current -= extractable;
			if (current > 0.0001) {
				fractionalAmounts.put(item, current);
			} else {
				fractionalAmounts.remove(item);
			}
		}

		return extractable;
	}
}
