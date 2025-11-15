package com.blockcompaction.client;

import com.blockcompaction.BlockCompactionMod;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

import java.util.*;

/**
 * Manages stonecutter recipes and provides transformation mappings with ratio tracking
 */
public class StonecutterRecipeManager {
	private static boolean initialized = false;

	// Recipe data: input -> output -> count (e.g., Stone -> Stone Slab -> 2)
	private static final Map<Item, Map<Item, Integer>> recipeData = new HashMap<>();

	// Block families: base block -> set of all blocks in the family
	private static final Map<Item, Set<Item>> blockFamilies = new HashMap<>();

	// Maps any block to its family's base block
	private static final Map<Item, Item> blockToBase = new HashMap<>();

	private static boolean waitingOnWorldLogged = false;
	private static boolean waitingOnRecipesLogged = false;
	private static boolean waitingOnDataLogged = false;
	private static boolean waitingOnFamiliesLogged = false;

	/**
	 * Reset the cached recipe data so it can be rebuilt when a new world is joined.
	 */
	public static void reset() {
		initialized = false;
		recipeData.clear();
		blockFamilies.clear();
		blockToBase.clear();
		waitingOnWorldLogged = false;
		waitingOnRecipesLogged = false;
		waitingOnDataLogged = false;
		waitingOnFamiliesLogged = false;
		BlockCompactionMod.LOGGER.info("BlockCompaction: cleared stonecutter recipe cache");
	}

	public static void initialize() {
		if (initialized) return;

		recipeData.clear();
		blockFamilies.clear();
		blockToBase.clear();

		Minecraft client = Minecraft.getInstance();
		if (client.level == null || client.getConnection() == null) {
			if (!waitingOnWorldLogged) {
				waitingOnWorldLogged = true;
				BlockCompactionMod.LOGGER.info("BlockCompaction: waiting for world/connection before loading stonecutter recipes");
			}
			return;
		}
		waitingOnWorldLogged = false;

		// Step 1: Load all stonecutter recipes with their output counts
		Map<Item, Item> directBaseMapping = new HashMap<>();
		boolean loadedRecipe = false;

		try {
			var level = client.level;
			if (level == null) {
				return;
			}

			SelectableRecipe.SingleInputSet<StonecutterRecipe> stonecutterEntries = level.recipeAccess()
				.stonecutterRecipes();
			if (stonecutterEntries == null) {
				if (!waitingOnRecipesLogged) {
					waitingOnRecipesLogged = true;
					BlockCompactionMod.LOGGER.info("BlockCompaction: waiting for level recipe data before building tooltip data");
				}
				return;
			}

			List<SelectableRecipe.SingleInputEntry<StonecutterRecipe>> entries = stonecutterEntries.entries();
			if (entries.isEmpty()) {
				if (!waitingOnRecipesLogged) {
					waitingOnRecipesLogged = true;
					BlockCompactionMod.LOGGER.info("BlockCompaction: level stonecutter recipes empty, waiting for retry");
				}
				return;
			}
			waitingOnRecipesLogged = false;

			var displayContext = SlotDisplayContext.fromLevel(level);

			for (SelectableRecipe.SingleInputEntry<StonecutterRecipe> entry : entries) {
				// Each ingredient may contain multiple possible input items (tags)
				List<Item> inputItems = entry.input().items()
					.map(Holder::value)
					.toList();

				if (inputItems.isEmpty()) {
					continue;
				}

				ItemStack output = entry.recipe()
					.optionDisplay()
					.resolveForFirstStack(displayContext);
				if (output.isEmpty()) {
					continue;
				}

				Item outputItem = output.getItem();
				int outputCount = output.getCount();

				for (Item inputItem : inputItems) {
					recipeData.computeIfAbsent(inputItem, k -> new HashMap<>())
						.put(outputItem, outputCount);
					directBaseMapping.putIfAbsent(outputItem, inputItem);
					loadedRecipe = true;
				}
			}

			if (!loadedRecipe || recipeData.isEmpty()) {
				if (!waitingOnDataLogged) {
					waitingOnDataLogged = true;
					BlockCompactionMod.LOGGER.info("BlockCompaction: stonecutter entries present (count {}) but no usable recipe data yet",
						entries.size());
				}
				return;
			}
			waitingOnDataLogged = false;
		} catch (Exception e) {
			BlockCompactionMod.LOGGER.error("Failed to initialize stonecutter recipes", e);
			return;
		}

		// Step 2: Find base blocks (blocks that aren't derived from anything)
		Set<Item> allBlocks = new HashSet<>();
		allBlocks.addAll(recipeData.keySet());
		for (Map<Item, Integer> outputs : recipeData.values()) {
			allBlocks.addAll(outputs.keySet());
		}

		// Find the ultimate base for each block
		for (Item block : allBlocks) {
			Item base = findBaseBlock(block, directBaseMapping);
			blockToBase.put(block, base);
			blockFamilies.computeIfAbsent(base, k -> new HashSet<>()).add(block);
		}

		// Step 3: Build cross-family transformations
		// For each family, any block can transform to any other block in the family
		for (Set<Item> family : blockFamilies.values()) {
			for (Item block : family) {
				// This block can transform to all other blocks in its family
				for (Item target : family) {
					if (block != target) {
						// Transformation exists through the base block
						blockToBase.put(block, blockToBase.get(block));
					}
				}
			}
		}

		if (blockFamilies.isEmpty()) {
			if (!waitingOnFamiliesLogged) {
				waitingOnFamiliesLogged = true;
				BlockCompactionMod.LOGGER.info("BlockCompaction: built recipe data but found no block families");
			}
			return;
		}
		waitingOnFamiliesLogged = false;

		initialized = true;
		waitingOnWorldLogged = false;
		waitingOnRecipesLogged = false;
		waitingOnDataLogged = false;
		waitingOnFamiliesLogged = false;
		BlockCompactionMod.LOGGER.info("Loaded {} block families with {} total blocks",
			blockFamilies.size(), allBlocks.size());
	}

