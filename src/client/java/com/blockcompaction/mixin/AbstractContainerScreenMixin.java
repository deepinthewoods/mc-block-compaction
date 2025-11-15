package com.blockcompaction.mixin;

import com.blockcompaction.BlockCompactionClient;
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
	 * Transform items when they're picked up from a slot
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
				Item item = carried.getItem();
				Item targetItem = TransformationSelectionManager.getSelectedTransformation(item);

				if (targetItem != null && targetItem != item) {
					// Create transformed stack
					ItemStack newStack = new ItemStack(targetItem, carried.getCount());
					newStack.setTag(carried.getTag());
					mc.player.containerMenu.setCarried(newStack);
				}
			}
		}
	}
}
