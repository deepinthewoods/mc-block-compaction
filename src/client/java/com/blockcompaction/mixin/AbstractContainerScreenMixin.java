package com.blockcompaction.mixin;

import com.blockcompaction.BlockCompactionClient;
import com.blockcompaction.client.FractionalBlockTracker;
import com.blockcompaction.client.StonecutterRecipeManager;
import com.blockcompaction.client.TransformationSelectionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {

	/**
	 * Transform items when they're picked up from a slot with ratio-aware conversion
	 */
	@Inject(method = "slotClicked", at = @At("RETURN"))
	private void onSlotClickedReturn(Slot slot, int slotId, int mouseButton, ClickType type, CallbackInfo ci) {
		if (!BlockCompactionClient.isEnabled()) {
			return;
		}

		// Only transform on pickup (left/right click)
		if (type != ClickType.PICKUP) {
			return;
		}

		// Check if player is now carrying an item
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && mc.player.containerMenu != null) {
			ItemStack carried = mc.player.containerMenu.getCarried();
			if (!carried.isEmpty()) {
				Item sourceItem = carried.getItem();
				Item targetItem = TransformationSelectionManager.getSelectedTransformation(sourceItem);

				if (targetItem != null && targetItem != sourceItem) {
					transformCarriedStack(mc, carried, sourceItem, targetItem);
				}
			}
		}
	}

	private void transformCarriedStack(Minecraft mc, ItemStack carried, Item sourceItem, Item targetItem) {
		int sourceCount = carried.getCount();

		// Calculate conversion ratio
		double ratio = StonecutterRecipeManager.getConversionRatio(sourceItem, targetItem);

		// Calculate total target items (as fractional amount)
		double totalTargetAmount = sourceCount * ratio;

		// Add any existing fractional amount for this target item
		double existingFractional = FractionalBlockTracker.getFractional(targetItem);
		totalTargetAmount += existingFractional;

		// Extract whole items
		int wholeItems = (int) totalTargetAmount;
		double remainder = totalTargetAmount - wholeItems;

		// Update fractional tracker
		if (remainder > 0.0001) {
			FractionalBlockTracker.clear(targetItem);
			FractionalBlockTracker.addAndExtract(targetItem, remainder);
		} else {
			FractionalBlockTracker.clear(targetItem);
		}

		// Create transformed stack with whole items only
		if (wholeItems > 0) {
			ItemStack newStack = new ItemStack(targetItem, wholeItems);
			newStack.applyComponents(carried.getComponents());
			mc.player.containerMenu.setCarried(newStack);
		} else {
			// No whole items, clear the carried stack
			mc.player.containerMenu.setCarried(ItemStack.EMPTY);
		}
	}
}