	/**
	 * Find the ultimate base block by following the chain
	 */
	private static Item findBaseBlock(Item item, Map<Item, Item> directMapping) {
		Item current = item;
		Set<Item> visited = new HashSet<>();

		while (directMapping.containsKey(current) && !visited.contains(current)) {
			visited.add(current);
			current = directMapping.get(current);
		}

		return current;
	}

	public static boolean isInitialized() {
		return initialized;
	}

	/**
	 * Get all possible transformations for an item (all blocks in its family)
	 */
	public static List<Item> getTransformations(Item item) {
		Item base = blockToBase.get(item);
		if (base == null) {
			return Collections.emptyList();
		}

		Set<Item> family = blockFamilies.get(base);
		if (family == null) {
			return Collections.emptyList();
		}

		// Return all blocks except the item itself
		List<Item> result = new ArrayList<>();
		for (Item familyMember : family) {
			if (familyMember != item) {
				result.add(familyMember);
			}
		}

		return result;
	}

	/**
	 * Check if an item has any transformations
	 */
	public static boolean hasTransformations(Item item) {
		Item base = blockToBase.get(item);
		if (base == null) return false;

		Set<Item> family = blockFamilies.get(base);
		return family != null && family.size() > 1;
	}

	/**
	 * Get the base block for an item
	 */
	public static Item getBaseBlock(Item item) {
		return blockToBase.getOrDefault(item, item);
	}

	/**
	 * Calculate the ratio between two blocks.
	 * Returns how many targetItems you get from 1 sourceItem.
	 * For example: Stone -> Stone Slab = 2.0
	 *              Stone Slab -> Stone = 0.5
	 */
	public static double getConversionRatio(Item sourceItem, Item targetItem) {
		if (sourceItem == targetItem) {
			return 1.0;
		}

		Item base = getBaseBlock(sourceItem);
		if (!base.equals(getBaseBlock(targetItem))) {
			return 1.0; // Different families, no conversion
		}

		// Calculate how many base blocks 1 sourceItem represents
		double sourceToBase = getItemToBaseRatio(sourceItem);
		// Calculate how many targetItems 1 base block makes
		double baseToTarget = getBaseToItemRatio(targetItem);

		return sourceToBase * baseToTarget;
	}

	/**
	 * Get how many base blocks 1 of this item represents
	 * Stone Slab -> 0.5 (2 slabs = 1 stone)
	 * Stone -> 1.0
	 */
	private static double getItemToBaseRatio(Item item) {
		Item base = getBaseBlock(item);
		if (item.equals(base)) {
			return 1.0;
		}

		// Find the recipe: base -> item
		Map<Item, Integer> baseRecipes = recipeData.get(base);
		if (baseRecipes != null && baseRecipes.containsKey(item)) {
			int count = baseRecipes.get(item);
			return 1.0 / count; // If 1 base makes 2 items, then 1 item = 0.5 base
		}

		return 1.0; // Default if no recipe found
	}

	/**
	 * Get how many of this item 1 base block makes
	 * Stone -> Stone Slab = 2.0
	 * Stone -> Stone = 1.0
	 */
	private static double getBaseToItemRatio(Item item) {
		Item base = getBaseBlock(item);
		if (item.equals(base)) {
			return 1.0;
		}

		Map<Item, Integer> baseRecipes = recipeData.get(base);
		if (baseRecipes != null && baseRecipes.containsKey(item)) {
			return baseRecipes.get(item).doubleValue();
		}

		return 1.0;
	}
}
