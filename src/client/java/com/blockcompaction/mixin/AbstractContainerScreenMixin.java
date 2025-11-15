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
	 * Transform slot contents BEFORE vanilla picks them up
	 */
	@Inject(method = "slotClicked", at = @At("HEAD"))
	private void onSlotClickedHead(Slot slot, int slotId, int mouseButton, ClickType type, CallbackInfo ci) {
		if (!BlockCompactionClient.isEnabled()) {
			return;
		}

		// Only transform on pickup (left/right click)
		if (type != ClickType.PICKUP) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.player.containerMenu == null || slot == null) {
			return;
		}

		// Only transform when picking up from a slot (not placing into one)
		ItemStack carried = mc.player.containerMenu.getCarried();
		ItemStack slotStack = slot.getItem();

		// Only transform on full pickup (slot has items, carried is empty)
		if (slotStack.isEmpty() || !carried.isEmpty()) {
			return;
		}

		Item sourceItem = slotStack.getItem();
		Item targetItem = TransformationSelectionManager.getSelectedTransformation(sourceItem);

		com.blockcompaction.BlockCompactionMod.LOGGER.info("BlockCompaction slotClicked HEAD: slot={}, sourceItem={}, targetItem={}, hasTransformations={}, selectionIndex={}",
			slotStack.getHoverName().getString(), sourceItem, targetItem,
			StonecutterRecipeManager.hasTransformations(sourceItem),
			TransformationSelectionManager.getSelectionIndex(sourceItem));

		if (targetItem != null && targetItem != sourceItem) {
			com.blockcompaction.BlockCompactionMod.LOGGER.info("BlockCompaction slotClicked HEAD: transforming slot contents {} to {}", sourceItem, targetItem);
			transformSlotStack(slot, slotStack, sourceItem, targetItem);
		}
	}

	private void transformSlotStack(Slot slot, ItemStack slotStack, Item sourceItem, Item targetItem) {
		int sourceCount = slotStack.getCount();

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

		// Replace slot contents with transformed items
		if (wholeItems > 0) {
			ItemStack newStack = new ItemStack(targetItem, wholeItems);
			newStack.applyComponents(slotStack.getComponents());
			slot.set(newStack);
			com.blockcompaction.BlockCompactionMod.LOGGER.info("BlockCompaction: transformed slot to {} x{}", targetItem, wholeItems);
		} else {
			// No whole items, clear the slot
			slot.set(ItemStack.EMPTY);
			com.blockcompaction.BlockCompactionMod.LOGGER.info("BlockCompaction: cleared slot (no whole items, stored {})", remainder);
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
