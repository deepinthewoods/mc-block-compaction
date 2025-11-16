package com.blockcompaction.client;

import net.minecraft.world.item.Item;

import java.util.*;

/**
 * Manages the selected transformation for each block family.
 * Selections are keyed by the family's base block so choosing an output
 * while hovering stairs also applies when interacting with slabs or full blocks.
 */
public final class TransformationSelectionManager {
	// Maps player -> (base block -> selected target item)
	private static final Map<UUID, Map<Item, Item>> selections = new HashMap<>();
	private static final UUID GLOBAL_OWNER = new UUID(0L, 0L);

	private TransformationSelectionManager() {
	}

	private static UUID resolveOwner(UUID ownerId) {
		return ownerId != null ? ownerId : GLOBAL_OWNER;
	}

	private static Map<Item, Item> getSelectionMap(UUID ownerId, boolean create) {
		UUID key = resolveOwner(ownerId);
		if (create) {
			return selections.computeIfAbsent(key, ignored -> new HashMap<>());
		}

		return selections.get(key);
	}

	private static void cleanup(UUID resolvedOwner, Map<Item, Item> map) {
		if (map.isEmpty()) {
			selections.remove(resolvedOwner);
		}
	}

	private static Item getFamilyKey(Item item) {
		return StonecutterRecipeManager.getBaseBlock(item);
	}

	private static Item getStoredTarget(UUID ownerId, Item item) {
		Map<Item, Item> map = getSelectionMap(ownerId, false);
		if (map == null) {
			return null;
		}
		Item baseKey = getFamilyKey(item);
		Item selected = map.get(baseKey);
		if (selected == null && baseKey != item) {
			selected = map.get(item);
			if (selected != null) {
				map.remove(item);
				map.put(baseKey, selected);
			}
		}
		return selected;
	}

	/**
	 * Get the raw stored target item for the given source (may be the same as the source).
	 */
	public static Item getStoredSelection(UUID ownerId, Item item) {
		return getStoredTarget(ownerId, item);
	}

	/**
	 * Get the currently selected transformation for an item.
	 * @return The selected output item, or null if no selection or no transformations
	 */
	public static Item getSelectedTransformation(UUID ownerId, Item item) {
		List<Item> transformations = StonecutterRecipeManager.getTransformations(item);
		if (transformations.isEmpty()) {
			return null;
		}

		Item selected = getStoredTarget(ownerId, item);
		if (selected == null || selected == item) {
			return null;
		}

		for (Item candidate : transformations) {
			if (candidate == selected) {
				return candidate;
			}
		}

		return null;
	}

	/**
	 * Get all transformations for an item.
	 */
	public static List<Item> getTransformations(Item item) {
		return StonecutterRecipeManager.getTransformations(item);
	}

	/**
	 * Get the current selection index for an item.
	 */
	public static int getSelectionIndex(UUID ownerId, Item item) {
		List<Item> transformations = StonecutterRecipeManager.getTransformations(item);
		if (transformations.isEmpty()) {
			return 0;
		}

		Item selected = getStoredTarget(ownerId, item);
		if (selected == null) {
			return 0;
		}

		for (int i = 0; i < transformations.size(); i++) {
			if (transformations.get(i) == selected) {
				return i;
			}
		}

		return 0;
	}

	/**
	 * Scroll the selection for an item.
	 * @param delta Positive for scroll up, negative for scroll down
	 */
	public static void scrollSelection(UUID ownerId, Item item, int delta) {
		List<Item> transformations = StonecutterRecipeManager.getTransformations(item);
		if (transformations.isEmpty()) {
			return;
		}

		int currentIndex = getSelectionIndex(ownerId, item);
		int newIndex = currentIndex - delta; // Invert delta for natural scrolling

		// Wrap around
		if (newIndex < 0) {
			newIndex = transformations.size() - 1;
		} else if (newIndex >= transformations.size()) {
			newIndex = 0;
		}

		setSelection(ownerId, item, newIndex);
	}

	/**
	 * Explicitly set the selection index for an item.
	 */
	public static void setSelection(UUID ownerId, Item item, int index) {
		List<Item> transformations = StonecutterRecipeManager.getTransformations(item);
		if (transformations.isEmpty()) {
			return;
		}

		int clampedIndex = Math.max(0, Math.min(index, transformations.size() - 1));
		Item targetItem = transformations.get(clampedIndex);
		setSelection(ownerId, item, targetItem);
	}

	/**
	 * Store an explicit target item for the selection.
	 */
	public static void setSelection(UUID ownerId, Item item, Item targetItem) {
		if (targetItem == null) {
			return;
		}

		Map<Item, Item> map = getSelectionMap(ownerId, true);
		map.put(getFamilyKey(item), targetItem);
	}

	/**
	 * Reset selection for an item.
	 */
	public static void resetSelection(UUID ownerId, Item item) {
		UUID key = resolveOwner(ownerId);
		Map<Item, Item> map = getSelectionMap(key, false);
		if (map == null) {
			return;
		}
		map.remove(getFamilyKey(item));
		cleanup(key, map);
	}

	/**
	 * Clear all selections for a specific player.
	 */
	public static void clearAll(UUID ownerId) {
		selections.remove(resolveOwner(ownerId));
	}

	/**
	 * Clear all selections for every player/context.
	 */
	public static void clearAll() {
		selections.clear();
	}
}
