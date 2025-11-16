package com.blockcompaction.mixin;

import com.blockcompaction.BlockCompactionClient;
import com.blockcompaction.client.StonecutterRecipeManager;
import com.blockcompaction.client.TransformationSelectionManager;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {

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
