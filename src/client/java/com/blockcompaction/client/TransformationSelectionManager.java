package com.blockcompaction.client;

import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the selected transformation index for each item type
 */
public class TransformationSelectionManager {
	// Maps item -> selected transformation index
	private static final Map<Item, Integer> selections = new HashMap<>();

	/**
	 * Get the currently selected transformation for an item
	 * @return The selected output item, or null if no selection or no transformations
	 */
	public static Item getSelectedTransformation(Item item) {
		List<Item> transformations = StonecutterRecipeManager.getTransformations(item);
		if (transformations.isEmpty()) {
			return null;
		}

		int index = selections.getOrDefault(item, 0);
		if (index >= 0 && index < transformations.size()) {
			return transformations.get(index);
		}

		return null;
	}

	/**
	 * Get all transformations for an item with the current selection index
	 */
	public static List<Item> getTransformations(Item item) {
		return StonecutterRecipeManager.getTransformations(item);
	}

	/**
	 * Get the current selection index for an item
	 */
	public static int getSelectionIndex(Item item) {
		return selections.getOrDefault(item, 0);
	}

	/**
	 * Scroll the selection for an item
	 * @param delta Positive for scroll up, negative for scroll down
	 */
	public static void scrollSelection(Item item, int delta) {
		List<Item> transformations = StonecutterRecipeManager.getTransformations(item);
		if (transformations.isEmpty()) {
			com.blockcompaction.BlockCompactionMod.LOGGER.info("BlockCompaction scroll: no transformations for {}", item);
			return;
		}

		int currentIndex = selections.getOrDefault(item, 0);
		int newIndex = currentIndex - delta; // Invert delta for natural scrolling

		// Wrap around
		if (newIndex < 0) {
			newIndex = transformations.size() - 1;
		} else if (newIndex >= transformations.size()) {
			newIndex = 0;
		}

		selections.put(item, newIndex);
		com.blockcompaction.BlockCompactionMod.LOGGER.info("BlockCompaction scroll: {} delta={} index {} -> {} (selected: {})",
			item, delta, currentIndex, newIndex, transformations.get(newIndex));
	}

	/**
	 * Reset selection for an item
	 */
	public static void resetSelection(Item item) {
		selections.remove(item);
	}

	/**
	 * Clear all selections
	 */
	public static void clearAll() {
		selections.clear();
	}
}
