package com.blockcompaction;

/**
 * Holds shared feature toggles so both the client and server logic can check whether
 * Block Compaction functionality should run.
 */
public final class BlockCompactionState {
	private static boolean enabled = true;
	private static boolean autoRefillEnabled = true;

	private BlockCompactionState() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void toggleEnabled() {
		enabled = !enabled;
	}

	public static void setEnabled(boolean value) {
		enabled = value;
	}

	public static boolean isAutoRefillEnabled() {
		return autoRefillEnabled;
	}

	public static void toggleAutoRefill() {
		autoRefillEnabled = !autoRefillEnabled;
	}

	public static void setAutoRefillEnabled(boolean value) {
		autoRefillEnabled = value;
	}
}

