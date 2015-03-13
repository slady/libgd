package info.miranda.gd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static Logger LOG = LoggerFactory.getLogger(GdUtils.class);
	
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

	/* resolution affects ttf font rendering, particularly hinting */
	public static final int GD_RESOLUTION = 96;      /* pixels per inch */

	/* Special colors. */
	public static final int SPECIAL_COLOR_STYLED = (-2);
	public static final int SPECIAL_COLOR_BRUSHED = (-3);
	public static final int SPECIAL_COLOR_STYLED_BRUSHED = (-4);
	public static final int SPECIAL_COLOR_TILED = (-5);
/* NOT the same as the transparent color index.
   This is used in line styles only. */
	public static final int SPECIAL_COLOR_TRANSPARENT = (-6);
	public static final int SPECIAL_COLOR_ANTI_ALIASED = (-7);

	public static final int GD_CMP_IMAGE		= 1;	/* Actual image IS different */
	public static final int GD_CMP_NUM_COLORS	= 2;	/* Number of Colours in pallette differ */
	public static final int GD_CMP_COLOR		= 4;	/* Image colours differ */
	public static final int GD_CMP_SIZE_X		= 8;	/* Image width differs */
	public static final int GD_CMP_SIZE_Y		= 16;	/* Image heights differ */
	public static final int GD_CMP_TRANSPARENT	= 32;	/* Transparent colour */
	public static final int GD_CMP_BACKGROUND	= 64;	/* Background colour */
	public static final int GD_CMP_INTERLACE	= 128;	/* Interlaced setting */
	public static final int GD_CMP_TRUECOLOR	= 256;	/* Truecolor vs palette differs */

	/* For backwards compatibility only. Use setStyle()
   for MUCH more flexible line drawing. Also see
   setBrush(). */
	public static final int DASH_SIZE = 4;

	private static final int COS_T[] = {
			1024,
			1023,
			1023,
			1022,
			1021,
			1020,
			1018,
			1016,
			1014,
			1011,
			1008,
			1005,
			1001,
			997,
			993,
			989,
			984,
			979,
			973,
			968,
			962,
			955,
			949,
			942,
			935,
			928,
			920,
			912,
			904,
			895,
			886,
			877,
			868,
			858,
			848,
			838,
			828,
			817,
			806,
			795,
			784,
			772,
			760,
			748,
			736,
			724,
			711,
			698,
			685,
			671,
			658,
			644,
			630,
			616,
			601,
			587,
			572,
			557,
			542,
			527,
			512,
			496,
			480,
			464,
			448,
			432,
			416,
			400,
			383,
			366,
			350,
			333,
			316,
			299,
			282,
			265,
			247,
			230,
			212,
			195,
			177,
			160,
			142,
			124,
			107,
			89,
			71,
			53,
			35,
			17,
			0,
			-17,
			-35,
			-53,
			-71,
			-89,
			-107,
			-124,
			-142,
			-160,
			-177,
			-195,
			-212,
			-230,
			-247,
			-265,
			-282,
			-299,
			-316,
			-333,
			-350,
			-366,
			-383,
			-400,
			-416,
			-432,
			-448,
			-464,
			-480,
			-496,
			-512,
			-527,
			-542,
			-557,
			-572,
			-587,
			-601,
			-616,
			-630,
			-644,
			-658,
			-671,
			-685,
			-698,
			-711,
			-724,
			-736,
			-748,
			-760,
			-772,
			-784,
			-795,
			-806,
			-817,
			-828,
			-838,
			-848,
			-858,
			-868,
			-877,
			-886,
			-895,
			-904,
			-912,
			-920,
			-928,
			-935,
			-942,
			-949,
			-955,
			-962,
			-968,
			-973,
			-979,
			-984,
			-989,
			-993,
			-997,
			-1001,
			-1005,
			-1008,
			-1011,
			-1014,
			-1016,
			-1018,
			-1020,
			-1021,
			-1022,
			-1023,
			-1023,
			-1024,
			-1023,
			-1023,
			-1022,
			-1021,
			-1020,
			-1018,
			-1016,
			-1014,
			-1011,
			-1008,
			-1005,
			-1001,
			-997,
			-993,
			-989,
			-984,
			-979,
			-973,
			-968,
			-962,
			-955,
			-949,
			-942,
			-935,
			-928,
			-920,
			-912,
			-904,
			-895,
			-886,
			-877,
			-868,
			-858,
			-848,
			-838,
			-828,
			-817,
			-806,
			-795,
			-784,
			-772,
			-760,
			-748,
			-736,
			-724,
			-711,
			-698,
			-685,
			-671,
			-658,
			-644,
			-630,
			-616,
			-601,
			-587,
			-572,
			-557,
			-542,
			-527,
			-512,
			-496,
			-480,
			-464,
			-448,
			-432,
			-416,
			-400,
			-383,
			-366,
			-350,
			-333,
			-316,
			-299,
			-282,
			-265,
			-247,
			-230,
			-212,
			-195,
			-177,
			-160,
			-142,
			-124,
			-107,
			-89,
			-71,
			-53,
			-35,
			-17,
			0,
			17,
			35,
			53,
			71,
			89,
			107,
			124,
			142,
			160,
			177,
			195,
			212,
			230,
			247,
			265,
			282,
			299,
			316,
			333,
			350,
			366,
			383,
			400,
			416,
			432,
			448,
			464,
			480,
			496,
			512,
			527,
			542,
			557,
			572,
			587,
			601,
			616,
			630,
			644,
			658,
			671,
			685,
			698,
			711,
			724,
			736,
			748,
			760,
			772,
			784,
			795,
			806,
			817,
			828,
			838,
			848,
			858,
			868,
			877,
			886,
			895,
			904,
			912,
			920,
			928,
			935,
			942,
			949,
			955,
			962,
			968,
			973,
			979,
			984,
			989,
			993,
			997,
			1001,
			1005,
			1008,
			1011,
			1014,
			1016,
			1018,
			1020,
			1021,
			1022,
			1023,
			1023
	};

	private static final int SIN_T[] = {
			0,
			17,
			35,
			53,
			71,
			89,
			107,
			124,
			142,
			160,
			177,
			195,
			212,
			230,
			247,
			265,
			282,
			299,
			316,
			333,
			350,
			366,
			383,
			400,
			416,
			432,
			448,
			464,
			480,
			496,
			512,
			527,
			542,
			557,
			572,
			587,
			601,
			616,
			630,
			644,
			658,
			671,
			685,
			698,
			711,
			724,
			736,
			748,
			760,
			772,
			784,
			795,
			806,
			817,
			828,
			838,
			848,
			858,
			868,
			877,
			886,
			895,
			904,
			912,
			920,
			928,
			935,
			942,
			949,
			955,
			962,
			968,
			973,
			979,
			984,
			989,
			993,
			997,
			1001,
			1005,
			1008,
			1011,
			1014,
			1016,
			1018,
			1020,
			1021,
			1022,
			1023,
			1023,
			1024,
			1023,
			1023,
			1022,
			1021,
			1020,
			1018,
			1016,
			1014,
			1011,
			1008,
			1005,
			1001,
			997,
			993,
			989,
			984,
			979,
			973,
			968,
			962,
			955,
			949,
			942,
			935,
			928,
			920,
			912,
			904,
			895,
			886,
			877,
			868,
			858,
			848,
			838,
			828,
			817,
			806,
			795,
			784,
			772,
			760,
			748,
			736,
			724,
			711,
			698,
			685,
			671,
			658,
			644,
			630,
			616,
			601,
			587,
			572,
			557,
			542,
			527,
			512,
			496,
			480,
			464,
			448,
			432,
			416,
			400,
			383,
			366,
			350,
			333,
			316,
			299,
			282,
			265,
			247,
			230,
			212,
			195,
			177,
			160,
			142,
			124,
			107,
			89,
			71,
			53,
			35,
			17,
			0,
			-17,
			-35,
			-53,
			-71,
			-89,
			-107,
			-124,
			-142,
			-160,
			-177,
			-195,
			-212,
			-230,
			-247,
			-265,
			-282,
			-299,
			-316,
			-333,
			-350,
			-366,
			-383,
			-400,
			-416,
			-432,
			-448,
			-464,
			-480,
			-496,
			-512,
			-527,
			-542,
			-557,
			-572,
			-587,
			-601,
			-616,
			-630,
			-644,
			-658,
			-671,
			-685,
			-698,
			-711,
			-724,
			-736,
			-748,
			-760,
			-772,
			-784,
			-795,
			-806,
			-817,
			-828,
			-838,
			-848,
			-858,
			-868,
			-877,
			-886,
			-895,
			-904,
			-912,
			-920,
			-928,
			-935,
			-942,
			-949,
			-955,
			-962,
			-968,
			-973,
			-979,
			-984,
			-989,
			-993,
			-997,
			-1001,
			-1005,
			-1008,
			-1011,
			-1014,
			-1016,
			-1018,
			-1020,
			-1021,
			-1022,
			-1023,
			-1023,
			-1024,
			-1023,
			-1023,
			-1022,
			-1021,
			-1020,
			-1018,
			-1016,
			-1014,
			-1011,
			-1008,
			-1005,
			-1001,
			-997,
			-993,
			-989,
			-984,
			-979,
			-973,
			-968,
			-962,
			-955,
			-949,
			-942,
			-935,
			-928,
			-920,
			-912,
			-904,
			-895,
			-886,
			-877,
			-868,
			-858,
			-848,
			-838,
			-828,
			-817,
			-806,
			-795,
			-784,
			-772,
			-760,
			-748,
			-736,
			-724,
			-711,
			-698,
			-685,
			-671,
			-658,
			-644,
			-630,
			-616,
			-601,
			-587,
			-572,
			-557,
			-542,
			-527,
			-512,
			-496,
			-480,
			-464,
			-448,
			-432,
			-416,
			-400,
			-383,
			-366,
			-350,
			-333,
			-316,
			-299,
			-282,
			-265,
			-247,
			-230,
			-212,
			-195,
			-177,
			-160,
			-142,
			-124,
			-107,
			-89,
			-71,
			-53,
			-35,
			-17
	};

	public static int trueColorGetAlpha(final int c) {
		return (((c) & 0x7F000000) >> 24);
	}

	public static int trueColorGetRed(final int c) {
		return (((c) & 0xFF0000) >> 16);
	}

	public static int trueColorGetGreen(final int c) {
		return (((c) & 0x00FF00) >> 8);
	}

	public static int trueColorGetBlue(final int c) {
		return ((c) & 0x0000FF);
	}

