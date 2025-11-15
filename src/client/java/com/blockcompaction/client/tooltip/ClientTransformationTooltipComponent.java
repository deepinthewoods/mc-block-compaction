package com.blockcompaction.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

/**
 * Displays a single-row strip of transformation icons with a subtle highlight on the selected entry.
 */
public class ClientTransformationTooltipComponent implements ClientTooltipComponent {

	private static final int ICON_SIZE = 16;
	private static final int CELL_PADDING = 0;
	private static final int CELL_SPACING = 0;

	private final TransformationTooltipData data;
	private final int entryCount;
	private final int cellWidth;
	private final int cellHeight;

	public ClientTransformationTooltipComponent(TransformationTooltipData data) {
		this.data = data;
		this.entryCount = data.entries().size();
		this.cellWidth = ICON_SIZE + CELL_PADDING * 2;
		this.cellHeight = ICON_SIZE + CELL_PADDING * 2;
	}

	@Override
	public int getHeight(Font font) {
		return entryCount == 0 ? 0 : cellHeight;
	}

	@Override
	public int getWidth(Font font) {
		if (entryCount == 0) {
			return 0;
		}
		return entryCount * cellWidth + (entryCount - 1) * CELL_SPACING;
	}

	@Override
	public void renderImage(Font font, int x, int y, int tooltipWidth, int tooltipHeight, GuiGraphics guiGraphics) {
		if (entryCount == 0) {
			return;
		}

		for (int index = 0; index < data.entries().size(); index++) {
			TransformationTooltipEntry entry = data.entries().get(index);
			int cellX = x + index * (cellWidth + CELL_SPACING);
			int cellY = y;

			drawSlotBackground(guiGraphics, cellX, cellY, entry.selected());

			int iconX = cellX + CELL_PADDING;
			int iconY = cellY + CELL_PADDING;
			guiGraphics.renderItem(entry.stack(), iconX, iconY);
			guiGraphics.renderItemDecorations(font, entry.stack(), iconX, iconY, "");
		}
	}

	private void drawSlotBackground(GuiGraphics guiGraphics, int x, int y, boolean selected) {
		int borderColor = selected ? 0xFFE6F3BF : 0x40101010;
		int fillColor = selected ? 0xFF303E21 : 0xFF050607;
		guiGraphics.fill(x, y, x + cellWidth, y + cellHeight, borderColor);
		guiGraphics.fill(x + 1, y + 1, x + cellWidth - 1, y + cellHeight - 1, fillColor);
	}
}
