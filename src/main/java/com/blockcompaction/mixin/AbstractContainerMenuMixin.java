package com.blockcompaction.mixin;

import com.blockcompaction.BlockCompactionMod;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {

	@Shadow
	public abstract ItemStack getCarried();

	@Shadow
	public abstract void setCarried(ItemStack stack);

	/**
	 * Intercept slot clicks on the SERVER side to transform items
	 */
	@Inject(method = "doClick", at = @At("HEAD"))
	private void onDoClickHead(int slotIndex, int button, ClickType clickType, CallbackInfo ci) {
		BlockCompactionMod.LOGGER.info("SERVER doClick: slot={}, button={}, type={}, carried={}",
			slotIndex, button, clickType,
			getCarried().isEmpty() ? "EMPTY" : getCarried().getItem() + "x" + getCarried().getCount());
	}
}