/* A simpler way to obtain an opaque truecolor value for drawing on a
   truecolor image. Not for use with palette images! */
	public static int trueColorMix(final int r, final int g, final int b) {
		return (((r) << 16) + ((g) << 8) + (b));
	}

/* Returns a truecolor value with an alpha channel component.
   ALPHA_MAX (127, **NOT 255**) is transparent, 0 is completely
   opaque. */
	public static int trueColorMixAlpha(final int r, final int g, final int b, final int a) {
		return (((a) << 24) + ((r) << 16) + ((g) << 8) + (b));
	}

	public static double DPCM2DPI(final double dpcm) {
		return ((dpcm)*2.54 + 0.5);
	}

	public static double DPM2DPI(final double dpm) {
		return ((dpm) * 0.0254 + 0.5);
	}

	public static double DPI2DPCM(final double dpi) {
		return ((dpi)/2.54 + 0.5);
	}

	public static double DPI2DPM(final double dpi) {
		return ((dpi)/0.0254 + 0.5);
	}

/*
   * gd_security.c
   *
   * Implements buffer overflow check routines.
   *
   * Written 2004, Phil Knirsch.
   * Based on netpbm fixes by Alan Cox.
   *
 */
	/* Returns nonzero if multiplying the two quantities will
		result in integer overflow. Also returns nonzero if
		either quantity is negative. By Phil Knirsch based on
		netpbm fixes by Alan Cox. */
	public static boolean overflow2(final long a, final long b) {
		if(a <= 0 || b <= 0) {
			LOG.warn("one parameter to a memory allocation multiplication is negative or zero, failing operation gracefully");
			return true;
		}

		if(a > Integer.MAX_VALUE / b) {
			LOG.warn("product of memory allocation multiplication would exceed INT_MAX, failing operation gracefully");
			return true;
		}

		return false;
	}

}
