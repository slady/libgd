package info.miranda.gd.enums;

public enum GdImageColorType {
	/**
	 * Palette-based images, with no more than 256 colors.
	 */
	PALETTE_BASED_COLOR,
	/**
	 * Truecolor images, with an essentially unlimited number of colors.
	 * Truecolor images are always filled with black at creation
	 * time. There is no concept of a "background" color index.
	 */
	TRUE_COLOR,
}
