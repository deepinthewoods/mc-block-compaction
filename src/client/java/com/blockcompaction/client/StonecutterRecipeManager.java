package com.blockcompaction.client;

import com.blockcompaction.BlockCompactionMod;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.StonecutterRecipe;

import java.util.*;

/**
 * Manages stonecutter recipes and provides transformation mappings
 */
public class StonecutterRecipeManager {
	private static boolean initialized = false;

	// Maps input item -> list of possible output items
	private static final Map<Item, List<Item>> transformations = new HashMap<>();

	// Maps input item -> base item (for reverse transformations)
	private static final Map<Item, Item> reverseTransformations = new HashMap<>();

	public static void initialize(RecipeManager recipeManager) {
		if (initialized) return;

		transformations.clear();
		reverseTransformations.clear();

		// Load all stonecutter recipes
		Collection<RecipeHolder<StonecutterRecipe>> recipes =
			recipeManager.getAllRecipesFor(RecipeType.STONECUTTING);

		for (RecipeHolder<StonecutterRecipe> holder : recipes) {
			StonecutterRecipe recipe = holder.value();

			// Get the input and output items
			ItemStack[] inputs = recipe.getIngredients().get(0).getItems();
			ItemStack output = recipe.getResultItem(null);

			if (inputs.length > 0 && !output.isEmpty()) {
				Item inputItem = inputs[0].getItem();
				Item outputItem = output.getItem();

				// Add forward transformation
				transformations.computeIfAbsent(inputItem, k -> new ArrayList<>())
					.add(outputItem);

				// Add reverse transformation (output -> input as base)
				reverseTransformations.putIfAbsent(outputItem, inputItem);
			}
		}

		// Build bidirectional transformations
		// If A -> B exists, also add B -> A
		Map<Item, List<Item>> additionalTransformations = new HashMap<>();
		for (Map.Entry<Item, List<Item>> entry : transformations.entrySet()) {
			Item input = entry.getKey();
			for (Item output : entry.getValue()) {
				additionalTransformations.computeIfAbsent(output, k -> new ArrayList<>())
					.add(input);
			}
		}

		// Merge additional transformations
		for (Map.Entry<Item, List<Item>> entry : additionalTransformations.entrySet()) {
			transformations.merge(entry.getKey(), entry.getValue(), (oldList, newList) -> {
				List<Item> merged = new ArrayList<>(oldList);
				for (Item item : newList) {
					if (!merged.contains(item)) {
						merged.add(item);
					}
				}
				return merged;
			});
		}

		initialized = true;
		BlockCompactionMod.LOGGER.info("Loaded {} stonecutter recipe groups", transformations.size());
	}

	public static boolean isInitialized() {
		return initialized;
	}

	/**
	 * Get all possible transformations for an item
	 */
	public static List<Item> getTransformations(Item item) {
		return transformations.getOrDefault(item, Collections.emptyList());
	}

	/**
	 * Check if an item has any transformations
	 */
	public static boolean hasTransformations(Item item) {
		return transformations.containsKey(item) && !transformations.get(item).isEmpty();
	}

	/**
	 * Get the base block for an item (for auto-pickup)
	 * This traces back through reverse transformations to find the "original" block
	 */
	public static Item getBaseBlock(Item item) {
		Item base = item;
		Set<Item> visited = new HashSet<>();

		// Follow reverse transformation chain to find base
		while (reverseTransformations.containsKey(base) && !visited.contains(base)) {
			visited.add(base);
			base = reverseTransformations.get(base);
		}

		return base;
	}
}
