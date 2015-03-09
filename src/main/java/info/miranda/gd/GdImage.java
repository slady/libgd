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
	private boolean gdImageBoundsSafeMacro(final int x, final int y) {
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
		if (gdImageBoundsSafeMacro(x, y)) {
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
				gdImageBrushApply(x, y);
				break;
			case GdUtils.SPECIAL_COLOR_TILED:
				gdImageTileApply(x, y);
				break;
			case GdUtils.SPECIAL_COLOR_ANTI_ALIASED:
		/* This shouldn't happen (2.0.26) because we just call
		  gdImageAALine now, but do something sane. */
				setPixel(x, y, AA_color);
				break;
			default:
				if (gdImageBoundsSafeMacro(x, y)) {
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

	private void gdImageBrushApply(final int x, final int y) {
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
									gdImageColorResolveAlpha(
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

	void gdImageTileApply(int x, int y) {
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
							gdImageColorResolveAlpha(
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
		return gdImageColorResolveAlpha(r, g, b, GdUtils.ALPHA_OPAQUE);
	}

	private int gdImageColorResolveAlpha(int r, int g, int b, int a) {
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

}
