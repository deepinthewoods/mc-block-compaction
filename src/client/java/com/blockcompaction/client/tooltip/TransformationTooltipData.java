package com.blockcompaction.client.tooltip;

import java.util.List;
import java.util.Objects;

/**
 * Aggregates tooltip grid data and where it should slot into the vanilla tooltip component list.
 *
 * @param entries     transformation entries to render
 * @param insertIndex index where the client component should be injected
 */
public record TransformationTooltipData(List<TransformationTooltipEntry> entries, int insertIndex) {

	public TransformationTooltipData {
		Objects.requireNonNull(entries, "entries");
		entries = List.copyOf(entries);
		insertIndex = Math.max(0, insertIndex);
	}
}
