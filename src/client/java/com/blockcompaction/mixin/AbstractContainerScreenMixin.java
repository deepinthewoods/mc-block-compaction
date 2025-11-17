package com.blockcompaction.mixin;

import com.blockcompaction.BlockCompactionState;
import com.blockcompaction.client.StonecutterRecipeManager;
import com.blockcompaction.client.TransformationSelectionManager;
import com.blockcompaction.network.BlockCompactionClientNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {

	/**
	 * Handle mouse scroll selection for transformations.
	 * Now triggers immediate slot transformation instead of just updating selection.
	 */
	@Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
	private void onMouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY, CallbackInfoReturnable<Boolean> cir) {
		if (!BlockCompactionState.isEnabled()) {
			return;
		}

		Slot hoveredSlot = ((AbstractContainerScreenAccessor) this).getHoveredSlot();
		if (hoveredSlot == null || !hoveredSlot.hasItem()) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) {
			return;
		}
		UUID playerId = minecraft.player.getUUID();

		ItemStack stack = hoveredSlot.getItem();
		Item sourceItem = stack.getItem();

		if (!StonecutterRecipeManager.hasTransformations(sourceItem)) {
			return;
		}

		double scrollDelta = Math.abs(scrollDeltaY) > 0.0001 ? scrollDeltaY : scrollDeltaX;
		if (Math.abs(scrollDelta) <= 0.0001) {
			return;
		}

		// Update selection
		TransformationSelectionManager.scrollSelection(playerId, sourceItem, (int) Math.signum(scrollDelta));
		Item targetItem = TransformationSelectionManager.getStoredSelection(playerId, sourceItem);

		if (targetItem != null && targetItem != sourceItem) {
			// Send transformation request to server
			int slotIndex = hoveredSlot.index;
			BlockCompactionClientNetwork.sendSlotTransformation(slotIndex, sourceItem, targetItem);
		}

		cir.setReturnValue(true);
	}
}
