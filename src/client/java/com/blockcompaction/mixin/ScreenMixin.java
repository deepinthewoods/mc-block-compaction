package com.blockcompaction.mixin;

import com.blockcompaction.BlockCompactionClient;
import com.blockcompaction.client.FractionalBlockTracker;
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

				// Calculate conversion ratio
				double ratio = StonecutterRecipeManager.getConversionRatio(item, transformItem);
				String ratioStr = formatRatio(ratio);

				Component line;
				if (i == selectedIndex) {
					line = Component.literal("► ")
						.withStyle(ChatFormatting.GREEN)
						.append(Component.literal(new ItemStack(transformItem).getHoverName().getString())
							.withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
						.append(Component.literal(" " + ratioStr)
							.withStyle(ChatFormatting.YELLOW));
				} else {
					line = Component.literal("  ")
						.append(Component.literal(new ItemStack(transformItem).getHoverName().getString())
							.withStyle(ChatFormatting.GRAY))
						.append(Component.literal(" " + ratioStr)
							.withStyle(ChatFormatting.DARK_GRAY));
				}

				newTooltip.add(line);
			}

			// Show fractional amount if any
			double fractional = FractionalBlockTracker.getFractional(item);
			if (fractional > 0.0001) {
				newTooltip.add(Component.literal(String.format("Stored: %.2fx", fractional))
					.withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
			}

			newTooltip.add(Component.literal("Scroll to select").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
			return newTooltip;
		}

		return tooltip;
	}

	private String formatRatio(double ratio) {
		if (Math.abs(ratio - 1.0) < 0.001) {
			return "(1:1)";
		} else if (ratio > 1.0) {
			int r = (int) Math.round(ratio);
			return "(1:" + r + ")";
		} else {
			int r = (int) Math.round(1.0 / ratio);
			return "(" + r + ":1)";
		}
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
