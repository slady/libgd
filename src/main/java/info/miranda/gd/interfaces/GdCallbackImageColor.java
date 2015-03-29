package info.miranda.gd.interfaces;

import info.miranda.gd.GdImage;

public interface GdCallbackImageColor {

	/**
	 * Image color callback.
	 * @param image image
	 * @param color color
	 * @return color integer
	 */
	int callbackImageColor(GdImage image, int color);

}
