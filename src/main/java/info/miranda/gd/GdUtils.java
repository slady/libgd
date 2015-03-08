package info.miranda.gd;

/* gd.h: declarations file for the graphic-draw module.
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose and without fee is hereby granted, provided
 * that the above copyright notice appear in all copies and that both that
 * copyright notice and this permission notice appear in supporting
 * documentation.  This software is provided "AS IS." Thomas Boutell and
 * Boutell.Com, Inc. disclaim all warranties, either express or implied,
 * including but not limited to implied warranties of merchantability and
 * fitness for a particular purpose, with respect to this code and accompanying
 * documentation. */
public class GdUtils {
	
	
/* The maximum number of palette entries in palette-based images.
   In the wonderful new world of gd 2.0, you can of course have
   many more colors when using truecolor mode. */

	public static final int MAX_COLORS = 256;

/* Image type. See functions below; you will not need to change
   the elements directly. Use the provided macros to
   access sx, sy, the color table, and colorsTotal for
   read-only purposes. */

/* If 'truecolor' is set true, the image is truecolor;
   pixels are represented by integers, which
   must be 32 bits wide or more.

   True colors are repsented as follows:
   
   ARGB
	
   Where 'A' (alpha channel) occupies only the
   LOWER 7 BITS of the MSB. This very small
   loss of alpha channel resolution allows gd 2.x
   to keep backwards compatibility by allowing
   signed integers to be used to represent colors,
   and negative numbers to represent special cases,
   just as in gd 1.x. */

	public static final int ALPHA_MAX = 127;
	public static final int ALPHA_OPAQUE = 0;
	public static final int ALPHA_TRANSPARENT = 127;
	public static final int RED_MAX = 255;
	public static final int GREEN_MAX = 255;
	public static final int BLUE_MAX = 255;

	public static final double GD_EPSILON = 1e-6;

	public static int gdTrueColorGetAlpha(final int c) {
		return (((c) & 0x7F000000) >> 24);
	}

	public static int gdTrueColorGetRed(final int c) {
		return (((c) & 0xFF0000) >> 16);
	}

	public static int gdTrueColorGetGreen(final int c) {
		return (((c) & 0x00FF00) >> 8);
	}

	public static int gdTrueColorGetBlue(final int c) {
		return ((c) & 0x0000FF);
	}

/* A simpler way to obtain an opaque truecolor value for drawing on a
   truecolor image. Not for use with palette images! */
	public static int gdTrueColor(final int r, final int g, final int b) {
		return (((r) << 16) + ((g) << 8) + (b));
	}

/* Returns a truecolor value with an alpha channel component.
   gdAlphaMax (127, **NOT 255**) is transparent, 0 is completely
   opaque. */
	public static int gdTrueColorAlpha(final int r, final int g, final int b, final int a) {
		return (((a) << 24) + ((r) << 16) + ((g) << 8) + (b));
	}

}
