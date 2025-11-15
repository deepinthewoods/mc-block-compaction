package com.blockcompaction.mixin;

import com.blockcompaction.client.tooltip.ClientTransformationTooltipComponent;
import com.blockcompaction.client.tooltip.TransformationTooltipData;
import com.blockcompaction.client.tooltip.TransformationTooltipState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.Optional;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {

	@Inject(
		method = "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/resources/ResourceLocation;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphics;setTooltipForNextFrameInternal(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/ResourceLocation;Z)V"
		),
		locals = LocalCapture.CAPTURE_FAILEXCEPTION
	)
	private void blockcompaction$injectTransformationTooltip(Font font, List<Component> tooltipLines, Optional<TooltipComponent> tooltipComponent,
	                                                         int mouseX, int mouseY, ResourceLocation style, CallbackInfo ci,
	                                                         List<ClientTooltipComponent> clientComponents) {
		TransformationTooltipData tooltipData = TransformationTooltipState.consume(tooltipLines);
		if (tooltipData == null || tooltipData.entries().isEmpty()) {
			return;
		}

		int insertIndex = tooltipData.insertIndex();
		if (tooltipComponent.isPresent() && insertIndex > 1) {
			insertIndex++;
		}
		insertIndex = Mth.clamp(insertIndex, 0, clientComponents.size());
		clientComponents.add(insertIndex, new ClientTransformationTooltipComponent(tooltipData));
	}
}
