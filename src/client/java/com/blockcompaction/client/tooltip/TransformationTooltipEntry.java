package com.blockcompaction.client.tooltip;

import net.minecraft.world.item.ItemStack;

/**
 * Holds display information for a transformation option in the tooltip grid.
 */
public record TransformationTooltipEntry(ItemStack stack, boolean selected) {
}
