package info.miranda.gd;

/*
   Group: Types

   typedef: gdImage

   typedef: gdImagePtr

   The data structure in which gd stores images. <gdImageCreate>,
   <gdImageCreateTrueColor> and the various image file-loading functions
   return a pointer to this type, and the other functions expect to
   receive a pointer to this type as their first argument.

   *gdImagePtr* is a pointer to *gdImage*.

   (Previous versions of this library encouraged directly manipulating
   the contents ofthe struct but we are attempting to move away from
   this practice so the fields are no longer documented here.  If you
   need to poke at the internals of this struct, feel free to look at
   *gd.h*.)
*/
public class GdImage {
	/* Palette-based image pixels */
	int[][] pixels;
	int sx;
	int sy;
	/* These are valid in palette images only. See also
	   'alpha', which appears later in the structure to
	   preserve binary backwards compatibility */
	int colorsTotal;
	int[] red = new int[GdUtils.MAX_COLORS];
	int[] green = new int[GdUtils.MAX_COLORS];
	int[] blue = new int[GdUtils.MAX_COLORS];
	boolean[] open = new boolean[GdUtils.MAX_COLORS];
	/* For backwards compatibility, this is set to the
	   first palette entry with 100% transparency,
	   and is also set and reset by the
	   gdImageColorTransparent function. Newer
	   applications can allocate palette entries
	   with any desired level of transparency; however,
	   bear in mind that many viewers, notably
	   many web browsers, fail to implement
	   full alpha channel for PNG and provide
	   support for full opacity or transparency only. */
	int transparent;
	int[] polyInts;
	int polyAllocated;
	GdImage brush;
	GdImage tile;
	int[] brushColorMap = new int[GdUtils.MAX_COLORS];
	int[] tileColorMap = new int[GdUtils.MAX_COLORS];
	int styleLength;
	int stylePos;
	int[] style;
	int interlace;
	/* New in 2.0: thickness of line. Initialized to 1. */
	int thick;
	/* New in 2.0: alpha channel for palettes. Note that only
	   Macintosh Internet Explorer and (possibly) Netscape 6
	   really support multiple levels of transparency in
	   palettes, to my knowledge, as of 2/15/01. Most
	   common browsers will display 100% opaque and
	   100% transparent correctly, and do something
	   unpredictable and/or undesirable for levels
	   in between. TBB */
	int[] alpha = new int[GdUtils.MAX_COLORS];
	/* Truecolor flag and pixels. New 2.0 fields appear here at the
	   end to minimize breakage of existing object code. */
	boolean trueColor;
	int[][] tpixels;
	/* Should alpha channel be copied, or applied, each time a
	   pixel is drawn? This applies to truecolor images only.
	   No attempt is made to alpha-blend in palette images,
	   even if semitransparent palette entries exist.
	   To do that, build your image as a truecolor image,
	   then quantize down to 8 bits. */
	GdEffect alphaBlendingFlag;
	/* Should the alpha channel of the image be saved? This affects
	   PNG at the moment; other future formats may also
	   have that capability. JPEG doesn't. */
	int saveAlphaFlag;

	/* There should NEVER BE ACCESSOR MACROS FOR ITEMS BELOW HERE, so this
	   part of the structure can be safely changed in new releases. */

	/* 2.0.12: anti-aliased globals. 2.0.26: just a few vestiges after
	  switching to the fast, memory-cheap implementation from PHP-gd. */
	int AA;
	int AA_color;
	int AA_dont_blend;

	/* 2.0.12: simple clipping rectangle. These values
	  must be checked for safety when set; please use
	  gdImageSetClip */
	int cx1;
	int cy1;
	int cx2;
	int cy2;

	/* 2.1.0: allows to specify resolution in dpi */
	int res_x;
	int res_y;

	/* Selects quantization method, see gdImageTrueColorToPaletteSetMethod() and gdPaletteQuantizationMethod enum. */
	int paletteQuantizationMethod;
	/* speed/quality trade-off. 1 = best quality, 10 = best speed. 0 = method-specific default.
	   Applicable to GD_QUANT_LIQ and GD_QUANT_NEUQUANT. */
	int paletteQuantizationSpeed;
	/* Image will remain true-color if conversion to palette cannot achieve given quality.
	   Value from 1 to 100, 1 = ugly, 100 = perfect. Applicable to GD_QUANT_LIQ.*/
	int paletteQuantizationMinQuality;
	/* Image will use minimum number of palette colors needed to achieve given quality. Must be higher than paletteQuantizationMinQuality
	   Value from 1 to 100, 1 = ugly, 100 = perfect. Applicable to GD_QUANT_LIQ.*/
	int paletteQuantizationMaxQuality;
	GdInterpolationMethod interpolation_id;
//	interpolation_method interpolation;

