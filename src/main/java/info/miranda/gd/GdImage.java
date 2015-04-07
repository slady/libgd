package info.miranda.gd;

import info.miranda.gd.enums.GdAxis;
import info.miranda.gd.enums.GdEffect;
import info.miranda.gd.enums.GdImageColorType;
import info.miranda.gd.enums.GdInterpolationMethod;
import info.miranda.gd.filter.*;
import info.miranda.gd.interfaces.GdCallbackImageColor;
import info.miranda.gd.interfaces.GdFilterInterface;
import info.miranda.gd.utils.GdAffine;
import info.miranda.gd.utils.GdRect;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import static info.miranda.gd.utils.GdMath.fmod;
import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.exp;
import static java.lang.Math.ceil;
import static java.lang.Math.cos;
import static java.lang.Math.floor;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.tan;

/*
   The data structure in which gd stores images.
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
	   colorTransparent function. Newer
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
	  setClip */
	int cx1;
	int cy1;
	int cx2;
	int cy2;

	/* 2.1.0: allows to specify resolution in dpi */
	int res_x;
	int res_y;

	/* Selects quantization method, see trueColorToPaletteSetMethod() and gdPaletteQuantizationMethod enum. */
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
	GdFilterInterface interpolation;

	/* 2.0.12: this now checks the clipping rectangle */
	private boolean isBoundsSafe(final int x, final int y) {
		return (!(((y < cy1) || (y > cy2)) || ((x < cx1) || (x > cx2))));
	}

	/**
	 * @param sx The image width.
	 * @param sy The image height.
	 * @param colorType image color type, either palette or true color.
	 */
	public GdImage(final int sx, final int sy, final GdImageColorType colorType) {
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
				tpixels = new int[sy][sx];
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
				pixels = new int[sy][sx];
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
		if (isBoundsSafe(x, y)) {
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
			return GdUtils.trueColorMixAlpha(red[p], green[p], blue[p],
					(transparent == p) ? GdUtils.ALPHA_TRANSPARENT : alpha[p]);
		} else {
			return p;
		}
	}

	public int getPalettePixel(final int x, final int y) {
		return pixels[(y)][(x)];
	}

	/* This function accepts truecolor pixel values only. The
     source color is composited with the destination color
     based on the alpha channel value of the source color.
     The resulting color is opaque. */

/* Thanks to Frank Warmerdam for this superior implementation
	of gdAlphaBlend(), which merges alpha in the
	destination color much better. */

	private int gdAlphaBlend(final int dst, final int src) {
		int src_alpha = GdUtils.trueColorGetAlpha(src);
		int dst_alpha, alpha, red, green, blue;
		int src_weight, dst_weight, tot_weight;

	/* -------------------------------------------------------------------- */
	/*      Simple cases we want to handle fast.                            */
	/* -------------------------------------------------------------------- */
		if (src_alpha == GdUtils.ALPHA_OPAQUE)
			return src;

		dst_alpha = GdUtils.trueColorGetAlpha(dst);
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

		red = (GdUtils.trueColorGetRed(src) * src_weight
				+ GdUtils.trueColorGetRed(dst) * dst_weight) / tot_weight;
		green = (GdUtils.trueColorGetGreen(src) * src_weight
				+ GdUtils.trueColorGetGreen(dst) * dst_weight) / tot_weight;
		blue = (GdUtils.trueColorGetBlue(src) * src_weight
				+ GdUtils.trueColorGetBlue(dst) * dst_weight) / tot_weight;

	/* -------------------------------------------------------------------- */
	/*      Return merged result.                                           */
	/* -------------------------------------------------------------------- */
		return ((alpha << 24) + (red << 16) + (green << 8) + blue);
	}

	private int gdLayerOverlay(final int dst, final int src) {
		int a1, a2;
		a1 = GdUtils.ALPHA_MAX - GdUtils.trueColorGetAlpha(dst);
		a2 = GdUtils.ALPHA_MAX - GdUtils.trueColorGetAlpha(src);
		return (((GdUtils.ALPHA_MAX - a1 * a2 / GdUtils.ALPHA_MAX) << 24) +
				(gdAlphaOverlayColor(GdUtils.trueColorGetRed(src),
						GdUtils.trueColorGetRed(dst), GdUtils.RED_MAX) << 16) +
				(gdAlphaOverlayColor(GdUtils.trueColorGetGreen(src),
						GdUtils.trueColorGetGreen(dst), GdUtils.GREEN_MAX) << 8) +
				(gdAlphaOverlayColor(GdUtils.trueColorGetBlue(src),
						GdUtils.trueColorGetBlue(dst), GdUtils.BLUE_MAX)));
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
		a1 = GdUtils.ALPHA_MAX - GdUtils.trueColorGetAlpha(src);
		a2 = GdUtils.ALPHA_MAX - GdUtils.trueColorGetAlpha(dst);

		r1 = GdUtils.RED_MAX - (a1 * (GdUtils.RED_MAX - GdUtils.trueColorGetRed(src))) / GdUtils.ALPHA_MAX;
		r2 = GdUtils.RED_MAX - (a2 * (GdUtils.RED_MAX - GdUtils.trueColorGetRed(dst))) / GdUtils.ALPHA_MAX;
		g1 = GdUtils.GREEN_MAX - (a1 * (GdUtils.GREEN_MAX - GdUtils.trueColorGetGreen(src))) / GdUtils.ALPHA_MAX;
		g2 = GdUtils.GREEN_MAX - (a2 * (GdUtils.GREEN_MAX - GdUtils.trueColorGetGreen(dst))) / GdUtils.ALPHA_MAX;
		b1 = GdUtils.BLUE_MAX - (a1 * (GdUtils.BLUE_MAX - GdUtils.trueColorGetBlue(src))) / GdUtils.ALPHA_MAX;
		b2 = GdUtils.BLUE_MAX - (a2 * (GdUtils.BLUE_MAX - GdUtils.trueColorGetBlue(dst))) / GdUtils.ALPHA_MAX ;

		a1 = GdUtils.ALPHA_MAX - a1;
		a2 = GdUtils.ALPHA_MAX - a2;
		return (((a1*a2/GdUtils.ALPHA_MAX) << 24) +
				((r1*r2/GdUtils.RED_MAX) << 16) +
				((g1*g2/GdUtils.GREEN_MAX) << 8) +
				((b1*b2/GdUtils.BLUE_MAX)));
	}


	/* Replaces or blends with the background depending on the
	   most recent call to alphaBlending and the
	   alpha channel value of 'color'; default is to overwrite.
	   Tiling and line styling are also implemented
	   here. All other gd drawing functions pass through this call,
	   allowing for many useful effects.
	   Overlay and multiply effects are used when alphaBlending
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
		  AALine now, but do something sane. */
				setPixel(x, y, AA_color);
				break;
			default:
				if (isBoundsSafe(x, y)) {
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
											GdUtils.trueColorGetRed(p),
											GdUtils.trueColorGetGreen(p),
											GdUtils.trueColorGetBlue(p),
											GdUtils.trueColorGetAlpha(p)));
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
					p = GdUtils.trueColorMixAlpha(tile.red[p], tile.green[p], tile.blue[p], tile.alpha[p]);
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
									GdUtils.trueColorGetRed(p),
									GdUtils.trueColorGetGreen(p),
									GdUtils.trueColorGetBlue(p),
									GdUtils.trueColorGetAlpha(p)));
				} else {
					setPixel(x, y, tileColorMap[p]);
				}
			}
		}
	}


