package com.blockcompaction.client.tooltip;

import net.minecraft.network.chat.Component;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks tooltip data so we can attach client components after vanilla composes the tooltip line list.
 */
public final class TransformationTooltipState {

	private static final Map<List<Component>, TransformationTooltipData> ACTIVE = new IdentityHashMap<>();

	private TransformationTooltipState() {
	}

	public static void attach(List<Component> tooltipLines, TransformationTooltipData data) {
		if (tooltipLines == null || data == null || data.entries().isEmpty()) {
			return;
		}
		ACTIVE.put(tooltipLines, data);
	}

	public static TransformationTooltipData consume(List<Component> tooltipLines) {
		if (tooltipLines == null) {
			return null;
		}
		return ACTIVE.remove(tooltipLines);
	}
}