	private static final int gdCosT[] = {
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

	private static final int gdSinT[] = {
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

	/* 2.0.12: this now checks the clipping rectangle */
	private boolean boundsSafeMacro(final int x, final int y) {
		return (!(((y < cy1) || (y > cy2)) || ((x < cx1) || (x > cx2))));
	}

	/**
	 * @param sx The image width.
	 * @param sy The image height.
	 * @param colorType GdImageColorType image color type, either pallete or true color.
	 */
	public GdImage(final int sx, final int sy, final GdImageColorType colorType) {
		pixels = new int[sy][sx];
		this.sx = sx;
		this.sy = sy;
		transparent = (-1);
		interlace = 0;
		thick = 1;
		AA = 0;
		cx1 = 0;
		cy1 = 0;
		cx2 = sx - 1;
		cy2 = sy - 1;
		res_x = GdUtils.GD_RESOLUTION;
		res_y = GdUtils.GD_RESOLUTION;
//		interpolation = NULL;
		interpolation_id = GdInterpolationMethod.GD_BILINEAR_FIXED;

		switch (colorType) {
			case TRUE_COLOR:
				trueColor = true;
	/* 2.0.2: alpha blending is now on by default, and saving of alpha is
	   off by default. This allows font antialiasing to work as expected
	   on the first try in JPEGs -- quite important -- and also allows
	   for smaller PNGs when saving of alpha channel is not really
	   desired, which it usually isn't! */
				saveAlphaFlag = 0;
				alphaBlendingFlag = GdEffect.ALPHA_BLEND;
				break;

			case PALETTE_BASED_COLOR:
				colorsTotal = 0;
				for (int i = 0; (i < GdUtils.MAX_COLORS); i++) {
					open[i] = true;
				};
				trueColor = false;
				break;

			default:
				throw new IllegalArgumentException("unsupported image color type: " + colorType);
		}
	}

	public int getPixel(final int x, final int y) {
		if (boundsSafeMacro(x, y)) {
			if (trueColor) {
				return tpixels[y][x];
			} else {
				return pixels[y][x];
			}
		} else {
			return 0;
		}
	}

	public int getTrueColorPixel(final int x, final int y) {
		int p = getPixel(x, y);
		if (!trueColor) {
			return GdUtils.gdTrueColorAlpha(red[p], green[p], blue[p],
					(transparent == p) ? GdUtils.ALPHA_TRANSPARENT : alpha[p]);
		} else {
			return p;
		}
	}

	/* This function accepts truecolor pixel values only. The
     source color is composited with the destination color
     based on the alpha channel value of the source color.
     The resulting color is opaque. */

/* Thanks to Frank Warmerdam for this superior implementation
	of gdAlphaBlend(), which merges alpha in the
	destination color much better. */

	private int gdAlphaBlend(final int dst, final int src) {
		int src_alpha = GdUtils.gdTrueColorGetAlpha(src);
		int dst_alpha, alpha, red, green, blue;
		int src_weight, dst_weight, tot_weight;

	/* -------------------------------------------------------------------- */
	/*      Simple cases we want to handle fast.                            */
	/* -------------------------------------------------------------------- */
		if (src_alpha == GdUtils.ALPHA_OPAQUE)
			return src;

		dst_alpha = GdUtils.gdTrueColorGetAlpha(dst);
		if (src_alpha == GdUtils.ALPHA_TRANSPARENT)
			return dst;
		if (dst_alpha == GdUtils.ALPHA_TRANSPARENT)
			return src;

	/* -------------------------------------------------------------------- */
	/*      What will the source and destination alphas be?  Note that      */
	/*      the destination weighting is substantially reduced as the       */
	/*      overlay becomes quite opaque.                                   */
	/* -------------------------------------------------------------------- */
		src_weight = GdUtils.ALPHA_TRANSPARENT - src_alpha;
		dst_weight = (GdUtils.ALPHA_TRANSPARENT - dst_alpha) * src_alpha / GdUtils.ALPHA_MAX;
		tot_weight = src_weight + dst_weight;

	/* -------------------------------------------------------------------- */
	/*      What red, green and blue result values will we use?             */
	/* -------------------------------------------------------------------- */
		alpha = src_alpha * dst_alpha / GdUtils.ALPHA_MAX;

		red = (GdUtils.gdTrueColorGetRed(src) * src_weight
				+ GdUtils.gdTrueColorGetRed(dst) * dst_weight) / tot_weight;
		green = (GdUtils.gdTrueColorGetGreen(src) * src_weight
				+ GdUtils.gdTrueColorGetGreen(dst) * dst_weight) / tot_weight;
		blue = (GdUtils.gdTrueColorGetBlue(src) * src_weight
				+ GdUtils.gdTrueColorGetBlue(dst) * dst_weight) / tot_weight;

	/* -------------------------------------------------------------------- */
	/*      Return merged result.                                           */
	/* -------------------------------------------------------------------- */
		return ((alpha << 24) + (red << 16) + (green << 8) + blue);
	}

	private int gdLayerOverlay(final int dst, final int src) {
		int a1, a2;
		a1 = GdUtils.ALPHA_MAX - GdUtils.gdTrueColorGetAlpha(dst);
		a2 = GdUtils.ALPHA_MAX - GdUtils.gdTrueColorGetAlpha(src);
		return (((GdUtils.ALPHA_MAX - a1 * a2 / GdUtils.ALPHA_MAX) << 24) +
				(gdAlphaOverlayColor(GdUtils.gdTrueColorGetRed(src),
						GdUtils.gdTrueColorGetRed(dst), GdUtils.RED_MAX) << 16) +
				(gdAlphaOverlayColor(GdUtils.gdTrueColorGetGreen(src),
						GdUtils.gdTrueColorGetGreen(dst), GdUtils.GREEN_MAX) << 8) +
				(gdAlphaOverlayColor(GdUtils.gdTrueColorGetBlue(src),
						GdUtils.gdTrueColorGetBlue(dst), GdUtils.BLUE_MAX)));
	}

	/* Apply 'overlay' effect - background pixels are colourised by the foreground colour */
	private int gdAlphaOverlayColor(final int src, int dst, final int max) {
		dst = dst << 1;
		if( dst > max ) {
		/* in the "light" zone */
			return dst + (src << 1) - (dst * src / max) - max;
		} else {
		/* in the "dark" zone */
			return dst * src / max;
		}
	}


	/* Apply 'multiply' effect */
	private int gdLayerMultiply(final int dst, final int src) {
		int a1, a2, r1, r2, g1, g2, b1, b2;
		a1 = GdUtils.ALPHA_MAX - GdUtils.gdTrueColorGetAlpha(src);
		a2 = GdUtils.ALPHA_MAX - GdUtils.gdTrueColorGetAlpha(dst);

		r1 = GdUtils.RED_MAX - (a1 * (GdUtils.RED_MAX - GdUtils.gdTrueColorGetRed(src))) / GdUtils.ALPHA_MAX;
		r2 = GdUtils.RED_MAX - (a2 * (GdUtils.RED_MAX - GdUtils.gdTrueColorGetRed(dst))) / GdUtils.ALPHA_MAX;
		g1 = GdUtils.GREEN_MAX - (a1 * (GdUtils.GREEN_MAX - GdUtils.gdTrueColorGetGreen(src))) / GdUtils.ALPHA_MAX;
		g2 = GdUtils.GREEN_MAX - (a2 * (GdUtils.GREEN_MAX - GdUtils.gdTrueColorGetGreen(dst))) / GdUtils.ALPHA_MAX;
		b1 = GdUtils.BLUE_MAX - (a1 * (GdUtils.BLUE_MAX - GdUtils.gdTrueColorGetBlue(src))) / GdUtils.ALPHA_MAX;
		b2 = GdUtils.BLUE_MAX - (a2 * (GdUtils.BLUE_MAX - GdUtils.gdTrueColorGetBlue(dst))) / GdUtils.ALPHA_MAX ;

		a1 = GdUtils.ALPHA_MAX - a1;
		a2 = GdUtils.ALPHA_MAX - a2;
		return (((a1*a2/GdUtils.ALPHA_MAX) << 24) +
				((r1*r2/GdUtils.RED_MAX) << 16) +
				((g1*g2/GdUtils.GREEN_MAX) << 8) +
				((b1*b2/GdUtils.BLUE_MAX)));
	}


	/* Replaces or blends with the background depending on the
	   most recent call to gdImageAlphaBlending and the
	   alpha channel value of 'color'; default is to overwrite.
	   Tiling and line styling are also implemented
	   here. All other gd drawing functions pass through this call,
	   allowing for many useful effects.
	   Overlay and multiply effects are used when gdImageAlphaBlending
	   is passed gdEffectOverlay and gdEffectMultiply */
	public void setPixel(final int x, final int y, final int color) {
		int p;
		switch (color) {
			case GdUtils.SPECIAL_COLOR_STYLED:
				if (style == null) {
			/* Refuse to draw if no style is set. */
					return;
				} else {
					p = style[stylePos++];
				}
				if (p != (GdUtils.SPECIAL_COLOR_TRANSPARENT)) {
					setPixel(x, y, p);
				}
				stylePos = stylePos % styleLength;
				break;
			case GdUtils.SPECIAL_COLOR_STYLED_BRUSHED:
				if (style == null) {
			/* Refuse to draw if no style is set. */
					return;
				}
				p = style[stylePos++];
				if ((p != GdUtils.SPECIAL_COLOR_TRANSPARENT) && (p != 0)) {
					setPixel(x, y, GdUtils.SPECIAL_COLOR_BRUSHED);
				}
				stylePos = stylePos % styleLength;
				break;
			case GdUtils.SPECIAL_COLOR_BRUSHED:
				brushApply(x, y);
				break;
			case GdUtils.SPECIAL_COLOR_TILED:
				tileApply(x, y);
				break;
			case GdUtils.SPECIAL_COLOR_ANTI_ALIASED:
		/* This shouldn't happen (2.0.26) because we just call
		  gdImageAALine now, but do something sane. */
				setPixel(x, y, AA_color);
				break;
			default:
				if (boundsSafeMacro(x, y)) {
					if (trueColor) {
						switch (alphaBlendingFlag) {
							default:
							case REPLACE:
								tpixels[y][x] = color;
								break;
							case ALPHA_BLEND:
							case NORMAL:
								tpixels[y][x] = gdAlphaBlend(tpixels[y][x], color);
								break;
							case OVERLAY:
								tpixels[y][x] = gdLayerOverlay(tpixels[y][x], color);
								break;
							case MULTIPLY:
								tpixels[y][x] = gdLayerMultiply(tpixels[y][x], color);
								break;
						}
					} else {
						pixels[y][x] = color;
					}
				}
				break;
		}
	}

	private void brushApply(final int x, final int y) {
		int x1, y1, x2, y2;
		int srcx, srcy;
		if (brush == null) {
			return;
		}
		y1 = y - (brush.sy / 2);
		y2 = y1 + brush.sy;
		x1 = x - (brush.sx / 2);
		x2 = x1 + brush.sx;
		srcy = 0;
		if (trueColor) {
			if (brush.trueColor) {
				for (int ly = y1; (ly < y2); ly++) {
					srcx = 0;
					for (int lx = x1; (lx < x2); lx++) {
						int p;
						p = brush.getTrueColorPixel(srcx, srcy);
					/* 2.0.9, Thomas Winzig: apply simple full transparency */
						if (p != brush.transparent) {
							setPixel(lx, ly, p);
						}
						srcx++;
					}
					srcy++;
				}
			} else {
			/* 2.0.12: Brush palette, image truecolor (thanks to Thorben Kundinger
			   for pointing out the issue) */
				for (int ly = y1; (ly < y2); ly++) {
					srcx = 0;
					for (int lx = x1; (lx < x2); lx++) {
						int p, tc;
						p = getPixel(srcx, srcy);
						tc = getTrueColorPixel(srcx, srcy);
					/* 2.0.9, Thomas Winzig: apply simple full transparency */
						if (p != brush.transparent) {
							setPixel(lx, ly, tc);
						}
						srcx++;
					}
					srcy++;
				}
			}
		} else {
			for (int ly = y1; (ly < y2); ly++) {
				srcx = 0;
				for (int lx = x1; (lx < x2); lx++) {
					int p;
					p = getPixel(srcx, srcy);
				/* Allow for non-square brushes! */
					if (p != brush.transparent) {
					/* Truecolor brush. Very slow
					   on a palette destination. */
						if (brush.trueColor) {
							setPixel(lx, ly,
									colorResolveAlpha(
											GdUtils.gdTrueColorGetRed(p),
											GdUtils.gdTrueColorGetGreen(p),
											GdUtils.gdTrueColorGetBlue(p),
											GdUtils.gdTrueColorGetAlpha(p)));
						} else {
							setPixel(lx, ly, brushColorMap[p]);
						}
					}
					srcx++;
				}
				srcy++;
			}
		}
	}

	void tileApply(int x, int y) {
		int srcx, srcy;
		int p;
		if (tile == null) {
			return;
		}
		srcx = x % tile.sx;
		srcy = y % tile.sy;
		if (trueColor) {
			p = tile.getPixel(srcx, srcy);
			if (p != tile.transparent) {
				if (!tile.trueColor) {
					p = GdUtils.gdTrueColorAlpha(tile.red[p], tile.green[p], tile.blue[p], tile.alpha[p]);
				}
				setPixel(x, y, p);
			}
		} else {
			p = tile.getPixel(srcx, srcy);
		/* Allow for transparency */
			if (p != tile.transparent) {
				if (tile.trueColor) {
				/* Truecolor tile. Very slow
				   on a palette destination. */
					setPixel(x, y,
							colorResolveAlpha(
									GdUtils.gdTrueColorGetRed(p),
									GdUtils.gdTrueColorGetGreen(p),
									GdUtils.gdTrueColorGetBlue(p),
									GdUtils.gdTrueColorGetAlpha(p)));
				} else {
					setPixel(x, y, tileColorMap[p]);
				}
			}
		}
	}


/*
 * gdImageColorResolve is an alternative for the code fragment:
 *
 *      if ((color=gdImageColorExact(im,R,G,B)) < 0)
 *        if ((color=gdImageColorAllocate(im,R,G,B)) < 0)
 *          color=gdImageColorClosest(im,R,G,B);
 *
 * in a single function.    Its advantage is that it is guaranteed to
 * return a color index in one search over the color table.
 */

	private int gdImageColorResolve(int r, int g, int b) {
		return colorResolveAlpha(r, g, b, GdUtils.ALPHA_OPAQUE);
	}

	private int colorResolveAlpha(int r, int g, int b, int a) {
		int c;
		int ct = -1;
		int op = -1;
		long rd, gd, bd, ad, dist;
		long mindist = 4 * 255 * 255;	/* init to max poss dist */
		if (trueColor) {
			return GdUtils.gdTrueColorAlpha(r, g, b, a);
		}

		for (c = 0; c < colorsTotal; c++) {
			if (open[c]) {
				op = c;		/* Save open slot */
				continue;		/* Color not in use */
			}
			if (c == transparent) {
			/* don't ever resolve to the color that has
			 * been designated as the transparent color */
				continue;
			}
			rd = (long) (red[c] - r);
			gd = (long) (green[c] - g);
			bd = (long) (blue[c] - b);
			ad = (long) (alpha[c] - a);
			dist = rd * rd + gd * gd + bd * bd + ad * ad;
			if (dist < mindist) {
				if (dist == 0) {
					return c;		/* Return exact match color */
				}
				mindist = dist;
				ct = c;
			}
		}
	/* no exact match.  We now know closest, but first try to allocate exact */
		if (op == -1) {
			op = colorsTotal;
			if (op == GdUtils.MAX_COLORS) {
			/* No room for more colors */
				return ct;		/* Return closest available color */
			}
			colorsTotal++;
		}
		red[op] = r;
		green[op] = g;
		blue[op] = b;
		alpha[op] = a;
		open[op] = false;
		return op;			/* Return newly allocated color */
	}

	/* Bresenham as presented in Foley & Van Dam */
	public void line(int x1, int y1, int x2, int y2, final int color) {
		int dx, dy, incr1, incr2, d, x, y, xend, yend, xdirflag, ydirflag;
		int wid;
		int w, wstart;

		if (color == GdUtils.SPECIAL_COLOR_ANTI_ALIASED) {
		/*
		  gdAntiAliased passed as color: use the much faster, much cheaper
		  and equally attractive gdImageAALine implementation. That
		  clips too, so don't clip twice.
		*/
			AALine(x1, y1, x2, y2, AA_color);
			return;
		}
	/* 2.0.10: Nick Atty: clip to edges of drawing rectangle, return if no
	   points need to be drawn. 2.0.26, TBB: clip to edges of clipping
	   rectangle. We were getting away with this because gdImageSetPixel
	   is used for actual drawing, but this is still more efficient and opens
	   the way to skip per-pixel bounds checking in the future. */

		final RectangleClip clip1 = new RectangleClip(x1, y1, x2, y2);
		if (clip1.clip_1d(cx1, cx2) == 0)
			return;
		x1 = clip1.x0;
		y1 = clip1.y0;
		x2 = clip1.x1;
		y2 = clip1.y1;

		final RectangleClip clip2 = new RectangleClip(y1, x1, y2, x2);
		if (clip2.clip_1d(cy1, cy2) == 0)
			return;
		y1 = clip2.x0;
		x1 = clip2.y0;
		y2 = clip2.x1;
		x2 = clip2.y1;

		dx = Math.abs(x2 - x1);
		dy = Math.abs(y2 - y1);

		if (dx == 0) {
			vLine(x1, y1, y2, color);
			return;
		} else if (dy == 0) {
			hLine(y1, x1, x2, color);
			return;
		}

		if (dy <= dx) {
		/* More-or-less horizontal. use wid for vertical stroke */
		/* Doug Claar: watch out for NaN in atan2 (2.0.5) */
			if ((dx == 0) && (dy == 0)) {
				wid = 1;
			} else {
			/* 2.0.12: Michael Schwartz: divide rather than multiply;
			   TBB: but watch out for /0! */
				double ac = Math.cos(Math.atan2(dy, dx));
				if (ac != 0) {
					wid = (int) (thick / ac);
				} else {
					wid = 1;
				}
				if (wid == 0) {
					wid = 1;
				}
			}
			d = 2 * dy - dx;
			incr1 = 2 * dy;
			incr2 = 2 * (dy - dx);
			if (x1 > x2) {
				x = x2;
				y = y2;
				ydirflag = (-1);
				xend = x1;
			} else {
				x = x1;
				y = y1;
				ydirflag = 1;
				xend = x2;
			}

		/* Set up line thickness */
			wstart = y - wid / 2;
			for (w = wstart; w < wstart + wid; w++)
				setPixel(x, w, color);

			if (((y2 - y1) * ydirflag) > 0) {
				while (x < xend) {
					x++;
					if (d < 0) {
						d += incr1;
					} else {
						y++;
						d += incr2;
					}
					wstart = y - wid / 2;
					for (w = wstart; w < wstart + wid; w++)
						setPixel(x, w, color);
				}
			} else {
				while (x < xend) {
					x++;
					if (d < 0) {
						d += incr1;
					} else {
						y--;
						d += incr2;
					}
					wstart = y - wid / 2;
					for (w = wstart; w < wstart + wid; w++)
						setPixel(x, w, color);
				}
			}
		} else {
		/* More-or-less vertical. use wid for horizontal stroke */
		/* 2.0.12: Michael Schwartz: divide rather than multiply;
		   TBB: but watch out for /0! */
			double as = Math.sin(Math.atan2(dy, dx));
			if (as != 0) {
				wid = (int) (thick / as);
			} else {
				wid = 1;
			}
			if (wid == 0)
				wid = 1;

			d = 2 * dx - dy;
			incr1 = 2 * dx;
			incr2 = 2 * (dx - dy);
			if (y1 > y2) {
				y = y2;
				x = x2;
				yend = y1;
				xdirflag = (-1);
			} else {
				y = y1;
				x = x1;
				yend = y2;
				xdirflag = 1;
			}

		/* Set up line thickness */
			wstart = x - wid / 2;
			for (w = wstart; w < wstart + wid; w++)
				setPixel(w, y, color);

			if (((x2 - x1) * xdirflag) > 0) {
				while (y < yend) {
					y++;
					if (d < 0) {
						d += incr1;
					} else {
						x++;
						d += incr2;
					}
					wstart = x - wid / 2;
					for (w = wstart; w < wstart + wid; w++)
						setPixel(w, y, color);
				}
			} else {
				while (y < yend) {
					y++;
					if (d < 0) {
						d += incr1;
					} else {
						x--;
						d += incr2;
					}
					wstart = x - wid / 2;
					for (w = wstart; w < wstart + wid; w++)
						setPixel(w, y, color);
				}
			}
		}
	}

	class RectangleClip {
		int x0, y0, x1, y1;
		RectangleClip(final int x0, final int y0, final int x1, final int y1) {
			this.x0 = x0;
			this.y0 = y0;
			this.x1 = x1;
			this.y1 = y1;
		}


/* 2.0.10: before the drawing routines, some code to clip points that are
 * outside the drawing window.  Nick Atty (nick@canalplan.org.uk)
 *
 * This is the Sutherland Hodgman Algorithm, as implemented by
 * Duvanenko, Robbins and Gyurcsik - SH(DRG) for short.  See Dr Dobb's
 * Journal, January 1996, pp107-110 and 116-117
 *
 * Given the end points of a line, and a bounding rectangle (which we
 * know to be from (0,0) to (SX,SY)), adjust the endpoints to be on
 * the edges of the rectangle if the line should be drawn at all,
 * otherwise return a failure code */

/* this does "one-dimensional" clipping: note that the second time it
   is called, all the x parameters refer to height and the y to width
   - the comments ignore this (if you can understand it when it's
   looking at the X parameters, it should become clear what happens on
   the second call!)  The code is simplified from that in the article,
   as we know that gd images always start at (0,0) */

/* 2.0.26, TBB: we now have to respect a clipping rectangle, it won't
	necessarily start at 0. */

		private int clip_1d(final int mindim, final int maxdim)
		{
			double m;			/* gradient of line */
			if (x0 < mindim) {
		/* start of line is left of window */
				if (x1 < mindim)		/* as is the end, so the line never cuts the window */
					return 0;
				m = (y1 - y0) / (double) (x1 - x0);	/* calculate the slope of the line */
		/* adjust x0 to be on the left boundary (ie to be zero), and y0 to match */
				y0 -= (int)(m * (x0 - mindim));
				x0 = mindim;
		/* now, perhaps, adjust the far end of the line as well */
				if (x1 > maxdim) {
					y1 += m * (maxdim - x1);
					x1 = maxdim;
				}
				return 1;
			}
			if (x0 > maxdim) {
		/* start of line is right of window -
		complement of above */
				if (x1 > maxdim)		/* as is the end, so the line misses the window */
					return 0;
				m = (y1 - y0) / (double) (x1 - x0);	/* calculate the slope of the line */
				y0 += (int)(m * (maxdim - x0));	/* adjust so point is on the right
							   boundary */
				x0 = maxdim;
		/* now, perhaps, adjust the end of the line */
				if (x1 < mindim) {
					y1 -= (int)(m * (x1 - mindim));
					x1 = mindim;
				}
				return 1;
			}
	/* the final case - the start of the line is inside the window */
			if (x1 > maxdim) {
		/* other end is outside to the right */
				m = (y1 - y0) / (double) (x1 - x0);	/* calculate the slope of the line */
				y1 += (int)(m * (maxdim - x1));
				x1 = maxdim;
				return 1;
			}
			if (x1 < mindim) {
		/* other end is outside to the left */
				m = (y1 - y0) / (double) (x1 - x0);	/* calculate the slope of the line */
				y1 -= (int)(m * (x1 - mindim));
				x1 = mindim;
				return 1;
			}
	/* only get here if both points are inside the window */
			return 1;
		}
	}

	private void vLine(final int x, int y1, int y2, final int col) {
		if (thick > 1) {
			int thickhalf = thick >> 1;
			filledRectangle(x - thickhalf, y1, x + thick - thickhalf - 1, y2, col);
		} else {
			if (y2 < y1) {
				int t = y1;
				y1 = y2;
				y2 = t;
			}

			for (; y1 <= y2; y1++) {
				setPixel(x, y1, col);
			}
		}
		return;
	}

	private void hLine(int y, int x1, int x2, final int col)
	{
		if (thick > 1) {
			int thickhalf = thick >> 1;
			filledRectangle(x1, y - thickhalf, x2, y + thick - thickhalf - 1, col);
		} else {
			if (x2 < x1) {
				int t = x2;
				x2 = x1;
				x1 = t;
			}

			for (; x1 <= x2; x1++) {
				setPixel(x1, y, col);
			}
		}
		return;
	}


	private void AALine(int x1, int y1, int x2, int y2, final int col) {
	/* keep them as 32bits */
		long x, y, inc, frac;
		long dx, dy,tmp;
		int w, wid, wstart;

		if (!trueColor) {
		/* TBB: don't crash when the image is of the wrong type */
			line(x1, y1, x2, y2, col);
			return;
		}

	/* TBB: use the clipping rectangle */
		final RectangleClip clip1 = new RectangleClip(x1, y1, x2, y2);
		if (clip1.clip_1d(cx1, cx2) == 0)
			return;
		x1 = clip1.x0;
		y1 = clip1.y0;
		x2 = clip1.x1;
		y2 = clip1.y1;

		final RectangleClip clip2 = new RectangleClip(y1, x1, y2, x2);
		if (clip2.clip_1d(cy1, cy2) == 0)
			return;
		y1 = clip2.x0;
		x1 = clip2.y0;
		y2 = clip2.x1;
		x2 = clip2.y1;

		dx = x2 - x1;
		dy = y2 - y1;

		if (dx == 0 && dy == 0) {
		/* TBB: allow setting points */
			setAAPixelColor(x1, y1, col, 0xFF);
			return;
		} else {
			double ag;
			ag = (Math.abs(dy) < Math.abs(dx)) ? Math.cos(Math.atan2(dy, dx)) : Math.sin(Math.atan2(dy, dx));
			if (ag != 0) {
				wid = (int) Math.abs(thick / ag);
			} else {
				wid = 1;
			}
			if (wid == 0) {
				wid = 1;
			}
		}

	/* Axis aligned lines */
		if (dx == 0) {
			vLine(x1, y1, y2, col);
			return;
		} else if (dy == 0) {
			hLine(y1, x1, x2, col);
			return;
		}

		if (Math.abs(dx) > Math.abs(dy)) {
			if (dx < 0) {
				tmp = x1;
				x1 = x2;
				x2 = (int) tmp;
				tmp = y1;
				y1 = y2;
				y2 = (int) tmp;
				dx = x2 - x1;
				dy = y2 - y1;
			}
			y = y1;
			inc = (dy * 65536) / dx;
			frac = 0;
		/* TBB: set the last pixel for consistency (<=) */
			for (x = x1 ; x <= x2 ; x++) {
				wstart = (int) (y - wid / 2);
				for (w = wstart; w < wstart + wid; w++) {
					setAAPixelColor((int) x, w, col, (int) ((frac >> 8) & 0xFF));
					setAAPixelColor((int) x, w + 1, col, (int) ((~frac >> 8) & 0xFF));
				}
				frac += inc;
				if (frac >= 65536) {
					frac -= 65536;
					y++;
				} else if (frac < 0) {
					frac += 65536;
					y--;
				}
			}
		} else {
			if (dy < 0) {
				tmp = x1;
				x1 = x2;
				x2 = (int) tmp;
				tmp = y1;
				y1 = y2;
				y2 = (int) tmp;
				dx = x2 - x1;
				dy = y2 - y1;
			}
			x = x1;
			inc = (dx * 65536) / dy;
			frac = 0;
		/* TBB: set the last pixel for consistency (<=) */
			for (y = y1 ; y <= y2 ; y++) {
				wstart = (int) (x - wid / 2);
				for (w = wstart; w < wstart + wid; w++) {
					setAAPixelColor(w, (int) y, col, (int) ((frac >> 8) & 0xFF));
					setAAPixelColor(w + 1, (int) y, col, (int) ((~frac >> 8) & 0xFF));
				}
				frac += inc;
				if (frac >= 65536) {
					frac -= 65536;
					x++;
				} else if (frac < 0) {
					frac += 65536;
					x--;
				}
			}
		}
	}

	private void setAAPixelColor(final int x, final int y, final int color, final int t) {
		int dr,dg,db,p,r,g,b;

	/* 2.0.34: watch out for out of range calls */
		if (!boundsSafeMacro(x, y)) {
			return;
		}
		p = getPixel(x, y);
	/* TBB: we have to implement the dont_blend stuff to provide
	  the full feature set of the old implementation */
		if ((p == color)
				|| ((p == AA_dont_blend)
				&& (t != 0x00))) {
		return;
	}
		dr = GdUtils.gdTrueColorGetRed(color);
		dg = GdUtils.gdTrueColorGetGreen(color);
		db = GdUtils.gdTrueColorGetBlue(color);

		r = GdUtils.gdTrueColorGetRed(p);
		g = GdUtils.gdTrueColorGetGreen(p);
		b = GdUtils.gdTrueColorGetBlue(p);

		dr = blendColor(t, r, dr);
		dg = blendColor(t, g, dg);
		db = blendColor(t, b, db);
		tpixels[y][x] = GdUtils.gdTrueColorAlpha(dr, dg, db, GdUtils.ALPHA_OPAQUE);
	}

	/**
 	 * Added on 2003/12 by Pierre-Alain Joye (pajoye@pearfr.org)
	 **/
	private int blendColor(final int a, final int c, final int cc) {
		return (cc) + (((((c) - (cc)) * (a)) + ((((c) - (cc)) * (a)) >> 8) + 0x80) >> 8);
	}

	/* Solid bar. Upper left corner first, lower right corner second. */
	public void filledRectangle(int x1, int y1, int x2, int y2, final int color) {
		int x, y;

		if (x1 == x2 && y1 == y2) {
			setPixel(x1, y1, color);
			return;
		}

		if (x1 > x2) {
			x = x1;
			x1 = x2;
			x2 = x;
		}

		if (y1 > y2) {
			y = y1;
			y1 = y2;
			y2 = y;
		}

		if (x1 < 0) {
			x1 = 0;
		}

		if (x2 >= sx) {
			x2 = sx - 1;
		}

		if (y1 < 0) {
			y1 = 0;
		}

		if (y2 >= sy) {
			y2 = sy - 1;
		}

		for (y = y1; (y <= y2); y++) {
			for (x = x1; (x <= x2); x++) {
				setPixel(x, y, color);
			}
		}
	}

}
