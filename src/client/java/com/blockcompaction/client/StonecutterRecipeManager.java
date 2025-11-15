package com.blockcompaction.client;

import com.blockcompaction.BlockCompactionMod;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.StonecutterRecipe;

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

	public static void initialize(RecipeManager recipeManager) {
		if (initialized) return;

		recipeData.clear();
		blockFamilies.clear();
		blockToBase.clear();

		// Step 1: Load all stonecutter recipes with their output counts
		Collection<RecipeHolder<StonecutterRecipe>> recipes =
			recipeManager.getAllRecipesFor(RecipeType.STONECUTTING);

		Map<Item, Item> directBaseMapping = new HashMap<>();

		for (RecipeHolder<StonecutterRecipe> holder : recipes) {
			StonecutterRecipe recipe = holder.value();

			ItemStack[] inputs = recipe.getIngredients().get(0).getItems();
			ItemStack output = recipe.getResultItem(null);

			if (inputs.length > 0 && !output.isEmpty()) {
				Item inputItem = inputs[0].getItem();
				Item outputItem = output.getItem();
				int outputCount = output.getCount();

				// Store recipe data
				recipeData.computeIfAbsent(inputItem, k -> new HashMap<>())
					.put(outputItem, outputCount);

				// Track that outputItem derives from inputItem
				directBaseMapping.putIfAbsent(outputItem, inputItem);
			}
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

		initialized = true;
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
