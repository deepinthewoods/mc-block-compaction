package com.blockcompaction.mixin;

import com.blockcompaction.BlockCompactionClient;
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
	 * Transform items to their base block when picked up from the ground
	 */
	@Inject(method = "take", at = @At("HEAD"))
	private void onTakeItem(ItemEntity itemEntity, int count, CallbackInfo ci) {
		if (!BlockCompactionClient.isEnabled()) {
			return;
		}

		ItemStack stack = itemEntity.getItem();
		if (!stack.isEmpty()) {
			Item currentItem = stack.getItem();
			Item baseItem = StonecutterRecipeManager.getBaseBlock(currentItem);

			// If this item has a base block (meaning it's a derived block), transform it
			if (baseItem != currentItem && StonecutterRecipeManager.isInitialized()) {
				ItemStack newStack = new ItemStack(baseItem, stack.getCount());
				newStack.setTag(stack.getTag());
				itemEntity.setItem(newStack);
			}
		}
	}
}
