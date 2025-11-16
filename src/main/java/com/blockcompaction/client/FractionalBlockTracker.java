package com.blockcompaction.client;

import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks fractional block amounts for each player.
 *
 * For example, if you pick up 1 stone slab (which is 0.5 base blocks),
 * we track that 0.5 and wait until you have enough for a full block.
 */
public final class FractionalBlockTracker {
	private static final UUID GLOBAL_CONTEXT = new UUID(0L, 0L);
	private static final Map<UUID, Map<Item, Double>> FRACTIONAL_AMOUNTS = new HashMap<>();

	private FractionalBlockTracker() {
	}

	private static UUID resolveKey(UUID ownerId) {
		return ownerId != null ? ownerId : GLOBAL_CONTEXT;
	}

	private static Map<Item, Double> getStore(UUID key, boolean create) {
		if (create) {
			return FRACTIONAL_AMOUNTS.computeIfAbsent(key, ignored -> new HashMap<>());
		}
		return FRACTIONAL_AMOUNTS.get(key);
	}

	private static void cleanupIfEmpty(UUID key, Map<Item, Double> store) {
		if (store.isEmpty()) {
			FRACTIONAL_AMOUNTS.remove(key);
		}
	}

	/**
	 * Add a fractional amount for an item type and extract whole blocks if possible.
	 *
	 * @param ownerId Player identifier (null defaults to a shared context)
	 * @param item The item type
	 * @param amount The amount in base block units
	 * @return The number of whole blocks that can be extracted
	 */
	public static int addAndExtract(UUID ownerId, Item item, double amount) {
		UUID key = resolveKey(ownerId);
		Map<Item, Double> store = getStore(key, true);

		double current = store.getOrDefault(item, 0.0);
		current += amount;

		int wholeBlocks = (int) current;
		double remainder = current - wholeBlocks;

		if (remainder > 0.0001) { // Keep small remainders
			store.put(item, remainder);
		} else {
			store.remove(item);
			cleanupIfEmpty(key, store);
		}

		return wholeBlocks;
	}

	public static int addAndExtract(Item item, double amount) {
		return addAndExtract(null, item, amount);
	}

	/**
	 * Get the current fractional amount for an item.
	 */
	public static double getFractional(UUID ownerId, Item item) {
		Map<Item, Double> store = getStore(resolveKey(ownerId), false);
		if (store == null) {
			return 0.0;
		}
		return store.getOrDefault(item, 0.0);
	}

	public static double getFractional(Item item) {
		return getFractional(null, item);
	}

	/**
	 * Clear fractional amount for an item.
	 */
	public static void clear(UUID ownerId, Item item) {
		UUID key = resolveKey(ownerId);
		Map<Item, Double> store = getStore(key, false);
		if (store == null) {
			return;
		}
		store.remove(item);
		cleanupIfEmpty(key, store);
	}

	public static void clear(Item item) {
		clear(null, item);
	}

	/**
	 * Clear all fractional amounts for a player.
	 */
	public static void clearAll(UUID ownerId) {
		FRACTIONAL_AMOUNTS.remove(resolveKey(ownerId));
	}

	/**
	 * Clear all fractional amounts for every player/context.
	 */
	public static void clearAll() {
		FRACTIONAL_AMOUNTS.clear();
	}

	/**
	 * Try to extract whole blocks from fractional amounts.
	 * Returns true if we can extract at least one whole block.
	 */
	public static boolean canExtractWhole(UUID ownerId, Item item) {
		return getFractional(ownerId, item) >= 1.0;
	}

	public static boolean canExtractWhole(Item item) {
		return canExtractWhole(null, item);
	}

	/**
	 * Extract up to maxBlocks from the fractional tracker.
	 * Returns the actual number extracted.
	 */
	public static int extract(UUID ownerId, Item item, int maxBlocks) {
		UUID key = resolveKey(ownerId);
		Map<Item, Double> store = getStore(key, false);
		if (store == null) {
			return 0;
		}

		double current = store.getOrDefault(item, 0.0);
		int extractable = Math.min((int) current, maxBlocks);

		if (extractable > 0) {
			current -= extractable;
			if (current > 0.0001) {
				store.put(item, current);
			} else {
				store.remove(item);
				cleanupIfEmpty(key, store);
			}
		}

		return extractable;
	}

	public static int extract(Item item, int maxBlocks) {
		return extract(null, item, maxBlocks);
	}
}
