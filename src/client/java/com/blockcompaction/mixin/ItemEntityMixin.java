package com.blockcompaction.mixin;

import com.blockcompaction.BlockCompactionState;
import com.blockcompaction.client.FractionalBlockTracker;
import com.blockcompaction.client.StonecutterRecipeManager;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

	@Shadow
	public abstract ItemStack getItem();

	@Shadow
	public abstract void setItem(ItemStack stack);

	/**
	 * Transform items to their base block when picked up from the ground,
	 * with fractional tracking for items like slabs
	 */
	@Inject(method = "playerTouch", at = @At("HEAD"))
	private void onPlayerTouch(Player player, CallbackInfo ci) {
		if (!BlockCompactionState.isEnabled()) {
			return;
		}

		ItemStack stack = getItem();
		if (!stack.isEmpty() && StonecutterRecipeManager.isInitialized()) {
			Item currentItem = stack.getItem();
			Item baseItem = StonecutterRecipeManager.getBaseBlock(currentItem);

			// If this item has a base block (meaning it's a derived block), transform it
			if (baseItem != currentItem) {
				transformToBaseWithFractional((ItemEntity) (Object) this, stack, currentItem, baseItem, player);
			}
		}
	}

	private void transformToBaseWithFractional(ItemEntity itemEntity, ItemStack stack, Item sourceItem, Item baseItem, Player player) {
		UUID playerId = player.getUUID();
		int sourceCount = stack.getCount();

		// FIRST: Check if player has space in existing stacks of the source item
		// If they do, don't convert - let Minecraft handle stacking naturally
		int spaceInSourceStacks = getSpaceInPartialStacks(player, sourceItem);
		if (spaceInSourceStacks > 0) {
			// Player has space in source item stacks, don't convert
			return;
		}

		// Calculate how many base blocks this represents
		double ratio = StonecutterRecipeManager.getConversionRatio(sourceItem, baseItem);
		double baseBlockAmount = sourceCount * ratio;

		// Add any existing fractional amount
		double existingFractional = FractionalBlockTracker.getFractional(playerId, baseItem);
		double totalAmount = baseBlockAmount + existingFractional;

		// Check if player has partial stacks of the base item we can add to
		int spaceInPartialStacks = getSpaceInPartialStacks(player, baseItem);

		if (spaceInPartialStacks > 0 && totalAmount >= 1.0) {
			// We have partial stacks and at least 1 whole block to add
			int wholeBlocks = (int) totalAmount;
			int blocksToAddToStacks = Math.min(wholeBlocks, spaceInPartialStacks);

			// Add to existing stacks via the item entity (Minecraft will handle stacking)
			// The remaining blocks (if any) will be handled normally
			double remainder = totalAmount - blocksToAddToStacks;

			// Update fractional tracker with remainder
			FractionalBlockTracker.clear(playerId, baseItem);
			if (remainder > 0.0001) {
				FractionalBlockTracker.addAndExtract(playerId, baseItem, remainder);
			}

			// Set the item entity to the blocks we're adding
			ItemStack newStack = new ItemStack(baseItem, blocksToAddToStacks);
			newStack.applyComponents(stack.getComponents());
			setItem(newStack);
		} else {
			// No partial stacks or not enough for a whole block
			// Use normal fractional tracking
			FractionalBlockTracker.clear(playerId, baseItem);
			int wholeBlocks = FractionalBlockTracker.addAndExtract(playerId, baseItem, totalAmount);

			if (wholeBlocks > 0) {
				ItemStack newStack = new ItemStack(baseItem, wholeBlocks);
				newStack.applyComponents(stack.getComponents());
				setItem(newStack);
			} else {
				// No whole blocks yet, remove the item entity (fractional amount is tracked)
				setItem(ItemStack.EMPTY);
				((ItemEntity) (Object) this).discard();
			}
		}
	}

	/**
	 * Get the total space available in partial stacks of the given item
	 */
	private int getSpaceInPartialStacks(Player player, Item item) {
		int totalSpace = 0;
		int maxStackSize = new ItemStack(item).getMaxStackSize();

		// Check main inventory (iterate through all slots)
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack invStack = player.getInventory().getItem(i);
			if (!invStack.isEmpty() && invStack.getItem() == item && invStack.getCount() < maxStackSize) {
				totalSpace += (maxStackSize - invStack.getCount());
			}
		}

		return totalSpace;
	}
}
