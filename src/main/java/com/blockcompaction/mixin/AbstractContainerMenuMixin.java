package com.blockcompaction.mixin;

import com.blockcompaction.BlockCompactionState;
import com.blockcompaction.client.StonecutterRecipeManager;
import com.blockcompaction.client.TransformationSelectionManager;
import com.blockcompaction.util.InventoryTransformationHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {

	@Shadow
	public abstract ItemStack getCarried();

	@Shadow
	public abstract void setCarried(ItemStack stack);

	@Unique
	private ItemStack blockcompaction$carriedBeforeClick = ItemStack.EMPTY;

	@Inject(method = "doClick", at = @At("HEAD"))
	private void blockcompaction$captureCarried(int slotIndex, int button, ClickType clickType, Player player, CallbackInfo ci) {
		this.blockcompaction$carriedBeforeClick = this.getCarried().copy();
	}

	@Inject(method = "doClick", at = @At("TAIL"))
	private void blockcompaction$applyTransform(int slotIndex, int button, ClickType clickType, Player player, CallbackInfo ci) {
		if (!BlockCompactionState.isEnabled() || !(player instanceof ServerPlayer)) {
			return;
		}

		if (!StonecutterRecipeManager.ensureInitialized(player.level())) {
			return;
		}

		ItemStack carried = this.getCarried();
		if (carried.isEmpty()) {
			return;
		}

		if (ItemStack.matches(carried, this.blockcompaction$carriedBeforeClick)) {
			return; // No change to carried stack
		}

		Item sourceItem = carried.getItem();
		Item targetItem = TransformationSelectionManager.getSelectedTransformation(player.getUUID(), sourceItem);
		if (targetItem == null || targetItem == sourceItem) {
			return;
		}

		ItemStack transformed = InventoryTransformationHelper.createTransformedStack(player.getUUID(), carried, sourceItem, targetItem);
		if (ItemStack.matches(transformed, carried)) {
			return;
		}

		this.setCarried(transformed);
	}
}
