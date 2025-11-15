package com.blockcompaction.mixin;

import com.blockcompaction.BlockCompactionClient;
import com.blockcompaction.BlockCompactionMod;
import com.blockcompaction.client.FractionalBlockTracker;
import com.blockcompaction.client.StonecutterRecipeManager;
import com.blockcompaction.client.TransformationSelectionManager;
import com.blockcompaction.client.tooltip.TransformationTooltipData;
import com.blockcompaction.client.tooltip.TransformationTooltipEntry;
import com.blockcompaction.client.tooltip.TransformationTooltipState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(Screen.class)
public abstract class ScreenMixin {

	/**
	 * Add transformation info to item tooltips
	 */
	@Inject(method = "getTooltipFromItem", at = @At("RETURN"), cancellable = true)
	private static void addTransformationTooltip(Minecraft minecraft, ItemStack stack, CallbackInfoReturnable<List<Component>> cir) {
        List<Component> tooltip = cir.getReturnValue();
        if (tooltip == null) {
            BlockCompactionMod.LOGGER.info("BlockCompaction tooltip skipped: base tooltip missing");
            return;
        }
        if (!BlockCompactionClient.isEnabled()) {
            BlockCompactionMod.LOGGER.info("BlockCompaction tooltip skipped: mod disabled");
            return;
        }
        if (stack.isEmpty()) {
            BlockCompactionMod.LOGGER.info("BlockCompaction tooltip skipped: empty stack");
            return;
        }

        if (!StonecutterRecipeManager.isInitialized()) {
            BlockCompactionMod.LOGGER.info("BlockCompaction tooltip skipped: recipe manager not initialized for {}", stack.getHoverName().getString());
            return;
        }

        Item item = stack.getItem();
        List<Item> transformations = TransformationSelectionManager.getTransformations(item);

        if (transformations.isEmpty()) {
            BlockCompactionMod.LOGGER.info("BlockCompaction tooltip skipped: no transformations for {}", stack.getHoverName().getString());
            return;
        }

        int selectedIndex = TransformationSelectionManager.getSelectionIndex(item);
        BlockCompactionMod.LOGGER.info("BlockCompaction tooltip: {} -> {} transformations (selected {}), initialized={}, tooltipBaseSize={}",
            stack.getHoverName().getString(), transformations.size(), selectedIndex, StonecutterRecipeManager.isInitialized(), tooltip.size());

        List<Component> newTooltip = new ArrayList<>(tooltip);
        List<TransformationTooltipEntry> entries = new ArrayList<>(transformations.size());

        int iconInsertIndex = newTooltip.size();

        for (int i = 0; i < transformations.size(); i++) {
            Item transformItem = transformations.get(i);
            entries.add(new TransformationTooltipEntry(new ItemStack(transformItem), i == selectedIndex));
        }

        double fractional = FractionalBlockTracker.getFractional(item);
        if (fractional > 0.0001) {
            newTooltip.add(Component.literal(String.format("Stored: %.2fx", fractional))
                .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
        }

        TransformationTooltipState.attach(newTooltip, new TransformationTooltipData(entries, iconInsertIndex));
        cir.setReturnValue(newTooltip);
	}
}
