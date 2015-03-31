package info.miranda.gd.utils;

public class GdRect {
	public int x, y;
	public int width, height;

	String gdDumpRect(final String msg) {
		return String.format("%s (%i, %i) (%i, %i)\n", msg, x, y, width, height);
	}

}
