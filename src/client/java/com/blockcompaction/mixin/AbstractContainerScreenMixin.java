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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {

	/**
	 * Transform items when they're picked up from a slot with ratio-aware conversion
	 */
	@Inject(method = "slotClicked", at = @At("RETURN"))
	private void onSlotClickedReturn(Slot slot, int slotId, int mouseButton, ClickType type, CallbackInfo ci) {
		if (!BlockCompactionClient.isEnabled()) {
			com.blockcompaction.BlockCompactionMod.LOGGER.info("BlockCompaction slotClicked: mod disabled");
			return;
		}

		// Only transform on pickup (left/right click)
		if (type != ClickType.PICKUP) {
			com.blockcompaction.BlockCompactionMod.LOGGER.info("BlockCompaction slotClicked: not PICKUP type, was {}", type);
			return;
		}

		// Check if player is now carrying an item
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && mc.player.containerMenu != null) {
			ItemStack carried = mc.player.containerMenu.getCarried();
			if (!carried.isEmpty()) {
				Item sourceItem = carried.getItem();
				Item targetItem = TransformationSelectionManager.getSelectedTransformation(sourceItem);

				com.blockcompaction.BlockCompactionMod.LOGGER.info("BlockCompaction slotClicked: sourceItem={}, targetItem={}, hasTransformations={}, selectionIndex={}",
					sourceItem, targetItem, StonecutterRecipeManager.hasTransformations(sourceItem),
					TransformationSelectionManager.getSelectionIndex(sourceItem));

				if (targetItem != null && targetItem != sourceItem) {
					com.blockcompaction.BlockCompactionMod.LOGGER.info("BlockCompaction slotClicked: transforming {} to {}", sourceItem, targetItem);
					transformCarriedStack(mc, carried, sourceItem, targetItem);
				} else {
					com.blockcompaction.BlockCompactionMod.LOGGER.info("BlockCompaction slotClicked: skipping transform (targetItem={}, same={})",
						targetItem, targetItem == sourceItem);
				}
			} else {
				com.blockcompaction.BlockCompactionMod.LOGGER.info("BlockCompaction slotClicked: carried is empty");
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

	/**
	 * Handle mouse scroll selection for transformations
	 */
	@Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
	private void onMouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY, CallbackInfoReturnable<Boolean> cir) {
		if (!BlockCompactionClient.isEnabled()) {
			return;
		}

		Slot hoveredSlot = ((AbstractContainerScreenAccessor) this).getHoveredSlot();
		if (hoveredSlot == null || !hoveredSlot.hasItem()) {
			com.blockcompaction.BlockCompactionMod.LOGGER.info("BlockCompaction mouseScrolled: no hovered slot or empty");
			return;
		}

		ItemStack stack = hoveredSlot.getItem();
		Item item = stack.getItem();

		if (!StonecutterRecipeManager.hasTransformations(item)) {
			com.blockcompaction.BlockCompactionMod.LOGGER.info("BlockCompaction mouseScrolled: {} has no transformations", item);
			return;
		}

		double scrollDelta = Math.abs(scrollDeltaY) > 0.0001 ? scrollDeltaY : scrollDeltaX;
		if (Math.abs(scrollDelta) <= 0.0001) {
			com.blockcompaction.BlockCompactionMod.LOGGER.info("BlockCompaction mouseScrolled: scroll delta too small");
			return;
		}

		com.blockcompaction.BlockCompactionMod.LOGGER.info("BlockCompaction mouseScrolled: processing scroll for {}", item);
		TransformationSelectionManager.scrollSelection(item, (int) Math.signum(scrollDelta));
		cir.setReturnValue(true);
	}
}
