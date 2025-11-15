package com.blockcompaction.mixin;

import com.blockcompaction.BlockCompactionClient;
import com.blockcompaction.client.FractionalBlockTracker;
import com.blockcompaction.client.StonecutterRecipeManager;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class PlayerMixin {

	/**
	 * Transform items to their base block when picked up from the ground,
	 * with fractional tracking for items like slabs
	 */
	@Inject(method = "take", at = @At("HEAD"))
	private void onTakeItem(ItemEntity itemEntity, int count, CallbackInfo ci) {
		if (!BlockCompactionClient.isEnabled()) {
			return;
		}

		ItemStack stack = itemEntity.getItem();
		if (!stack.isEmpty() && StonecutterRecipeManager.isInitialized()) {
			Item currentItem = stack.getItem();
			Item baseItem = StonecutterRecipeManager.getBaseBlock(currentItem);

			// If this item has a base block (meaning it's a derived block), transform it
			if (baseItem != currentItem) {
				transformToBaseWithFractional(itemEntity, stack, currentItem, baseItem);
			}
		}
	}

	private void transformToBaseWithFractional(ItemEntity itemEntity, ItemStack stack, Item sourceItem, Item baseItem) {
		int sourceCount = stack.getCount();

		// Calculate how many base blocks this represents
		double ratio = StonecutterRecipeManager.getConversionRatio(sourceItem, baseItem);
		double baseBlockAmount = sourceCount * ratio;

		// Add to fractional tracker and extract whole blocks
		int wholeBlocks = FractionalBlockTracker.addAndExtract(baseItem, baseBlockAmount);

		// Update the item entity with whole base blocks only
		if (wholeBlocks > 0) {
			ItemStack newStack = new ItemStack(baseItem, wholeBlocks);
			newStack.setTag(stack.getTag());
			itemEntity.setItem(newStack);
		} else {
			// No whole blocks yet, remove the item entity (fractional amount is tracked)
			itemEntity.setItem(ItemStack.EMPTY);
			itemEntity.discard();
		}
	}
}