/*
 * colorResolve is an alternative for the code fragment:
 *
 *      if ((color=colorExact(im,R,G,B)) < 0)
 *        if ((color=colorAllocate(im,R,G,B)) < 0)
 *          color=colorClosest(im,R,G,B);
 *
 * in a single function.    Its advantage is that it is guaranteed to
 * return a color index in one search over the color table.
 */

	/* Opaque only */
	private int colorResolve(int r, int g, int b) {
		return colorResolveAlpha(r, g, b, GdUtils.ALPHA_OPAQUE);
	}

	/* Based on gdImageColorExactAlpha and gdImageColorClosestAlpha */
	private int colorResolveAlpha(int r, int g, int b, int a) {
		int c;
		int ct = -1;
		int op = -1;
		long rd, gd, bd, ad, dist;
		long mindist = 4 * 255 * 255;	/* init to max poss dist */
		if (trueColor) {
			return GdUtils.trueColorMixAlpha(r, g, b, a);
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

	/* Returns exact, 100% opaque matches only */
	public int colorExact(final int r, final int g, final int b) {
		return colorExactAlpha(r, g, b, GdUtils.ALPHA_OPAQUE);
	}

	/* Returns an exact match only, including alpha */
	public int colorExactAlpha(final int r, final int g, final int b, final int a) {
		if (trueColor) {
			return GdUtils.trueColorMixAlpha(r, g, b, a);
		}
		for (int i = 0; (i < (colorsTotal)); i++) {
			if (open[i]) {
				continue;
			}
			if ((red[i] == r) && (green[i] == g) && (blue[i] == b) && (alpha[i] == a)) {
				return i;
			}
		}
		return -1;
	}

	/* These functions still work with truecolor images,
	   for which they never return error. */
	public int colorAllocate(final int r, int g, int b) {
		return colorAllocateAlpha(r, g, b, GdUtils.ALPHA_OPAQUE);
	}

	/* gd 2.0: palette entries with non-opaque transparency are permitted. */
	public int colorAllocateAlpha(int r, int g, int b, int a) {
		int i;
		int ct = (-1);
		if (trueColor) {
			return GdUtils.trueColorMixAlpha(r, g, b, a);
		}
		for (i = 0; (i < (colorsTotal)); i++) {
			if (open[i]) {
				ct = i;
				break;
			}
		}
		if (ct == (-1)) {
			ct = colorsTotal;
			if (ct == GdUtils.MAX_COLORS) {
				return -1;
			}
			colorsTotal++;
		}
		red[ct] = r;
		green[ct] = g;
		blue[ct] = b;
		alpha[ct] = a;
		open[ct] = false;
		return ct;
	}

	public void gdImageColorDeallocate(int color) {
		if (trueColor || (color >= GdUtils.MAX_COLORS) || (color < 0)) {
			return;
		}
	/* Mark it open. */
		open[color] = true;
	}

	/* Bresenham as presented in Foley & Van Dam */
	public void drawLine(int x1, int y1, int x2, int y2, final int color) {
		int dx, dy, incr1, incr2, d, x, y, xend, yend, xdirflag, ydirflag;
		int wid;
		int w, wstart;

		if (color == GdUtils.SPECIAL_COLOR_ANTI_ALIASED) {
		/*
		  gdAntiAliased passed as color: use the much faster, much cheaper
		  and equally attractive AALine implementation. That
		  clips too, so don't clip twice.
		*/
			AALine(x1, y1, x2, y2, AA_color);
			return;
		}
	/* 2.0.10: Nick Atty: clip to edges of drawing rectangle, return if no
	   points need to be drawn. 2.0.26, TBB: clip to edges of clipping
	   rectangle. We were getting away with this because setPixel
	   is used for actual drawing, but this is still more efficient and opens
	   the way to skip per-pixel bounds checking in the future. */

		final GdClipRectangle clip1 = new GdClipRectangle(x1, y1, x2, y2);
		if (!clip1.clip_1d(cx1, cx2))
			return;
		x1 = clip1.x0;
		y1 = clip1.y0;
		x2 = clip1.x1;
		y2 = clip1.y1;

		final GdClipRectangle clip2 = new GdClipRectangle(y1, x1, y2, x2);
		if (!clip2.clip_1d(cy1, cy2))
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

	private void vLine(final int x, int y1, int y2, final int col) {
		if (thick > 1) {
			int thickhalf = thick >> 1;
			fillRectangle(x - thickhalf, y1, x + thick - thickhalf - 1, y2, col);
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
			fillRectangle(x1, y - thickhalf, x2, y + thick - thickhalf - 1, col);
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
			drawLine(x1, y1, x2, y2, col);
			return;
		}

	/* TBB: use the clipping rectangle */
		final GdClipRectangle clip1 = new GdClipRectangle(x1, y1, x2, y2);
		if (!clip1.clip_1d(cx1, cx2))
			return;
		x1 = clip1.x0;
		y1 = clip1.y0;
		x2 = clip1.x1;
		y2 = clip1.y1;

		final GdClipRectangle clip2 = new GdClipRectangle(y1, x1, y2, x2);
		if (!clip2.clip_1d(cy1, cy2))
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
		if (!isBoundsSafe(x, y)) {
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
		dr = GdUtils.trueColorGetRed(color);
		dg = GdUtils.trueColorGetGreen(color);
		db = GdUtils.trueColorGetBlue(color);

		r = GdUtils.trueColorGetRed(p);
		g = GdUtils.trueColorGetGreen(p);
		b = GdUtils.trueColorGetBlue(p);

		dr = blendColor(t, r, dr);
		dg = blendColor(t, g, dg);
		db = blendColor(t, b, db);
		tpixels[y][x] = GdUtils.trueColorMixAlpha(dr, dg, db, GdUtils.ALPHA_OPAQUE);
	}

	/**
 	 * Added on 2003/12 by Pierre-Alain Joye (pajoye@pearfr.org)
	 **/
	private int blendColor(final int a, final int c, final int cc) {
		return (cc) + (((((c) - (cc)) * (a)) + ((((c) - (cc)) * (a)) >> 8) + 0x80) >> 8);
	}

	/* Solid bar. Upper left corner first, lower right corner second. */
	public void fillRectangle(int x1, int y1, int x2, int y2, final int color) {
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

	/* Corners specified (not width and height). Upper left first, lower right
	   second. */
	public void drawRectangle(int x1, int y1, int x2, int y2, final int color) {
		if (x1 == x2 && y1 == y2 && thick == 1) {
			setPixel(x1, y1, color);
			return;
		}

		if (y2 < y1) {
			int t;
			t = y1;
			y1 = y2;
			y2 = t;

			t = x1;
			x1 = x2;
			x2 = t;
		}

		if (thick > 1) {
			int cx, cy, x1ul, y1ul, x2lr, y2lr;
			int half = thick >> 1;
			x1ul = x1 - half;
			y1ul = y1 - half;

			x2lr = x2 + half;
			y2lr = y2 + half;

			cy = y1ul + thick;
			while (cy-- > y1ul) {
				cx = x1ul - 1;
				while (cx++ < x2lr) {
					setPixel(cx, cy, color);
				}
			}

			cy = y2lr - thick;
			while (cy++ < y2lr) {
				cx = x1ul - 1;
				while (cx++ < x2lr) {
					setPixel(cx, cy, color);
				}
			}

			cy = y1ul + thick - 1;
			while (cy++ < y2lr -thick) {
				cx = x1ul - 1;
				while (cx++ < x1ul + thick) {
					setPixel(cx, cy, color);
				}
			}

			cy = y1ul + thick - 1;
			while (cy++ < y2lr -thick) {
				cx = x2lr - thick - 1;
				while (cx++ < x2lr) {
					setPixel(cx, cy, color);
				}
			}

			return;
		} else {
			drawLine(x1, y1, x2, y1, color);
			drawLine(x1, y2, x2, y2, color);
			drawLine(x1, y1 + 1, x1, y2 - 1, color);
			drawLine(x2, y1 + 1, x2, y2 - 1, color);
		}
	}

	public void drawPolygon(final GdPoint[] p, final int c) {
		drawLine(p[0].x, p[0].y, p[p.length - 1].x, p[p.length - 1].y, c);
		drawOpenPolygon(p, c);
	}

	public void drawOpenPolygon(final GdPoint[] p, final int c) {
		int i;
		int lx, ly;

		lx = p[0].x;
		ly = p[0].y;
		for (i = 1; (i < p.length); i++) {
			drawLine(lx, ly, p[i].x, p[i].y, c);
			lx = p[i].x;
			ly = p[i].y;
		}

	}

/* THANKS to Kirsten Schulz for the polygon fixes! */

/* The intersection finding technique of this code could be improved  */
/* by remembering the previous intersection, and by using the slope. */
/* That could help to adjust intersections  to produce a nice */
/* interior_extrema. */
	public void fillPolygon(final GdPoint[] p, final int c) {
		int i;
		int j;
		int index;
		int y;
		int miny, maxy, pmaxy;
		int x1, y1;
		int x2, y2;
		int ind1, ind2;
		int ints;
		int fill_color;

		if (c == GdUtils.SPECIAL_COLOR_ANTI_ALIASED) {
			fill_color = AA_color;
		} else {
			fill_color = c;
		}
		if (polyAllocated == 0) {
			polyInts = new int[p.length];
			polyAllocated = p.length;
		}
		if (polyAllocated < p.length) {
			while (polyAllocated < p.length) {
				polyAllocated *= 2;
			}
			polyInts = new int[polyAllocated];
		}
		miny = p[0].y;
		maxy = p[0].y;
		for (i = 1; (i < p.length); i++) {
			if (p[i].y < miny) {
				miny = p[i].y;
			}
			if (p[i].y > maxy) {
				maxy = p[i].y;
			}
		}
		pmaxy = maxy;
	/* 2.0.16: Optimization by Ilia Chipitsine -- don't waste time offscreen */
	/* 2.0.26: clipping rectangle is even better */
		if (miny < cy1) {
			miny = cy1;
		}
		if (maxy > cy2) {
			maxy = cy2;
		}
	/* Fix in 1.3: count a vertex only once */
		for (y = miny; (y <= maxy); y++) {
			ints = 0;
			for (i = 0; (i < p.length); i++) {
				if (i == 0) {
					ind1 = p.length - 1;
					ind2 = 0;
				} else {
					ind1 = i - 1;
					ind2 = i;
				}
				y1 = p[ind1].y;
				y2 = p[ind2].y;
				if (y1 < y2) {
					x1 = p[ind1].x;
					x2 = p[ind2].x;
				} else if (y1 > y2) {
					y2 = p[ind1].y;
					y1 = p[ind2].y;
					x2 = p[ind1].x;
					x1 = p[ind2].x;
				} else {
					continue;
				}

			/* Do the following math as float intermediately, and round to ensure
			 * that Polygon and FilledPolygon for the same set of points have the
			 * same footprint. */

				if ((y >= y1) && (y < y2)) {
					polyInts[ints++] = (int) ((float) ((y - y1) * (x2 - x1)) /
							(float) (y2 - y1) + 0.5 + x1);
				} else if ((y == pmaxy) && (y == y2)) {
					polyInts[ints++] = x2;
				}
			}
		/*
		  2.0.26: polygons pretty much always have less than 100 points,
		  and most of the time they have considerably less. For such trivial
		  cases, insertion sort is a good choice. Also a good choice for
		  future implementations that may wish to indirect through a table.
		*/
			for (i = 1; (i < ints); i++) {
				index = polyInts[i];
				j = i;
				while ((j > 0) && (polyInts[j - 1] > index)) {
					polyInts[j] = polyInts[j - 1];
					j--;
				}
				polyInts[j] = index;
			}
			for (i = 0; (i < (ints-1)); i += 2) {
			/* 2.0.29: back to line to prevent segfaults when
			  performing a pattern fill */
				drawLine(polyInts[i], y, polyInts[i + 1], y,
						fill_color);
			}
		}
	/* If we are drawing this AA, then redraw the border with AA lines. */
	/* This doesn't work as well as I'd like, but it doesn't clash either. */
		if (c == GdUtils.SPECIAL_COLOR_ANTI_ALIASED) {
			drawPolygon(p, c);
		}
	}

	public void setStyle(int[] style, int noOfPixels) {
		this.style = new int[noOfPixels];
		System.arraycopy(style, 0, this.style, 0, noOfPixels);
		styleLength = noOfPixels;
		stylePos = 0;
	}

/* s and e are integers modulo 360 (degrees), with 0 degrees
   being the rightmost extreme and degrees changing clockwise.
   cx and cy are the center in pixels; w and h are the horizontal
   and vertical diameter in pixels. Nice interface, but slow.
   See gd_arc_f_buggy.c for a better version that doesn't
   seem to be bug-free yet. */

	public void drawArc(final int cx, final int cy, final int w, final int h,
						   final int s, final int e, final int color) {
		fillArc(cx, cy, w, h, s, e, color, GdUtils.gdNoFill);
	}

	public void fillArc(final int cx, final int cy, final int w, final int h, int s, int e,
								 final int color, final int style) {
		GdPoint[] pts = new GdPoint[3];
		int lx = 0, ly = 0;
		int fx = 0, fy = 0;

		if ((s % 360)  == (e % 360)) {
			s = 0;
			e = 360;
		} else {
			if (s > 360) {
				s = s % 360;
			}

			if (e > 360) {
				e = e % 360;
			}

			while (s < 0) {
				s += 360;
			}

			while (e < s) {
				e += 360;
			}

			if (s == e) {
				s = 0;
				e = 360;
			}
		}

		for (int i = s; (i <= e); i++) {
			final int x = (int) (((long) GdUtils.COS_T[i % 360] * (long) w / (2 * 1024)) + cx);
			final int y = (int) (((long) GdUtils.SIN_T[i % 360] * (long) h / (2 * 1024)) + cy);
			if (i != s) {
				if ((style & GdUtils.gdChord) == 0) {
					if ((style & GdUtils.gdNoFill) != 0) {
						drawLine(lx, ly, x, y, color);
					} else {
					/* This is expensive! */
						pts[0].x = lx;
						pts[0].y = ly;
						pts[1].x = x;
						pts[1].y = y;
						pts[2].x = cx;
						pts[2].y = cy;
						fillPolygon(pts, color);
					}
				}
			} else {
				fx = x;
				fy = y;
			}
			lx = x;
			ly = y;
		}
		if ((style & GdUtils.gdChord) != 0) {
			if ((style & GdUtils.gdNoFill) != 0) {
				if ((style & GdUtils.gdEdged) != 0) {
					drawLine(cx, cy, lx, ly, color);
					drawLine(cx, cy, fx, fy, color);
				}
				drawLine(fx, fy, lx, ly, color);
			} else {
				pts[0].x = fx;
				pts[0].y = fy;
				pts[1].x = lx;
				pts[1].y = ly;
				pts[2].x = cx;
				pts[2].y = cy;
				fillPolygon(pts, color);
			}
		} else {
			if ((style & GdUtils.gdNoFill) != 0) {
				if ((style & GdUtils.gdEdged) != 0) {
					drawLine(cx, cy, lx, ly, color);
					drawLine(cx, cy, fx, fy, color);
				}
			}
		}
	}

	public void drawEllipse(int mx, int my, int w, int h, int c) {
		int mx1=0,mx2=0,my1=0,my2=0;
		long aq,bq,dx,dy,r,rx,ry;

		final int a=w>>1;
		final int b=h>>1;
		setPixel(mx + a, my, c);
		setPixel(mx - a, my, c);
		mx1 = mx-a;
		my1 = my;
		mx2 = mx+a;
		my2 = my;

		aq = a * a;
		bq = b * b;
		dx = aq << 1;
		dy = bq << 1;
		r  = a * bq;
		rx = r << 1;
		ry = 0;
		int x = a;
		while (x > 0) {
			if (r > 0) {
				my1++;
				my2--;
				ry +=dx;
				r  -=ry;
			}
			if (r <= 0) {
				x--;
				mx1++;
				mx2--;
				rx -=dy;
				r  +=rx;
			}
			setPixel(mx1, my1, c);
			setPixel(mx1, my2, c);
			setPixel(mx2, my1, c);
			setPixel(mx2, my2, c);
		}
	}

	public void fillEllipse(final int mx, final int my, final int w, final int h, final int c) {
		int mx1=0,mx2=0,my1=0,my2=0;
		long aq,bq,dx,dy,r,rx,ry;
		int i;
		int old_y2;

		final int a=w>>1;
		final int b=h>>1;

		for (int x = mx-a; x <= mx+a; x++) {
			setPixel(x, my, c);
		}

		mx1 = mx-a;
		my1 = my;
		mx2 = mx+a;
		my2 = my;

		aq = a * a;
		bq = b * b;
		dx = aq << 1;
		dy = bq << 1;
		r  = a * bq;
		rx = r << 1;
		ry = 0;
		int x = a;
		old_y2=-2;
		while (x > 0) {
			if (r > 0) {
				my1++;
				my2--;
				ry +=dx;
				r  -=ry;
			}
			if (r <= 0) {
				x--;
				mx1++;
				mx2--;
				rx -=dy;
				r  +=rx;
			}
			if(old_y2!=my2) {
				for(i=mx1; i<=mx2; i++) {
					setPixel(i, my1, c);
				}
			}
			if(old_y2!=my2) {
				for(i=mx1; i<=mx2; i++) {
					setPixel(i, my2, c);
				}
			}
			old_y2 = my2;
		}
	}

	public void fillToBorder(final int x, final int y, final int border, final int color) {
		boolean lastBorder;
	/* Seek left */
		int leftLimit, rightLimit;
		int i;
		GdEffect restoreAlphaBleding;

		if (border < 0) {
		/* Refuse to fill to a non-solid border */
			return;
		}

		leftLimit = (-1);

		restoreAlphaBleding = alphaBlendingFlag;
		alphaBlendingFlag = GdEffect.REPLACE;

		for (i = x; (i >= 0); i--) {
			if (getPixel(i, y) == border) {
				break;
			}
			setPixel(i, y, color);
			leftLimit = i;
		}
		if (leftLimit == (-1)) {
			alphaBlendingFlag = restoreAlphaBleding;
			return;
		}
	/* Seek right */
		rightLimit = x;
		for (i = (x + 1); (i < sx); i++) {
		if (getPixel(i, y) == border) {
			break;
		}
		setPixel(i, y, color);
		rightLimit = i;
	}
	/* Look at lines above and below and start paints */
	/* Above */
		if (y > 0) {
			lastBorder = true;
			for (i = leftLimit; (i <= rightLimit); i++) {
				int c;
				c = getPixel(i, y - 1);
				if (lastBorder) {
					if ((c != border) && (c != color)) {
						fillToBorder(i, y - 1, border, color);
						lastBorder = false;
					}
				} else if ((c == border) || (c == color)) {
					lastBorder = true;
				}
			}
		}
	/* Below */
		if (y < ((sy) - 1)) {
			lastBorder = true;
			for (i = leftLimit; (i <= rightLimit); i++) {
				int c = getPixel(i, y + 1);
				if (lastBorder) {
					if ((c != border) && (c != color)) {
						fillToBorder(i, y + 1, border, color);
						lastBorder = false;
					}
				} else if ((c == border) || (c == color)) {
					lastBorder = true;
				}
			}
		}
		alphaBlendingFlag = restoreAlphaBleding;
	}

/*
 * set the pixel at (x,y) and its 4-connected neighbors
 * with the same pixel value to the new pixel value nc (new color).
 * A 4-connected neighbor:  pixel above, below, left, or right of a pixel.
 * ideas from comp.graphics discussions.
 * For tiled fill, the use of a flag buffer is mandatory. As the tile image can
 * contain the same color as the color to fill. To do not bloat normal filling
 * code I added a 2nd private function.
 */
	private int gdImageTileGet(final int x, final int y) {
		int tileColor;
		if (tile == null) {
			return -1;
		}
		final int srcx = x % (tile.sx);
		final int srcy = y % (tile.sy);
		final int p = tile.getPixel(srcx, srcy);
		if (p == tile.transparent) {
			tileColor = transparent;
		} else if (trueColor) {
			if (tile.trueColor) {
				tileColor = p;
			} else {
				tileColor = GdUtils.trueColorMixAlpha(tile.getRed(p), tile.getGreen(p), tile.getBlue(p), tile.getAlpha(p));
			}
		} else {
			if (tile.trueColor) {
				tileColor = colorResolveAlpha(GdUtils.trueColorGetRed(p), GdUtils.trueColorGetGreen(p), GdUtils.trueColorGetBlue(p), GdUtils.trueColorGetAlpha(p));
			} else {
				tileColor = colorResolveAlpha(tile.getRed(p), tile.getGreen(p), tile.getBlue(p), tile.getAlpha(p));
			}
		}
		return tileColor;
	}

	/* horizontal segment of scan line y */
	private class FillStack {
		private Stack<FillStack> stack = new Stack<FillStack>();
		int y, xl, xr, dy;

		public void push(final int Y, final int XL, final int XR, final int DY) {
			final FillStack sp = new FillStack();
			if (Y+(DY)>=0 && Y+(DY)<sy)
			{sp.y = Y; sp.xl = XL; sp.xr = XR; sp.dy = DY;}
			stack.push(sp);
		}

		public FillStack pop() {
			return stack.pop();
		}

		public boolean empty() {
			return stack.empty();
		}
	};

/* max depth of stack */
//	#define FILL_MAX ((int)(sy*sx)/4)
//			#define FILL_PUSH(Y, XL, XR, DY) \
//			if (sp<stack+FILL_MAX && Y+(DY)>=0 && Y+(DY)<wy2) \
//	{sp->y = Y; sp->xl = XL; sp->xr = XR; sp->dy = DY; sp++;}
//
//	#define FILL_POP(Y, XL, XR, DY) \
//	{sp--; Y = sp->y+(DY = sp->dy); XL = sp->xl; XR = sp->xr;}

	public void gdImageFill(int x, int y, final int nc) {
		int l, x1, x2, dy;
		int oc;   /* old pixel value */

		final GdEffect alphablending_bak;

	/* stack of filled segments */
	/* struct seg stack[FILL_MAX],*sp = stack; */
		final FillStack stack = new FillStack();

		if (!trueColor && nc > (colorsTotal - 1)) {
			return;
		}

		alphablending_bak = alphaBlendingFlag;
		alphaBlendingFlag = GdEffect.REPLACE;

		if (nc==GdUtils.SPECIAL_COLOR_TILED) {
			fillTiled(x, y, nc);
			alphaBlendingFlag = alphablending_bak;
			return;
		}

		final int wx2=sx;
		final int wy2=sy;
		oc = getPixel(x, y);
		if (oc==nc || x<0 || x>wx2 || y<0 || y>wy2) {
			alphaBlendingFlag = alphablending_bak;
			return;
		}

	/* Do not use the 4 neighbors implementation with
	* small images
	*/
		if (sx < 4) {
			int ix = x, iy = y, c;
			do {
				do {
					c = getPixel(ix, iy);
					if (c != oc) {
						alphaBlendingFlag = alphablending_bak;
						return;
					}
					setPixel(ix, iy, nc);
				} while(ix++ < (sx -1));
				ix = x;
			} while(iy++ < (sy -1));
			alphaBlendingFlag = alphablending_bak;
			return;
		}

		if(GdUtils.overflow2(sy, sx)) {
			return;
		}

	/* required! */
		stack.push(y, x, x, 1);
	/* seed segment (popped 1st) */
		stack.push(y + 1, x, x, -1);
		while (!stack.empty()) {
			final FillStack sp = stack.pop();
			y = sp.y+(dy = sp.dy); x1 = sp.xl; x2 = sp.xr;

			for (x=x1; x>=0 && getPixel(x, y)==oc; x--) {
				setPixel(x, y, nc);
			}
			if (x>=x1) {
				for (x++; x<=x2 && (getPixel(x, y)!=oc); x++);

				l = x;
			} else {
				l = x+1;

		/* leak on left? */
				if (l<x1) {
					stack.push(y, l, x1-1, -dy);
				}
				x = x1+1;
			}

			do {
				for (; x<=wx2 && getPixel(x, y)==oc; x++) {
					setPixel(x, y, nc);
				}
				stack.push(y, l, x-1, dy);
			/* leak on right? */
				if (x>x2+1) {
					stack.push(y, x2+1, x-1, -dy);
				}
				for (x++; x<=x2 && (getPixel(x, y)!=oc); x++);

				l = x;
			} while (x<=x2);
		}

		alphaBlendingFlag = alphablending_bak;
	}

	private void fillTiled(int x, int y, int nc) {
		int l, x1, x2, dy;
		int oc;   /* old pixel value */
	/* stack of filled segments */
		final FillStack stack = new FillStack();
		boolean[] pts = new boolean[sx * sy];

		if (tile == null) {
			return;
		}

		final int wx2=sx;
		final int wy2=sy;

		if(GdUtils.overflow2(sy, sx)) {
			return;
		}

		oc = getPixel(x, y);

	/* required! */
		stack.push(y,x, x, 1);
	/* seed segment (popped 1st) */
		stack.push(y+1, x, x, -1);
		while (!stack.empty()) {
			final FillStack sp = stack.pop();
			y = sp.y+(dy = sp.dy); x1 = sp.xl; x2 = sp.xr;
			for (x=x1; x>=0 && (!pts[y + x*wx2] && getPixel(x,y)==oc); x--) {
				nc = gdImageTileGet(x,y);
				pts[y + x*wx2]=true;
				setPixel(x, y, nc);
			}
			if (x>=x1) {
				for (x++; x<=x2 && (pts[y + x*wx2] || getPixel(x, y)!=oc); x++);
				l = x;
			} else {
				l = x+1;

		/* leak on left? */
				if (l<x1) {
					stack.push(y, l, x1-1, -dy);
				}
				x = x1+1;
			}

			do {
				for (; x<wx2 && (!pts[y + x*wx2] && getPixel(x, y)==oc) ; x++) {
					if (pts[y + x*wx2]) {
					/* we should never be here */
						break;
					}
					nc = gdImageTileGet(x,y);
					pts[y + x*wx2]=true;
					setPixel(x, y, nc);
				}
				stack.push(y, l, x-1, dy);
			/* leak on right? */
				if (x>x2+1) {
					stack.push(y, x2+1, x-1, -dy);
				}
				for (x++; x<=x2 && (pts[y + x*wx2] || getPixel(x, y)!=oc); x++);
				l = x;
			} while (x<=x2);
		}
	}

	/* Line thickness (defaults to 1). Affects lines, ellipses,
	   rectangles, polygons and so forth. */
	public void setThickness(int thickness) {
		thick = thickness;
	}

	public int getRed(final int c) {
		return trueColor ? GdUtils.trueColorGetRed(c) : red[(c)];
	}

	public int getGreen(final int c) {
		return trueColor ? GdUtils.trueColorGetGreen(c) : green[(c)];
	}

	public int getBlue(final int c) {
		return trueColor ? GdUtils.trueColorGetBlue(c) : blue[(c)];
	}

	public int getAlpha(final int c) {
		return trueColor ? GdUtils.trueColorGetAlpha(c) : alpha[(c)];
	}

	public void setBrush(final GdImage brush) {
		int i;
		this.brush = brush;
		if ((!trueColor) && (!this.brush.trueColor)) {
			for (i = 0; (i < brush.colorsTotal); i++) {
				int index;
				index = colorResolveAlpha(
						brush.getRed(i),
						brush.getGreen(i),
						brush.getBlue(i),
						brush.getAlpha(i));
				brushColorMap[i] = index;
			}
		}
	}

	public void setTile(final GdImage tile) {
		int i;
		this.tile = tile;
		if ((!trueColor) && (!tile.trueColor)) {
			for (i = 0; (i < tile.colorsTotal); i++) {
				int index;
				index = colorResolveAlpha(
						tile.getRed(i),
						tile.getGreen(i),
						tile.getBlue(i),
						tile.getAlpha(i));
				tileColorMap[i] = index;
			}
		}
	}

	public void setAntiAliased(final int c) {
		AA = 1;
		AA_color = c;
		AA_dont_blend = -1;
	}

	public void setAntiAliasedDontBlend(final int c, final int dont_blend) {
		AA = 1;
		AA_color = c;
		AA_dont_blend = dont_blend;
	}

	/* On or off (1 or 0) for all three of these. */
	public void setInterlace(final int interlaceArg) {
		interlace = interlaceArg;
	}

	/* Image comparison definitions */
	public int compare(final GdImage im1, final GdImage im2) {
		int x, y;
		int p1, p2;
		int cmpStatus = 0;
		int sx, sy;

		if (im1.interlace != im2.interlace) {
			cmpStatus |= GdUtils.GD_CMP_INTERLACE;
		}

		if (im1.transparent != im2.transparent) {
			cmpStatus |= GdUtils.GD_CMP_TRANSPARENT;
		}

		if (im1.trueColor != im2.trueColor) {
			cmpStatus |= GdUtils.GD_CMP_TRUECOLOR;
		}

		sx = im1.sx;
		if (im1.sx != im2.sx) {
			cmpStatus |= GdUtils.GD_CMP_SIZE_X + GdUtils.GD_CMP_IMAGE;
			if (im2.sx < im1.sx) {
				sx = im2.sx;
			}
		}

		sy = im1.sy;
		if (im1.sy != im2.sy) {
			cmpStatus |= GdUtils.GD_CMP_SIZE_Y + GdUtils.GD_CMP_IMAGE;
			if (im2.sy < im1.sy) {
				sy = im2.sy;
			}
		}

		if (im1.colorsTotal != im2.colorsTotal) {
			cmpStatus |= GdUtils.GD_CMP_NUM_COLORS;
		}

		for (y = 0; (y < sy); y++) {
			for (x = 0; (x < sx); x++) {
				p1 = im1.getPixel(x, y);
				p2 = im2.getPixel(x, y);
				if (im1.getRed(p1) != im2.getRed(p2)) {
					cmpStatus |= GdUtils.GD_CMP_COLOR + GdUtils.GD_CMP_IMAGE;
					break;
				}
				if (im1.getGreen(p1) != im2.getGreen(p2)) {
					cmpStatus |= GdUtils.GD_CMP_COLOR + GdUtils.GD_CMP_IMAGE;
					break;
				}
				if (im1.getBlue(p1) != im2.getBlue(p2)) {
					cmpStatus |= GdUtils.GD_CMP_COLOR + GdUtils.GD_CMP_IMAGE;
					break;
				}
			/* Soon we'll add alpha channel to palettes */
				if (im1.getAlpha(p1) != im2.getAlpha(p2)) {
					cmpStatus |= GdUtils.GD_CMP_COLOR + GdUtils.GD_CMP_IMAGE;
					break;
				}
			}
			if ((cmpStatus & GdUtils.GD_CMP_COLOR) != 0) {
				break;
			};
		}

		return cmpStatus;
	}


	public void setAlphaBlending(final GdEffect alphaBlendingArg) {
		this.alphaBlendingFlag = alphaBlendingArg;
	}

	public void setSaveAlpha(final int saveAlphaArg) {
		this.saveAlphaFlag = saveAlphaArg;
	}

	public void setClip(int x1, int y1, int x2, int y2) {
		if (x1 < 0) {
			x1 = 0;
		}
		if (x1 >= sx) {
			x1 = sx - 1;
		}
		if (x2 < 0) {
			x2 = 0;
		}
		if (x2 >= sx) {
			x2 = sx - 1;
		}
		if (y1 < 0) {
			y1 = 0;
		}
		if (y1 >= sy) {
			y1 = sy - 1;
		}
		if (y2 < 0) {
			y2 = 0;
		}
		if (y2 >= sy) {
			y2 = sy - 1;
		}
		cx1 = x1;
		cy1 = y1;
		cx2 = x2;
		cy2 = y2;
	}

	public GdClipRectangle getClip() {
		return new GdClipRectangle(cx1, cy1, cx2, cy2);
	}

	public void setResolution(final int res_x, final int res_y) {
		if (res_x > 0) this.res_x = res_x;
		if (res_y > 0) this.res_y = res_y;
	}


	/* convert a palette image to true color */
	public void paletteToTrueColor() {
		int y;
		int yy;

		if (trueColor) {
			return;
		}

		int x;
		final int sy = this.sy;
		final int sx = this.sx;

		tpixels = new int[sy][sx];

		for (y = 0; y < sy; y++) {
			int[] src_row = pixels[y];
			int[] dst_row = tpixels[y];

			for (x = 0; x < sx; x++) {
				final int c = src_row[x];
				if (c == transparent) {
					dst_row[x] = GdUtils.trueColorMixAlpha(0, 0, 0, 127);
				} else {
					dst_row[x] = GdUtils.trueColorMixAlpha(red[c], green[c], blue[c], alpha[c]);
				}
			}
		}

		/* free old palette buffer */
		pixels = null;
		trueColor = true;
		alphaBlendingFlag = GdEffect.REPLACE;
		saveAlphaFlag = 1;
	}

	/* Assumes opaque is the preferred alpha channel value */
	public int findColorClosest(final int r, final int g, final int b) {
		return findColorClosestAlpha(r, g, b, GdUtils.ALPHA_OPAQUE);
	}

	/* Closest match taking all four parameters into account.
	   A slightly different color with the same transparency
	   beats the exact same color with radically different
	   transparency */
	public int findColorClosestAlpha(final int r, final int g, final int b, final int a) {
		int i;
		long rd, gd, bd, ad;
		int ct = (-1);
		boolean first = true;
		long mindist = 0;
		if (trueColor) {
			return GdUtils.trueColorMixAlpha(r, g, b, a);
		}
		for (i = 0; (i < (colorsTotal)); i++) {
			long dist;
			if (open[i]) {
				continue;
			}
			rd = (red[i] - r);
			gd = (green[i] - g);
			bd = (blue[i] - b);
		/* gd 2.02: whoops, was - b (thanks to David Marwood) */
		/* gd 2.16: was blue rather than alpha! Geez! Thanks to
		   Artur Jakub Jerzak */
			ad = (alpha[i] - a);
			dist = rd * rd + gd * gd + bd * bd + ad * ad;
			if (first || (dist < mindist)) {
				mindist = dist;
				ct = i;
				first = false;
			}
		}
		return ct;
	}



	public GdImage imageClone() {
		GdImage dst;
		int i, x;

		if (this.trueColor) {
			dst = new GdImage(this.sx , this.sy, GdImageColorType.TRUE_COLOR);
		} else {
			dst = new GdImage(this.sx , this.sy, GdImageColorType.PALETTE_BASED_COLOR);
		}

		if (!this.trueColor) {
			dst.colorsTotal = this.colorsTotal;
			for (i = 0; i < GdUtils.MAX_COLORS; i++) {
				dst.red[i]   = this.red[i];
				dst.green[i] = this.green[i];
				dst.blue[i]  = this.blue[i];
				dst.alpha[i] = this.alpha[i];
				dst.open[i]  = this.open[i];
			}
			for (i = 0; i < this.sy; i++) {
				for (x = 0; x < this.sx; x++) {
					dst.pixels[i][x] = this.pixels[i][x];
				}
			}
		} else {
			for (i = 0; i < this.sy; i++) {
				for (x = 0; x < this.sx; x++) {
					dst.tpixels[i][x] = this.tpixels[i][x];
				}
			}
		}

		if (this.styleLength > 0) {
			dst.styleLength = this.styleLength;
			dst.stylePos    = this.stylePos;
			for (i = 0; i < this.styleLength; i++) {
				dst.style[i] = this.style[i];
			}
		}

		dst.interlace   = this.interlace;

		dst.alphaBlendingFlag = this.alphaBlendingFlag;
		dst.saveAlphaFlag     = this.saveAlphaFlag;
		dst.AA                = this.AA;
		dst.AA_color          = this.AA_color;
		dst.AA_dont_blend     = this.AA_dont_blend;

		dst.cx1 = this.cx1;
		dst.cy1 = this.cy1;
		dst.cx2 = this.cx2;
		dst.cy2 = this.cy2;

		dst.res_x = this.res_x;
		dst.res_y = this.res_x;

		dst.paletteQuantizationMethod     = this.paletteQuantizationMethod;
		dst.paletteQuantizationSpeed      = this.paletteQuantizationSpeed;
		dst.paletteQuantizationMinQuality = this.paletteQuantizationMinQuality;
		dst.paletteQuantizationMinQuality = this.paletteQuantizationMinQuality;

		dst.interpolation_id = this.interpolation_id;
		dst.interpolation    = this.interpolation;

		if (this.brush != null) {
			dst.brush = this.brush.imageClone();
		}

		if (this.tile != null) {
			dst.tile = this.tile.imageClone();
		}

		if (this.style != null) {
			dst.setStyle(this.style, this.styleLength);
		}

		for (i = 0; i < GdUtils.MAX_COLORS; i++) {
			dst.brushColorMap[i] = this.brushColorMap[i];
			dst.tileColorMap[i] = this.tileColorMap[i];
		}

		if (this.polyAllocated > 0) {
			dst.polyAllocated = this.polyAllocated;
			for (i = 0; i < this.polyAllocated; i++) {
				dst.polyInts[i] = this.polyInts[i];
			}
		}

		return dst;
	}

	public static void imageCopy(final GdImage dst, final GdImage src, final int dstX, final int dstY, final int srcX,
						  final int srcY, final int w, final int h) {
		if (dst.trueColor) {
		/* 2.0: much easier when the destination is truecolor. */
		/* 2.0.10: needs a transparent-index check that is still valid if
		 * the source is not truecolor. Thanks to Frank Warmerdam.
		 */

			if (src.trueColor) {
				for (int y = 0; (y < h); y++) {
					for (int x = 0; (x < w); x++) {
						int c = src.getTrueColorPixel(srcX + x, srcY + y);
						if (c != src.transparent) {
							dst.setPixel(dstX + x, dstY + y, c);
						}
					}
				}
			} else {
			/* source is palette based */
				for (int y = 0; (y < h); y++) {
					for (int x = 0; (x < w); x++) {
						int c = src.getPixel(srcX + x, srcY + y);
						if (c != src.transparent) {
							dst.setPixel(dstX + x, dstY + y, GdUtils.trueColorMixAlpha(
									src.red[c], src.green[c], src.blue[c], src.alpha[c]));
						}
					}
				}
			}
			return;
		}


		int[] colorMap = new int[GdUtils.MAX_COLORS];

		for (int i = 0; (i < GdUtils.MAX_COLORS); i++) {
			colorMap[i] = (-1);
		}
		int toy = dstY;
		for (int y = srcY; (y < (srcY + h)); y++) {
			int tox = dstX;
			for (int x = srcX; (x < (srcX + w)); x++) {
				int nc;
				int mapTo;
				int c = src.getPixel(x, y);
			/* Added 7/24/95: support transparent copies */
				if (src.transparent == c) {
					tox++;
					continue;
				}
			/* Have we established a mapping for this color? */
				if (src.trueColor) {
				/* 2.05: remap to the palette available in the
				 destination image. This is slow and
				 works badly, but it beats crashing! Thanks
				 to Padhrig McCarthy. */
					mapTo = dst.colorResolveAlpha(
							GdUtils.trueColorGetRed(c),
							GdUtils.trueColorGetGreen(c),
							GdUtils.trueColorGetBlue(c),
							GdUtils.trueColorGetAlpha(c));
				} else if (colorMap[c] == (-1)) {
				/* If it's the same image, mapping is trivial */
					if (dst == src) {
						nc = c;
					} else {
					/* Get best match possible. This
					   function never returns error. */
						nc = dst.colorResolveAlpha(
								src.red[c], src.green[c],
								src.blue[c], src.alpha[c]);
					}
					colorMap[c] = nc;
					mapTo = colorMap[c];
				} else {
					mapTo = colorMap[c];
				}
				dst.setPixel(tox, toy, mapTo);
				tox++;
			}
			toy++;
		}
	}

	/* This function is a substitute for real alpha channel operations,
	   so it doesn't pay attention to the alpha channel. */
	public static void imageCopyMerge(final GdImage dst, final GdImage src, final int dstX, final int dstY,
									  final int srcX, final int srcY, final int w, final int h, final int pct) {
		int toy = dstY;
		for (int y = srcY; (y < (srcY + h)); y++) {
			int tox = dstX;
			for (int x = srcX; (x < (srcX + w)); x++) {
				int nc;
				final int c = src.getPixel(x, y);
			/* Added 7/24/95: support transparent copies */
				if (src.transparent == c) {
					tox++;
					continue;
				}
			/* If it's the same image, mapping is trivial */
				if (dst == src) {
					nc = c;
				} else {
					final int dc = dst.getPixel(tox, toy);

					final int ncR = (int) (src.getRed(c) * (pct / 100.0)
							+ dst.getRed(dc) * ((100 - pct) / 100.0));
					final int ncG = (int) (src.getGreen(c) * (pct / 100.0)
							+ dst.getGreen(dc) * ((100 - pct) / 100.0));
					final int ncB = (int) (src.getBlue(c) * (pct / 100.0)
							+ dst.getBlue(dc) * ((100 - pct) / 100.0));

				/* Find a reasonable color */
					nc = dst.colorResolve(ncR, ncG, ncB);
				}
				dst.setPixel(tox, toy, nc);
				tox++;
			}
			toy++;
		}
	}

	/* This function is a substitute for real alpha channel operations,
	   so it doesn't pay attention to the alpha channel. */
	public static void imageCopyMergeGray(GdImage dst, GdImage src, int dstX, int dstY,
	int srcX, int srcY, int w, int h, int pct)
	{

		int c, dc;
		int x, y;
		int tox, toy;
		int ncR, ncG, ncB;
		float g;
		toy = dstY;
		for (y = srcY; (y < (srcY + h)); y++) {
			tox = dstX;
			for (x = srcX; (x < (srcX + w)); x++) {
				int nc;
				c = src.getPixel(x, y);
			/* Added 7/24/95: support transparent copies */
				if (src.transparent == c) {
					tox++;
					continue;
				}
			/*
			 * If it's the same image, mapping is NOT trivial since we
			 * merge with greyscale target, but if pct is 100, the grey
			 * value is not used, so it becomes trivial. pjw 2.0.12.
			 */
				if (dst == src && pct == 100) {
					nc = c;
				} else {
					dc = dst.getPixel(tox, toy);
					g = (int) (0.29900 * dst.getRed(dc)
							+ 0.58700 * dst.getGreen(dc) + 0.11400 * dst.getBlue(dc));

					ncR = (int) (src.getRed(c) * (pct / 100.0)
							+ g * ((100 - pct) / 100.0));
					ncG = (int) (src.getGreen(c) * (pct / 100.0)
							+ g * ((100 - pct) / 100.0));
					ncB = (int) (src.getBlue(c) * (pct / 100.0)
							+ g * ((100 - pct) / 100.0));

				/* First look for an exact match */
					nc = dst.colorExact(ncR, ncG, ncB);
					if (nc == (-1)) {
					/* No, so try to allocate it */
						nc = dst.colorAllocate(ncR, ncG, ncB);
					/* If we're out of colors, go for the
					   closest color */
						if (nc == (-1)) {
							nc = dst.findColorClosest(ncR, ncG, ncB);
						}
					}
				}
				dst.setPixel(tox, toy, nc);
				tox++;
			}
			toy++;
		}
	}

/* Stretches or shrinks to fit, as needed. Does NOT attempt
   to average the entire set of source pixels that scale down onto the
   destination pixel. */
	public static void imageCopyResized(final GdImage dst, final GdImage src, final int dstX, final int dstY,
								   final int srcX, final int srcY, final int dstW, final int dstH,
								   final int srcW, final int srcH) {
		int[] colorMap = new int[GdUtils.MAX_COLORS];
	/* Stretch vectors */
	/* We only need to use floating point to determine the correct
	   stretch vector for one line's worth. */
		int[] stx = new int[srcW];
		int[] sty = new int[srcH];

	/* Fixed by Mao Morimoto 2.0.16 */
		for (int i = 0; (i < srcW); i++) {
			stx[i] = dstW * (i + 1) / srcW - dstW * i / srcW;
		}
		for (int i = 0; (i < srcH); i++) {
			sty[i] = dstH * (i + 1) / srcH - dstH * i / srcH;
		}
		for (int i = 0; (i < GdUtils.MAX_COLORS); i++) {
			colorMap[i] = (-1);
		}
		int toy = dstY;
		for (int y = srcY; (y < (srcY + srcH)); y++) {
			for (int ydest = 0; (ydest < sty[y - srcY]); ydest++) {
				int tox = dstX;
				for (int x = srcX; (x < (srcX + srcW)); x++) {
					int nc = 0;
					int mapTo;
					if (stx[x - srcX] == 0) {
						continue;
					}
					if (dst.trueColor) {
					/* 2.0.9: Thorben Kundinger: Maybe the source image is not
					   a truecolor image */
						if (!src.trueColor) {
							int tmp = src.getPixel(x, y);
							mapTo = src.getTrueColorPixel(x, y);
							if (src.transparent == tmp) {
							/* 2.0.21, TK: not tox++ */
								tox += stx[x - srcX];
								continue;
							}
						} else {
						/* TK: old code follows */
							mapTo = src.getTrueColorPixel(x, y);
						/* Added 7/24/95: support transparent copies */
							if (src.transparent == mapTo) {
							/* 2.0.21, TK: not tox++ */
								tox += stx[x - srcX];
								continue;
							}
						}
					} else {
						final int c = src.getPixel(x, y);
					/* Added 7/24/95: support transparent copies */
						if (src.transparent == c) {
							tox += stx[x - srcX];
							continue;
						}
						if (src.trueColor) {
						/* Remap to the palette available in the
						   destination image. This is slow and
						   works badly. */
							mapTo = dst.colorResolveAlpha(
									GdUtils.trueColorGetRed(c),
									GdUtils.trueColorGetGreen(c),
									GdUtils.trueColorGetBlue(c),
									GdUtils.trueColorGetAlpha(c));
						} else {
						/* Have we established a mapping for this color? */
							if (colorMap[c] == (-1)) {
							/* If it's the same image, mapping is trivial */
								if (dst == src) {
									nc = c;
								} else {
								/* Find or create the best match */
								/* 2.0.5: can't use gdTrueColorGetRed, etc with palette */
									nc = dst.colorResolveAlpha(
											src.getRed(c),
											src.getGreen(c),
											src.getBlue(c),
											src.getAlpha(c));
								}
								colorMap[c] = nc;
							}
							mapTo = colorMap[c];
						}
					}
					for (int i = 0; (i < stx[x - srcX]); i++) {
						dst.setPixel(tox, toy, mapTo);
						tox++;
					}
				}
				toy++;
			}
		}
	}

/* gd 2.0.8: gdImageCopyRotated is added. Source
	is a rectangle, with its upper left corner at
	srcX and srcY. Destination is the *center* of
		the rotated copy. Angle is in degrees, same as
		gdImageArc. Floating point destination center
	coordinates allow accurate rotation of
	objects of odd-numbered width or height. */

/* gd 2.0.8: gdImageCopyRotated is added. Source
   is a rectangle, with its upper left corner at
   srcX and srcY. Destination is the *center* of
   the rotated copy. Angle is in degrees, same as
   gdImageArc. Floating point destination center
   coordinates allow accurate rotation of
   objects of odd-numbered width or height. */
	public static void imageCopyRotated(final GdImage dst, final GdImage src, final double dstX, final double dstY,
								 final int srcX, final int srcY,
								 final int srcWidth, final int srcHeight, final int angle) {
		double radius = sqrt(srcWidth * srcWidth + srcHeight * srcHeight);
		double aCos = cos(angle * .0174532925);
		double aSin = sin(angle * .0174532925);
		double scX = srcX + ((double) srcWidth) / 2;
		double scY = srcY + ((double) srcHeight) / 2;
		int[] cmap = new int[GdUtils.MAX_COLORS];
		int i;

	/*
		 2.0.34: transparency preservation. The transparentness of
		 the transparent color is more important than its hue.
	*/
		if (src.transparent != -1) {
			if (dst.transparent == -1) {
				dst.transparent = src.transparent;
			}
		}

		for (i = 0; (i < GdUtils.MAX_COLORS); i++) {
			cmap[i] = (-1);
		}
		for (double dy = dstY - radius; (dy <= dstY + radius); dy++) {
			for (double dx = dstX - radius; (dx <= dstX + radius); dx++) {
				double sxd = (dx - dstX) * aCos - (dy - dstY) * aSin;
				double syd = (dy - dstY) * aCos + (dx - dstX) * aSin;
				int sx = (int) (sxd + scX);
				int sy = (int) (syd + scY);
				if ((sx >= srcX) && (sx < srcX + srcWidth) &&
						(sy >= srcY) && (sy < srcY + srcHeight)) {
					int c = src.getPixel(sx, sy);
				/* 2.0.34: transparency wins */
					if (c == src.transparent) {
						dst.setPixel((int) dx, (int) dy, dst.transparent);
					} else if (!src.trueColor) {
					/* Use a table to avoid an expensive
					   lookup on every single pixel */
						if (cmap[c] == -1) {
							cmap[c] = dst.colorResolveAlpha(
									src.getRed(c),
									src.getGreen(c),
									src.getBlue(c),
									src.getAlpha(c));
						}
						dst.setPixel((int) dx, (int) dy, cmap[c]);
					} else {
						dst.setPixel((int) dx, (int) dy, dst.colorResolveAlpha(
								src.getRed(c),
								src.getGreen(c),
								src.getBlue(c),
								src.getAlpha(c)));
					}
				}
			}
		}
	}

/* When gd 1.x was first created, floating point was to be avoided.
   These days it is often faster than table lookups or integer
   arithmetic. The routine below is shamelessly, gloriously
   floating point. TBB */

/* 2.0.10: cast instead of floor() yields 35% performance improvement.
	Thanks to John Buckman. */

//	#define floor2(exp) ((long) exp)
	private static long floor2(final double exp) {
		return (long) exp;
	}
/*#define floor2(exp) floor(exp)*/

/* gd 2.0: stretches or shrinks to fit, as needed. When called with a
   truecolor destination image, this function averages the
   entire set of source pixels that scale down onto the
   destination pixel, taking into account what portion of the
   destination pixel each source pixel represents. This is a
   floating point operation, but this is not a performance issue
   on modern hardware, except for some embedded devices. If the
   destination is a palette image, gdImageCopyResized is
   substituted automatically. */
	public static void imageCopyResampled(final GdImage dst, final GdImage src,
										  final int dstX, final int dstY, final int srcX, final int srcY,
										  final int dstW, final int dstH, final int srcW, final int srcH) {
		double sy1, sy2, sx1, sx2;
		if (!dst.trueColor) {
			imageCopyResized(dst, src, dstX, dstY, srcX, srcY, dstW, dstH, srcW, srcH);
			return;
		}
		for (int y = dstY; (y < dstY + dstH); y++) {
			sy1 = ((double) y - (double) dstY) * (double) srcH / (double) dstH;
			sy2 = ((double) (y + 1) - (double) dstY) * (double) srcH /
					(double) dstH;
			for (int x = dstX; (x < dstX + dstW); x++) {
				double sx, sy;
				double spixels = 0;
				double red = 0.0, green = 0.0, blue = 0.0, alpha = 0.0;
				double alpha_sum = 0.0, contrib_sum = 0.0;

				sx1 = ((double) x - (double) dstX) * (double) srcW / dstW;
				sx2 = ((double) (x + 1) - (double) dstX) * (double) srcW / dstW;
				sy = sy1;
				do {
					double yportion;
					if (floor2 (sy) == floor2 (sy1)) {
						yportion = 1.0 - (sy - floor2 (sy));
						if (yportion > sy2 - sy1) {
							yportion = sy2 - sy1;
						}
						sy = floor2 (sy);
					} else if (sy == floor2 (sy2)) {
						yportion = sy2 - floor2 (sy2);
					} else {
						yportion = 1.0;
					}
					sx = sx1;
					do {
						double xportion;
						double pcontribution;
						int p;
						if (floor2 (sx) == floor2 (sx1)) {
							xportion = 1.0 - (sx - floor2 (sx));
							if (xportion > sx2 - sx1) {
								xportion = sx2 - sx1;
							}
							sx = floor2 (sx);
						} else if (sx == floor2 (sx2)) {
							xportion = sx2 - floor2 (sx2);
						} else {
							xportion = 1.0;
						}
						pcontribution = xportion * yportion;
					/* 2.08: previously srcX and srcY were ignored.
					   Andrew Pattison */
						p = src.getTrueColorPixel(
								(int) sx + srcX,
								(int) sy + srcY);
						red += GdUtils.trueColorGetRed(p) * pcontribution;
						green += GdUtils.trueColorGetGreen(p) * pcontribution;
						blue += GdUtils.trueColorGetBlue(p) * pcontribution;
						alpha += GdUtils.trueColorGetAlpha(p) * pcontribution;
						spixels += xportion * yportion;
						sx += 1.0;
					} while (sx < sx2);
					sy += 1.0;
				} while (sy < sy2);
				if (spixels != 0.0) {
					red /= spixels;
					green /= spixels;
					blue /= spixels;
					alpha /= spixels;
					alpha += 0.5;
				}
				if ( alpha_sum != 0.0f) {
					if( contrib_sum != 0.0f) {
						alpha_sum /= contrib_sum;
					}
					red /= alpha_sum;
					green /= alpha_sum;
					blue /= alpha_sum;
				}
			/* Clamping to allow for rounding errors above */
				if (red > 255.0) {
					red = 255.0;
				}
				if (green > 255.0) {
					green = 255.0;
				}
				if (blue > 255.0) {
					blue = 255.0;
				}
				if (alpha > GdUtils.ALPHA_MAX) {
					alpha = GdUtils.ALPHA_MAX;
				}
				dst.setPixel(
						x, y,
						GdUtils.trueColorMixAlpha((int) red,
								(int) green,
								(int) blue, (int) alpha));
			}
		}
	}

	private boolean isColorMatch(final int col1, final int col2, final float threshold) {
		final int dr = getRed(col1) - getRed(col2);
		final int dg = getGreen(col1) - getGreen(col2);
		final int db = getBlue(col1) - getBlue(col2);
		final int da = getAlpha(col1) - getAlpha(col2);
		final int dist = dr * dr + dg * dg + db * db + da * da;

		return (100.0 * dist / 195075) < threshold;
	}

	public int colorReplace(final int src, final int dst) {
		if (src == dst) {
			return 0;
		}

		int n = 0;

		if (trueColor) {
			for (int y = cy1; y <= cy2; y++) {
				for (int x = cx1; x <= cx2; x++) {
					if (getTrueColorPixel(x, y) == src) {
						setPixel(x, y, dst);
						n++;
					}
				}
			}
		} else {
			for (int y = cy1; y <= cy2; y++) {
				for (int x = cx1; x <= cx2; x++) {
					if (getPalettePixel(x, y) == src) {
						setPixel(x, y, dst);
						n++;
					}
				}
			}
		}

		return n;
	}

	public int colorReplaceThreshold(final int src, final int dst, final float threshold) {
		if (src == dst) {
			return 0;
		}

		int n = 0;

		if (trueColor) {
			for (int y = cy1; y <= cy2; y++) {
				for (int x = cx1; x <= cx2; x++) {
					if (isColorMatch(src, getTrueColorPixel(x, y), threshold)) {
						setPixel(x, y, dst);
						n++;
					}
				}
			}
		} else {
			for (int y = cy1; y <= cy2; y++) {
				for (int x = cx1; x <= cx2; x++) {
					if (isColorMatch(src, getPalettePixel(x, y), threshold)) {
						setPixel(x, y, dst);
						n++;
					}
				}
			}
		}

		return n;
	}

	public int colorReplaceArray(final int[] src, final int[] dst) {
		if (src.length != dst.length || src == dst) {
			return 0;
		}
		if (src.length == 1) {
			return colorReplace(src[0], dst[0]);
		}

		final Map<Integer, Integer> base = new HashMap<Integer, Integer>();
		for (int i = 0; i < src.length; i++) {
			base.put(src[i], dst[i]);
		}

		int n = 0;

		if (trueColor) {
			for (int y = cy1; y <= cy2; y++) {
				for (int x = cx1; x <= cx2; x++) {
					final int c = getTrueColorPixel(x, y);
					if (base.containsKey(c)) {
						setPixel(x, y, base.get(c));
						n++;
					}
				}
			}
		} else {
			for (int y = cy1; y <= cy2; y++) {
				for (int x = cx1; x <= cx2; x++) {
					final int c = getPalettePixel(x, y);
					if (base.containsKey(c)) {
						setPixel(x, y, base.get(c));
						n++;
					}
				}
			}
		}

		return n;
	}

	public int colorReplaceCallback(GdCallbackImageColor callback) {
		if (callback == null) {
			return 0;
		}

		int n = 0;

		if (trueColor) {
			for (int y = cy1; y <= cy2; y++) {
				for (int x = cx1; x <= cx2; x++) {
					final int c = getTrueColorPixel(x, y);
					final int d = callback.callbackImageColor(this, c);
					if (d != c) {
						setPixel(x, y, d);
						n++;
					}
				}
			}
		} else { /* palette */
			int len = 0;

			int[] sarr = new int[colorsTotal];
			for (int c = 0; c < colorsTotal; c++) {
				if (!open[c]) {
					sarr[len++] = c;
				}
			}
			final int[] darr = new int[len];
			for (int k = 0; k < len; k++) {
				darr[k] = callback.callbackImageColor(this, sarr[k]);
			}
			n = colorReplaceArray(sarr, darr);
		}
		return n;
	}

	public void writeChar(final GdFont f, final int x, final int y, final int c, final int color) {
		int cx = 0;
		int cy = 0;
		if ((c < f.offset) || (c >= (f.offset + f.nchars))) {
			return;
		}
		final int fline = (c - f.offset) * f.h * f.w;
		for (int py = y; (py < (y + f.h)); py++) {
			for (int px = x; (px < (x + f.w)); px++) {
				if (f.data[fline + cy * f.w + cx] != 0) {
					setPixel(px, py, color);
				}
				cx++;
			}
			cx = 0;
			cy++;
		}
	}

	public void writeCharUp(final GdFont f, final int x, final int y, final int c, final int color) {
		int cx = 0;
		int cy = 0;
		if ((c < f.offset) || (c >= (f.offset + f.nchars))) {
			return;
		}

		final int fline = (c - f.offset) * f.h * f.w;
		for (int py = y; (py > (y - f.w)); py--) {
			for (int px = x; (px < (x + f.h)); px++) {
				if (f.data[fline + cy * f.w + cx] != 0) {
					setPixel(px, py, color);
				}
				cy++;
			}
			cy = 0;
			cx++;
		}
	}

	public void writeString(final GdFont f, int x, final int y, final String s, final int color) {
		final int l = s.length();
		for (int i = 0; (i < l); i++) {
			writeChar(f, x, y, s.charAt(i), color);
			x += f.w;
		}
	}

	public void writeStringUp(final GdFont f, final int x, int y, final String s, final int color) {
		final int l = s.length();
		for (int i = 0; (i < l); i++) {
			writeCharUp(f, x, y, s.charAt(i), color);
			y -= f.w;
		}
	}

	public void flipVertical() {
		if (trueColor) {
			for (int y = 0; y < sy / 2; y++) {
				int[] row_dst = tpixels[y];
				int[] row_src = tpixels[sy - 1 - y];
				for (int x = 0; x < sx; x++) {
					int p;
					p = row_dst[x];
					row_dst[x] = tpixels[sy - 1 - y][x];
					row_src[x] = p;
				}
			}
		} else {
			for (int y = 0; y < sy / 2; y++) {
				for (int x = 0; x < sx; x++) {
					int p = tpixels[y][x];
					tpixels[y][x] =	tpixels[sy - 1 - y][x];
					tpixels[sy - 1 - y][x] = p;
				}
			}
		}
	}

	public void flipHorizontal() {
		if (trueColor) {
			for (int y = 0; y < sy; y++) {
				final int[] row = tpixels[y];
				for (int x = 0; x < (sx >> 1); x++) {
					final int z = sx - 1 - x;
					final int tmp = row[x];
					row[x] = row[z];
					row[z] = tmp;
				}
			}
		} else {
			for (int y = 0; y < sy; y++) {
				final int[] row = pixels[y];
				for (int x = 0; x < (sx >> 1); x++) {
					final int z = sx - 1 - x;
					final int tmp = row[x];
					row[x] = row[z];
					row[z] = tmp;
				}
			}
		}
	}

	public void flipBoth() {
		flipVertical();
		flipHorizontal();
	}

/* This code is taken from http://www.acm.org/jgt/papers/SmithLyons96/hwb_rgb.html, an article
 * on colour conversion to/from RBG and HWB colour systems.
 * It has been modified to return the converted value as a * parameter.
 */

//	#define RETURN_HWB(h, w, b) {HWB->H = h; HWB->W = w; HWB->B = b; return HWB;}
//	#define RETURN_RGB(r, g, b) {RGB->R = r; RGB->G = g; RGB->B = b; return RGB;}
	private static double HWB_UNDEFINED = -1;

	private static double MIN(final double a, final double b) {
		return ((a)<(b)?(a):(b));
	}

	private static double MIN3(final double a,final double b,final double c) {
		return ((a)<(b)?(MIN(a,c)):(MIN(b,c)));
	}

	private static double MAX(final double a, final double b) {
		return ((a)<(b)?(b):(a));
	}

	private static double MAX3(final double a, final double b, final double c) {
		return ((a)<(b)?(MAX(b,c)):(MAX(a,c)));
	}

	private static int MIN(final int a, final int b) {
		return ((a)<(b)?(a):(b));
	}

	private static int MAX(final int a, final int b) {
		return ((a)<(b)?(b):(a));
	}


	/*
	 * Theoretically, hue 0 (pure red) is identical to hue 6 in these transforms. Pure
	 * red always maps to 6 in this implementation. Therefore UNDEFINED can be
	 * defined as 0 in situations where only unsigned numbers are desired.
	 */
	static class GdRGBType {
		double R, G, B;
		public GdRGBType(final double r, final double g, final double b) {
			this.R = r/255.0; this.G = g/255.0; this.B = b/255.0;
		}
	}

	static class GdHWBType {
		double H, W, B;
		public GdHWBType(final double h, final double w, final double b) {
			this.H = h; this.W = w; this.B = b;
		}
	}

	private static GdHWBType RGB_to_HWB(GdRGBType RGB) {

	/*
	 * RGB are each on [0, 1]. W and B are returned on [0, 1] and H is
	 * returned on [0, 6]. Exception: H is returned UNDEFINED if W == 1 - B.
	 */

		double R = RGB.R, G = RGB.G, B = RGB.B, w, v, b, f;
		int i;

		w = MIN3(R, G, B);
		v = MAX3(R, G, B);
		b = 1 - v;
		if (v == w)
			return new GdHWBType(HWB_UNDEFINED, w, b);
		f = (R == w) ? G - B : ((G == w) ? B - R : R - G);
		i = (R == w) ? 3 : ((G == w) ? 5 : 1);
		return new GdHWBType(i - f / (v - w), w, b);
	}

	static double HWB_Diff(final int r1, final int g1, final int b1, final int r2, final int g2, final int b2) {
		double diff;

		final GdRGBType RGB1 = new GdRGBType(r1, g1, b1);
		final GdRGBType RGB2 = new GdRGBType(r2, g2, b2);

		final GdHWBType HWB1 = RGB_to_HWB(RGB1);
		final GdHWBType HWB2 = RGB_to_HWB (RGB2);

	/*
	 * I made this bit up; it seems to produce OK results, and it is certainly
	 * more visually correct than the current RGB metric. (PJW)
	 */

		if ((HWB1.H == HWB_UNDEFINED) || (HWB2.H == HWB_UNDEFINED)) {
			diff = 0;			/* Undefined hues always match... */
		} else {
			diff = (HWB1.H - HWB2.H);
			if (diff > 3) {
				diff = 6 - diff;	/* Remember, it's a colour circle */
			}
		}

		diff =
				diff * diff + (HWB1.W - HWB2.W) * (HWB1.W - HWB2.W) + (HWB1.B -
						HWB2.B) * (HWB1.B -
						HWB2.B);

		return diff;
	}

	/* An alternate method */
	public int colorClosestHWB(final int r, final int g, final int b) {
		int i;
	/* long rd, gd, bd; */
		int ct = (-1);
		boolean first = true;
		double mindist = 0;
		if (trueColor) {
			return GdUtils.trueColorMix(r, g, b);
		}
		for (i = 0; (i < (colorsTotal)); i++) {
			double dist;
			if (open[i]) {
				continue;
			}
			dist = HWB_Diff (red[i], green[i], blue[i], r, g, b);
			if (first || (dist < mindist)) {
				mindist = dist;
				ct = i;
				first = false;
			}
		}
		return ct;
	}

	/* Specifies a color index (if a palette image) or an
	   RGB color (if a truecolor image) which should be
	   considered 100% transparent. FOR TRUECOLOR IMAGES,
	   THIS IS IGNORED IF AN ALPHA CHANNEL IS BEING
	   SAVED. Use gdImageSaveAlpha(im, 0); to
	   turn off the saving of a full alpha channel in
	   a truecolor image. Note that gdImageColorTransparent
	   is usually compatible with older browsers that
	   do not understand full alpha channels well. TBB */
	public void gdImageColorTransparent(int color) {
		if (!trueColor) {
			if((color < -1) || (color >= GdUtils.MAX_COLORS)) {
				return;
			}
			if (transparent != -1) {
				alpha[transparent] = GdUtils.ALPHA_OPAQUE;
			}
			if (color != -1) {
				alpha[color] = GdUtils.ALPHA_TRANSPARENT;
			}
		}
		transparent = color;
	}

	public static void paletteCopy(GdImage to, GdImage from) {
		int i;
		int x, y, p;
		int[] xlate = new int[256];
		if (to.trueColor) {
			return;
		}
		if (from.trueColor) {
			return;
		}

		for (i = 0; i < 256; i++) {
			xlate[i] = -1;
		};

		for (y = 0; y < (to.sy); y++) {
			for (x = 0; x < (to.sx); x++) {
			/* Optimization: no gdImageGetPixel */
				p = to.pixels[y][x];
				if (xlate[p] == -1) {
				/* This ought to use HWB, but we don't have an alpha-aware
				   version of that yet. */
					xlate[p] = from.findColorClosestAlpha(to.red[p], to.green[p], to.blue[p], to.alpha[p]);
				/*printf("Mapping %d (%d, %d, %d, %d) to %d (%d, %d, %d, %d)\n", */
				/*      p,  to->red[p], to->green[p], to->blue[p], to->alpha[p], */
				/*      xlate[p], from->red[xlate[p]], from->green[xlate[p]], from->blue[xlate[p]], from->alpha[xlate[p]]); */
				};
			/* Optimization: no gdImageSetPixel */
				to.pixels[y][x] = xlate[p];
			};
		};

		for (i = 0; (i < (from.colorsTotal)); i++) {
		/*printf("Copying color %d (%d, %d, %d, %d)\n", i, from->red[i], from->blue[i], from->green[i], from->alpha[i]); */
			to.red[i] = from.red[i];
			to.blue[i] = from.blue[i];
			to.green[i] = from.green[i];
			to.alpha[i] = from.alpha[i];
			to.open[i] = false;
		};

		for (i = from.colorsTotal; (i < to.colorsTotal); i++) {
			to.open[i] = true;
		};

		to.colorsTotal = from.colorsTotal;
	}
/*
 * The two pass scaling function is based on:
 * Filtered Image Rescaling
 * Based on Gems III
 *  - Schumacher general filtered image rescaling
 * (pp. 414-424)
 * by Dale Schumacher
 *
 * 	Additional changes by Ray Gardener, Daylon Graphics Ltd.
 * 	December 4, 1999
 *
 * 	Ported to libgd by Pierre Joye. Support for multiple channels
 * 	added (argb for now).
 *
 * 	Initial sources code is avaibable in the Gems Source Code Packages:
 * 	http://www.acm.org/pubs/tog/GraphicsGems/GGemsIII.tar.gz
 *
 */

/*
	Summary:

		- Horizontal filter contributions are calculated on the fly,
		  as each column is mapped from src to dst image. This lets
		  us omit having to allocate a temporary full horizontal stretch
		  of the src image.

		- If none of the src pixels within a sampling region differ,
		  then the output pixel is forced to equal (any of) the source pixel.
		  This ensures that filters do not corrupt areas of constant color.

		- Filter weight contribution results, after summing, are
		  rounded to the nearest pixel color value instead of
		  being casted to ILubyte (usually an int or char). Otherwise,
		  artifacting occurs.

*/

/*
	Additional functions are available for simple rotation or up/downscaling.
	downscaling using the fixed point implementations are usually much faster
	than the existing gdImageCopyResampled while having a similar or better
	quality.

	For image rotations, the optimized versions have a lazy antialiasing for
	the edges of the images. For a much better antialiased result, the affine
	function is recommended.
*/

/*
TODO:
 - Optimize pixel accesses and loops once we have continuous buffer
 - Add scale support for a portion only of an image (equivalent of copyresized/resampled)
 */

	public int CLAMP(final int x, final int low, final int high) {
		return (((x) > (high)) ? (high) : (((x) < (low)) ? (low) : (x)));
	}

/* only used here, let do a generic fixed point integers later if required by other
   part of GD */
//	typedef long gdFixed;
/* Integer to fixed point */
	private static long gd_itofx(final int x) {
		return ((x) << 8);
	}

/* Float to fixed point */
	private static long gd_ftofx(final float x) {
		return (long)((x) * 256);
	}

/*  Double to fixed point */
	private static long gd_dtofx(final double x) {
		return (long)((x) * 256);
	}

/* Fixed point to integer */
	private static int gd_fxtoi(final long x) {
		return (int) ((x) >> 8);
	}

/* Fixed point to float */
	private static float gd_fxtof(final long x) {
		return ((float)(x) / 256);
	}

/* Fixed point to double */
	private static double gd_fxtod(final long x) {
		return ((double)(x) / 256);
	}

/* Multiply a fixed by a fixed */
	private static long gd_mulfx(final long x, final long y) {
		return (((x) * (y)) >> 8);
	}

/* Divide a fixed by a fixed */
	private static long gd_divfx(final long x, final long y) {
		return (((x) << 8) / (y));
	}

	private static class ContributionType {
		double[] Weights;  /* Normalized weights of neighboring pixels */
		int Left,Right;   /* Bounds of source pixels window */

		public ContributionType(final int windows_size) {
			this.Weights = new double[windows_size];
		}
	}  /* Contirbution information for a single pixel */

	private static class LineContribType {
		ContributionType[] ContribRow; /* Row (or column) of contribution weights */
		int WindowSize,      /* Filter window size (of affecting source pixels) */
			LineLength;      /* Length of line (no. or rows / cols) */

		public LineContribType(final int line_length, final int windows_size) {
			this.WindowSize = windows_size;
			this.LineLength = line_length;
			this.ContribRow = new ContributionType[line_length];
			for (int u = 0 ; u < line_length ; u++) {
				this.ContribRow[u] = new ContributionType(windows_size);
			}
		}
	}

/* Each core filter has its own radius */
	private static final double DEFAULT_FILTER_BICUBIC				= 3.0;
	private static final double DEFAULT_FILTER_RADIUS				= 1.0;
	private static final double DEFAULT_HERMITE_RADIUS				= 1.0;
	private static final double DEFAULT_BOX_RADIUS					= 0.5;
	private static final double DEFAULT_TRIANGLE_RADIUS				= 1.0;
	private static final double DEFAULT_BELL_RADIUS					= 1.5;
	private static final double DEFAULT_CUBICSPLINE_RADIUS			= 2.0;
	private static final double DEFAULT_MITCHELL_RADIUS				= 2.0;
	private static final double DEFAULT_COSINE_RADIUS				= 1.0;
	private static final double DEFAULT_CATMULLROM_RADIUS			= 2.0;
	private static final double DEFAULT_QUADRATIC_RADIUS			= 1.5;
	private static final double DEFAULT_QUADRATICBSPLINE_RADIUS		= 1.5;
	private static final double DEFAULT_CUBICCONVOLUTION_RADIUS		= 3.0;
	private static final double DEFAULT_GAUSSIAN_RADIUS				= 1.0;
	private static final double DEFAULT_HANNING_RADIUS				= 1.0;
	private static final double DEFAULT_HAMMING_RADIUS				= 1.0;
	private static final double DEFAULT_SINC_RADIUS					= 1.0;
	private static final double DEFAULT_WELSH_RADIUS				= 1.0;

/* Copied from upstream's libgd */
	public int _color_blend (final int dst, final int src)
	{
		final int src_alpha = GdUtils.trueColorGetAlpha(src);

		if( src_alpha == GdUtils.ALPHA_OPAQUE ) {
			return src;
		} else {
			final int dst_alpha = GdUtils.trueColorGetAlpha(dst);

			if( src_alpha == GdUtils.ALPHA_TRANSPARENT ) return dst;
			if( dst_alpha == GdUtils.ALPHA_TRANSPARENT ) {
				return src;
			} else {
				int alpha, red, green, blue;
				final int src_weight = GdUtils.ALPHA_TRANSPARENT - src_alpha;
				final int dst_weight = (GdUtils.ALPHA_TRANSPARENT - dst_alpha) * src_alpha / GdUtils.ALPHA_MAX;
				final int tot_weight = src_weight + dst_weight;

				alpha = src_alpha * dst_alpha / GdUtils.ALPHA_MAX;

				red = (GdUtils.trueColorGetRed(src) * src_weight
						+ GdUtils.trueColorGetRed(dst) * dst_weight) / tot_weight;
				green = (GdUtils.trueColorGetGreen(src) * src_weight
						+ GdUtils.trueColorGetGreen(dst) * dst_weight) / tot_weight;
				blue = (GdUtils.trueColorGetBlue(src) * src_weight
						+ GdUtils.trueColorGetBlue(dst) * dst_weight) / tot_weight;

				return ((alpha << 24) + (red << 16) + (green << 8) + blue);
			}
		}
	}

	private int _setEdgePixel(final int x, final int y, final long coverage, final int bgColor) {
		final long f_127 = gd_itofx(127);
		int c = this.tpixels[y][x];
		c = c | (( (int) (gd_fxtof(gd_mulfx(coverage, f_127)) + 50.5f)) << 24);
		return _color_blend(bgColor, c);
	}

	private int getPixelOverflowTC(final int x, final int y, final int bgColor) {
		if (isBoundsSafe(x, y)) {
			final int c = this.tpixels[y][x];
			if (c == this.transparent) {
				return bgColor == -1 ? GdUtils.trueColorMixAlpha(0, 0, 0, 127) : bgColor;
			}
			return c;
		} else {
			int border = 0;

			if (y < this.cy1) {
				border = this.tpixels[0][this.cx1];
				return getPixelOverflowTCProcessBorder(border);
			}

			if (y < this.cy1) {
				border = this.tpixels[0][this.cx1];
				return getPixelOverflowTCProcessBorder(border);
			}

			if (y > this.cy2) {
				if (x >= this.cx1 && x <= this.cx1) {
					border = this.tpixels[this.cy2][x];
					return getPixelOverflowTCProcessBorder(border);
				} else {
					return GdUtils.trueColorMixAlpha(0, 0, 0, 127);
				}
			}

		/* y is bound safe at this point */
			if (x < this.cx1) {
				border = this.tpixels[y][this.cx1];
				return getPixelOverflowTCProcessBorder(border);
			}

			if (x > this.cx2) {
				border = this.tpixels[y][this.cx2];
			}

			return getPixelOverflowTCProcessBorder(border);
		}
	}

	private int getPixelOverflowTCProcessBorder(final int border) {
		if (border == this.transparent) {
			return GdUtils.trueColorMixAlpha(0, 0, 0, 127);
		} else{
			return GdUtils.trueColorMixAlpha(GdUtils.trueColorGetRed(border), GdUtils.trueColorGetGreen(border), GdUtils.trueColorGetBlue(border), 127);
		}
	}

	private int colorIndex2RGBA(final int c) {
		return GdUtils.trueColorMixAlpha(this.red[(c)], this.green[(c)], this.blue[(c)], this.alpha[(c)]);
	}

	private int colorIndex2RGBcustomA(final int c, final int a) {
		return GdUtils.trueColorMixAlpha(this.red[(c)], this.green[(c)], this.blue[(c)], this.alpha[(a)]);
	}

	private int getPixelOverflowPalette(final int x, final int y, final int bgColor)
	{
		if (isBoundsSafe(x, y)) {
			final int c = this.pixels[y][x];
			if (c == this.transparent) {
				return bgColor == -1 ? GdUtils.trueColorMixAlpha(0, 0, 0, 127) : bgColor;
			}
			return colorIndex2RGBA(c);
		} else {
			int border = 0;
			if (y < this.cy1) {
				border = getPixel(this.cx1, 0);
				return getPixelOverflowPaletteProcessBorder(border);
			}

			if (y < this.cy1) {
				border = getPixel(this.cx1, 0);
				return getPixelOverflowPaletteProcessBorder(border);
			}

			if (y > this.cy2) {
				if (x >= this.cx1 && x <= this.cx1) {
					border = getPixel(x, this.cy2);
					return getPixelOverflowPaletteProcessBorder(border);
				} else {
					return GdUtils.trueColorMixAlpha(0, 0, 0, 127);
				}
			}

		/* y is bound safe at this point */
			if (x < this.cx1) {
				border = getPixel(this.cx1, y);
				return getPixelOverflowPaletteProcessBorder(border);
			}

			if (x > this.cx2) {
				border = getPixel(this.cx2, y);
			}

			return getPixelOverflowPaletteProcessBorder(border);
		}
	}

	private int getPixelOverflowPaletteProcessBorder(final int border) {
		if (border == this.transparent) {
			return GdUtils.trueColorMixAlpha(0, 0, 0, 127);
		} else{
			return colorIndex2RGBcustomA(border, 127);
		}
	}

	private int getPixelInterpolateWeight(final double x, final double y, final int bgColor)
	{
	/* Closest pixel <= (xf,yf) */
		int sx = (int)(x);
		int sy = (int)(y);
		final double xf = x - (double)sx;
		final double yf = y - (double)sy;
		final double nxf = (double) 1.0 - xf;
		final double nyf = (double) 1.0 - yf;
		final double m1 = xf * yf;
		final double m2 = nxf * yf;
		final double m3 = xf * nyf;
		final double m4 = nxf * nyf;

	/* get color values of neighbouring pixels */
		final int c1 = this.trueColor ? getPixelOverflowTC(sx, sy, bgColor)         : getPixelOverflowPalette(sx, sy, bgColor);
		final int c2 = this.trueColor ? getPixelOverflowTC(sx - 1, sy, bgColor)     : getPixelOverflowPalette(sx - 1, sy, bgColor);
		final int c3 = this.trueColor ? getPixelOverflowTC(sx, sy - 1, bgColor)     : getPixelOverflowPalette(sx, sy - 1, bgColor);
		final int c4 = this.trueColor ? getPixelOverflowTC(sx - 1, sy - 1, bgColor) : getPixelOverflowPalette(sx, sy - 1, bgColor);
		int r, g, b, a;

		if (x < 0) sx--;
		if (y < 0) sy--;

	/* component-wise summing-up of color values */
		if (this.trueColor) {
			r = (int)(m1*GdUtils.trueColorGetRed(c1)   + m2*GdUtils.trueColorGetRed(c2)   + m3*GdUtils.trueColorGetRed(c3)   + m4*GdUtils.trueColorGetRed(c4));
			g = (int)(m1*GdUtils.trueColorGetGreen(c1) + m2*GdUtils.trueColorGetGreen(c2) + m3*GdUtils.trueColorGetGreen(c3) + m4*GdUtils.trueColorGetGreen(c4));
			b = (int)(m1*GdUtils.trueColorGetBlue(c1)  + m2*GdUtils.trueColorGetBlue(c2)  + m3*GdUtils.trueColorGetBlue(c3)  + m4*GdUtils.trueColorGetBlue(c4));
			a = (int)(m1*GdUtils.trueColorGetAlpha(c1) + m2*GdUtils.trueColorGetAlpha(c2) + m3*GdUtils.trueColorGetAlpha(c3) + m4*GdUtils.trueColorGetAlpha(c4));
		} else {
			r = (int)(m1*this.red[(c1)]   + m2*this.red[(c2)]   + m3*this.red[(c3)]   + m4*this.red[(c4)]);
			g = (int)(m1*this.green[(c1)] + m2*this.green[(c2)] + m3*this.green[(c3)] + m4*this.green[(c4)]);
			b = (int)(m1*this.blue[(c1)]  + m2*this.blue[(c2)]  + m3*this.blue[(c3)]  + m4*this.blue[(c4)]);
			a = (int)(m1*this.alpha[(c1)] + m2*this.alpha[(c2)] + m3*this.alpha[(c3)] + m4*this.alpha[(c4)]);
		}

		r = CLAMP(r, 0, 255);
		g = CLAMP(g, 0, 255);
		b = CLAMP(b, 0, 255);
		a = CLAMP(a, 0, GdUtils.ALPHA_MAX);
		return GdUtils.trueColorMixAlpha(r, g, b, a);
	}

	/**
	 * InternalFunction: getPixelInterpolated
	 *  Returns the interpolated color value using the default interpolation
	 *  method. The returned color is always in the ARGB format (truecolor).
	 *
	 * Parameters:
	 * 	im - Image to set the default interpolation method
	 *  y - X value of the ideal position
	 *  y - Y value of the ideal position
	 *  method - Interpolation method <gdInterpolationMethod>
	 *
	 * Returns:
	 *  GD_TRUE if the affine is rectilinear or GD_FALSE
	 *
	 * See also:
	 *  <gdSetInterpolationMethod>
	 */
	int getPixelInterpolated(final double x, final double y, final int bgColor)
	{
		final int xi=(int)((x) < 0 ? x - 1: x);
		final int yi=(int)((y) < 0 ? y - 1: y);
		int yii;
		int i;
		double kernel, kernel_cache_y;
		double[] kernel_x = new double[12];
		double[] kernel_y = new double[4];
		double new_r = 0.0f, new_g = 0.0f, new_b = 0.0f, new_a = 0.0f;

	/* These methods use special implementations */
		if (this.interpolation_id == GdInterpolationMethod.GD_BILINEAR_FIXED || this.interpolation_id == GdInterpolationMethod.GD_BICUBIC_FIXED || this.interpolation_id == GdInterpolationMethod.GD_NEAREST_NEIGHBOUR) {
			return -1;
		}

		if (this.interpolation_id == GdInterpolationMethod.GD_WEIGHTED4) {
			return getPixelInterpolateWeight(x, y, bgColor);
		}

		if (this.interpolation_id == GdInterpolationMethod.GD_NEAREST_NEIGHBOUR) {
			if (this.trueColor) {
				return getPixelOverflowTC(xi, yi, bgColor);
			} else {
				return getPixelOverflowPalette(xi, yi, bgColor);
			}
		}
		if (this.interpolation != null) {
			for (i=0; i<4; i++) {
				kernel_x[i] = (double) this.interpolation.filter((double) (xi + i - 1 - x));
				kernel_y[i] = (double) this.interpolation.filter((double)(yi+i-1-y));
			}
		} else {
			return -1;
		}

	/*
	 * TODO: use the known fast rgba multiplication implementation once
	 * the new formats are in place
	 */
		for (yii = yi-1; yii < yi+3; yii++) {
			int xii;
			kernel_cache_y = kernel_y[yii-(yi-1)];
			if (this.trueColor) {
				for (xii=xi-1; xii<xi+3; xii++) {
					final int rgbs = getPixelOverflowTC(xii, yii, bgColor);

					kernel = kernel_cache_y * kernel_x[xii-(xi-1)];
					new_r += kernel * GdUtils.trueColorGetRed(rgbs);
					new_g += kernel * GdUtils.trueColorGetGreen(rgbs);
					new_b += kernel * GdUtils.trueColorGetBlue(rgbs);
					new_a += kernel * GdUtils.trueColorGetAlpha(rgbs);
				}
			} else {
				for (xii=xi-1; xii<xi+3; xii++) {
					final int rgbs = getPixelOverflowPalette(xii, yii, bgColor);

					kernel = kernel_cache_y * kernel_x[xii-(xi-1)];
					new_r += kernel * GdUtils.trueColorGetRed(rgbs);
					new_g += kernel * GdUtils.trueColorGetGreen(rgbs);
					new_b += kernel * GdUtils.trueColorGetBlue(rgbs);
					new_a += kernel * GdUtils.trueColorGetAlpha(rgbs);
				}
			}
		}

		new_r = CLAMP((int) new_r, 0, 255);
		new_g = CLAMP((int) new_g, 0, 255);
		new_b = CLAMP((int) new_b, 0, 255);
		new_a = CLAMP((int) new_a, 0, GdUtils.ALPHA_MAX);

		return GdUtils.trueColorMixAlpha(((int) new_r), ((int) new_g), ((int) new_b), ((int) new_a));
	}

	private LineContribType _gdContributionsCalc(int line_size, int src_size, double scale_d,  final GdFilterInterface filter) {
		double width_d;
		double scale_f_d = 1.0;
		final double filter_width_d = DEFAULT_BOX_RADIUS;
		int windows_size;
		int u;

		if (scale_d < 1.0) {
			width_d = filter_width_d / scale_d;
			scale_f_d = scale_d;
		}  else {
			width_d= filter_width_d;
		}

		windows_size = 2 * (int)ceil(width_d) + 1;
		final LineContribType res = new LineContribType(line_size, windows_size);

		for (u = 0; u < line_size; u++) {
			final double dCenter = (double)u / scale_d;
		/* get the significant edge points affecting the pixel */
			int iLeft = MAX(0, (int)floor (dCenter - width_d));
			int iRight = MIN((int)ceil(dCenter + width_d), (int)src_size - 1);
			double dTotalWeight = 0.0;
			int iSrc;

		/* Cut edge points to fit in filter window in case of spill-off */
			if (iRight - iLeft + 1 > windows_size)  {
				if (iLeft < ((int)src_size - 1 / 2))  {
					iLeft++;
				} else {
					iRight--;
				}
			}

			res.ContribRow[u].Left = iLeft;
			res.ContribRow[u].Right = iRight;

			for (iSrc = iLeft; iSrc <= iRight; iSrc++) {
				dTotalWeight += (res.ContribRow[u].Weights[iSrc-iLeft] =  scale_f_d * filter.filter(scale_f_d * (dCenter - (double) iSrc)));
			}

			if (dTotalWeight < 0.0) {
				return null;
			}

			if (dTotalWeight > 0.0) {
				for (iSrc = iLeft; iSrc <= iRight; iSrc++) {
					res.ContribRow[u].Weights[iSrc-iLeft] /= dTotalWeight;
				}
			}
		}
		return res;
	}


	private void
				  _gdScaleOneAxis(GdImage pSrc, GdImage dst,
								  int dst_len, int row, LineContribType contrib,
								  GdAxis axis)
	{
		int ndx;

		for (ndx = 0; ndx < dst_len; ndx++) {
			double r = 0, g = 0, b = 0, a = 0;
			final int left = contrib.ContribRow[ndx].Left;
			final int right = contrib.ContribRow[ndx].Right;

		/* Accumulate each channel */
			for (int i = left; i <= right; i++) {
				final int left_channel = i - left;
				final int srcpx;
				if (axis == GdAxis.HORIZONTAL) {
					srcpx = pSrc.tpixels[row][i];
				} else {
					srcpx = pSrc.tpixels[i][row];
				}

				r += contrib.ContribRow[ndx].Weights[left_channel]
						* (double)(GdUtils.trueColorGetRed(srcpx));
				g += contrib.ContribRow[ndx].Weights[left_channel]
						* (double)(GdUtils.trueColorGetGreen(srcpx));
				b += contrib.ContribRow[ndx].Weights[left_channel]
						* (double)(GdUtils.trueColorGetBlue(srcpx));
				a += contrib.ContribRow[ndx].Weights[left_channel]
						* (double)(GdUtils.trueColorGetAlpha(srcpx));
			}/* for */

			final int dest_val = GdUtils.trueColorMixAlpha(uchar_clamp(r, 0xFF), uchar_clamp(g, 0xFF),
					uchar_clamp(b, 0xFF),
					uchar_clamp(a, 0x7F)); /* alpha is 0..127 */
			if (axis == GdAxis.HORIZONTAL) {
				dst.tpixels[row][ndx] = dest_val;
				dst.tpixels[ndx][row] = dest_val;
			}
		}/* for */
	}/* _gdScaleOneAxis*/


	private int _gdScalePass(final GdImage pSrc, final int src_len,
							   final GdImage pDst, final int dst_len,
							   final int num_lines,
							   final GdAxis axis)
	{
		int line_ndx;
		LineContribType contrib;

    /* Same dim, just copy it. */
		assert(dst_len != src_len); // TODO: caller should handle this.

		contrib = _gdContributionsCalc(dst_len, src_len,
				(double)dst_len / (double)src_len,
				pSrc.interpolation);
		if (contrib == null) {
			return 0;
		}

	/* Scale each line */
		for (line_ndx = 0; line_ndx < num_lines; line_ndx++) {
			_gdScaleOneAxis(pSrc, pDst, dst_len, line_ndx, contrib, axis);
		}

		return 1;
	}/* _gdScalePass*/


	private GdImage scaleTwoPass(final int new_width, final int new_height) {
		final int src_width = sx;
		final int src_height = sy;
		GdImage tmp_im = null;
		GdImage dst = null;

    /* First, handle the trivial case. */
		if (src_width == new_width && src_height == new_height) {
			return imageClone();
		}/* if */

	/* Convert to truecolor if it isn't; this code requires it. */
		if (!trueColor) {
			paletteToTrueColor();
		}/* if */

    /* Scale horizontally unless sizes are the same. */
		if (src_width == new_width) {
			tmp_im = this;
		} else {
			tmp_im = new GdImage(new_width, src_height, GdImageColorType.TRUE_COLOR);
			tmp_im.setInterpolationMethod(interpolation_id);

			_gdScalePass(this, src_width, tmp_im, new_width, src_height, GdAxis.HORIZONTAL);
		}/* if .. else*/

    /* If vertical sizes match, we're done. */
		if (src_height == new_height) {
			assert(tmp_im != this);
			return tmp_im;
		}/* if */

    /* Otherwise, we need to scale vertically. */
		dst = new GdImage(new_width, new_height, GdImageColorType.TRUE_COLOR);
		if (dst != null) {
			dst.setInterpolationMethod(interpolation_id);
			_gdScalePass(tmp_im, src_height, dst, new_height, new_width, GdAxis.VERTICAL);
		}/* if */

		return dst;
	}/* gdImageScaleTwoPass*/


	/*
		BilinearFixed, BicubicFixed and nearest implementations are
		rewamped versions of the implementation in CBitmapEx

		http://www.codeproject.com/Articles/29121/CBitmapEx-Free-C-Bitmap-Manipulation-Class

		Integer only implementation, good to have for common usages like
		pre scale very large images before using another interpolation
		methods for the last step.
	*/
	private GdImage scaleNearestNeighbour(final int width, final int height) {
		final int new_width = MAX(1, width);
		final int new_height = MAX(1, height);
		final float dx = (float)this.sx / (float)new_width;
		final float dy = (float)this.sy / (float)new_height;
		final long f_dx = gd_ftofx(dx);
		final long f_dy = gd_ftofx(dy);

		GdImage dst_img;
		int dst_offset_x;
		int dst_offset_y = 0;
		int i;

		dst_img = new GdImage(new_width, new_height, GdImageColorType.TRUE_COLOR);

		if (dst_img == null) {
			return null;
		}

		for (i=0; i<new_height; i++) {
			int j;
			dst_offset_x = 0;
			if (this.trueColor) {
				for (j=0; j<new_width; j++) {
					final long f_i = gd_itofx(i);
					final long f_j = gd_itofx(j);
					final long f_a = gd_mulfx(f_i, f_dy);
					final long f_b = gd_mulfx(f_j, f_dx);
					final int m = gd_fxtoi(f_a);
					final int n = gd_fxtoi(f_b);

					dst_img.tpixels[dst_offset_y][dst_offset_x++] = this.tpixels[m][n];
				}
			} else {
				for (j=0; j<new_width; j++) {
					final long f_i = gd_itofx(i);
					final long f_j = gd_itofx(j);
					final long f_a = gd_mulfx(f_i, f_dy);
					final long f_b = gd_mulfx(f_j, f_dx);
					final int m = gd_fxtoi(f_a);
					final int n = gd_fxtoi(f_b);

					dst_img.tpixels[dst_offset_y][dst_offset_x++] = colorIndex2RGBA(this.pixels[m][n]);
				}
			}
			dst_offset_y++;
		}
		return dst_img;
	}

	private int getPixelOverflowColorTC(final int x, final int y, final int color) {
		if (isBoundsSafe(x, y)) {
			final int c = this.tpixels[y][x];
			if (c == this.transparent) {
				return GdUtils.trueColorMixAlpha(0, 0, 0, 127);
			}
			return c;
		} else {
			int border = 0;
			if (y < this.cy1) {
				border = this.tpixels[0][this.cx1];
				return getPixelOverflowColorTCProcessBorder(border);
			}

			if (y < this.cy1) {
				border = this.tpixels[0][this.cx1];
				return getPixelOverflowColorTCProcessBorder(border);
			}

			if (y > this.cy2) {
				if (x >= this.cx1 && x <= this.cx1) {
					border = this.tpixels[this.cy2][x];
					return getPixelOverflowColorTCProcessBorder(border);
				} else {
					return GdUtils.trueColorMixAlpha(0, 0, 0, 127);
				}
			}

		/* y is bound safe at this point */
			if (x < this.cx1) {
				border = this.tpixels[y][this.cx1];
				return getPixelOverflowColorTCProcessBorder(border);
			}

			if (x > this.cx2) {
				border = this.tpixels[y][this.cx2];
			}

			return getPixelOverflowColorTCProcessBorder(border);
		}
	}

	private int getPixelOverflowColorTCProcessBorder(final int border) {
		if (border == this.transparent) {
			return GdUtils.trueColorMixAlpha(0, 0, 0, 127);
		} else{
			return GdUtils.trueColorMixAlpha(GdUtils.trueColorGetRed(border), GdUtils.trueColorGetGreen(border), GdUtils.trueColorGetBlue(border), 127);
		}
	}

	private GdImage scaleBilinearPalette(final int new_width, final int new_height) {
		long _width = MAX(1, new_width);
		long _height = MAX(1, new_height);
		float dx = (float)sx / (float)_width;
		float dy = (float)sy / (float)_height;
		long f_dx = gd_ftofx(dx);
		long f_dy = gd_ftofx(dy);
		long f_1 = gd_itofx(1);

		int dst_offset_h;
		int dst_offset_v = 0;
		GdImage new_img;
		final int transparent = this.transparent;

		new_img = new GdImage(new_width, new_height, GdImageColorType.TRUE_COLOR);
		if (new_img == null) {
			return null;
		}
		new_img.transparent = GdUtils.trueColorMixAlpha(this.red[transparent], this.green[transparent], this.blue[transparent], this.alpha[transparent]);

		for (int i=0; i < _height; i++) {
			final long f_i = gd_itofx(i);
			final long f_a = gd_mulfx(f_i, f_dy);
			int m = gd_fxtoi(f_a);

			dst_offset_h = 0;

			for (int j=0; j < _width; j++) {
			/* Update bitmap */
				long f_j = gd_itofx(j);
				long f_b = gd_mulfx(f_j, f_dx);

				final int n = gd_fxtoi(f_b);
				long f_f = f_a - gd_itofx(m);
				long f_g = f_b - gd_itofx(n);

				final long f_w1 = gd_mulfx(f_1-f_f, f_1-f_g);
				final long f_w2 = gd_mulfx(f_1-f_f, f_g);
				final long f_w3 = gd_mulfx(f_f, f_1-f_g);
				final long f_w4 = gd_mulfx(f_f, f_g);
				int pixel1;
				int pixel2;
				int pixel3;
				int pixel4;
				long f_r1, f_r2, f_r3, f_r4,
						f_g1, f_g2, f_g3, f_g4,
						f_b1, f_b2, f_b3, f_b4,
						f_a1, f_a2, f_a3, f_a4;

			/* zero for the background color, nothig gets outside anyway */
				pixel1 = getPixelOverflowPalette(n, m, 0);
				pixel2 = getPixelOverflowPalette(n + 1, m, 0);
				pixel3 = getPixelOverflowPalette(n, m + 1, 0);
				pixel4 = getPixelOverflowPalette(n + 1, m + 1, 0);

				f_r1 = gd_itofx(GdUtils.trueColorGetRed(pixel1));
				f_r2 = gd_itofx(GdUtils.trueColorGetRed(pixel2));
				f_r3 = gd_itofx(GdUtils.trueColorGetRed(pixel3));
				f_r4 = gd_itofx(GdUtils.trueColorGetRed(pixel4));
				f_g1 = gd_itofx(GdUtils.trueColorGetGreen(pixel1));
				f_g2 = gd_itofx(GdUtils.trueColorGetGreen(pixel2));
				f_g3 = gd_itofx(GdUtils.trueColorGetGreen(pixel3));
				f_g4 = gd_itofx(GdUtils.trueColorGetGreen(pixel4));
				f_b1 = gd_itofx(GdUtils.trueColorGetBlue(pixel1));
				f_b2 = gd_itofx(GdUtils.trueColorGetBlue(pixel2));
				f_b3 = gd_itofx(GdUtils.trueColorGetBlue(pixel3));
				f_b4 = gd_itofx(GdUtils.trueColorGetBlue(pixel4));
				f_a1 = gd_itofx(GdUtils.trueColorGetAlpha(pixel1));
				f_a2 = gd_itofx(GdUtils.trueColorGetAlpha(pixel2));
				f_a3 = gd_itofx(GdUtils.trueColorGetAlpha(pixel3));
				f_a4 = gd_itofx(GdUtils.trueColorGetAlpha(pixel4));

				{
					final char red = (char) gd_fxtoi(gd_mulfx(f_w1, f_r1) + gd_mulfx(f_w2, f_r2) + gd_mulfx(f_w3, f_r3) + gd_mulfx(f_w4, f_r4));
					final char green = (char) gd_fxtoi(gd_mulfx(f_w1, f_g1) + gd_mulfx(f_w2, f_g2) + gd_mulfx(f_w3, f_g3) + gd_mulfx(f_w4, f_g4));
					final char blue = (char) gd_fxtoi(gd_mulfx(f_w1, f_b1) + gd_mulfx(f_w2, f_b2) + gd_mulfx(f_w3, f_b3) + gd_mulfx(f_w4, f_b4));
					final char alpha = (char) gd_fxtoi(gd_mulfx(f_w1, f_a1) + gd_mulfx(f_w2, f_a2) + gd_mulfx(f_w3, f_a3) + gd_mulfx(f_w4, f_a4));

					new_img.tpixels[dst_offset_v][dst_offset_h] = GdUtils.trueColorMixAlpha(red, green, blue, alpha);
				}

				dst_offset_h++;
			}

			dst_offset_v++;
		}
		return new_img;
	}

	private GdImage scaleBilinearTC(final int new_width, final int new_height) {
		long dst_w = MAX(1, new_width);
		long dst_h = MAX(1, new_height);
		float dx = (float)sx / (float)dst_w;
		float dy = (float)sy / (float)dst_h;
		long f_dx = gd_ftofx(dx);
		long f_dy = gd_ftofx(dy);
		long f_1 = gd_itofx(1);

		int dst_offset_h;
		int dst_offset_v = 0;
		final GdImage new_img = new GdImage(new_width, new_height, GdImageColorType.TRUE_COLOR);

		for (int i=0; i < dst_h; i++) {
			dst_offset_h = 0;
			for (int j=0; j < dst_w; j++) {
			/* Update bitmap */
				long f_i = gd_itofx(i);
				long f_j = gd_itofx(j);
				long f_a = gd_mulfx(f_i, f_dy);
				long f_b = gd_mulfx(f_j, f_dx);
				final int m = gd_fxtoi(f_a);
				final int n = gd_fxtoi(f_b);
				long f_f = f_a - gd_itofx(m);
				long f_g = f_b - gd_itofx(n);

				final long f_w1 = gd_mulfx(f_1-f_f, f_1-f_g);
				final long f_w2 = gd_mulfx(f_1-f_f, f_g);
				final long f_w3 = gd_mulfx(f_f, f_1-f_g);
				final long f_w4 = gd_mulfx(f_f, f_g);
				int pixel1;
				int pixel2;
				int pixel3;
				int pixel4;
				long f_r1, f_r2, f_r3, f_r4,
						f_g1, f_g2, f_g3, f_g4,
						f_b1, f_b2, f_b3, f_b4,
						f_a1, f_a2, f_a3, f_a4;
			/* 0 for bgColor, nothing gets outside anyway */
				pixel1 = getPixelOverflowTC(n, m, 0);
				pixel2 = getPixelOverflowTC(n + 1, m, 0);
				pixel3 = getPixelOverflowTC(n, m + 1, 0);
				pixel4 = getPixelOverflowTC(n + 1, m + 1, 0);

				f_r1 = gd_itofx(GdUtils.trueColorGetRed(pixel1));
				f_r2 = gd_itofx(GdUtils.trueColorGetRed(pixel2));
				f_r3 = gd_itofx(GdUtils.trueColorGetRed(pixel3));
				f_r4 = gd_itofx(GdUtils.trueColorGetRed(pixel4));
				f_g1 = gd_itofx(GdUtils.trueColorGetGreen(pixel1));
				f_g2 = gd_itofx(GdUtils.trueColorGetGreen(pixel2));
				f_g3 = gd_itofx(GdUtils.trueColorGetGreen(pixel3));
				f_g4 = gd_itofx(GdUtils.trueColorGetGreen(pixel4));
				f_b1 = gd_itofx(GdUtils.trueColorGetBlue(pixel1));
				f_b2 = gd_itofx(GdUtils.trueColorGetBlue(pixel2));
				f_b3 = gd_itofx(GdUtils.trueColorGetBlue(pixel3));
				f_b4 = gd_itofx(GdUtils.trueColorGetBlue(pixel4));
				f_a1 = gd_itofx(GdUtils.trueColorGetAlpha(pixel1));
				f_a2 = gd_itofx(GdUtils.trueColorGetAlpha(pixel2));
				f_a3 = gd_itofx(GdUtils.trueColorGetAlpha(pixel3));
				f_a4 = gd_itofx(GdUtils.trueColorGetAlpha(pixel4));
				{
					final char red   = (char) gd_fxtoi(gd_mulfx(f_w1, f_r1) + gd_mulfx(f_w2, f_r2) + gd_mulfx(f_w3, f_r3) + gd_mulfx(f_w4, f_r4));
					final char green = (char) gd_fxtoi(gd_mulfx(f_w1, f_g1) + gd_mulfx(f_w2, f_g2) + gd_mulfx(f_w3, f_g3) + gd_mulfx(f_w4, f_g4));
					final char blue  = (char) gd_fxtoi(gd_mulfx(f_w1, f_b1) + gd_mulfx(f_w2, f_b2) + gd_mulfx(f_w3, f_b3) + gd_mulfx(f_w4, f_b4));
					final char alpha = (char) gd_fxtoi(gd_mulfx(f_w1, f_a1) + gd_mulfx(f_w2, f_a2) + gd_mulfx(f_w3, f_a3) + gd_mulfx(f_w4, f_a4));

					new_img.tpixels[dst_offset_v][dst_offset_h] = GdUtils.trueColorMixAlpha(red, green, blue, alpha);
				}

				dst_offset_h++;
			}

			dst_offset_v++;
		}
		return new_img;
	}

	private GdImage scaleBilinear(final int new_width, final int new_height) {
		if (this.trueColor) {
			return scaleBilinearTC(new_width, new_height);
		} else {
			return scaleBilinearPalette(new_width, new_height);
		}
	}

	private GdImage scaleBicubicFixed(final int width, final int height) {
		final int new_width = MAX(1, width);
		final int new_height = MAX(1, height);
		final int src_w = sx;
		final int src_h = sy;
		final long f_dx = gd_ftofx((float)src_w / (float)new_width);
		final long f_dy = gd_ftofx((float) src_h / (float) new_height);
		final long f_1 = gd_itofx(1);
		final long f_2 = gd_itofx(2);
		final long f_4 = gd_itofx(4);
		final long f_6 = gd_itofx(6);
		final long f_gamma = gd_ftofx(1.04f);

		int dst_offset_x;
		int dst_offset_y = 0;

	/* impact perf a bit, but not that much. Implementation for palette
	   images can be done at a later point.
	*/
		if (!trueColor) {
			paletteToTrueColor();
		}

		final GdImage dst = new GdImage(new_width, new_height, GdImageColorType.TRUE_COLOR);

		dst.saveAlphaFlag = 1;

		for (int i=0; i < new_height; i++) {
			dst_offset_x = 0;

			for (int j=0; j < new_width; j++) {
				int[] dst_row = dst.tpixels[dst_offset_y];
				long f_red = 0, f_green = 0, f_blue = 0, f_alpha = 0;
				char red, green, blue, alpha = 0;
				final long f_f, f_g;
				int[] src_offset_x = new int[16];
				int[] src_offset_y = new int[16];
				{
					final long f_a = gd_mulfx(gd_itofx(i), f_dy);
					final long f_b = gd_mulfx(gd_itofx(j), f_dx);
					final int m = gd_fxtoi(f_a);
					final int n = gd_fxtoi(f_b);
					f_f = f_a - gd_itofx(m);
					f_g = f_b - gd_itofx(n);


					if ((m < 1) || (n < 1)) {
						src_offset_x[0] = n;
						src_offset_y[0] = m;
					} else {
						src_offset_x[0] = n - 1;
						src_offset_y[0] = m;
					}

					if (m < 1) {
						src_offset_x[1] = n;
						src_offset_y[1] = m;
					} else {
						src_offset_x[1] = n;
						src_offset_y[1] = m;
					}

					if ((m < 1) || (n >= src_w - 1)) {
						src_offset_x[2] = n;
						src_offset_y[2] = m;
					} else {
						src_offset_x[2] = n + 1;
						src_offset_y[2] = m;
					}

					if ((m < 1) || (n >= src_w - 2)) {
						src_offset_x[3] = n;
						src_offset_y[3] = m;
					} else {
						src_offset_x[3] = n + 1 + 1;
						src_offset_y[3] = m;
					}

					if (n < 1) {
						src_offset_x[4] = n;
						src_offset_y[4] = m;
					} else {
						src_offset_x[4] = n - 1;
						src_offset_y[4] = m;
					}

					src_offset_x[5] = n;
					src_offset_y[5] = m;
					if (n >= src_w-1) {
						src_offset_x[6] = n;
						src_offset_y[6] = m;
					} else {
						src_offset_x[6] = n + 1;
						src_offset_y[6] = m;
					}

					if (n >= src_w - 2) {
						src_offset_x[7] = n;
						src_offset_y[7] = m;
					} else {
						src_offset_x[7] = n + 1 + 1;
						src_offset_y[7] = m;
					}

					if ((m >= src_h - 1) || (n < 1)) {
						src_offset_x[8] = n;
						src_offset_y[8] = m;
					} else {
						src_offset_x[8] = n - 1;
						src_offset_y[8] = m;
					}

					if (m >= src_h - 1) {
						src_offset_x[8] = n;
						src_offset_y[8] = m;
					} else {
						src_offset_x[9] = n;
						src_offset_y[9] = m;
					}

					if ((m >= src_h-1) || (n >= src_w-1)) {
						src_offset_x[10] = n;
						src_offset_y[10] = m;
					} else {
						src_offset_x[10] = n + 1;
						src_offset_y[10] = m;
					}

					if ((m >= src_h - 1) || (n >= src_w - 2)) {
						src_offset_x[11] = n;
						src_offset_y[11] = m;
					} else {
						src_offset_x[11] = n + 1 + 1;
						src_offset_y[11] = m;
					}

					if ((m >= src_h - 2) || (n < 1)) {
						src_offset_x[12] = n;
						src_offset_y[12] = m;
					} else {
						src_offset_x[12] = n - 1;
						src_offset_y[12] = m;
					}

					if (m >= src_h - 2) {
						src_offset_x[13] = n;
						src_offset_y[13] = m;
					} else {
						src_offset_x[13] = n;
						src_offset_y[13] = m;
					}

					if ((m >= src_h - 2) || (n >= src_w - 1)) {
						src_offset_x[14] = n;
						src_offset_y[14] = m;
					} else {
						src_offset_x[14] = n + 1;
						src_offset_y[14] = m;
					}

					if ((m >= src_h - 2) || (n >= src_w - 2)) {
						src_offset_x[15] = n;
						src_offset_y[15] = m;
					} else {
						src_offset_x[15] = n  + 1 + 1;
						src_offset_y[15] = m;
					}
				}

				for (int k = -1; k < 3; k++) {
					long f_RY;
					{
						final long f = gd_itofx(k)-f_f;
						final long f_fm1 = f - f_1;
						final long f_fp1 = f + f_1;
						final long f_fp2 = f + f_2;
						long f_a = 0, f_b = 0, f_d = 0, f_c = 0;

						if (f_fp2 > 0) f_a = gd_mulfx(f_fp2, gd_mulfx(f_fp2,f_fp2));
						if (f_fp1 > 0) f_b = gd_mulfx(f_fp1, gd_mulfx(f_fp1,f_fp1));
						if (f > 0)     f_c = gd_mulfx(f, gd_mulfx(f,f));
						if (f_fm1 > 0) f_d = gd_mulfx(f_fm1, gd_mulfx(f_fm1,f_fm1));

						f_RY = gd_divfx((f_a - gd_mulfx(f_4,f_b) + gd_mulfx(f_6,f_c) - gd_mulfx(f_4,f_d)),f_6);
					}

					for (int l = -1; l < 3; l++) {
						final long f = gd_itofx(l) - f_g;
						final long f_fm1 = f - f_1;
						final long f_fp1 = f + f_1;
						final long f_fp2 = f + f_2;
						long f_a = 0, f_b = 0, f_c = 0, f_d = 0;
						long f_RX, f_R, f_rs, f_gs, f_bs, f_ba;
						int c;
						final int _k = ((k+1)*4) + (l+1);

						if (f_fp2 > 0) f_a = gd_mulfx(f_fp2,gd_mulfx(f_fp2,f_fp2));

						if (f_fp1 > 0) f_b = gd_mulfx(f_fp1,gd_mulfx(f_fp1,f_fp1));

						if (f > 0) f_c = gd_mulfx(f,gd_mulfx(f,f));

						if (f_fm1 > 0) f_d = gd_mulfx(f_fm1,gd_mulfx(f_fm1,f_fm1));

						f_RX = gd_divfx((f_a-gd_mulfx(f_4,f_b)+gd_mulfx(f_6,f_c)-gd_mulfx(f_4,f_d)),f_6);
						f_R = gd_mulfx(f_RY,f_RX);

						c = tpixels[src_offset_y[_k]][src_offset_x[_k]];
						f_rs = gd_itofx(GdUtils.trueColorGetRed(c));
						f_gs = gd_itofx(GdUtils.trueColorGetGreen(c));
						f_bs = gd_itofx(GdUtils.trueColorGetBlue(c));
						f_ba = gd_itofx(GdUtils.trueColorGetAlpha(c));

						f_red += gd_mulfx(f_rs,f_R);
						f_green += gd_mulfx(f_gs,f_R);
						f_blue += gd_mulfx(f_bs,f_R);
						f_alpha += gd_mulfx(f_ba,f_R);
					}
				}

				red    = (char) CLAMP(gd_fxtoi(gd_mulfx(f_red,   f_gamma)),  0, 255);
				green  = (char) CLAMP(gd_fxtoi(gd_mulfx(f_green, f_gamma)),  0, 255);
				blue   = (char) CLAMP(gd_fxtoi(gd_mulfx(f_blue,  f_gamma)),  0, 255);
				alpha  = (char) CLAMP(gd_fxtoi(gd_mulfx(f_alpha,  f_gamma)), 0, 127);

				dst_row[dst_offset_x] = GdUtils.trueColorMixAlpha(red, green, blue, alpha);

				dst_offset_x++;
			}
			dst_offset_y++;
		}
		return dst;
	}

	public GdImage scale(final int new_width, final int new_height) {
		if (interpolation_id == null) {
			return null;
		}

		GdImage im_scaled = null;

		switch (interpolation_id) {
		/*Special cases, optimized implementations */
			case GD_NEAREST_NEIGHBOUR:
				im_scaled = scaleNearestNeighbour(new_width, new_height);
				break;

			case GD_BILINEAR_FIXED:
				im_scaled = scaleBilinear(new_width, new_height);
				break;

			case GD_BICUBIC_FIXED:
				im_scaled = scaleBicubicFixed(new_width, new_height);
				break;

		/* generic */
			default:
				if (interpolation == null) {
					return null;
				}
				im_scaled = scaleTwoPass(new_width, new_height);
				break;
		}

		return im_scaled;
	}

	private GdImage rotateNearestNeighbour(final float degrees, final int bgColor) {
		float _angle = ((float) (-degrees / 180.0f) * (float)PI);
		final int src_w = sx;
		final int src_h = sy;
		final int new_width = (int)(abs((int)(src_w * cos(_angle))) + abs((int)(src_h * sin(_angle))) + 0.5f);
		final int new_height = (int)(abs((int)(src_w * sin(_angle))) + abs((int)(src_h * cos(_angle))) + 0.5f);
		final long f_0_5 = gd_ftofx(0.5f);
		final long f_H = gd_itofx(src_h/2);
		final long f_W = gd_itofx(src_w / 2);
		final long f_cos = gd_dtofx(cos(-_angle));
		final long f_sin = gd_dtofx(sin(-_angle));

		int dst_offset_x;
		int dst_offset_y = 0;
		int i;

	/* impact perf a bit, but not that much. Implementation for palette
	   images can be done at a later point.
	*/
		if (!trueColor) {
			paletteToTrueColor();
		}

		final GdImage dst = new GdImage(new_width, new_height, GdImageColorType.TRUE_COLOR);
		dst.saveAlphaFlag = 1;
		for (i = 0; i < new_height; i++) {
			int j;
			dst_offset_x = 0;
			for (j = 0; j < new_width; j++) {
				long f_i = gd_itofx((int)i - (int)new_height / 2);
				long f_j = gd_itofx((int)j - (int)new_width  / 2);
				long f_m = gd_mulfx(f_j,f_sin) + gd_mulfx(f_i,f_cos) + f_0_5 + f_H;
				long f_n = gd_mulfx(f_j,f_cos) - gd_mulfx(f_i,f_sin) + f_0_5 + f_W;
				int m = gd_fxtoi(f_m);
				int n = gd_fxtoi(f_n);

				if ((m > 0) && (m < src_h-1) && (n > 0) && (n < src_w-1)) {
					if (dst_offset_y < new_height) {
						dst.tpixels[dst_offset_y][dst_offset_x++] = this.tpixels[m][n];
					}
				} else {
					if (dst_offset_y < new_height) {
						dst.tpixels[dst_offset_y][dst_offset_x++] = bgColor;
					}
				}
			}
			dst_offset_y++;
		}
		return dst;
	}

	private GdImage rotateGeneric(final float degrees, final int bgColor) {
		float _angle = ((float) (-degrees / 180.0f) * (float)PI);
		final int src_w  = sx;
		final int src_h = sy;
		final int new_width = (int)(abs((int)(src_w * cos(_angle))) + abs((int)(src_h * sin(_angle))) + 0.5f);
		final int new_height = (int)(abs((int)(src_w * sin(_angle))) + abs((int)(src_h * cos(_angle))) + 0.5f);
		final long f_0_5 = gd_ftofx(0.5f);
		final long f_H = gd_itofx(src_h/2);
		final long f_W = gd_itofx(src_w/2);
		final long f_cos = gd_dtofx(cos(-_angle));
		final long f_sin = gd_dtofx(sin(-_angle));

		int dst_offset_x;
		int dst_offset_y = 0;
		int i;
		GdImage dst;

		final long f_slop_y = f_sin;
		final long f_slop_x = f_cos;
		final long f_slop = f_slop_x > 0 && f_slop_x > 0 ?
			f_slop_x > f_slop_y ? gd_divfx(f_slop_y, f_slop_x) : gd_divfx(f_slop_x, f_slop_y)
			: 0;

		if (bgColor < 0) {
			return null;
		}

	/* impact perf a bit, but not that much. Implementation for palette
	   images can be done at a later point.
	*/
		if (!trueColor) {
			paletteToTrueColor();
		}

		dst = new GdImage(new_width, new_height, GdImageColorType.TRUE_COLOR);
		dst.saveAlphaFlag = 1;

		for (i = 0; i < new_height; i++) {
			int j;
			dst_offset_x = 0;
			for (j = 0; j < new_width; j++) {
				long f_i = gd_itofx((int)i - (int)new_height / 2);
				long f_j = gd_itofx((int)j - (int)new_width  / 2);
				long f_m = gd_mulfx(f_j,f_sin) + gd_mulfx(f_i,f_cos) + f_0_5 + f_H;
				long f_n = gd_mulfx(f_j,f_cos) - gd_mulfx(f_i,f_sin) + f_0_5 + f_W;
				long m = gd_fxtoi(f_m);
				long n = gd_fxtoi(f_n);

				if ((n <= 0) || (m <= 0) || (m >= src_h) || (n >= src_w)) {
					dst.tpixels[dst_offset_y][dst_offset_x++] = bgColor;
				} else if ((n <= 1) || (m <= 1) || (m >= src_h - 1) || (n >= src_w - 1)) {
					int c = getPixelInterpolated(n, m, bgColor);
					c = c | (( GdUtils.trueColorGetAlpha(c) + ((int)(127* gd_fxtof(f_slop)))) << 24);

					dst.tpixels[dst_offset_y][dst_offset_x++] = _color_blend(bgColor, c);
				} else {
					dst.tpixels[dst_offset_y][dst_offset_x++] = getPixelInterpolated(n, m, bgColor);
				}
			}
			dst_offset_y++;
		}
		return dst;
	}

	private GdImage rotateBilinear(final float degrees, final int bgColor) {
		float _angle = (float)((- degrees / 180.0f) * PI);
		final int src_w = sx;
		final int src_h = sy;
		int new_width = abs((int)(src_w*cos(_angle))) + abs((int)(src_h*sin(_angle) + 0.5f));
		int new_height = abs((int)(src_w*sin(_angle))) + abs((int)(src_h*cos(_angle) + 0.5f));
		final long f_0_5 = gd_ftofx(0.5f);
		final long f_H = gd_itofx(src_h/2);
		final long f_W = gd_itofx(src_w/2);
		final long f_cos = gd_dtofx(cos(-_angle));
		final long f_sin = gd_dtofx(sin(-_angle));
		final long f_1 = gd_itofx(1);
		int i;
		int dst_offset_x;
		int dst_offset_y = 0;
		int src_offset_x, src_offset_y;
		GdImage dst;

	/* impact perf a bit, but not that much. Implementation for palette
	   images can be done at a later point.
	*/
		if (!trueColor) {
			paletteToTrueColor();
		}

		dst = new GdImage(new_width, new_height, GdImageColorType.TRUE_COLOR);
		dst.saveAlphaFlag = 1;

		for (i = 0; i < new_height; i++) {
			int j;
			dst_offset_x = 0;

			for (j=0; j < new_width; j++) {
				final long f_i = gd_itofx((int)i - (int)new_height / 2);
				final long f_j = gd_itofx((int)j - (int)new_width  / 2);
				final long f_m = gd_mulfx(f_j,f_sin) + gd_mulfx(f_i,f_cos) + f_0_5 + f_H;
				final long f_n = gd_mulfx(f_j,f_cos) - gd_mulfx(f_i,f_sin) + f_0_5 + f_W;
				final int m = gd_fxtoi(f_m);
				final int n = gd_fxtoi(f_n);

				if ((m > 0) && (m < src_h - 1) && (n > 0) && (n < src_w - 1)) {
					final long f_f = f_m - gd_itofx(m);
					final long f_g = f_n - gd_itofx(n);
					final long f_w1 = gd_mulfx(f_1-f_f, f_1-f_g);
					final long f_w2 = gd_mulfx(f_1-f_f, f_g);
					final long f_w3 = gd_mulfx(f_f, f_1-f_g);
					final long f_w4 = gd_mulfx(f_f, f_g);

					src_offset_x = n + 1;
					src_offset_y = m + 1;

					if (n < src_w - 1) {
						src_offset_y = m;
					}

					if (m < src_h - 1) {
						src_offset_x = n;
					}

					{
						final int pixel1 = tpixels[src_offset_y][src_offset_x];
						int pixel2, pixel3, pixel4;

						if (src_offset_y + 1 >= src_h) {
							pixel2 = bgColor;
							pixel3 = bgColor;
							pixel4 = bgColor;
						} else if (src_offset_x + 1 >= src_w) {
							pixel2 = bgColor;
							pixel3 = bgColor;
							pixel4 = bgColor;
						} else {
							pixel2 = tpixels[src_offset_y][src_offset_x + 1];
							pixel3 = tpixels[src_offset_y + 1][src_offset_x];
							pixel4 = tpixels[src_offset_y + 1][src_offset_x + 1];
						}
						{
							final long f_r1 = gd_itofx(GdUtils.trueColorGetRed(pixel1));
							final long f_r2 = gd_itofx(GdUtils.trueColorGetRed(pixel2));
							final long f_r3 = gd_itofx(GdUtils.trueColorGetRed(pixel3));
							final long f_r4 = gd_itofx(GdUtils.trueColorGetRed(pixel4));
							final long f_g1 = gd_itofx(GdUtils.trueColorGetGreen(pixel1));
							final long f_g2 = gd_itofx(GdUtils.trueColorGetGreen(pixel2));
							final long f_g3 = gd_itofx(GdUtils.trueColorGetGreen(pixel3));
							final long f_g4 = gd_itofx(GdUtils.trueColorGetGreen(pixel4));
							final long f_b1 = gd_itofx(GdUtils.trueColorGetBlue(pixel1));
							final long f_b2 = gd_itofx(GdUtils.trueColorGetBlue(pixel2));
							final long f_b3 = gd_itofx(GdUtils.trueColorGetBlue(pixel3));
							final long f_b4 = gd_itofx(GdUtils.trueColorGetBlue(pixel4));
							final long f_a1 = gd_itofx(GdUtils.trueColorGetAlpha(pixel1));
							final long f_a2 = gd_itofx(GdUtils.trueColorGetAlpha(pixel2));
							final long f_a3 = gd_itofx(GdUtils.trueColorGetAlpha(pixel3));
							final long f_a4 = gd_itofx(GdUtils.trueColorGetAlpha(pixel4));
							final long f_red = gd_mulfx(f_w1, f_r1) + gd_mulfx(f_w2, f_r2) + gd_mulfx(f_w3, f_r3) + gd_mulfx(f_w4, f_r4);
							final long f_green = gd_mulfx(f_w1, f_g1) + gd_mulfx(f_w2, f_g2) + gd_mulfx(f_w3, f_g3) + gd_mulfx(f_w4, f_g4);
							final long f_blue = gd_mulfx(f_w1, f_b1) + gd_mulfx(f_w2, f_b2) + gd_mulfx(f_w3, f_b3) + gd_mulfx(f_w4, f_b4);
							final long f_alpha = gd_mulfx(f_w1, f_a1) + gd_mulfx(f_w2, f_a2) + gd_mulfx(f_w3, f_a3) + gd_mulfx(f_w4, f_a4);

							final char red   = (char) CLAMP(gd_fxtoi(f_red),   0, 255);
							final char green = (char) CLAMP(gd_fxtoi(f_green), 0, 255);
							final char blue  = (char) CLAMP(gd_fxtoi(f_blue),  0, 255);
							final char alpha = (char) CLAMP(gd_fxtoi(f_alpha), 0, 127);

							dst.tpixels[dst_offset_y][dst_offset_x++] = GdUtils.trueColorMixAlpha(red, green, blue, alpha);
						}
					}
				} else {
					dst.tpixels[dst_offset_y][dst_offset_x++] = bgColor;
				}
			}
			dst_offset_y++;
		}
		return dst;
	}

	private GdImage rotateBicubicFixed(final float degrees,final int bgColor) {
		final float _angle = (float)((- degrees / 180.0f) * PI);
		final int src_w = sx;
		final int src_h = sy;
		final int new_width = abs((int)(src_w*cos(_angle))) + abs((int)(src_h*sin(_angle) + 0.5f));
		final int new_height = abs((int)(src_w*sin(_angle))) + abs((int)(src_h*cos(_angle) + 0.5f));
		final long f_0_5 = gd_ftofx(0.5f);
		final long f_H = gd_itofx(src_h/2);
		final long f_W = gd_itofx(src_w/2);
		final long f_cos = gd_dtofx(cos(-_angle));
		final long f_sin = gd_dtofx(sin(-_angle));
		final long f_1 = gd_itofx(1);
		final long f_2 = gd_itofx(2);
		final long f_4 = gd_itofx(4);
		final long f_6 = gd_itofx(6);
		final long f_gama = gd_ftofx(1.04f);

		int dst_offset_x;
		int dst_offset_y = 0;
		int i;
		GdImage dst;

	/* impact perf a bit, but not that much. Implementation for palette
	   images can be done at a later point.
	*/
		if (!trueColor) {
			paletteToTrueColor();
		}

		dst = new GdImage(new_width, new_height, GdImageColorType.TRUE_COLOR);

		if (dst == null) {
			return null;
		}
		dst.saveAlphaFlag = 1;

		for (i=0; i < new_height; i++) {
			int j;
			dst_offset_x = 0;

			for (j=0; j < new_width; j++) {
				final long f_i = gd_itofx((int)i - (int)new_height / 2);
				final long f_j = gd_itofx((int)j - (int)new_width  / 2);
				final long f_m = gd_mulfx(f_j,f_sin) + gd_mulfx(f_i,f_cos) + f_0_5 + f_H;
				final long f_n = gd_mulfx(f_j,f_cos) - gd_mulfx(f_i,f_sin) + f_0_5 + f_W;
				final int m = gd_fxtoi(f_m);
				final int n = gd_fxtoi(f_n);

				if ((m > 0) && (m < src_h - 1) && (n > 0) && (n < src_w-1)) {
					final long f_f = f_m - gd_itofx(m);
					final long f_g = f_n - gd_itofx(n);
					int[] src_offset_x = new int[16];
					int[] src_offset_y = new int[16];
					char red, green, blue, alpha;
					long f_red=0, f_green=0, f_blue=0, f_alpha=0;

					if ((m < 1) || (n < 1)) {
						src_offset_x[0] = n;
						src_offset_y[0] = m;
					} else {
						src_offset_x[0] = n - 1;
						src_offset_y[0] = m;
					}

					if (m < 1) {
						src_offset_x[1] = n;
						src_offset_y[1] = m;
					} else {
						src_offset_x[1] = n;
						src_offset_y[1] = m ;
					}

					if ((m < 1) || (n >= src_w-1)) {
						src_offset_x[2] = - 1;
						src_offset_y[2] = - 1;
					} else {
						src_offset_x[2] = n + 1;
						src_offset_y[2] = m ;
					}

					if ((m < 1) || (n >= src_w-2)) {
						src_offset_x[3] = - 1;
						src_offset_y[3] = - 1;
					} else {
						src_offset_x[3] = n + 1 + 1;
						src_offset_y[3] = m ;
					}

					if (n < 1) {
						src_offset_x[4] = - 1;
						src_offset_y[4] = - 1;
					} else {
						src_offset_x[4] = n - 1;
						src_offset_y[4] = m;
					}

					src_offset_x[5] = n;
					src_offset_y[5] = m;
					if (n >= src_w-1) {
						src_offset_x[6] = - 1;
						src_offset_y[6] = - 1;
					} else {
						src_offset_x[6] = n + 1;
						src_offset_y[6] = m;
					}

					if (n >= src_w-2) {
						src_offset_x[7] = - 1;
						src_offset_y[7] = - 1;
					} else {
						src_offset_x[7] = n + 1 + 1;
						src_offset_y[7] = m;
					}

					if ((m >= src_h-1) || (n < 1)) {
						src_offset_x[8] = - 1;
						src_offset_y[8] = - 1;
					} else {
						src_offset_x[8] = n - 1;
						src_offset_y[8] = m;
					}

					if (m >= src_h-1) {
						src_offset_x[8] = - 1;
						src_offset_y[8] = - 1;
					} else {
						src_offset_x[9] = n;
						src_offset_y[9] = m;
					}

					if ((m >= src_h-1) || (n >= src_w-1)) {
						src_offset_x[10] = - 1;
						src_offset_y[10] = - 1;
					} else {
						src_offset_x[10] = n + 1;
						src_offset_y[10] = m;
					}

					if ((m >= src_h-1) || (n >= src_w-2)) {
						src_offset_x[11] = - 1;
						src_offset_y[11] = - 1;
					} else {
						src_offset_x[11] = n + 1 + 1;
						src_offset_y[11] = m;
					}

					if ((m >= src_h-2) || (n < 1)) {
						src_offset_x[12] = - 1;
						src_offset_y[12] = - 1;
					} else {
						src_offset_x[12] = n - 1;
						src_offset_y[12] = m;
					}

					if (m >= src_h-2) {
						src_offset_x[13] = - 1;
						src_offset_y[13] = - 1;
					} else {
						src_offset_x[13] = n;
						src_offset_y[13] = m;
					}

					if ((m >= src_h-2) || (n >= src_w - 1)) {
						src_offset_x[14] = - 1;
						src_offset_y[14] = - 1;
					} else {
						src_offset_x[14] = n + 1;
						src_offset_y[14] = m;
					}

					if ((m >= src_h-2) || (n >= src_w-2)) {
						src_offset_x[15] = - 1;
						src_offset_y[15] = - 1;
					} else {
						src_offset_x[15] = n  + 1 + 1;
						src_offset_y[15] = m;
					}

					for (int k=-1; k<3; k++) {
						long f_RY;
						{
							final long f = gd_itofx(k)-f_f;
							final long f_fm1 = f - f_1;
							final long f_fp1 = f + f_1;
							final long f_fp2 = f + f_2;
							long f_a = 0, f_b = 0,f_c = 0, f_d = 0;

							if (f_fp2 > 0) {
								f_a = gd_mulfx(f_fp2,gd_mulfx(f_fp2,f_fp2));
							}

							if (f_fp1 > 0) {
								f_b = gd_mulfx(f_fp1,gd_mulfx(f_fp1,f_fp1));
							}

							if (f > 0) {
								f_c = gd_mulfx(f,gd_mulfx(f,f));
							}

							if (f_fm1 > 0) {
								f_d = gd_mulfx(f_fm1,gd_mulfx(f_fm1,f_fm1));
							}
							f_RY = gd_divfx((f_a-gd_mulfx(f_4,f_b)+gd_mulfx(f_6,f_c)-gd_mulfx(f_4,f_d)),f_6);
						}

						for (int l=-1;  l< 3; l++) {
							final long f = gd_itofx(l) - f_g;
							final long f_fm1 = f - f_1;
							final long f_fp1 = f + f_1;
							final long f_fp2 = f + f_2;
							long f_a = 0, f_b = 0, f_c = 0, f_d = 0;
							long f_RX, f_R;
							final int _k = ((k + 1) * 4) + (l + 1);
							long f_rs, f_gs, f_bs, f_as;
							int c;

							if (f_fp2 > 0) {
								f_a = gd_mulfx(f_fp2,gd_mulfx(f_fp2,f_fp2));
							}

							if (f_fp1 > 0) {
								f_b = gd_mulfx(f_fp1,gd_mulfx(f_fp1,f_fp1));
							}

							if (f > 0) {
								f_c = gd_mulfx(f,gd_mulfx(f,f));
							}

							if (f_fm1 > 0) {
								f_d = gd_mulfx(f_fm1,gd_mulfx(f_fm1,f_fm1));
							}

							f_RX = gd_divfx((f_a - gd_mulfx(f_4, f_b) + gd_mulfx(f_6, f_c) - gd_mulfx(f_4, f_d)), f_6);
							f_R = gd_mulfx(f_RY, f_RX);

							if ((src_offset_x[_k] <= 0) || (src_offset_y[_k] <= 0) || (src_offset_y[_k] >= src_h) || (src_offset_x[_k] >= src_w)) {
								c = bgColor;
							} else if ((src_offset_x[_k] <= 1) || (src_offset_y[_k] <= 1) || (src_offset_y[_k] >= (int)src_h - 1) || (src_offset_x[_k] >= (int)src_w - 1)) {
								long f_127 = gd_itofx(127);
								c = tpixels[src_offset_y[_k]][src_offset_x[_k]];
								c = c | (( (int) (gd_fxtof(gd_mulfx(f_R, f_127)) + 50.5f)) << 24);
								c = _color_blend(bgColor, c);
							} else {
								c = tpixels[src_offset_y[_k]][src_offset_x[_k]];
							}

							f_rs = gd_itofx(GdUtils.trueColorGetRed(c));
							f_gs = gd_itofx(GdUtils.trueColorGetGreen(c));
							f_bs = gd_itofx(GdUtils.trueColorGetBlue(c));
							f_as = gd_itofx(GdUtils.trueColorGetAlpha(c));

							f_red   += gd_mulfx(f_rs, f_R);
							f_green += gd_mulfx(f_gs, f_R);
							f_blue  += gd_mulfx(f_bs, f_R);
							f_alpha += gd_mulfx(f_as, f_R);
						}
					}

					red   = (char) CLAMP(gd_fxtoi(gd_mulfx(f_red, f_gama)),   0, 255);
					green = (char) CLAMP(gd_fxtoi(gd_mulfx(f_green, f_gama)), 0, 255);
					blue  = (char) CLAMP(gd_fxtoi(gd_mulfx(f_blue, f_gama)),  0, 255);
					alpha = (char) CLAMP(gd_fxtoi(gd_mulfx(f_alpha, f_gama)), 0, 127);

					dst.tpixels[dst_offset_y][dst_offset_x] =  GdUtils.trueColorMixAlpha(red, green, blue, alpha);
				} else {
					dst.tpixels[dst_offset_y][dst_offset_x] =  bgColor;
				}
				dst_offset_x++;
			}

			dst_offset_y++;
		}
		return dst;
	}

	public GdImage rotateInterpolated(final float angle, int bgcolor) {
		if (interpolation_id == null) {
			return null;
		}

	/* round to two decimals and keep the 100x multiplication to use it in the common square angles
	   case later. Keep the two decimal precisions so smaller rotation steps can be done, useful for
	   slow animations, f.e. */
		final int angle_rounded = fmod((int) floor(angle * 100), 360 * 100);

		if (bgcolor < 0) {
			return null;
		}

	/* 0 && 90 degrees multiple rotation, 0 rotation simply clones the return image and convert it
	   to truecolor, as we must return truecolor image. */
		switch (angle_rounded) {
			case    0: {
				GdImage dst = imageClone();

				if (dst == null) {
					return null;
				}
				if (!dst.trueColor) {
					dst.paletteToTrueColor();
				}
				return dst;
			}

			case -2700:
			case   9000:
				return rotate90(false);

			case -18000:
			case  18000:
				return rotate180(false);

			case  -9000:
			case  27000:
				return rotate270(false);
		}

		switch (interpolation_id) {
			case GD_NEAREST_NEIGHBOUR:
				return rotateNearestNeighbour(angle, bgcolor);

			case GD_BILINEAR_FIXED:
				return rotateBilinear(angle, bgcolor);

			case GD_BICUBIC_FIXED:
				return rotateBicubicFixed(angle, bgcolor);

			default:
				return rotateGeneric(angle, bgcolor);
		}
	}

/**
 * Title: Affine transformation
 **/

	/**
	 * Group: Transform
	 **/

	private void gdImageClipRectangle(final GdRect r) {
		int c1x, c1y, c2x, c2y;
		int x1,y1;

		final GdClipRectangle clip = getClip();
		c1x = clip.x0;
		c1y = clip.y0;
		c2x = clip.x1;
		c2y = clip.y1;
		x1 = r.x + r.width - 1;
		y1 = r.y + r.height - 1;
		r.x = CLAMP(r.x, c1x, c2x);
		r.y = CLAMP(r.y, c1y, c2y);
		r.width = CLAMP(x1, c1x, c2x) - r.x + 1;
		r.height = CLAMP(y1, c1y, c2y) - r.y + 1;
	}

	/**
	 * Function: gdTransformAffineGetImage
	 *  Applies an affine transformation to a region and return an image
	 *  containing the complete transformation.
	 *
	 * Parameters:
	 * 	dst - Pointer to a gdImagePtr to store the created image, NULL when
	 *        the creation or the transformation failed
	 *  src - Source image
	 *  src_area - rectangle defining the source region to transform
	 *  dstY - Y position in the destination image
	 *  affine - The desired affine transformation
	 *
	 * Returns:
	 *  GD_TRUE if the affine is rectilinear or GD_FALSE
	 */
	public GdImage gdTransformAffineGetImage(GdRect src_area, final GdAffine affine) {
		final int res;
		final GdRect area_full = new GdRect();

		if (src_area == null) {
			area_full.x = 0;
			area_full.y = 0;
			area_full.width  = this.sx;
			area_full.height = this.sy;
			src_area = area_full;
		}

		final GdRect bbox = this.gdTransformAffineBoundingBox(src_area, affine);

		final GdImage dst = new GdImage(bbox.width, bbox.height, GdImageColorType.TRUE_COLOR);
		dst.saveAlphaFlag = 1;

		if (!this.trueColor) {
			this.paletteToTrueColor();
		}

	/* Translate to dst origin (0,0) */
		final GdAffine m = GdAffine.translate(-bbox.x, -bbox.y);
		GdAffine.concat(m, affine, m);

		dst.setAlphaBlending(GdEffect.REPLACE);

		gdTransformAffineCopy(dst, 0, 0, this, src_area, m);
		return dst;
	}

	/**
	 * Function: gdTransformAffineCopy
	 *  Applies an affine transformation to a region and copy the result
	 *  in a destination to the given position.
	 *
	 * Parameters:
	 * 	dst - Image to draw the transformed image
	 *  src - Source image
	 *  dstX - X position in the destination image
	 *  dstY - Y position in the destination image
	 *  src_area - Rectangular region to rotate in the src image
	 *
	 * Returns:
	 *  GD_TRUE if the affine is rectilinear or GD_FALSE
	 */
	public boolean gdTransformAffineCopy(GdImage dst, int dst_x, int dst_y, final GdImage src, GdRect src_region, final GdAffine affine) {
		int c1x,c1y,c2x,c2y;
		final GdClipRectangle backup_clip;
		int end_x, end_y;
		GdInterpolationMethod interpolation_id_bak = GdInterpolationMethod.GD_DEFAULT;

	/* These methods use special implementations */
		if (src.interpolation_id == GdInterpolationMethod.GD_BILINEAR_FIXED || src.interpolation_id == GdInterpolationMethod.GD_BICUBIC_FIXED || src.interpolation_id == GdInterpolationMethod.GD_NEAREST_NEIGHBOUR) {
			interpolation_id_bak = src.interpolation_id;

			src.setInterpolationMethod(GdInterpolationMethod.GD_BICUBIC);
		}


		src.gdImageClipRectangle(src_region);

		if (src_region.x > 0 || src_region.y > 0
				|| src_region.width < src.sx
				|| src_region.height < src.sy) {
			backup_clip = src.getClip();

			src.setClip(src_region.x, src_region.y,
					src_region.x + src_region.width - 1,
					src_region.y + src_region.height - 1);
		} else {
			backup_clip = null;
		}

		final GdRect bbox = gdTransformAffineBoundingBox(src_region, affine);

		final GdClipRectangle clip = dst.getClip();
		c1x = clip.x0;
		c1y = clip.y0;
		c2x = clip.x1;
		c2y = clip.y1;

		end_x = bbox.width  + (int) abs(bbox.x);
		end_y = bbox.height + (int) abs(bbox.y);

	/* Get inverse affine to let us work with destination -> source */
		final GdAffine inv = affine.invert();

		final int src_offset_x =  src_region.x;
		final int src_offset_y =  src_region.y;
		final GdPointF pt = new GdPointF();

		if (dst.alphaBlendingFlag == GdEffect.REPLACE) {
			for (int y = bbox.y; y <= end_y; y++) {
				pt.y = y + 0.5;
				for (int x = 0; x <= end_x; x++) {
					pt.x = x + 0.5;
					final GdPointF src_pt = inv.applyToPointF(pt);
					dst.setPixel(dst_x + x, dst_y + y, src.getPixelInterpolated(src_offset_x + src_pt.x, src_offset_y + src_pt.y, 0));
				}
			}
		} else {
			for (int y = 0; y <= end_y; y++) {
				pt.y = y + 0.5 + bbox.y;
				if ((dst_y + y) < 0 || ((dst_y + y) > dst.sy -1)) {
					continue;
				}

				final int[] dst_row = dst.tpixels[dst_y + y];
				int dst_p = dst_x;

				for (int x = 0; x <= end_x; x++) {
					pt.x = x + 0.5 + bbox.x;
					final GdPointF src_pt = inv.applyToPointF(pt);

					if ((dst_x + x) < 0 || (dst_x + x) > (dst.sx - 1)) {
						break;
					}
					dst_row[dst_p] = src.getPixelInterpolated(src_offset_x + src_pt.x, src_offset_y + src_pt.y, -1);
					dst_p++;
				}
			}
		}

	/* Restore clip if required */
		if (backup_clip != null) {
			src.setClip(clip.x0, clip.y0, clip.x1, clip.y1);
		}

		src.setInterpolationMethod(interpolation_id_bak);
		return true;
	}

	/**
	 * Function: gdTransformAffineBoundingBox
	 *  Returns the bounding box of an affine transformation applied to a
	 *  rectangular area <gdRect>
	 *
	 * Parameters:
	 * 	src - Rectangular source area for the affine transformation
	 *  affine - the affine transformation
	 *  bbox - the resulting bounding box
	 *
	 * Returns:
	 *  GD_TRUE if the affine is rectilinear or GD_FALSE
	 */
	public GdRect gdTransformAffineBoundingBox(final GdRect src, final GdAffine affine) {
		GdPointF[] extent = new GdPointF[4];
		GdPointF min, max, point;
		int i;

		extent[0].x=0.0;
		extent[0].y=0.0;
		extent[1].x=(double) src.width;
		extent[1].y=0.0;
		extent[2].x=(double) src.width;
		extent[2].y=(double) src.height;
		extent[3].x=0.0;
		extent[3].y=(double) src.height;

		for (i=0; i < 4; i++) {
			point=extent[i];
			extent[i] = affine.applyToPointF(point);
		}
		min=extent[0];
		max=extent[0];

		for (i=1; i < 4; i++) {
			if (min.x > extent[i].x)
				min.x=extent[i].x;
			if (min.y > extent[i].y)
				min.y=extent[i].y;
			if (max.x < extent[i].x)
				max.x=extent[i].x;
			if (max.y < extent[i].y)
				max.y=extent[i].y;
		}
		final GdRect bbox = new GdRect();
		bbox.x = (int) min.x;
		bbox.y = (int) min.y;
		bbox.width  = (int) floor(max.x - min.x) - 1;
		bbox.height = (int) floor(max.y - min.y);
		return bbox;
	}

	public int setInterpolationMethod(GdInterpolationMethod id) {
		switch (id) {
			case GD_DEFAULT:
				id = GdInterpolationMethod.GD_BILINEAR_FIXED;
		/* Optimized versions */
			case GD_BILINEAR_FIXED:
			case GD_BICUBIC_FIXED:
			case GD_NEAREST_NEIGHBOUR:
			case GD_WEIGHTED4:
				this.interpolation = null;
				break;

		/* generic versions*/
			case GD_BELL:
				this.interpolation = new GdFilterBell();
				break;
			case GD_BESSEL:
				this.interpolation = new GdFilterBessel();
				break;
			case GD_BICUBIC:
				this.interpolation = new GdFilterBicubic();
				break;
			case GD_BLACKMAN:
				this.interpolation = new GdFilterBlackman();
				break;
			case GD_BOX:
				this.interpolation = new GdFilterBox();
				break;
			case GD_BSPLINE:
				this.interpolation = new GdFilterBspline();
				break;
			case GD_CATMULLROM:
				this.interpolation = new GdFilterCatmullrom();
				break;
			case GD_GAUSSIAN:
				this.interpolation = new GdFilterGaussian();
				break;
			case GD_GENERALIZED_CUBIC:
				this.interpolation = new GdFilterGeneralizedCubic();
				break;
			case GD_HERMITE:
				this.interpolation = new GdFilterHermite();
				break;
			case GD_HAMMING:
				this.interpolation = new GdFilterHamming();
				break;
			case GD_HANNING:
				this.interpolation = new GdFilterHanning();
				break;
			case GD_MITCHELL:
				this.interpolation = new GdFilterMitchell();
				break;
			case GD_POWER:
				this.interpolation = new GdFilterPower();
				break;
			case GD_QUADRATIC:
				this.interpolation = new GdFilterQuadratic();
				break;
			case GD_SINC:
				this.interpolation = new GdFilterSinc();
				break;
			case GD_TRIANGLE:
				this.interpolation = new GdFilterTriangle();
				break;

			default:
				return 0;
		}
		this.interpolation_id = id;
		return 1;
	}


	/* Return the interpolation mode set in 'im'.  This is here so that
	 * the value can be read via a language or VM with an FFI but no
	 * (portable) way to extract the value from the struct. */
	public GdInterpolationMethod getInterpolationMethod() {
		return this.interpolation_id;
	}

	/* Convert a double to an unsigned char, rounding to the nearest
	 * integer and clamping the result between 0 and max.  The absolute
	 * value of clr must be less than the maximum value of an unsigned
	 * short. */
	private static int uchar_clamp(final double clr, final int max) {
		int result;

		//assert(abs(clr) <= SHRT_MAX);

	/* Casting a negative float to an unsigned short is undefined.
	 * However, casting a float to a signed truncates toward zero and
	 * casting a negative signed value to an unsigned of the same size
	 * results in a bit-identical value (assuming twos-complement
	 * arithmetic).	 This is what we want: all legal negative values
	 * for clr will be greater than 255. */

	/* Convert and clamp. */
		result = (short)(clr + 0.5);
		if (result > max) {
			result = (clr < 0) ? 0 : max;
		}/* if */

		return result;
	}/* uchar_clamp*/

/*
 * Rotate function Added on 2003/12
 * by Pierre-Alain Joye (pierre@php.net)
 **/
	private static final double ROTATE_DEG2RAD = PI/180;
	public void skewX(final GdImage dst, final int uRow, final int iOffset, final double dWeight, int clrBack, final boolean ignoretransparent) {
		int i, r, g, b, a, clrBackR, clrBackG, clrBackB, clrBackA;
		final int[][] f;

		int pxlOldLeft, pxlLeft=0, pxlSrc;

	/* Keep clrBack as color index if required */
		if (this.trueColor) {
			pxlOldLeft = clrBack;
			f = tpixels;
		} else {
			pxlOldLeft = clrBack;
			clrBackR = this.red[clrBack];
			clrBackG = this.green[clrBack];
			clrBackB = this.blue[clrBack];
			clrBackA = this.alpha[clrBack];
			clrBack =  GdUtils.trueColorMixAlpha(clrBackR, clrBackG, clrBackB, clrBackA);
			f = pixels;
		}

		for (i = 0; i < iOffset; i++) {
			dst.setPixel(i, uRow, clrBack);
		}

		if (i < dst.sx) {
			dst.setPixel(i, uRow, clrBack);
		}

		for (i = 0; i < this.sx; i++) {
			pxlSrc = f[uRow][i];

			r = (int)(this.red[pxlSrc] * dWeight);
			g = (int)(this.green[pxlSrc] * dWeight);
			b = (int)(this.blue[pxlSrc] * dWeight);
			a = (int)(this.alpha[pxlSrc] * dWeight);

			pxlLeft = this.colorAllocateAlpha(r, g, b, a);

			if (pxlLeft == -1) {
				pxlLeft = this.findColorClosestAlpha(r, g, b, a);
			}

			r = this.red[pxlSrc] - (this.red[pxlLeft] - this.red[pxlOldLeft]);
			g = this.green[pxlSrc] - (this.green[pxlLeft] - this.green[pxlOldLeft]);
			b = this.blue[pxlSrc] - (this.blue[pxlLeft] - this.blue[pxlOldLeft]);
			a = this.alpha[pxlSrc] - (this.alpha[pxlLeft] - this.alpha[pxlOldLeft]);

			if (r>255) {
				r = 255;
			}

			if (g>255) {
				g = 255;
			}

			if (b>255) {
				b = 255;
			}

			if (a>127) {
				a = 127;
			}

			if (ignoretransparent && pxlSrc == dst.transparent) {
				pxlSrc = dst.transparent;
			} else {
				pxlSrc = dst.colorAllocateAlpha(r, g, b, a);

				if (pxlSrc == -1) {
					pxlSrc = dst.findColorClosestAlpha(r, g, b, a);
				}
			}

			if ((i + iOffset >= 0) && (i + iOffset < dst.sx)) {
				dst.setPixel(i + iOffset, uRow, pxlSrc);
			}

			pxlOldLeft = pxlLeft;
		}

		i += iOffset;

		if (i < dst.sx) {
			dst.setPixel(i, uRow, pxlLeft);
		}

		dst.setPixel(iOffset, uRow, clrBack);

		i--;

		while (++i < dst.sx) {
			dst.setPixel(i, uRow, clrBack);
		}
	}

	public void skewY(GdImage dst, int uCol, int iOffset, double dWeight, final int clrBack, final boolean ignoretransparent) {
		int i, iYPos=0, r, g, b, a;
		final int[][] f;
		int pxlOldLeft, pxlLeft=0, pxlSrc;

		if (this.trueColor) {
			f = tpixels;
		} else {
			f = pixels;
		}

		for (i = 0; i<=iOffset; i++) {
			dst.setPixel(uCol, i, clrBack);
		}
		r = (int)((double)this.red[clrBack] * dWeight);
		g = (int)((double)this.green[clrBack] * dWeight);
		b = (int)((double)this.blue[clrBack] * dWeight);
		a = (int)((double)this.alpha[clrBack] * dWeight);

		pxlOldLeft = dst.colorAllocateAlpha(r, g, b, a);

		for (i = 0; i < this.sy; i++) {
			pxlSrc = f[i][uCol];
			iYPos = i + iOffset;

			r = (int)((double)this.red[pxlSrc] * dWeight);
			g = (int)((double)this.green[pxlSrc] * dWeight);
			b = (int)((double)this.blue[pxlSrc] * dWeight);
			a = (int)((double)this.alpha[pxlSrc] * dWeight);

			pxlLeft = this.colorAllocateAlpha(r, g, b, a);

			if (pxlLeft == -1) {
				pxlLeft = this.findColorClosestAlpha(r, g, b, a);
			}

			r = this.red[pxlSrc] - (this.red[pxlLeft] - this.red[pxlOldLeft]);
			g = this.green[pxlSrc] - (this.green[pxlLeft] - this.green[pxlOldLeft]);
			b = this.blue[pxlSrc] - (this.blue[pxlLeft] - this.blue[pxlOldLeft]);
			a = this.alpha[pxlSrc] - (this.alpha[pxlLeft] - this.alpha[pxlOldLeft]);

			if (r>255) {
				r = 255;
			}

			if (g>255) {
				g = 255;
			}

			if (b>255) {
				b = 255;
			}

			if (a>127) {
				a = 127;
			}

			if (ignoretransparent && pxlSrc == dst.transparent) {
				pxlSrc = dst.transparent;
			} else {
				pxlSrc = dst.colorAllocateAlpha(r, g, b, a);

				if (pxlSrc == -1) {
					pxlSrc = dst.findColorClosestAlpha(r, g, b, a);
				}
			}

			if ((iYPos >= 0) && (iYPos < dst.sy)) {
				dst.setPixel(uCol, iYPos, pxlSrc);
			}

			pxlOldLeft = pxlLeft;
		}

		i = iYPos;
		if (i < dst.sy) {
			dst.setPixel(uCol, i, pxlLeft);
		}

		i--;
		while (++i < dst.sy) {
			dst.setPixel(uCol, i, clrBack);
		}
	}

	/* Rotates an image by 90 degrees (counter clockwise) */
	public GdImage rotate90(final boolean ignoretransparent) {
		int uY, uX;
		int c,r,g,b,a;
		final int[][] f;

		if (this.trueColor) {
			f = tpixels;
		} else {
			f = pixels;
		}
		final GdImage dst = new GdImage(sy, sx, GdImageColorType.TRUE_COLOR);
		final GdEffect old_blendmode = dst.alphaBlendingFlag;
		dst.alphaBlendingFlag = GdEffect.REPLACE;

		dst.transparent = transparent;

		paletteCopy(dst, this);

		for (uY = 0; uY<sy; uY++) {
			for (uX = 0; uX<sx; uX++) {
				c = f[uY][uX];
				if (!trueColor) {
					r = this.red[c];
					g = this.green[c];
					b = this.blue[c];
					a = this.alpha[c];
					c = GdUtils.trueColorMixAlpha(r, g, b, a);
				}
				if (ignoretransparent && c == dst.transparent) {
					dst.setPixel(uY, (dst.sy - uX - 1), dst.transparent);
				} else {
					dst.setPixel(uY, (dst.sy - uX - 1), c);
				}
			}
		}
		dst.alphaBlendingFlag = old_blendmode;

		return dst;
	}

	/* Rotates an image by 180 degrees (counter clockwise) */
	GdImage rotate180(final boolean ignoretransparent) {
		int uY, uX;
		int c,r,g,b,a;
		GdImage dst;
		final int[][] f;

		if (this.trueColor) {
			f = tpixels;
		} else {
			f = pixels;
		}
		dst = new GdImage(this.sx, this.sy, GdImageColorType.TRUE_COLOR);

		if (dst != null) {
			final GdEffect old_blendmode = dst.alphaBlendingFlag;
			dst.alphaBlendingFlag = GdEffect.REPLACE;

			dst.transparent = this.transparent;

			paletteCopy(dst, this);

			for (uY = 0; uY<this.sy; uY++) {
				for (uX = 0; uX<this.sx; uX++) {
					c = f[uY][uX];
					if (!this.trueColor) {
						r = this.red[c];
						g = this.green[c];
						b = this.blue[c];
						a = this.alpha[c];
						c = GdUtils.trueColorMixAlpha(r, g, b, a);
					}

					if (ignoretransparent && c == dst.transparent) {
						dst.setPixel((dst.sx - uX - 1), (dst.sy - uY - 1), dst.transparent);
					} else {
						dst.setPixel((dst.sx - uX - 1), (dst.sy - uY - 1), c);
					}
				}
			}
			dst.alphaBlendingFlag = old_blendmode;
		}

		return dst;
	}

	/* Rotates an image by 270 degrees (counter clockwise) */
	GdImage rotate270(final boolean ignoretransparent) {
		int uY, uX;
		int c,r,g,b,a;
		GdImage dst;
		final int[][] f;

		if (this.trueColor) {
			f = tpixels;
		} else {
			f = pixels;
		}
		dst = new GdImage(this.sy, this.sx, GdImageColorType.TRUE_COLOR);

		if (dst != null) {
			final GdEffect old_blendmode = dst.alphaBlendingFlag;
			dst.alphaBlendingFlag = GdEffect.REPLACE;

			dst.transparent = this.transparent;

			paletteCopy(dst, this);

			for (uY = 0; uY<this.sy; uY++) {
				for (uX = 0; uX<this.sx; uX++) {
					c = f[uY][uX];
					if (!this.trueColor) {
						r = this.red[c];
						g = this.green[c];
						b = this.blue[c];
						a = this.alpha[c];
						c = GdUtils.trueColorMixAlpha(r, g, b, a);
					}

					if (ignoretransparent && c == dst.transparent) {
						dst.setPixel((dst.sx - uY - 1), uX, dst.transparent);
					} else {
						dst.setPixel((dst.sx - uY - 1), uX, c);
					}
				}
			}
			dst.alphaBlendingFlag = old_blendmode;
		}

		return dst;
	}

	public GdImage rotate45(final double dAngle, int clrBack, final boolean ignoretransparent) {
		GdImage dst1,dst2,dst3;
		double dRadAngle, dSinE, dTan, dShear;
		double dOffset;     /* Variable skew offset */
		int u, iShear, newx, newy;
		int clrBackR, clrBackG, clrBackB, clrBackA;

	/* See GEMS I for the algorithm details */
		dRadAngle = dAngle * ROTATE_DEG2RAD; /* Angle in radians */
		dSinE = sin (dRadAngle);
		dTan = tan(dRadAngle / 2.0);

		newx = (int)(this.sx + this.sy * abs(dTan));
		newy = this.sy;

	/* 1st shear */
		dst1 = new GdImage(newx, newy, GdImageColorType.TRUE_COLOR);
		/******* Perform 1st shear (horizontal) ******/
		if (dst1 == null) {
			return null;
		}

		dst1.alphaBlendingFlag = GdEffect.REPLACE;

		if (dAngle == 0.0) {
		/* Returns copy of src */
			imageCopy(dst1, this, 0, 0, 0, 0, this.sx, this.sy);
			return dst1;
		}

		paletteCopy(dst1, this);

		if (ignoretransparent) {
			if (this.trueColor) {
				dst1.transparent = this.transparent;
			} else {

				dst1.transparent = GdUtils.trueColorMixAlpha(this.red[transparent], this.blue[transparent], this.green[transparent], 127);
			}
		}

		for (u = 0; u < dst1.sy; u++) {
			if (dTan >= 0.0) {
				dShear = ((double)(u + 0.5)) * dTan;
			} else {
				dShear = ((double)(u - dst1.sy) + 0.5) * dTan;
			}

			iShear = (int)floor(dShear);
			skewX(dst1, u, iShear, (dShear - iShear), clrBack, ignoretransparent);
		}

	/*
	The 1st shear may use the original clrBack as color index
	Convert it once here
	*/
		if(!this.trueColor) {
			clrBackR = this.red[clrBack];
			clrBackG = this.green[clrBack];
			clrBackB = this.blue[clrBack];
			clrBackA = this.alpha[clrBack];
			clrBack =  GdUtils.trueColorMixAlpha(clrBackR, clrBackG, clrBackB, clrBackA);
		}
	/* 2nd shear */
		newx = dst1.sx;

		if (dSinE > 0.0) {
			dOffset = (this.sx-1) * dSinE;
		} else {
			dOffset = -dSinE *  (this.sx - newx);
		}

		newy = (int) ((double) this.sx * abs(dSinE) + (double) this.sy * cos (dRadAngle))+1;

		dst2 = new GdImage(newx, newy, GdImageColorType.TRUE_COLOR);

		dst2.alphaBlendingFlag = GdEffect.REPLACE;

		if (ignoretransparent) {
			dst2.transparent = dst1.transparent;
		}

		for (u = 0; u < dst2.sx; u++, dOffset -= dSinE) {
			iShear = (int)floor (dOffset);
			dst1.skewY(dst2, u, iShear, (dOffset - (double) iShear), clrBack, ignoretransparent);
		}

	/* 3rd shear */
		newx = (int) ((double)this.sy * abs(dSinE) + (double)this.sx * cos (dRadAngle)) + 1;
		newy = dst2.sy;

		dst3 = new GdImage(newx, newy, GdImageColorType.TRUE_COLOR);

		dst3.alphaBlendingFlag = GdEffect.REPLACE;

		if (ignoretransparent) {
			dst3.transparent = dst2.transparent;
		}

		if (dSinE >= 0.0) {
			dOffset = (double)(this.sx - 1) * dSinE * -dTan;
		} else {
			dOffset = dTan * ((double)(this.sx - 1) * -dSinE + (double)(1 - newy));
		}

		for (u = 0; u < dst3.sy; u++, dOffset += dTan) {
			iShear = (int)floor(dOffset);
			dst2.skewX(dst3, u, iShear, (dOffset - iShear), clrBack, ignoretransparent);
		}

		return dst3;
	}

	GdImage rotate(double dAngle, final int clrBack, final boolean ignoretransparent) {
		GdImage pMidImg;
		GdImage rotatedImg;

		if (!this.trueColor && (clrBack < 0 || clrBack>=this.colorsTotal)) {
			return null;
		}

		while (dAngle >= 360.0) {
			dAngle -= 360.0;
		}

		while (dAngle < 0) {
			dAngle += 360.0;
		}

		if (dAngle == 90.00) {
			return rotate90(ignoretransparent);
		}
		if (dAngle == 180.00) {
			return rotate180(ignoretransparent);
		}
		if(dAngle == 270.00) {
			return rotate270(ignoretransparent);
		}

		if ((dAngle > 45.0) && (dAngle <= 135.0)) {
			pMidImg = rotate90(ignoretransparent);
			dAngle -= 90.0;
		} else if ((dAngle > 135.0) && (dAngle <= 225.0)) {
			pMidImg = rotate180(ignoretransparent);
			dAngle -= 180.0;
		} else if ((dAngle > 225.0) && (dAngle <= 315.0)) {
			pMidImg = rotate270(ignoretransparent);
			dAngle -= 270.0;
		} else {
			return rotate45(dAngle, clrBack, ignoretransparent);
		}

		if (pMidImg == null) {
			return null;
		}

		rotatedImg = pMidImg.rotate45(dAngle, clrBack, ignoretransparent);

		return rotatedImg;
	}
/* End Rotate function */

}
