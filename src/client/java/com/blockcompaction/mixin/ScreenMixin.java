package com.blockcompaction.mixin;

import com.blockcompaction.BlockCompactionClient;
import com.blockcompaction.client.StonecutterRecipeManager;
import com.blockcompaction.client.TransformationSelectionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(Screen.class)
public abstract class ScreenMixin {

	/**
	 * Add transformation info to item tooltips
	 */
	@ModifyVariable(
		method = "getTooltipFromItem",
		at = @At(value = "RETURN"),
		ordinal = 0
	)
	private List<Component> addTransformationTooltip(List<Component> tooltip, ItemStack stack) {
		if (!BlockCompactionClient.isEnabled() || stack.isEmpty()) {
			return tooltip;
		}

		Item item = stack.getItem();
		List<Item> transformations = TransformationSelectionManager.getTransformations(item);

		if (!transformations.isEmpty()) {
			List<Component> newTooltip = new ArrayList<>(tooltip);
			int selectedIndex = TransformationSelectionManager.getSelectionIndex(item);

			newTooltip.add(Component.empty());
			newTooltip.add(Component.literal("Transformations:").withStyle(ChatFormatting.GOLD));

			for (int i = 0; i < transformations.size(); i++) {
				Item transformItem = transformations.get(i);
				Component line;

				if (i == selectedIndex) {
					line = Component.literal("► ")
						.withStyle(ChatFormatting.GREEN)
						.append(Component.literal(new ItemStack(transformItem).getHoverName().getString())
							.withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
				} else {
					line = Component.literal("  ")
						.append(Component.literal(new ItemStack(transformItem).getHoverName().getString())
							.withStyle(ChatFormatting.GRAY));
				}

				newTooltip.add(line);
			}

			newTooltip.add(Component.literal("Scroll to select").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
			return newTooltip;
		}

		return tooltip;
	}

	/**
	 * Handle mouse scroll for selection
	 */
	@Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
	private void onMouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
		if (!BlockCompactionClient.isEnabled()) {
			return;
		}

		Screen screen = (Screen) (Object) this;
		if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
			return;
		}

		Slot hoveredSlot = ((AbstractContainerScreenAccessor) containerScreen).getHoveredSlot();
		if (hoveredSlot != null && hoveredSlot.hasItem()) {
			ItemStack stack = hoveredSlot.getItem();
			Item item = stack.getItem();

			if (StonecutterRecipeManager.hasTransformations(item)) {
				TransformationSelectionManager.scrollSelection(item, (int) Math.signum(scrollY));
				cir.setReturnValue(true);
			}
		}
	}
}
