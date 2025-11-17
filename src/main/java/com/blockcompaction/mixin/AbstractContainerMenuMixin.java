package com.blockcompaction.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;

/**
 * This mixin previously handled transformation of carried items on inventory clicks.
 * Transformations now happen immediately when scrolling through options in the tooltip.
 * Keeping the mixin file for potential future use.
 */
@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {
	// No longer transforming on clicks - transformations happen on scroll
}
